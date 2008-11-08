/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.ui.transfer;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.util.compare.TransferComparator;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

/**
 * A Tablemodel adapter which acts upon a transfermanager.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.5.2.1 $
 */
public class UploadsTableModel extends PFComponent implements TableModel,
        SortedTableModel {

    public static final int UPDATE_TIME = 1000;

    private static final int COLTYPE = 0;
    private static final int COLFILE = 1;
    private static final int COLPROGRESS = 2;
    private static final int COLSIZE = 3;
    private static final int COLFOLDER = 4;
    private static final int COLTO = 5;

    private Collection<TableModelListener> listeners;
    private List<Upload> uploads;
    private int fileInfoComparatorType = -1;
    private boolean sortAscending = true;
    private int sortColumn;
    private final TransferManagerModel model;

    /**
     * Constructs a new table model for uploads.
     * 
     * @param model
     *            the transfermanager model
     * @param enabledPeriodicalUpdates
     *            true if periodical updates should be fired.
     */
    public UploadsTableModel(TransferManagerModel model,
        boolean enabledPeriodicalUpdates)
    {
        super(model.getController());
        this.model = model;
        listeners = Collections
            .synchronizedCollection(new LinkedList<TableModelListener>());
        uploads = Collections.synchronizedList(new LinkedList<Upload>());
        // Add listener
        model.getTransferManager().addListener(
            new UploadTransferManagerListener());

        if (enabledPeriodicalUpdates) {
            MyTimerTask task = new MyTimerTask();
            getController().scheduleAndRepeat(task, UPDATE_TIME);
        }
    }

    /**
     * Initalizes the model upon a transfer manager
     * 
     * @param tm
     */
    public void initialize() {
        TransferManager tm = model.getTransferManager();
        Upload[] uls = tm.getActiveUploads();
        uploads.addAll(Arrays.asList(uls));

        uls = tm.getQueuedUploads();
        uploads.addAll(Arrays.asList(uls));
    }

    // Public exposing ********************************************************

    /**
     * @param rowIndex
     * @return the upload at the specified upload row
     */
    public Upload getUploadAtRow(int rowIndex) {
        synchronized (uploads) {
            if (rowIndex >= uploads.size() || rowIndex == -1) {
                logSevere(
                    "Illegal rowIndex requested. rowIndex " + rowIndex
                        + ", uploads " + uploads.size());
                return null;
            }
            return uploads.get(rowIndex);
        }
    }

    // Application logic ******************************************************

    public void clearCompleted() {
        logWarning("Clearing completed uploads");

        List<Upload> ul2remove = new LinkedList<Upload>();
        synchronized (uploads) {
            for (Object upload1 : uploads) {
                Upload upload = (Upload) upload1;
                if (upload.isCompleted()) {
                    ul2remove.add(upload);
                }
            }
        }

        // Remove ul and Fire ui model change
        for (Object anUl2remove : ul2remove) {
            Upload upload = (Upload) anUl2remove;
            int index = removeUpload(upload);
            rowRemoved(index);
        }
    }


    public boolean sortBy(int modelColumnNo) {
        sortColumn = modelColumnNo;
        switch (modelColumnNo) {
            case COLTYPE :
                return sortMe(TransferComparator.BY_EXT);
            case COLFILE :
                return sortMe(TransferComparator.BY_FILE_NAME);
            case COLPROGRESS :
                return sortMe(TransferComparator.BY_PROGRESS);
            case COLSIZE :
                return sortMe(TransferComparator.BY_SIZE);
            case COLFOLDER :
                return sortMe(TransferComparator.BY_FOLDER);
            case COLTO :
                return sortMe(TransferComparator.BY_MEMBER);
        }

        sortColumn = -1;
        return false;
    }

    /**
     * Re-sorts the file list with the new comparator only if comparator differs
     * from old one
     *
     * @param newComparator
     * @return if the table was freshly sorted
     */
    public boolean sortMe(int newComparatorType) {
        int oldComparatorType = fileInfoComparatorType;

        fileInfoComparatorType = newComparatorType;
            if (oldComparatorType != newComparatorType) {
                boolean sorted = sort();
                if (sorted) {
                    fireModelChanged();
                    return true;
                }
            }
        return false;
    }

    private boolean sort() {
        if (fileInfoComparatorType != -1) {
            TransferComparator comparator = new TransferComparator(
                fileInfoComparatorType);

            if (sortAscending) {
                Collections.sort(uploads, comparator);
            } else {
                Collections.sort(uploads, new ReverseComparator(comparator));
            }
            return true;
        }
        return false;
    }

    private void fireModelChanged() {
        Runnable runner = new Runnable() {
            public void run() {
                TableModelEvent e = new TableModelEvent(
                    UploadsTableModel.this);
                for (Object aTableListener : listeners) {
                    TableModelListener listener =
                            (TableModelListener) aTableListener;
                    listener.tableChanged(e);
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    public void reverseList() {
        sortAscending = !sortAscending;
        List<Upload> tmpDisplayList =
                Collections.synchronizedList(new ArrayList<Upload>(
            uploads.size()));
        synchronized (uploads) {
            int size = uploads.size();
            for (int i = 0; i < size; i++) {
                tmpDisplayList.add(uploads.get(size - 1 - i));
            }
            uploads = tmpDisplayList;
        }
        fireModelChanged();
    }


    // Listener on TransferManager ********************************************

    /**
     * Listener on Transfer manager with new event system. TODO: Consolidate
     * removing uploads on abort/complete/broken
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class UploadTransferManagerListener extends TransferAdapter {

        public void uploadRequested(TransferManagerEvent event) {
            addOrUpdateUpload(event.getUpload());
        }

        public void uploadStarted(TransferManagerEvent event) {
            addOrUpdateUpload(event.getUpload());
        }

        public void uploadAborted(TransferManagerEvent event) {
            int index;
            synchronized (uploads) {
                index = removeUpload(event.getUpload());
            }
            if (index >= 0) {
                rowRemoved(index);
            }
        }

        public void uploadBroken(TransferManagerEvent event) {
            int index;
            synchronized (uploads) {
                index = removeUpload(event.getUpload());
            }
            if (index >= 0) {
                rowRemoved(index);
            }
        }

        public void uploadCompleted(TransferManagerEvent event) {
            int index = uploads.indexOf(event.getUpload());
            if (index >= 0) {
                rowsUpdated(index, index);
            } else {
                logSevere(
                    "Download not found in model: " + event.getDownload());
                rowsUpdatedAll();
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void completedUploadRemoved(TransferManagerEvent event) {
            int index;
            synchronized (uploads) {
                index = removeUpload(event.getUpload());
            }
            if (index >= 0) {
                rowRemoved(index);
            }
        }
    }

    // Model helper methods ***************************************************

    private void addOrUpdateUpload(Upload ul) {
        boolean added = false;
        int index;
        synchronized (uploads) {
            index = findCompletelyIdenticalUploadIndex(ul);
            Upload alreadyUl = index >= 0 ? uploads.get(index) : null;
            if (alreadyUl == null) {
                uploads.add(ul);
                added = true;
            } else {
                // Update if completely identical found
                uploads.set(index, ul);
            }
        }

        if (added) {
            rowAdded();
        } else {
            rowsUpdated(index, index);
        }
    }

    /**
     * Searches downloads for a download with identical FileInfo.
     *
     * @param downloadArg
     *            download to search for identical copy
     * @return index of the download with identical FileInfo, -1 if not
     *         found
     */
    private int findCompletelyIdenticalUploadIndex(Upload uploadArg) {
        synchronized (uploads) {
            for (int i = 0; i < uploads.size(); i++) {
                Upload ul = uploads.get(i);
                if (ul.getFile()
                    .isCompletelyIdentical(uploadArg.getFile())
                    && (uploadArg.getPartner() == null
                        || uploadArg.getPartner().equals(ul.getPartner())))
                {
                    return i;
                }
            }
        }

        // No match
        return -1;
    }


    /**
     * Removes one upload from the model an returns its previous index
     * 
     * @param upload
     * @return the index where this upload was removed from.
     */
    private int removeUpload(Upload upload) {
        int index;
        synchronized (uploads) {
            index = uploads.indexOf(upload);
            if (index >= 0) {
                logFiner("Remove upload from tablemodel: " + upload);
                uploads.remove(index);
            } else {
                logSevere(
                    "Unable to remove upload from tablemodel, not found: "
                        + upload);
            }
        }
        return index;
    }

    // Permanent updater ******************************************************

    /**
     * Continouosly updates the ui
     */
    private class MyTimerTask extends TimerTask {
        public void run() {
            Runnable wrapper = new Runnable() {
                public void run() {
                    if (fileInfoComparatorType == TransferComparator.BY_PROGRESS) {
                        // Always sort on a PROGRESS change, so that the table
                        // reorders correctly.
                        sort();
                    }
                    rowsUpdatedAll();
                }
            };
            try {
                SwingUtilities.invokeAndWait(wrapper);
            } catch (InterruptedException e) {
                logFiner("Interrupteed while updating downloadstable", e);

            } catch (InvocationTargetException e) {
                logSevere("Unable to update downloadstable", e);
            }
        }
    }

    // TableModel interface ***************************************************

    public int getColumnCount() {
        return 6;
    }

    public int getRowCount() {
        return uploads.size();
    }

    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case COLTYPE :
                return "";
            case COLFILE :
                return Translation.getTranslation("general.file");
            case COLPROGRESS :
                return Translation.getTranslation("transfers.progress");
            case COLSIZE :
                return Translation.getTranslation("general.size");
            case COLFOLDER :
                return Translation.getTranslation("general.folder");
            case COLTO :
                return Translation.getTranslation("transfers.to");
        }
        return null;
    }

    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case COLTYPE :
            case COLFILE :
                return FileInfo.class;
            case COLPROGRESS :
                return Upload.class;
            case COLSIZE :
                return Long.class;
            case COLFOLDER :
                return FolderInfo.class;
            case COLTO :
                return Member.class;
        }
        return null;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= uploads.size()) {
            logSevere(
                "Illegal rowIndex requested. rowIndex " + rowIndex
                    + ", uploads " + uploads.size());
            return null;
        }
        Upload upload = uploads.get(rowIndex);
        switch (columnIndex) {
            case COLTYPE :
            case COLFILE :
                return upload.getFile();
            case COLPROGRESS :
                return upload;
            case COLSIZE :
                return upload.getFile().getSize();
            case COLFOLDER :
                return upload.getFile().getFolderInfo();
            case COLTO :
                return upload.getPartner();
        }
        return null;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new IllegalStateException(
            "Unable to set value in UploadTableModel, not editable");
    }

    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    // Helper method **********************************************************

    /**
     * Tells listeners, that a new row at the end of the table has been added
     */
    private void rowAdded() {
        TableModelEvent e = new TableModelEvent(this, getRowCount() - 1,
            getRowCount() - 1, TableModelEvent.ALL_COLUMNS,
            TableModelEvent.INSERT);
        modelChanged(e);
    }

    private synchronized void rowRemoved(int row) {
        TableModelEvent e = new TableModelEvent(this, row, row,
            TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE);
        modelChanged(e);
    }

    private void rowsUpdated(int start, int end) {
        TableModelEvent e = new TableModelEvent(this, start, end,
            TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
        modelChanged(e);
    }

    /**
     * fire change on whole model
     */
    private void rowsUpdatedAll() {
        rowsUpdated(0, uploads.size());
    }

    /**
     * Fires an modelevent to all listeners, that model has changed
     */
    private void modelChanged(final TableModelEvent e) {
        // logFiner("Upload tablemodel changed");
        Runnable runner = new Runnable() {
            public void run() {
                synchronized (listeners) {
                    for (Object listener1 : listeners) {
                        TableModelListener listener = (TableModelListener) listener1;
                        listener.tableChanged(e);
                    }
                }
            }
        };

        UIUtil.invokeLaterInEDT(runner);
    }

    public int getSortColumn() {
        return sortColumn;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

}