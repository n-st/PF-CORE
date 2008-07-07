/*
* Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.transfer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.RequestPart;
import de.dal33t.powerfolder.transfer.Transfer.TransferState;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;
import de.dal33t.powerfolder.util.delta.FilePartsState.PartState;

/**
 * This download manager will try to download from all available sources.
 * 
 * @author Dennis "Bytekeeper" Waldherr
 */
public class MultiSourceDownloadManager extends AbstractDownloadManager {
    private final ConcurrentMap<MemberInfo, Download> downloads = new ConcurrentHashMap<MemberInfo, Download>();

    private Download pendingPartRecordFrom;

    private boolean usingPartRequests;

    public static final DownloadManagerFactory factory = new DownloadManagerFactory() {
        public DownloadManager createDownloadManager(Controller controller,
            FileInfo file, boolean automatic) throws IOException
        {
            return new MultiSourceDownloadManager(controller, file, automatic);
        }
    };
    
    public MultiSourceDownloadManager(Controller controller, FileInfo file,
        boolean automatic) throws IOException
    {
        super(controller, file, automatic);
    }

    @Override
    protected void addSourceImpl(Download download) {
        assert download != null;
        assert allowsSourceFor(download.getPartner());
        
        // log().debug("Adding source: " + download);

        if (downloads.put(download.getPartner().getInfo(), download) != null) {
            log().error(
                "Overridden previous download for member: "
                    + download.getPartner() + ". " + download);
        }

        // Non-automatic overrides automatic
        if (isRequestedAutomatic() != download.isRequestedAutomatic()) {
            setAutomatic(false);
        }

        if (isUsingPartRequests()
            || Util.usePartRequests(getController(), download.getPartner()))
        {
            usingPartRequests = true;
        }
    }

    public boolean allowsSourceFor(Member member) {
        Reject.ifNull(member, "Member is null");
        return downloads.isEmpty() || (
            isUsingPartRequests()
            && Util.useSwarming(getController(), member));
    }

    public Download getSourceFor(Member member) {
        Reject.ifNull(member, "Member is null");
        return downloads.get(member.getInfo());
    }

    /*
     * Returns the sources of this manager.
     * 
     * @see de.dal33t.powerfolder.transfer.MultiSourceDownload#getSources()
     */
    public Collection<Download> getSources() {
        return new ArrayList<Download>(downloads.values());
    }

    public boolean hasSources() {
        return !downloads.isEmpty();
    }

    @Override
    protected void removeSourceImpl(Download download) {
        assert download != null;
        
        if (downloads.remove(download.getPartner().getInfo()) == null) {
            throw new AssertionError("Removed non-managed download:" + download + " " + download.getPartner().getInfo());
        }
        if (isUsingPartRequests()) {
            // All pending requests from that download are void.
            if (filePartsState != null) {
                for (RequestPart req : download.getPendingRequests()) {
                    filePartsState.setPartState(req.getRange(),
                        PartState.NEEDED);
                }
            }
        }
    }

    @Override
    public String toString() {
        String string = super.toString() + "; sources=" + downloads.values() + "; pending requested bytes: ";
        if (filePartsState != null) {
            string += filePartsState.countPartStates(filePartsState.getRange(), PartState.PENDING)
                + "; available: "
                + filePartsState.countPartStates(filePartsState.getRange(), PartState.AVAILABLE)
                + "; needed: "
                + filePartsState.countPartStates(filePartsState.getRange(), PartState.NEEDED);
        }
        return string;
    }

    /**
     * Returns an available source for requesting the {@link FilePartsRecord}
     * 
     * @param download
     * @return
     */
    protected Download findPartRecordSource(Download download) {
        assert download != null;
        
        for (Download d : downloads.values()) {
            if (d.isStarted() && !d.isBroken()
                && Util.useDeltaSync(getController(), d.getPartner())) {
                download = d;
                break;
            }
        }
        return download;
    }

    protected void requestFilePartsRecord(Download download) {
        assert download == null || Util.useDeltaSync(getController(), download.getPartner());
        assert isUsingPartRequests();
        
        if (pendingPartRecordFrom != null) {
            // log().debug("Pending FPR from: " + pendingPartRecordFrom);

            // Check if we really need to do this first
            if (!pendingPartRecordFrom.isBroken()) {
                return;
            }
            log().error(
                "Source should have been removed: " + pendingPartRecordFrom);
            pendingPartRecordFrom = null;
        }
        if (download == null) {
            download = findPartRecordSource(null);
        }

        // log().debug("Selected FPR source: " + download);

        if (download != null) {
            assert Util.useDeltaSync(getController(), download.getPartner());
            
            log().debug("Requesting Filepartsrecord from " + download);
            setTransferState(TransferState.FILERECORD_REQUEST);
            pendingPartRecordFrom = download;
            pendingPartRecordFrom.requestFilePartsRecord();

            setStarted();
        }
    }

    protected void sendPartRequests() throws BrokenDownloadException {
        // log().debug("Sending part requests: " +
        // filePartsState.countPartStates(filePartsState.getRange(),
        // PartState.NEEDED));

        setTransferState(TransferState.DOWNLOADING);

        Range range;
        while (true) {
            range = filePartsState.findFirstPart(PartState.NEEDED);
            if (range == null) {
                // File completed, or only pending requests left
                break;
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
        assert filePartsState.isCompleted() 
            || filePartsState.countPartStates(filePartsState.getRange(), PartState.PENDING) > 0
            || isNoSourcesAreReady()
            : "AVAIL: " + filePartsState.countPartStates(filePartsState.getRange(), PartState.AVAILABLE)
            + ", NEED : " + filePartsState.countPartStates(filePartsState.getRange(), PartState.NEEDED)
            + ", PEND : " + filePartsState.countPartStates(filePartsState.getRange(), PartState.PENDING);
    }
    
    /**
     * Checks if all sources are not available for requests.
     */
    private boolean isNoSourcesAreReady() {
        for (Download d: downloads.values()) {
            if (d.isStarted() && !d.isBroken()) {
                return false;
            }
        }
        return true;
    }

    private boolean findAndRequestDownloadFor(Range range) throws BrokenDownloadException {
        assert range != null;
        
        for (Download d : downloads.values()) {
            if (!d.isStarted() || d.isBroken()) {
                continue;
            }
            if (d.requestPart(range)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isUsingPartRequests() {
        return usingPartRequests;
    }
}
