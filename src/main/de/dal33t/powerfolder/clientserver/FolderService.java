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
 * $Id: FolderService.java 20771 2013-02-05 12:01:32Z krickl $
 */
package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderStatisticInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.util.ArchiveMode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Access/Control over folders of a server.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface FolderService {

    /**
     * Creates a new folder to be mirrored by the server. Default Sync
     *
     * @param foInfo
     * @param profile
     *            the transfer mode to use on the server or null if default mode
     *            of server should be used.
     * @see SyncProfile#getDefault(de.dal33t.powerfolder.Controller)
     */
    void createFolder(FolderInfo foInfo, SyncProfile profile);

    void createFolder(FolderInfo foInfo, SyncProfile profile,
        Path targetDir);

    void createFolder(FolderInfo foInfo, SyncProfile profile,
                      Path targetDir, boolean isEncryptedFolder);

    /**
     * Removes a folder from the account. Required owner permission if
     * deletedFiles is true.
     *
     * @param foInfo
     * @param deleteFiles
     *            true to delete all file contained in the folder. Requires
     *            ownership.
     * @deprecated legacy support. remove after major 4.0 distribution
     */
    void removeFolder(FolderInfo foInfo, boolean deleteFiles);


    /**
     * Removes a folder from the account. Required owner permission if
     * deletedFiles is true.
     *
     * @param foInfo
     * @param deleteFiles
     *            true to delete all file contained in the folder. Requires
     *            ownership.
     * @param removePermission
     *            if the permission to this folder should also be removed.
     */
    void removeFolder(FolderInfo foInfo, boolean deleteFiles,
        boolean removePermission);

    /**
     * #854 - PFS-486
     *
     * @param foInfo
     *            the folder to change the name for!
     * @param newName
     *            the new name of the folder.
     * @return {@code True} if the folder was moved, {@code false} otherwise.
     */
    boolean renameFolder(FolderInfo foInfo, String newName);

    /**
     * Invites a user to a folder. The invited user gains read/write
     * permissions.
     *
     * @param user
     *            the name of the user to be invited
     * @param invitation
     *            the folder to be invited to
     * @deprecated Use {@link SendInvitationEmail} instead
     */
    @Deprecated
    void inviteUser(Invitation invitation, String user);

    /**
     * @param request
     */
    void sendInvitationEmail(SendInvitationEmail request);
    
    /**
     * @param request
     * @param wait block until send process is finished
     * @deprecated use {@link #sendInvitation(Invitation, boolean)}
     */
    void sendInvitationEmail(SendInvitationEmail request, boolean wait);

    /**
     * @param invitation
     * @param wait block until send process is finished
     */
    void sendInvitation(Invitation invitation, boolean wait);

    /**
     * Changes the sync profile on the remote server for this folder.
     *
     * @param foInfo
     * @param profile
     */
    void setSyncProfile(FolderInfo foInfo, SyncProfile profile);

    /**
     * Changes the sync profile of all folders on this server (Except maintenance folder). Server-Admin
     *
     * @param profile
     */
    void setSyncProfile(SyncProfile profile);

    /**
     * TRAC #991
     * <p>
     * To get the default synchronized folder use
     * <code>Account.getDefaultSynchronizedFolder()</code>.
     *
     * @param foInfo
     *            the folder that should be used as default synchronized folder
     *            for the current account.
     */
    void setDefaultSynchronizedFolder(FolderInfo foInfo);

    /**
     * @param foInfos
     *            the list of folders to retrieve the hosted servers for.
     * @return the list of servers the folders are hosted on.
     */
    Collection<MemberInfo> getHostingServers(FolderInfo... foInfos);

    // Server archive calls ***************************************************

    /**
     * Retrieves a List of existing FileInfos for an archived file.
     *
     * @param fileInfo
     *            fileInfo of the file to get archived versions for.
     * @return list of archived {@link FileInfo}.
     */
    List<FileInfo> getArchivedFilesInfos(FileInfo fileInfo);

    /**
     * Restores/Copies a file version from the archive to a new File within the
     * folder. Does NOT deleted the file in the archive. Does scan the related
     * folder and returns the new FileInfo of the restored file.
     *
     * @param versionInfo
     *            the FileInfo of the archived file.
     * @param sameLocation
     *            if the file should be restored under the same location and
     *            name. otherwise server stores file under a different name.
     *            Check returned FileInfo at any case.
     * @return the fileInfo of the restored file. Can be used for automatic
     *         downloading this file from the server after restoring.
     * @throws IOException
     *             problem restoring the file.
     */
    @Deprecated
    FileInfo restore(FileInfo versionInfo, boolean sameLocation)
        throws IOException;

    /**
     * Restores/Copies a file version from the archive to a new File within the
     * folder. Does NOT deleted the file in the archive. Does scan the related
     * folder and returns the new FileInfo of the restored file.
     *
     * @param versionInfo
     *            the FileInfo of the archived file.
     * @param newRelativeName
     *            the new relative name. Leave null for same location
     * @return the fileInfo of the restored file. Can be used for automatic
     *         downloading this file from the server after restoring.
     * @throws IOException
     *             problem restoring the file.
     */
    FileInfo restore(FileInfo versionInfo, String newRelativeName)
        throws IOException;

    /**
     * Controls the archive configuration on the server.
     *
     * @param foInfo
     * @param versionsPerFile
     */
    void setArchiveMode(FolderInfo foInfo, int versionsPerFile);

    /**
     * Controls the archive configuration on the server.
     *
     * @param foInfo
     * @param mode
     * @param versionsPerFile
     */
    @Deprecated
    void setArchiveMode(FolderInfo foInfo, ArchiveMode mode, int versionsPerFile);

    @Deprecated
    ArchiveMode getArchiveMode(FolderInfo foInfo);

    /**
     * To empty/purge the online stored archive.
     *
     * @param foInfo
     * @return if succeeded
     */
    boolean purgeArchive(FolderInfo foInfo);

    int getVersionsPerFile(FolderInfo foInfo);

    // Information ************************************************************

    /**
     * @param foInfo
     * @return true if this folder is joined by the remote side.
     */
    boolean hasJoined(FolderInfo foInfo);

    /**
     * The web DAV URL of a folder.
     *
     * @param foInfo
     * @return
     */
    String getWebDAVURL(FolderInfo foInfo);
    
    /**
     * PFC-2284
     * 
     * @param foInfo
     * @return the display name of the owner
     */
    String getOwnerDisplayname(FolderInfo foInfo);

    /**
     * Create a file link.
     *
     * @param fInfo
     * @return the URL to the file link
     */
    String getFileLink(FileInfo fInfo);

    /**
     * Remove a file link.
     * 
     * @param fInfo
     * @return
     */
    void removeFileLink(FileInfo fInfo);

    /**
     * Create a download link.
     *
     * @param fInfo
     */
    String getDownloadLink(FileInfo fInfo);

    /**
     * Bulk get of archive and local folders size.
     *
     * @param foInfos
     * @return [0] = the local size occupied by the given folders.
     *         <p>
     *         [1] = the archive size occupied by the given folders.
     */
    long[] calculateSizes(Collection<FolderInfo> foInfos);

    /**
     * Returns stats for all folders which are available in the cluster.
     *
     * @param foInfos
     * @return the {@link FolderStatisticInfo} for the given {@link FolderInfo}
     *         s.
     */
    Map<FolderInfo, FolderStatisticInfo> getCloudStatisticInfo(
        Collection<FolderInfo> foInfos);

    /**
     * Returns stats only for the locally synced folders.
     *
     * @param foInfos
     * @return the {@link FolderStatisticInfo} for the given {@link FolderInfo}
     *         s.
     */
    Map<FolderInfo, FolderStatisticInfo> getLocalStatisticInfo(
        Collection<FolderInfo> foInfos);

    /**
     * PFS-869: Creating a FileLink for unregistered user uploads.
     */
    String prepareFileLink(FolderInfo foInfo, String name, String mailAddress, char[] password, Date date);

    void startFileLinkUploadMailTask(String fileLinkID, String uploaderMailAddress, String uploaderUserName);

    /**
     * Checks if the storage path of a folder is correct (default)
     *
     * @param folderInfo The folder info
     * @param account    The account owning the folder
     * @return True if storage path is incorrect
     */
    boolean isStoragePathCorrect(FolderInfo folderInfo, Account account);

    /**
     * Corrects the storage path of a folder
     *
     * @param folderInfo The folder info
     * @param account    The account owning the folder
     */
    void correctStoragePath(FolderInfo folderInfo, Account account);

    /**
     * PFS-2343: Checks if a folder is encrypted.
     *
     * @param folderInfo
     * @return
     */
    boolean isEncrypted(FolderInfo folderInfo);

    /**
     * PFS-2343: Encrypt a single folder for the given account.
     *
     * @param folderInfo
     * @param account
     * @return true if folder was successfully encrypted.
     * @throws IOException
     */
    boolean encrypt(FolderInfo folderInfo, Account account);
}
