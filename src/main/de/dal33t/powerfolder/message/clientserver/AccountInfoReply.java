package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.protocol.AccountInfoProto;
import de.dal33t.powerfolder.protocol.AccountInfoReplyProto;
import de.dal33t.powerfolder.protocol.PermissionInfoProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.FolderPermission;

import java.util.Collection;

public class AccountInfoReply extends D2DReplyMessage {

    private Account account;
    private Collection<FolderPermission> invitations;
    private long avatarLastModifiedDate;
    private AccountInfo accountInfo;

    public AccountInfoReply() {
    }

    public AccountInfoReply(String replyCode, ReplyStatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public AccountInfoReply(String replyCode, ReplyStatusCode replyStatusCode, Account account, Collection<FolderPermission> invitations, long avatarLastModifiedDate) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.account = account;
        this.invitations = invitations;
        this.avatarLastModifiedDate = avatarLastModifiedDate;
    }

    public AccountInfoReply(String replyCode, ReplyStatusCode replyStatusCode, AccountInfo accountInfo, long avatarLastModifiedDate) {
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
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
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
        if (this.replyStatusCode != null) builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode) this.replyStatusCode.toD2D());
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
            // Inject avatarLastModifiedDate into AccountInfo
            accountInfoBuilder.setAvatarLastModifiedDate(this.avatarLastModifiedDate);
            // Add AccountInfo to message
            accountInfo = accountInfoBuilder.build();
            builder.setAccountInfo(accountInfo);
        } else if (this.accountInfo != null) builder.setAccountInfo((AccountInfoProto.AccountInfo) this.accountInfo.toD2D());
        return builder.build();
    }

}