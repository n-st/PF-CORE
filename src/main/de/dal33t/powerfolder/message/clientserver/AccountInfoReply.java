package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.d2d.D2DReplyFromServer;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.protocol.*;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.FolderPermission;

import java.util.Collection;
import java.util.Map;

public class AccountInfoReply extends D2DReplyMessage implements D2DReplyFromServer {

    private Account account;
    private Collection<FolderPermission> invitations;
    private Map<String, String> folderMapping;
    private long avatarLastModifiedDate;
    private long usedQuota;
    private long backupQuota;
    private long freeQuota;
    private int maxFolders;
    private String organizationName;
    private AccountInfo accountInfo;

    public AccountInfoReply() {
    }

    public AccountInfoReply(String replyCode, StatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public AccountInfoReply(String replyCode, StatusCode replyStatusCode, Account account, Collection<FolderPermission> invitations, Map<String, String> folderMapping, long avatarLastModifiedDate, long usedQuota, long backupQuota, long freeQuota, int maxFolders, String organizationName) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.account = account;
        this.invitations = invitations;
        this.folderMapping = folderMapping;
        this.avatarLastModifiedDate = avatarLastModifiedDate;
        this.usedQuota = usedQuota;
        this.backupQuota = backupQuota;
        this.freeQuota = freeQuota;
        this.maxFolders = maxFolders;
        this.organizationName = organizationName;
    }

    public AccountInfoReply(String replyCode, StatusCode replyStatusCode, AccountInfo accountInfo, long avatarLastModifiedDate) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.accountInfo = accountInfo;
        this.avatarLastModifiedDate = avatarLastModifiedDate;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public AccountInfoReply(AbstractMessage message) {
        initFromD2D(message);
    }

    public Account getAccount() {
        return account;
    }

    public Collection<FolderPermission> getInvitations() {
        return invitations;
    }

    public long getAvatarLastModifiedDate() {
        return avatarLastModifiedDate;
    }

    public long getUsedQuota() {
        return usedQuota;
    }

    public void setUsedQuota(long usedQuota) {
        this.usedQuota = usedQuota;
    }

    public long getBackupQuota() {
        return backupQuota;
    }

    public void setBackupQuota(long backupQuota) {
        this.backupQuota = backupQuota;
    }

    public long getFreeQuota() {
        return freeQuota;
    }

    public void setFreeQuota(long freeQuota) {
        this.freeQuota = freeQuota;
    }

    public AccountInfo getAccountInfo() {
        return accountInfo;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof AccountInfoReplyProto.AccountInfoReply) {
            AccountInfoReplyProto.AccountInfoReply proto = (AccountInfoReplyProto.AccountInfoReply) message;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = StatusCode.getEnum(proto.getReplyStatusCode());
            this.accountInfo = new AccountInfo(proto.getAccountInfo());
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        AccountInfoReplyProto.AccountInfoReply.Builder builder = AccountInfoReplyProto.AccountInfoReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        builder.setReplyStatusCode(this.replyStatusCode.getCode());
        if (this.account != null) {
            // Send Account as AccountInfo
            // Create AccountInfo message from Account
            AccountInfoProto.AccountInfo accountInfo = (AccountInfoProto.AccountInfo) this.account.toD2D();
            AccountInfoProto.AccountInfo.Builder accountInfoBuilder = accountInfo.toBuilder();
            // Inject invitations into AccountInfo
            for (FolderPermission folderPermission : this.invitations) {
                PermissionInfoProto.PermissionInfo.Builder permissionInfoBuilder = ((PermissionInfoProto.PermissionInfo) folderPermission.toD2D()).toBuilder();
                permissionInfoBuilder.setIsInvitation(true);
                accountInfoBuilder.addPermissionInfos(permissionInfoBuilder.build());
            }
            if (this.account.getServer() != null && this.account.getServer().getNode() != null)
                accountInfoBuilder.setHostingServerInfo((ServerInfoProto.ServerInfo) this.account.getServer().toD2D());
            // Inject serverInfos into AccountInfo
            for (Map.Entry<ServerInfo, String> entry : account.getTokens().entrySet()) {
                ServerInfoProto.ServerInfo serverInfo = (ServerInfoProto.ServerInfo) entry.getKey().toD2D();
                ServerInfoProto.ServerInfo.Builder serverInfoBuilder = serverInfo.toBuilder();
                // Inject token into ServerInfo
                serverInfoBuilder.setToken(entry.getValue());
                accountInfoBuilder.addServerInfos(serverInfoBuilder.build());
            }
            // Inject folderMapping into AccountInfo
            if (this.folderMapping != null) accountInfoBuilder.putAllFolderMapping(this.folderMapping);
            // Inject avatarLastModifiedDate into AccountInfo
            accountInfoBuilder.setAvatarLastModifiedDate(this.avatarLastModifiedDate);
            // Inject quotas
            accountInfoBuilder.setUsedQuota(this.usedQuota);
            accountInfoBuilder.setBackupQuota(this.backupQuota);
            accountInfoBuilder.setFreeQuota(this.freeQuota);
            accountInfoBuilder.setMaxFolders(this.maxFolders);
            // Inject organization name into AccountInfo
            if (this.organizationName != null) accountInfoBuilder.setOrganizationId(this.organizationName);
            // Add AccountInfo to message
            accountInfo = accountInfoBuilder.build();
            builder.setAccountInfo(accountInfo);
        } else if (this.accountInfo != null) builder.setAccountInfo((AccountInfoProto.AccountInfo) this.accountInfo.toD2D());
        return builder.build();
    }

}
