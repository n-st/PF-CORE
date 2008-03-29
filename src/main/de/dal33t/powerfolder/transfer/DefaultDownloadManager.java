package de.dal33t.powerfolder.transfer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang.Validate;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.RequestPart;
import de.dal33t.powerfolder.transfer.Transfer.State;
import de.dal33t.powerfolder.transfer.Transfer.TransferState;
import de.dal33t.powerfolder.util.FileCheckWorker;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;
import de.dal33t.powerfolder.util.delta.FilePartsState;
import de.dal33t.powerfolder.util.delta.MatchCopyWorker;
import de.dal33t.powerfolder.util.delta.MatchInfo;
import de.dal33t.powerfolder.util.delta.MatchResultWorker;
import de.dal33t.powerfolder.util.delta.FilePartsState.PartState;

public class DefaultDownloadManager extends PFComponent implements
    MultiSourceDownload
{
    private final FileInfo fileInfo;
    private final ConcurrentMap<Member, Download> downloads = new ConcurrentHashMap<Member, Download>();

    private FilePartsState filePartsState;
    private FilePartsRecord remotePartRecord;
    private Download pendingPartRecordFrom;

    private RandomAccessFile tempFile;

    private State transferState = new State();
    private boolean usingPartRequests;
    protected boolean completed;
    private boolean broken;
    private boolean started;
    private TransferCounter counter;

    public DefaultDownloadManager(Controller controller, FileInfo file)
        throws IOException
    {
        super(controller);
        Validate.notNull(file);
        this.fileInfo = file;

        // Create temp-file directory structure if necessary
        if (!getTempFile().getParentFile().exists()) {
            if (!getTempFile().getParentFile().mkdirs()) {
                throw new FileNotFoundException(
                    "Couldn't create parent directory!");
            }
        }

        tempFile = new RandomAccessFile(getTempFile(), "rw");
        // TODO: I'm deleting any previous progress here since
        //      we don't store the parts state anywhere. Therefore no assumption can be made
        //      on what has already been downloaded and what is garbage.
        //      Note that the old code didn't do this, even when using delta sync. That was
        //      actually wrong, but didn't show up since the parts where received "in order".
        //      But with swarming there might be holes!
        tempFile.setLength(0);

        if (!isNeedingFilePartsRecord()) {
            log().verbose(
                "Won't send FPR request: Minimum requirements not fulfilled!");
            filePartsState = new FilePartsState(file.getSize());
            filePartsState.setPartState(Range.getRangeByLength(0,
                filePartsState.getFileLength()), PartState.NEEDED);
        }
    }

    public synchronized void addSource(Download download) {
        Validate.notNull(download);
        Validate.isTrue(downloads.isEmpty()
            || (download.getPartner().isSupportingPartRequests())
            && usingPartRequests);
//        log().debug("Adding source: " + download);
        
        if (downloads.put(download.getPartner(), download) != null) {
            log().error(
                "Overridden previous download for member: "
                    + download.getPartner());
        }

        // Don't really request data for empty files
        if (fileInfo.getSize() == 0) {
            log().debug("Empty file detected, setting transfers completed immediately.");
            setCompleted();
        }
        
        if (completed) {
            return;
        }
        
        if (usingPartRequests 
            || Util.usePartRequests(getController(), download))
        {
            usingPartRequests = true;
        }
        
        Range r = filePartsState == null ? null : filePartsState.findFirstPart(PartState.NEEDED);
        if (r != null) {
            download.request(r.getStart());
        } else {
            download.request(0);
        }
        
        sendRequests();
        
        log().debug("Now having " + downloads.values().size() + " sources!");
    }

    public synchronized void receivedChunk(Download download, FileChunk chunk)
        throws IOException
    {
//        log().debug("Received " + chunk + " from " + download);
        if (completed || broken) {
            return;
        }
        Reject.noNullElements(download, chunk);
        
        if (filePartsState == null) {
            log().warn("Not ready to receive data, but received " + chunk + " from " + download);
            return;
        }
        
        setStarted();

        tempFile.seek(chunk.offset);
        tempFile.write(chunk.data);

        getCounter().chunkTransferred(chunk);
        
        Range range = Range.getRangeByLength(chunk.offset, chunk.data.length);
        filePartsState.setPartState(range, PartState.AVAILABLE);

        long avs = filePartsState.countPartStates(filePartsState.getRange(),
            PartState.AVAILABLE);
        setTransferState(TransferState.DOWNLOADING, (double) avs / fileInfo.getSize());

        // add bytes to transferred status
        FolderStatistic stat = fileInfo.getFolder(
            getController().getFolderRepository()).getStatistic();
        if (stat != null) {
            stat.getDownloadCounter().chunkTransferred(chunk);
        }

//        log().debug("Remaining: " + (fileInfo.getSize() - avs) + " " + filePartsState.isCompleted());
        
        // Maybe we're done already ?
        if (filePartsState.isCompleted()) {
            log().debug("Download completed, awaiting verification.");
            checkCompleted();
            return;
        }

        // Finally request more if needed.
        sendRequests();
    }

    public boolean isUsingPartRequests() {
        return usingPartRequests;
    }

    public synchronized void receivedFilePartsRecord(Download download,
        final FilePartsRecord record)
    {
        if (completed) {
            return;
        }
        Reject.noNullElements(download, record);
        log().debug("Received FilePartsRecord.");
        if (remotePartRecord != null) {
            log().warn("Received unrequested FilePartsRecord from " + download);
            if (!remotePartRecord.equals(record)) {
                log().error("The new and the old record differ!!");
            }
        }
        remotePartRecord = record;
        getController().getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    File src = getFile();

                    setTransferState(TransferState.MATCHING);
                    Callable<List<MatchInfo>> mInfoWorker = new MatchResultWorker(
                        record, src) {
                        
                        @Override
                        protected void setProgress(int percent) {
                            setTransferState(percent / 100.0);
                        }
                    };
                    List<MatchInfo> mInfoRes;
                    mInfoRes = mInfoWorker.call();

//                    log().debug("Records: " + record.getInfos().length);
//                    log().debug("Matches: " + mInfoRes.size() + " which are "  
//                        + (record.getPartLength() * mInfoRes.size()) + " bytes (bit less maybe).");
                    
                    transferState.setState(TransferState.COPYING);
                    setTransferState(TransferState.COPYING);
                    Callable<FilePartsState> pStateWorker = new MatchCopyWorker(
                        src, getTempFile(), record, mInfoRes) {
                        @Override
                        protected void setProgress(int percent) {
                            setTransferState(percent / 100.0);
                        }  
                    };
                    FilePartsState state = pStateWorker.call();
                    synchronized (DefaultDownloadManager.this) {
                        filePartsState = state;
                        counter = new TransferCounter(filePartsState
                            .countPartStates(filePartsState.getRange()
                                , PartState.AVAILABLE), fileInfo.getSize());

                        if (filePartsState.isCompleted()) {
                            log().debug(
                                "Download completed (no change detected): "
                                    + this);
                            checkCompleted();
                        } else {
                            sendRequests();
                        }
                    }
                } catch (NoSuchAlgorithmException e) {
                    log().error("SHA Digest not found. Fatal error", e);
                    throw new RuntimeException(
                        "SHA Digest not found. Fatal error", e);
                } catch (FileNotFoundException e) {
                    log().error(e);
                    setBroken(TransferProblem.FILE_NOT_FOUND_EXCEPTION, e
                        .getMessage());
                } catch (IOException e) {
                    log().error(e);
                    setBroken(TransferProblem.IO_EXCEPTION, e.getMessage());
                } catch (Exception e) {
                    log().error(e);
                    setBroken(TransferProblem.GENERAL_EXCEPTION, e.getMessage());
                }
            }
        });
    }

    protected synchronized void sendRequests() {
        if (completed) {
            return;
        }
        // If we aren't allowed to send requests, just don't do it
        if (!isUsingPartRequests()) {
            return;
        }
        // We also won't request while waiting for part state initialization 
        if (filePartsState == null) {
            return;
        }
        
        setTransferState(TransferState.DOWNLOADING);
//        log().debug("Sending requests!");
        
        Range range;
        while (true) {
            range = filePartsState.findFirstPart(PartState.NEEDED);
            if (range == null) {
                // File completed, or only pending requests left
                return;
            } 
            range = Range.getRangeByLength(range.getStart(), Math.min(
                TransferManager.MAX_CHUNK_SIZE, range.getLength()));
            // Split requests across sources
            if (findAndRequestDownloadFor(range)) {
                filePartsState.setPartState(range, PartState.PENDING);
            } else {
                break;
            }
        }
        
        setStarted();
    }
    
    private boolean findAndRequestDownloadFor(Range range) {
        for (Download d: downloads.values()) {
            if (!d.isStarted() || d.isBroken()) {
                continue;
            }
            if (d.requestPart(range)) {
                return true;
            }
        }
        return false;
    }

    private void setTransferState(TransferState state) {
        if (transferState.getState() == state) {
            return;
        }
        transferState.setState(state);
        transferState.setProgress(0);
        for (Download d: downloads.values()) {
            d.transferState.setState(state);
            d.transferState.setProgress(0);
        }
    }

    private void setTransferState(TransferState state, double progress) {
        transferState.setState(state);
        transferState.setProgress(progress);
        for (Download d: downloads.values()) {
            d.transferState.setState(state);
            d.transferState.setProgress(progress);
        }
    }

    private void setTransferState(double progress) {
        transferState.setProgress(progress);
        for (Download d: downloads.values()) {
            d.transferState.setProgress(progress);
        }
    }

    protected void checkCompleted() {
        if (completed) {
            return;
        }
        setTransferState(TransferState.VERIFYING);
        log().debug("Verifying file hash.");
        getController().getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    // If we don't have a record, the file is assumed to be "valid"
                    boolean fileValid = true;
                    if (remotePartRecord != null) {
                        Callable<Boolean> fileChecker = new FileCheckWorker(
                            getTempFile(), MessageDigest.getInstance("MD5"),
                            remotePartRecord.getFileDigest()) {
                            @Override
                            protected void setProgress(int percent) {
                                setTransferState(percent / 100.0);
                            }                            
                        };
                        fileValid = fileChecker.call();
                    } 
                    if (fileValid) {
                        setCompleted();
                        
                    } else {
                        filePartsState.setPartState(Range.getRangeByLength(0,
                            filePartsState.getFileLength()), PartState.NEEDED);
                        sendRequests();
                        counter = new TransferCounter(0, fileInfo.getSize());
                    }
                } catch (NoSuchAlgorithmException e) {
                    // If this error occurs, no downloads will ever succeed.
                    log().error(e);
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    log().error(e);
                    setBroken(TransferProblem.GENERAL_EXCEPTION, e.getMessage());
                } finally {
                    log().debug("DONE - Validating file hash.");
                }
            }
        });
    }

    protected synchronized void setCompleted() {
        completed = true;

        shutdown();
        
        getController().getTransferManager().setCompleted(this);

        for (Download d : downloads.values()) {
            getController().getTransferManager().setCompleted(d);
        }
    }
    
    private void setBroken(TransferProblem problem, String message) {
        completed = false;
        broken = true;
        for (Download d : downloads.values()) {
            getController().getTransferManager().setBroken(d, problem, message);
        }
        shutdown();
    }

    /**
     * Releases resources not required anymore 
     */
    public void shutdown() {
        filePartsState = null;
        // TODO: Actually the remote record shouldn't be dropped since if somebody wants to download the file from us
        //      we could just send it, instead of recalculating it!! (So it should be stored "somewhere" - like in the
        //      folders database or so)
        remotePartRecord = null;
        try {
            if (tempFile != null) {
                tempFile.close();
                tempFile = null;
            }
        } catch (IOException e) {
            log().error(e);
        }
        updateTempFile();
    }

    public synchronized void removeSource(Download download) {
        Validate.notNull(download);
        if (downloads.remove(download.getPartner()) == null) {
            log().error("Removed non-managed download:" + download);
        }
        if (isUsingPartRequests()) {
            // All pending requests from that download are void.
            if (filePartsState != null) {
                for (RequestPart req : download.getPendingRequests()) {
                    filePartsState.setPartState(req.getRange(), PartState.NEEDED);
                }
            }
            if (pendingPartRecordFrom == download) {
                pendingPartRecordFrom = null;
                requestFilePartsRecord(null);
            }
        }
        sendRequests();
    }

    /* Returns the sources of this manager.
     * @see de.dal33t.powerfolder.transfer.MultiSourceDownload#getSources()
     */
    public Collection<Download> getSources() {
        return new ArrayList<Download>(downloads.values());
    }

    public Download getSourceFor(Member member) {
        Validate.notNull(member);
        return downloads.get(member);
    }

    private boolean isNeedingFilePartsRecord() {
        return fileInfo.getSize() >= Constants.MIN_SIZE_FOR_PARTTRANSFERS
            && fileInfo.diskFileExists(getController());
    }

    private void requestFilePartsRecord(Download download) {
        if (!isNeedingFilePartsRecord() || completed) {
            return;
        }
        if (pendingPartRecordFrom != null) {
            // Check if we really need to do this first
            if (!pendingPartRecordFrom.isBroken()) {
                return;
            }
            log().error(
                "Source should have been removed: " + pendingPartRecordFrom);
            pendingPartRecordFrom = null;
        }
        if (download == null) {
            for (Download d : downloads.values()) {
                if (d.isStarted() && !d.isBroken() && d.usePartialTransfers()) {
                    download = d;
                    break;
                }
            }
        }
        
        if (download != null) {
            log().debug("Requesting Filepartsrecord from " + download);
            transferState.setState(TransferState.FILERECORD_REQUEST);
            pendingPartRecordFrom = download;
            pendingPartRecordFrom.requestFilePartsRecord();
        }
        
        setStarted();
    }

    private void updateTempFile() {
//        log().debug("Updating tempfile modification date to: " + fileInfo.getModifiedDate());
        if (!getTempFile().setLastModified(fileInfo.getModifiedDate().getTime())) {
            log().error("Failed to update modification date!");
        }
    }

    /**
     * @return the tempfile for this download
     */
    public File getTempFile() {
        File diskFile = fileInfo.getDiskFile(getController()
            .getFolderRepository());
        if (diskFile == null) {
            return null;
        }
        File tempFile = new File(diskFile.getParentFile(), "(incomplete) "
            + diskFile.getName());
        return tempFile;
    }

    private File getFile() {
        return fileInfo.getDiskFile(getController().getFolderRepository());
    }

    public boolean isCompleted() {
        return completed;
    }

    public void readyForRequests(Download download) {
        log().debug("Using partial transfers: " + download.usePartialTransfers());
        if (download.usePartialTransfers()) {
            requestFilePartsRecord(download);
        }
        // Recheck requesting
        sendRequests();
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public boolean hasSources() {
        return !downloads.isEmpty();
    }

    public void setBroken() {
        setBroken(TransferProblem.BROKEN_DOWNLOAD, "");
    }

    public void abortAndCleanup() {
        try {
            if (tempFile != null) {
                tempFile.close();
            }
        } catch (IOException e) {
            log().error(e);
        }
        if (!getTempFile().delete()) {
            log().error("Failed to delete temporary file:" + getTempFile());
        }
        abort();
    }

    protected void setStarted() {
        started = true;
    }
    
    public boolean isStarted() {
        return started;
    }

    public void abort() {
        completed = false;
        for (Download d : downloads.values()) {
            d.abort();
        }
        shutdown();
    }

    /**
     * Returns the transfer counter
     * 
     * @return
     */
    public synchronized TransferCounter getCounter() {
        if (counter == null) {
            counter = new TransferCounter(0, fileInfo.getSize());
        }
        return counter;
    }
    
    @Override
    public String toString() {
        return "[DefaultDownloadManager: #sources=" + downloads.values().size() + "]";
    }
}
