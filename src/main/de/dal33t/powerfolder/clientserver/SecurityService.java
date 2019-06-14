/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: FolderService.java 4655 2008-07-19 15:32:32Z bytekeeper $
 */
package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.security.Permission;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service for client authentication and permission checks.
 * <P>
 * TODO Traffic optimize
 *
 * @author sprajc
 */
public interface SecurityService {

    // Login stuff ************************************************************

    /**
     * Logs in from a remote location.
     *
     * @param username
     * @param password
     *            the password
     * @throws SecurityException
     *             if the log in failed, but credentials were right.
     * @return {@code True} if login succeeded, {@code false} if the credentials
     *         were wrong
     */
    boolean login(String username, char[] password);

    /**
     * Logs in from a remote location.
     *
     * @param username
     * @param credentials
     *            the credentials
     * @throws SecurityException
     *             if the log in failed, but credentials were right.
     * @return {@code True} if login succeeded, {@code false} if the credentials
     *         were wrong
     */
    boolean login(String username, byte[] credentials);

    /**
     * PFC-2548: Logs in by a token secret (device specific token).
     * 
     * @param tokenSecret
     * @return {@code True} if login succeeded, {@code false} if the token is
     *         invalid or token based authentication is disabled.
     */
    boolean login(String tokenSecret);

    /**
     * @return true if a user is logged in currently = has open session.
     */
    boolean isLoggedIn();

    /**
     * Logs out.
     */
    void logout();

    /**
     * Notifies nodes that their accounts have changed
     *
     * @param accounts The changed accounts
     */
    void notifyAccountStateChanged(Account... accounts);

    /**
     * PFS-862
     *
     * @return the valid OTP. Usable once within the next minute only.
     */
    String requestOTP();

    /**
     * PFC-2548
     * 
     * @return a generic device token to be used for further authentication. May
     *         have an expiration date.
     */
    String requestToken();

    /**
     * Request a number of tokens for an iOS device
     *
     * @param nodeIds The node IDs to request tokens for
     * @return The requested tokens
     */
    @Nullable Collection<String> requestTokensiOS(@Nullable Collection<String> nodeIds);

    /**
     * PFS-1685
     *
     * @return a generic token without any reference to a specific node
     */
    String requestWebDAVToken();

    // Nodes information retrieval ********************************************

    /**
     * @return Account details about the currently logged in user.
     */
    AccountDetails getAccountDetails();

    /**
     * Resulting map may not contain all nodes only those connected to the
     * server.
     *
     * @param nodes
     * @return the {@link AccountInfo} for the nodes.
     */
    Map<MemberInfo, AccountInfo> getAccountInfos(Collection<MemberInfo> nodes);

    /**
     * TRAC #1566
     *
     * @param pattern
     * @return the nodes
     */
    Collection<MemberInfo> searchNodes(String pattern);

    // Security / Permission stuff ********************************************

    /**
     * @param accountInfo
     * @param permission
     * @return true if the account with has that permission.
     */
    boolean hasPermission(AccountInfo accountInfo, Permission permission);

    /**
     * Bulk method to reduce RPC overhead. Supported by versions HIGHER than
     * "4.2.9".
     *
     * @param accountInfo
     * @param permissions
     * @return the list of results
     */
    List<Boolean> hasPermissions(AccountInfo accountInfo,
        List<Permission> permissions);

    /**
     * @param foInfo
     * @return the default permission for the given folder.
     */
    FolderPermission getDefaultPermission(FolderInfo foInfo);

    /**
     * Sets the default permission for the given folder.
     *
     * @param foInfo
     * @param permission
     */
    void setDefaultPermission(FolderInfo foInfo, FolderPermission permission);

    /**
     * @param foInfo
     * @return the Anonyoumus permission for the given folder.
     */
    FolderPermission getWebPermission(FolderInfo foInfo);

    /**
     * Sets the Anonyoumus permission for the given folder.
     *
     * @param foInfo
     * @param permission
     */
    void setWebPermission(FolderInfo foInfo, FolderPermission permission);

    /**
     * @param foInfo
     * @return the permissions on the folder.
     */
    Map<AccountInfo, FolderPermission> getFolderPermissions(FolderInfo foInfo);

    /**
     * @param foInfo
     * @return All permissions to an account and group on the folder.
     */
    Map<Serializable, FolderPermission> getAllFolderPermissions(FolderInfo foInfo);

    /**
     * Tries to obtain a permission on the given folder for the logged in
     * account.
     *
     * @param foInfo
     * @return the permission that was granted to the logged in account. null if
     *         not possible.
     */
    FolderPermission obtainFolderPermission(FolderInfo foInfo);

    /**
     * Changes a folder permission of a target account. Removes all existing
     * FolderPermissions of this account.
     *
     * @param aInfo
     *            the target account.
     * @param foInfo
     *            the folder
     * @param newPermission
     */
    void setFolderPermission(AccountInfo aInfo, FolderInfo foInfo,
        FolderPermission newPermission);

    /**
     * Returns all invitations for the logged in user
     *
     * @return the invitations
     */
    Collection<FolderPermission> getInvitations();

    /**
     * Returns all invitations to the folder
     *
     * @param folderInfo
     * @return the invitations on the folder.
     */
    Map<AccountInfo, FolderPermission> getInvitations(FolderInfo folderInfo);


    /**
     * Accept an invitation to a folder.
     *
     * @param invitation
     *          the invitation.
     */
    void acceptInvitation(Invitation invitation);

    /**
     * Decline an invitation to a folder.
     *
     * @param invitation
     *            the invitation.
     */
    void declineInvitation(Invitation invitation);

    /**
     * PF-102: In federation, get the hosting service of a given username.
     *
     * @param username The accounts username.
     * @return The ServerInfo the account is hosted on.
     */
    ServerInfo getHostingService(String username);
}
