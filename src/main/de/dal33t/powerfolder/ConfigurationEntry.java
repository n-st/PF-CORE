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
 * $Id: ConfigurationEntry.java 21134 2013-03-17 01:20:03Z sprajc $
 */
package de.dal33t.powerfolder;

import com.google.common.collect.Maps;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.RequestNodeInformation;
import de.dal33t.powerfolder.security.AccessMode;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Refelects a entry setting in the configuration file. Provides basic method
 * for accessing and setting the configuration.
 *
 * @author Christian Sprajc
 * @version $Revision: 1.5 $
 */
public enum ConfigurationEntry {

    // Basics *****************************************************************

    /**
     * If the GUI should be disabled (=console mode). Default: False (Show GUI).
     */
    DISABLE_GUI("disableui", false),

    /**
     * If the user interface is locked and needs to be unlocked before starting
     * PowerFolder.
     * <p>
     * TRAC #1784
     */
    USER_INTERFACE_LOCKED("uilock.locked", false),

    /**
     * If any running instance should be killed when starting PowerFolder.
     * <p>
     * TRAC #2028
     */
    KILL_RUNNING_INSTANCE("kill.running.instance", false),

    /**
     * #2425: Sync and exit after connect to server.
     */
    SYNC_AND_EXIT("sync.exit", false),

    /**
     * Don't prompt on update. Simply auto-upgrade silently
     */
    AUTO_UPDATE("auto.update", false),

    // PFC-2461: possibility to disable updates completely for MSI installation
    /**
     * Enables/disables all updates. If disabled, prevents background process
     * hides updates from the UI.
     */
    ENABLE_UPDATE("enable.update", true) {
        // Always return false if software was installed via MSI
        @Override
        public Boolean getValueBoolean(Controller controller) {
            if (WinUtils.isSupported() && WinUtils.isMSI()) {
                return Boolean.FALSE;
            }
            return super.getValueBoolean(controller);
        }

        @Override
        public String getDefaultValue() {
            // Hack for PFC-2461
            // Updates disabled by default if software was installed via MSI
            if (WinUtils.isSupported() && WinUtils.isMSI()) {
                return Boolean.FALSE.toString();
            }
            return super.getDefaultValue();
        }
    },

    /**
     * If some client options are available only with permissions such as create
     * folder or change client preferences.
     * <p>
     * TRAC #1979
     *
     * @deprecated since 14.0
     */
    @Deprecated
    SECURITY_PERMISSIONS_STRICT("security.permissions.strict", false),

    /**
     * Don't show the FolderAdminPermission
     */
    SECURITY_PERMISSIONS_SHOW_FOLDER_ADMIN(
            "security.permissions.show_folder_admin", true),

    /**
     * PFS-1040
     */
    FOLDER_OWNERSHIP_CHANGEABLE("folder.change_owner.allowed", true),

    /**
     * Required permission to access/use the archive of a folder.
     * <p>
     * PFS-1336
     * <p>
     * http://www.powerfolder.com/wiki/Security_Permissions
     */
    SECURITY_FOLDER_ARCHIVE_PERMISSION("security.folder.archive.permission",
            AccessMode.READ_WRITE.name(), true),

    /**
     * PFC-2670: Trust self-signed/any SSL certificate
     */
    SECURITY_SSL_TRUST_ANY("security.ssl.trust_any", false),

    // Node setup *************************************************************

    /**
     * The nickname to use.
     */
    NICK("nick") {
        @Override
        public String getDefaultValue() {
            String def = null;
            if (StringUtils.isNotBlank(System.getenv("COMPUTERNAME"))) {
                def = System.getenv("COMPUTERNAME").toLowerCase();
            }
            if (def == null) {
                try {
                    InetAddress addr = InetAddress.getLocalHost();
                    def = addr.getHostName();
                } catch (UnknownHostException e) {
                }
            }
            if (def == null) {
                def = System.getProperty("user.name");
            }
            return def;
        }
    },

    /**
     * The node id to use. Advanced entry, usually automatically generated and
     * stored in preferences.
     */
    NODE_ID("nodeid"),

    /**
     * The network ID (#1373). PowerFolder can separate logical peer-to-peer
     * networks. Nodes with different network IDs won't connect to each other.
     * They even don't have other nodes in its local peer-to-peer nodes
     * database.
     * <p>
     * The default network ID of the open PowerFolder network is X.
     */
    NETWORK_ID("networkid", "X", true),

    // Provider Settings ******************************************************

    /**
     * URL of the PowerFolder homepage
     */
    PROVIDER_URL("provider.url.main", "https://www.powerfolder.com"),

    /**
     * URL of the Online Storage features
     */
    PROVIDER_ABOUT_URL("provider.url.about", "https://www.powerfolder.com/about-us-2/"),

    /**
     * Quickstart guides to PowerFolder
     */
    PROVIDER_QUICKSTART_URL("provider.url.quickstart", "https://wiki.powerfolder.com/display/PFC/Getting+started"),

    /**
     * URL of the PowerFolder Support
     */
    PROVIDER_SUPPORT_URL("provider.url.support", "https://wiki.powerfolder.com"),

    /**
     * Whether to show support url in application menu
     */

    PROVIDER_SUPPORT_URL_SHOW_IN_MENU("provider.url.support.show_in_menu", false),

    /**
     * URL where bugs or tickets can be filed.
     */
    PROVIDER_SUPPORT_FILE_TICKET_URL("provider.url.ticket", "https://www.powerfolder.com/support/index.php?/Tickets/Submit"),

    /**
     * URL of the PowerFolder Pro page.
     * <p>
     * Recommended use: {@link ProUtil#getBuyNowURL(Controller)}
     */
    PROVIDER_BUY_URL("provider.url.buy", "https://powerfolder.com/buynow.html"),

    /**
     * URL where the contact form resides
     */
    PROVIDER_CONTACT_URL("provider.url.contact", "https://www.powerfolder.com/contact"),

    /**
     * URL of the PowerFolder Wiki. ATTENTION: This URL gets extended by article
     * URI from many help links
     */
    PROVIDER_WIKI_URL("provider.url.wiki", "https://wiki.powerfolder.com"),

    // Distribution infos *****************************************************

    DIST_BINARY_NAME("dist.binaryname", "PowerFolder"),

    DIST_NAME("dist.name", "PowerFolder"),

    DIST_DESCRIPTION("dist.description", "Sync your world"),

    DIST_FOLDERS_BASE_NAME("dist.folderbasename",
            Constants.FOLDERS_BASE_DIR_SUBDIR_NAME),

    DIST_EMAIL("dist.email", ""),

    DIST_COMPANY("dist.company", ""),

    DIST_URL("dist.url", ""),

    /**
     * PFS-1117: Optional name of class to use as distribution. Forces and overrides
     * loading via ServiceLoader /
     * de.dal33t.powerfolder.distribution.Distribution
     */
    DIST_CLASSNAME("dist.classname"),

    // Server settings ********************************************************

    /**
     * PFC-2580: No connection to powerfolder.com / Cloud Replication options
     */
    SERVER_CONNECT("server.connect.enabled", true, true),

    /**
     * The optional name of the sever to connect to.
     */
    SERVER_NAME("server.name", "PowerFolder Cloud"),

    /**
     * The optional url of the server.
     */
    SERVER_WEB_URL("server.url", "https://my.powerfolder.com") {
        @Override
        public String getValue(Controller controller) {
            String str = super.getValue(controller);
            if (str != null && str.toLowerCase().trim()
                    .startsWith("https://access.powerfolder.com/node/os")) {
                return getDefaultValue();
            }
            return str;
        }
    },

    /**
     * The node id of the server to connect to. Not mandatory but at
     * recommended. At leat host or ID of a server has to be set to connect to a
     * server.
     */
    SERVER_NODEID("server.nodeid", "WEBSERVICE03"),

    /**
     * The optional server hostname to connect to. Example:
     * server.powerfolder.com:1234
     */
    SERVER_HOST("server.host", "os003.powerfolder.com:1337"),

    /**
     * The D2D port of the server
     */
    SERVER_PORT_D2D("server.port.d2d", 0),

    /**
     * HTTP tunnel relay URL.
     */
    SERVER_HTTP_TUNNEL_RPC_URL("provider.url.httptunnel",
            "http://my.powerfolder.com/rpc"),

    /**
     * #1687: How this computer should behave when the server is not connected.
     *
     * @deprecated for testing use {@link Feature#P2P_REQUIRES_LOGIN_AT_SERVER}
     */
    SERVER_DISCONNECT_SYNC_ANYWAYS("server.disconnect.sync_anyways", false) {
        @Override
        public String getValue(Controller controller) {
            return Boolean.toString(Feature.P2P_REQUIRES_LOGIN_AT_SERVER
                    .isDisabled());
        }
    },

    /**
     * If the config should be update when connection to the server was
     * established
     */
    SERVER_CONFIG_UPDATE("server.config.update", true),

    /**
     * If to load server nodes from server URL
     */
    SERVER_LOAD_NODES("server.load.nodes", true),

    /**
     * Specify several server URLs with a name like
     * "Server 1=http://pf.example.org/;Server 2=https://www.example.com/pf/"
     * to be displayed on the log in dialog in a combo box.
     */
    SERVER_CONNECTION_URLS("server.connection.urls"),

    /**
     * Specify a URL to get a list of Shibboleth Identity Provider.
     */
    SERVER_IDP_DISCO_FEED_URL("server.idp.disco_feed.url", null, true),

    /**
     * The last Identity Provider URL that was selected.
     */
    SERVER_IDP_LAST_CONNECTED("server.idp.last_connected"),

    /**
     * The corresponding ECP binding for the last connected Identity Provider.
     */
    SERVER_IDP_LAST_CONNECTED_ECP("server.idp.last_connected.ecp"),

    /**
     * PFC-2838
     * PFS-2006: Comma-seperated list to add custom LDAP-Organizations to institution
     * dropdown on Shibboleth login page.
     */
    SERVER_IDP_EXTERNAL_NAMES("server.idp.external_names", "", true),

    /**
     * PFC-2534: Skip auto login for the specified number. Defaults to retrying
     * every 5 minutes.
     */
    SERVER_LOGIN_SKIP_RETRY("server.skip.auto.login", 1000),

    /**
     * Server federation support.
     */
    SERVER_FEDERATION_ENABLED("server.federation.enabled", false),

    /**
     * PFS-2817: Disable sharing.
     */
    SERVER_FEDERATED_SHARING("server.federation.sharing_enabled", true),

    /**
     * PFS-2425: Federated login.
     */
    SERVER_FEDERATED_LOGIN("server.federation.login_enabled", false),

    /**
     * PFS-3335: Merge accounts in federation
     */
    SERVER_FEDERATED_MERGE("server.federation.auto_merge_accounts", false),

    // Server WEB settings ****************************************************

    /**
     * #2448: Option to disable Web access
     *
     * @deprecated since 14.0
     */
    @Deprecated
    WEB_LOGIN_ALLOWED("web.login.allowed", true),

    /**
     * PFS-862: Change to FALSE after major distribution of v9.2 If the client
     * is allowed to pass the current password to the web browser.
     */
    WEB_PASSWORD_ALLOWED("web.login.password", false),

    /**
     * If WebDAV should be enabled.
     */
    WEB_DAV_ENABLED("web.dav.enabled", true),

    /**
     * Enable/Disable the Members Tab
     */
    MEMBERS_ENABLED("members.enabled", true),

    /**
     * Enable/Disable the Settings Tab
     */
    SETTINGS_ENABLED("settings.enabled", true),

    /**
     * Enable/Disable the Files Tab
     */
    FILES_ENABLED("files.enabled", true),

    ARCHIVE_DIRECTORY_NAME("files.archive.dir.name", "archive"),

    /**
     * Enable/Disable the Problems Tab
     */
    PROBLEMS_ENABLED("problems.enabled", true),

    MY_ACCOUNT_ENABLED("web.my_account.enabled", true),

    // Config META information ************************************************

    /**
     * Prompt for the server address if currently set server belongs to the
     * powerfolder.com cloud.
     */
    CONFIG_PROMPT_SERVER_IF_PF_COM("config.promptifpf", false),

    /**
     * The URL of the current config
     */
    CONFIG_URL("config.url"),

    /**
     * If this URL should override all previously set or default values.
     */
    CONFIG_OVERWRITE_VALUES("config.overwrite", false),

    /**
     * If all folder settings should be dropped when this config is loaded
     */
    @Deprecated
    CONFIG_DROP_FOLDER_SETTINGS("config.drop.folders.settings", false),

    /**
     * #2248 Automatically assign client to server by IP address
     */
    CONFIG_ASSIGN_IP_LIST("config.assign.iplist"),

    /**
     * PFS-1107 associate a config to an ldap group
     */
    CONFIG_LDAP_GROUP("config.ldap.group"),

    /**
     * PFS-1593 associate a config with a organizations
     */
    CONFIG_ORGANIZATION("config.organization"),

    /**
     * PFC-2184 Specify server URL as installer command line parameter
     */
    INSTALLER_FILENAME("installer.file"),

    // Update settings ********************************************************

    /**
     * http://checkversion.powerfolder.com/PowerFolder_LatestVersion.txt
     */
    UPDATE_VERSION_URL("update.version.url"),

    /**
     * http://checkversion.powerfolder.com/PowerFolder_DownloadLocation.txt
     */
    UPDATE_DOWNLOADLINK_INFO_URL("update.download_info.url"),

    /**
     * http://download.powerfolder.com/free/PowerFolder_Latest_Win32_Installer.
     * exe
     */
    UPDATE_WINDOWS_EXE_URL("update.windows_exe.url"),

    /**
     * PFC-2167: Installer launches PowerFolder under the account used for elevation.
     */
    UPDATE_SILENT_ALLOWED("update.silent.allowed", true),

    UPDATE_FORCE("update.force", false),

    // Server connection ******************************************************

    /**
     * #1715 If it should be possible to register at the server.
     */
    SERVER_REGISTER_ENABLED("server.register.enabled", false),

    /**
     * PFS-485 If it should be possible to send invite others.
     */
    SERVER_INVITE_ENABLED("server.invite.enabled", true),

    /**
     * PFS-871: The user has to agree to invitations, if enabled
     */
    FOLDER_AGREE_INVITATION_ENABLED("folder.agree.invitation.enabled", false),

    /**
     * PFS-798: If invitor can invite "external" non existing users (e.g. not in LDAP nor in DB).
     * Will create a new user account with server default settings for invitee.
     */
    SERVER_INVITE_NEW_USERS_ENABLED("server.invite.new_users.enabled", true),

    /**
     * If "Password recovery" should be enabled. If not the server automatically
     * sets this. (Should be yes if not LDAP).
     */
    SERVER_RECOVER_PASSWORD_ENABLED("server.recover.password.enabled", true),

    /**
     * #2401: If the login/usernames are Email addresses.
     */
    SERVER_USERNAME_IS_EMAIL("server.username.isemail", "both"),

    /**
     * PFS-3212: Validate invite input
     */
    SERVER_INVITE_VALIDATE_EMAIL_ENABLED("server.invite.validate_email.enabled", false),

    /**
     * Username for connection
     */
    SERVER_CONNECT_USERNAME("server.connect.username"),

    /**
     * Password for connection (obfuscated)
     */
    SERVER_CONNECT_PASSWORD("server.connect.passwordobf"),

    /**
     * PFC-2548: The token to use for authentication.
     */
    SERVER_CONNECT_TOKEN("server.connect.token"),

    /**
     * PFS-1685: Token for webdav authentication
     */
    SERVER_CONNECT_TOKEN_WEBDAV("server.connect.token.webdav"),

    /**
     * Password for connection (clear text)
     */
    SERVER_CONNECT_PASSWORD_CLEAR("server.connect.password"),

    /**
     * #2219: Allow clients to login with no password. Use system property
     * "user.name" as login username.
     */
    SERVER_CONNECT_NO_PASSWORD_ALLOWED("server.connect.nopassword.allowed",
            false),

    /**
     * #2518
     */
    SERVER_CONNECT_REMEMBER_PASSWORD_ALLOWED(
            "server.connect.rememberpassword.allowed", true),

    /**
     * #2229: Disallow change of login
     */
    SERVER_CONNECT_CHANGE_LOGIN_ALLOWED("server.connect.changelogin.allowed",
            true),

    /**
     * #2338: Always connect to server, even in LAN only mode
     */
    SERVER_CONNECT_FROM_LAN_TO_INTERNET("server.connect.lan2internet", true),

    // Kerberos settings ******************************************************

    /**
     * PFC-2446
     * <p>
     * Enable Single Sign-on via Kerberos
     */
    KERBEROS_SSO_ENABLED("kerberos.sso.enabled", false),

    /**
     * The Realm aka Domain
     */
    KERBEROS_SSO_REALM("kerberos.sso.realm", "WORKGROUP"),

    /**
     * The Key Distribution Center
     */
    KERBEROS_SSO_KDC("kerberos.sso.kdc"),

    /**
     * Kerberos service name, as registered at the Domain Controller
     */
    KERBEROS_SSO_SERVICE_NAME("kerberos.sso.service_name", "domain/hostname"), // + @realm

    // General settings *******************************************************

    /**
     * The networking mode. See class <code>NetworkingMode</code> for more
     * information.
     *
     * @see NetworkingMode
     */
    NETWORKING_MODE("networkingmode", NetworkingMode.PRIVATEMODE.name()),

    /**
     * The ip/address where powerfolder should bind to.
     */
    NET_BIND_ADDRESS("net.bindaddress"),

    /**
     * The port(s) to bind to.
     */
    NET_PORT("net.port", null, true) {
        @Override
        public String getValue(Controller controller) {
            String value = super.getValue(controller);
            if (value == null) {
                value = controller.getConfig().getProperty("port");
            }
            return value;
        }

        @Override
        public void removeValue(Controller controller) {
            super.removeValue(controller);
            controller.getConfig().remove("port");
        }

        @Override
        public void setValue(Controller controller, String value) {
            super.setValue(controller, value);
            controller.getConfig().remove("port");
        }

    },

    /**
     * The TCP port for D2D
     */
    NET_PORT_D2D("net.port.d2d", 0, true) {
        @Override
        public String getValue(Controller controller) {
            String value = super.getValue(controller);
            if (value == null || value.equals(super.defaultValue)) {
                String oldValue = controller.getConfig().getProperty("d2d.port");
                if (oldValue != null) {
                    value = oldValue;
                }
            }
            return value;
        }

        @Override
        public void removeValue(Controller controller) {
            super.removeValue(controller);
            controller.getConfig().remove("d2d.port");
        }

        @Override
        public void setValue(Controller controller, String value) {
            super.setValue(controller, value);
            controller.getConfig().remove("d2d.port");
        }

    },

    /**
     * The TCP port for the {@link RemoteCommandManager}
     */
    NET_PORT_RCON("net.port.rcon", 1338) {
        @Override
        public String getValue(Controller controller) {
            String value = super.getValue(controller);
            if (value == null) {
                // Old entry
                value = controller.getConfig().getProperty("net.rcon.port");
            }
            return value;
        }

        @Override
        public void removeValue(Controller controller) {
            super.removeValue(controller);
            controller.getConfig().remove("net.rcon.port");
        }

        @Override
        public void setValue(Controller controller, String value) {
            super.setValue(controller, value);
            controller.getConfig().remove("net.rcon.port");
        }

    },

    /**
     * If relayed or tunnel connections should be tried for LAN based computers.
     * Usually this does not make sense. Only for special scenarios. e.g.
     * ILY-570834
     */
    NET_USE_RELAY_TUNNEL_ON_LAN("net.relaytunnel.lan.enabled", false),

    /**
     * If the {@link RemoteCommandManager} should be started or not.
     */
    NET_RCON_MANAGER("net.rcon", true),

    /**
     * If broadcast on LAN
     */
    NET_BROADCAST("net.broadcast", true),

    /**
     * PFC-3001: Reduce network UDP broadcast from 10 to 60 seconds (configurable)
     */
    NET_BROADCAST_INTERVAL_SECONDS("net.broadcast.interval.seconds", 60),

    /**
     * Use a random port in the (49152) 0 to 65535 range, overides NET_PORT
     */
    NET_BIND_RANDOM_PORT("random-port", true),

    /**
     * The TCP/IP socket buffer size for TCP/UDT connections over Internet.
     */
    NET_SOCKET_INTERNET_BUFFER_SIZE("net.socket.internet.buffer.size",
            1024 * 1024),

    /**
     * The TCP/IP socket buffer size for TCP/UDT connections in LAN.
     */
    NET_SOCKET_LAN_BUFFER_SIZE("net.socket.lan.buffer.size", 4 * 1024 * 1024),

    /**
     * The TCP/IP socket buffer size limit for UDT connections over Internet.
     */
    NET_SOCKET_INTERNET_BUFFER_LIMIT("net.socket.internet.buffer.limit",
            8 * 1024 * 1024),

    /**
     * The TCP/IP socket buffer size limit for UDT connections in LAN.
     */
    NET_SOCKET_LAN_BUFFER_LIMIT("net.socket.lan.buffer.limit", 32 * 1024 * 1024),

    /**
     * Auto detect WAN speeds
     */
    TRANSFER_LIMIT_AUTODETECT("transfer.limit.autodetect", false),

    /**
     * The upload limit for WAN (Internet) connections in KB/s
     */
    UPLOAD_LIMIT_WAN("uploadlimit", 0),

    /**
     * The download limit for WAN (Internet) connections in KB/s
     */
    DOWNLOAD_LIMIT_WAN("downloadlimit", 0),

    /**
     * The upload limit for LAN connections in KB/s
     */
    UPLOAD_LIMIT_LAN("lanuploadlimit", 0),

    /**
     * The download limit for LAN connections in KB/s
     */
    DOWNLOAD_LIMIT_LAN("landownloadlimit", 0),

    /**
     * The maximum size (in bytes) of an {@link FileChunk} used for file
     * transfers
     */
    TRANSFERS_MAX_FILE_CHUNK_SIZE("transfers.max.file.chunk.size", 32 * 1024),

    /**
     * The maximum number of queued request for {@link FileChunk}s
     */
    TRANSFERS_MAX_REQUESTS_QUEUED("transfers.max.request.queued", 15),

    /**
     * PF-972
     * Files that are smaller than this threshold are being buffered into memory
     * directly and the upload is serviced from this buffer. For larger files
     * the file is held open during upload and the upload is serviced from the
     * storage.
     */
    TRANSFER_BUFFER_THRESHOLD("transfer.buffer.threshold", 2 * 1024 * 1024),

    /**
     * My dynamic dns hostname or fix ip.
     */
    HOSTNAME("hostname", null, true) {
        @Override
        public String getValue(Controller controller) {
            String value = super.getValue(controller);
            if (value == null) {
                // Old entry
                value = controller.getConfig().getProperty("mydyndns");
            }
            return value;
        }

        @Override
        public void removeValue(Controller controller) {
            super.removeValue(controller);
            controller.getConfig().remove("mydyndns");
        }

        @Override
        public void setValue(Controller controller, String value) {
            super.setValue(controller, value);
            controller.getConfig().remove("mydyndns");
        }
    },

    /**
     * Setting to enable/disable zip compression on LAN
     */
    USE_ZIP_ON_LAN("use_zip_on_lan", false),

    /**
     * Setting to enable/disable swarming in an LAN environment. If swarming
     * below is set to false, this is ignored!
     */
    USE_SWARMING_ON_LAN("swarming.lan.enabled", true),

    /**
     * Delta-sync: Enable/Disable it.
     */
    USE_DELTA_ON_INTERNET("deltasync.internet.enabled", true),

    USE_DELTA_ON_LAN("deltasync.lan.enabled", true),

    /**
     * Setting to enable/disable swarming.
     */
    USE_SWARMING_ON_INTERNET("swarming.internet.enabled", true),

    /**
     * The basedir for all powerfolder.
     */
    FOLDER_BASEDIR("foldersbase") {
        @Override
        public String getDefaultValue() {
            String rootDir = System.getProperty("user.home");

            // Also place the base dir into user home on Vista and 7
            if (OSUtil.isWindowsSystem() && OSUtil.isWindowsXPSystem()) {
                WinUtils util = WinUtils.getInstance();
                if (util != null) {
                    String can = util.getSystemFolderPath(
                            WinUtils.CSIDL_PERSONAL, false);
                    if (StringUtils.isNotBlank(can)) {
                        rootDir = can;
                    }
                }
            }
            return Paths.get(rootDir).resolve(
                    Constants.FOLDERS_BASE_DIR_SUBDIR_NAME).toString();
        }
    },

    /**
     * PFC-2165 Base directory does not longer change back to default directory
     * if inaccessible during program start, e.g. USB- or Network-Drive
     */
    FOLDER_BASEDIR_FALLBACK_TO_DEFAULT("folderbase.fallback.enabled", false),

    /**
     * Lets do this flexible.
     */
    FOLDER_BASEDIR_DELETED_DIR("folderbase.deleteddir", "BACKUP_REMOVE"),

    /**
     * Contains a comma-separated list of all plugins to load.
     */
    PLUGINS("plugins"),

    /**
     * Contains a comma-separated list of all plugins, which are disabled.
     */
    PLUGINS_DISABLED("plugins.disabled"),

    /**
     * Flag if update at start should performed.
     */
    DYNDNS_AUTO_UPDATE("dyndns.autoUpdate", false) {
        @Override
        public String getValue(Controller controller) {
            String value = super.getValue(controller);
            if (value == null) {
                value = controller.getConfig().getProperty("onStartUpdate");
            }
            return value != null ? value : Boolean.FALSE.toString();
        }

    },

    /**
     * The username to use for the dyndns update.
     */
    DYNDNS_USERNAME("dyndnsUserName"),

    /**
     * The password to use for the dyndns update.
     */
    DYNDNS_PASSWORD("dyndnsPassword"),

    /**
     * The ip of the last dyndns update.
     */
    DYNDNS_LAST_UPDATED_IP("lastUpdatedIP"),

    /**
     * Comma-seperated list of ip-ranges that are (forced) in our LAN.
     */
    LANLIST("lanlist", ""),

    /**
     * Whether to use the PowerFolder icon in Windows Explorer folders.
     */
    USE_PF_ICON("use.pf.icon", true),

    /**
     * #2256: Simple conflict detection.
     */
    CONFLICT_DETECTION("conflict.detection", true),

    LOOK_FOR_FOLDER_CANDIDATES("look.for.folder.candidates", true, true) {
        @Override
        public String getValue(Controller controller) {
            if (controller.getMySelf().isServer()) {
                return Boolean.FALSE.toString();
            } else {
                return super.getValue(controller);
            }
        }
    },

    LOOK_FOR_FOLDERS_TO_BE_REMOVED("look.for.folder.removes", false),

    /**
     * Whether to log verbose.
     */
    VERBOSE("verbose", false),

    /**
     * PFS-3218: If profiling should be enabled
     */
    PROFILING("profiling", false),

    /**
     * The loglevel to write to debug file when verbose=true
     */
    LOG_LEVEL_FILE("log.file.level", Level.FINE.getName(), true),

    /**
     * #2585
     */
    LOG_FILE_ROTATE("log.file.rotate", true, true),

    /**
     * PFS-475: Remove old log files
     */
    LOG_FILE_DELETE_DAYS("log.file.keep.days", 31, true),

    /**
     * The loglevel to print to console when verbose=true
     */
    LOG_LEVEL_CONSOLE("log.console.level", Level.INFO.getName(), true),

    /**
     * PFS-1017: Logging to syslog
     */
    LOG_SYSLOG_LEVEL("log.syslog.level", Level.INFO.getName(), true),

    LOG_SYSLOG_HOST("log.syslog.host", null, true),

    LOG_SYSLOG_PORT("log.syslog.port", 514, true),

    /**
     * Should the active threads be logged?
     */
    LOG_ACTIVE_THREADS("log.active_threads", false, true),

    /**
     * Whether to request debug reports
     *
     * @see RequestNodeInformation
     */
    DEBUG_REPORTS("debug.reports", false),

    /**
     * If it should be automatically connected to other nodes. FIX: Currently
     * only affects ReconnectManager.
     */
    AUTO_CONNECT("auto.connect", true),

    /**
     * The number of seconds to go on idle between connection tries per
     * Reconnector.
     */
    CONNECT_WAIT("connect.wait.seconds", 120),

    /**
     * PFC-2982: Disabled relayed connections by default.
     */
    RELAYED_CONNECTIONS_ENABLED("connections.relayed", false),

    /**
     * Enable/Disable relayed connections.
     */
    UDT_CONNECTIONS_ENABLED("connections.udt", true),

    /**
     * Enable/Disable node manager (for debugging only)
     */
    NODEMANAGER_ENABLED("nodemanager.enabled", true),

    /**
     * Enable/Disable transfermanager (for debugging only)
     */
    TRANSFER_MANAGER_ENABLED("transfermanager.enabled", true),

    /**
     * Enable/Disable folder repo (for debugging only)
     */
    FOLDER_REPOSITORY_ENABLED("folderepository.enabled", true),

    /**
     * The number of seconds to wait to recalc a {@link FolderStatistic} when a
     * change to the folder happened. Applies to large folders only, that host
     * more files than {@link FolderStatistic#MAX_ITEMS}
     */
    FOLDER_STATS_CALC_TIME("filedb.stats.seconds", 30),

    /**
     * The maximum time powerfolder keeps the folder database dirty in memory
     * before writing it to disk in seconds.
     */
    FOLDER_DB_PERSIST_TIME("filedb.persist.seconds", 60),

    /**
     * #2637: Disabling can save OS resources.
     */
    FOLDER_WATCHER_ENABLED("folder.watcher.enabled", true, true),
    /**
     * #2405: The delay for syncing after folderWatcher detects a change.
     */
    FOLDER_WATCHER_DELAY("folder.watcher.delay.seconds", 1),

    FILE_REQUESTOR_INTERVAL("file.requestor.interval.seconds", 60),

    /**
     * Enable to copy and delete a newly transfered file instead of moveing.
     */
    FOLDER_COPY_AFTER_TRANSFER("folder.copy_after_transfer.enabled", false, true),

    /**
     * The number of seconds between db maintenance (1 hour).
     */
    DB_MAINTENANCE_SECONDS("filedb.maintenance.seconds", 3600),

    /**
     * The age of a deleted file until it gets removed by the folder db
     * maintenance. In Seconds! Default: 3 month
     */
    MAX_FILEINFO_DELETED_AGE_SECONDS("filedb.deleted.maxage", 60 * 60 * 24 * 30
            * 3),

    /**
     * The http proxy to use for HTTP tunneled connections
     */
    HTTP_PROXY_HOST("http.proxy.host", null, true) {
        @Override
        public String getDefaultValue() {
            String host = System.getProperty("https.proxyHost");
            if (StringUtils.isBlank(host)) {
                host = System.getProperty("http.proxyHost");
            }
            return host;
        }
    },

    /**
     * The http proxy port to use for HTTP tunneled connections
     */
    HTTP_PROXY_PORT("http.proxy.port", 80, true) {
        @Override
        public String getDefaultValue() {
            String port = System.getProperty("https.proxyPort");
            if (StringUtils.isBlank(port)) {
                port = System.getProperty("http.proxyPort");
            }
            if (StringUtils.isBlank(port)) {
                port = "80";
            }
            return port;
        }
    },

    /**
     * The http proxy username to use for HTTP tunneled connections
     */
    HTTP_PROXY_USERNAME("http.proxy.username", null, true),

    /**
     * The http password proxy to use for HTTP tunneled connections
     */
    HTTP_PROXY_PASSWORD("http.proxy.password", null, true),

    HTTP_PROXY_NON_PROXY_HOSTS("http.proxy.nonproxyhosts") {
        @Override
        public String getDefaultValue() {
            String hosts = System.getProperty("https.nonProxyHosts");
            if (StringUtils.isBlank(hosts)) {
                hosts = System.getProperty("http.nonProxyHosts");
            }
            return hosts;
        }
    },

    /**
     * Days until auto cleanup of uploads. Zero = cleanup on completion. NOTE -
     * true cleanup days dereferenced through Constants.CLEANUP_VALUES
     */
    UPLOAD_AUTO_CLEANUP_FREQUENCY("uploads.auto.cleanup.frequency", 2),

    /**
     * Days until auto cleanup of downloads. Zero = cleanup on completion. NOTE
     * - true cleanup days dereferenced through Constants.CLEANUP_VALUES
     */
    DOWNLOAD_AUTO_CLEANUP_FREQUENCY("downloads.auto.cleanup.frequency", 2),

    /**
     * Warning about unsyned folders.
     */
    FOLDER_SYNC_USE("sync.folder.use", false),

    /**
     * Seconds before warning about unsynced folders (10 days).
     */
    FOLDER_SYNC_WARN_SECONDS("sync.folder.warn.seconds", 864000) {
        @Override
        public String getValue(Controller controller) {
            String value = super.getValue(controller);
            if (value == null) {
                // Old entry
                try {
                    value = String.valueOf(24L * 60 * 60 * Integer
                            .valueOf(controller.getConfig().getProperty(
                                    "sync.folder.warn")));
                } catch (Exception e) {
                }
            }
            return value;
        }

        @Override
        public void removeValue(Controller controller) {
            super.removeValue(controller);
            controller.getConfig().remove("sync.folder.warn");
        }

        @Override
        public void setValue(Controller controller, String value) {
            super.setValue(controller, value);
            controller.getConfig().remove("sync.folder.warn");
        }
    },

    /**
     * TRAC #1776
     * <p>
     * Checks and avoids duplicate folders with the same name or base dir.
     * Duplicate folders by should be automatically prevented.
     */
    FOLDER_CREATE_AVOID_DUPES("create.folder.avoid.dupes", false),

    /**
     * Uses any existing directory found at the default path, even if not empty.
     */
    FOLDER_CREATE_USE_EXISTING("create.folder.use.existing", true),

    /**
     * PFC-2572: Possibility to disallow networked drives or UNC shares
     */
    FOLDER_CREATE_ALLOW_NETWORK("create.folder.allow.network", true),

    /**
     * PFC-2226: Option to restrict new folder creation to the default storage
     * path PFC-2424: If "Beginner mode" is switched on, set to "true"
     */
    FOLDER_CREATE_IN_BASEDIR_ONLY("create.folder.basedir.only", false) {
        @Override
        public Boolean getValueBoolean(Controller controller) {
            String value = getValue(controller);

            if (value == null) {
                if (!PreferencesEntry.EXPERT_MODE
                        .getValueBoolean(controller)) {
                    return Boolean.TRUE;
                }
                value = getDefaultValue();
            }

            try {
                return value.trim().equalsIgnoreCase("true");
            } catch (NumberFormatException e) {
                LOG.log(
                        Level.WARNING,
                        "Unable to parse configuration entry 'create.folder.basedir.only' into a boolean. Value: "
                                + value, e);
                return "true".equalsIgnoreCase(getDefaultValue());
            }
        }
    },

    /**
     * Remove folder from setup if disappeared/deleted from basedir.
     */
    FOLDER_REMOVE_IN_BASEDIR_WHEN_DISAPPEARED("remove.folder.basedir.when_disappeared", true),

    /**
     * PFC-2709: Enable/Disabled client sync with special directories, e.g. Documents‚ (User directories)
     */
    FOLDER_MAP_USER_DIRECTORIES("create.folder.map.user.directories", true),

    /**
     * The number of file versions to use when creating a new folder.
     */
    DEFAULT_ARCHIVE_VERSIONS("default.archive.versions", 25),

    /**
     * How many days before an archive file is cleaned up. Values 1, 7, 31, 365,
     * 0 (== never)
     */
    DEFAULT_ARCHIVE_CLEANUP_DAYS("archive.cleanup.days", 0),

    /**
     * #2132: This transfer mode will be recommend by default.
     */
    DEFAULT_TRANSFER_MODE("default.transfer.mode",
            SyncProfile.AUTOMATIC_SYNCHRONIZATION.getFieldList()),

    /**
     * PFC-2545: Special transfer mode if UNC path is encountered (used for
     * "Passive" client/Virtual Desktop/O4IT/CFN).
     * https://wiki.powerfolder.com/display/PFD/Passive+Client
     */
    UNC_TRANSFER_MODE("unc.transfer.mode"),

    /**
     * The number of maximum activate
     */
    FOLDER_SCANNER_MAX_CRAWLERS("sync.folder.max_crawlers", 3),

    /**
     * PFC-3018: The max number of file requesting workers to spawn
     */
    FOLDER_FILE_REQUESTOR_MAX_WORKERS("sync.folder.max_filerequestors", 300),

    /**
     * PFC-3018: The number of folders one requestor should handle
     */
    FOLDER_FOLDERS_PER_FILE_REQUESTOR("sync.folder.folders_per_filerequestors", 200),

    /**
     * Automatically setup all folders the user has access to and also
     * automatically accept folder invites.
     */
    AUTO_SETUP_ACCOUNT_FOLDERS("auto.setup.account.folders", true),

    /**
     * PFS-2227: List of paths not to be automatically setup as folders.
     */
    AUTO_SETUP_FOLDERS_IGNORED("auto.setup.folders.ignored", ""),

    /**
     * #2485: {@link Integer#MAX_VALUE} for never resume. 0 for adaptive resume
     * = resume when user is not working on his PC
     */
    PAUSE_RESUME_SECONDS("pause.resume.seconds", 3600), // One hour default.

    SHOW_TINY_WIZARDS("show.tiny.wizards", false),

    SHOW_CREATE_FOLDER("show.create.folder", true),

    /**
     * PFC-2638: Desktop sync option
     */
    SHOW_DESKTOP_SYNC_OPTION("show.desktop.sync", false),

    SHOW_WALLPAPER_OPTION("show.wallpaper", false),

    COPY_GETTING_STARTED_GUIDE("copy.getting_started.guide", false),

    RECOVER_0BYTE_FILES("recover.zero_byte.files", true),

    /**
     * PFS-1994: En-/Disable encrypted storage for this server.
     */
    ENCRYPTED_STORAGE("encrypted_storage.enabled", false),
    ENCRYPTED_STORAGE_PASSPHRASE("encrypted_storage.passphrase", null),

    /**
     * PF-1178: Add configuration entry for folder encryption checkbox default value.
     */
    ENCRYPTED_STORAGE_DEFAULT("encrypted_storage.default", true),

    /**
     * PFS-2871: Port for client authentication over web requests.
     */
    WEB_CLIENT_PORT("web.client.port", 0),

    /**
     * PFS-1766: Event API to trigger WebHooks.
     */
    EVENT_API_URL_UPLOADED_FILE_CLIENT("event.api.file.uploaded.client"),
    EVENT_API_URL_DOWNLOADED_FILE_CLIENT("event.api.file.downloaded.client"),

    EVENT_API_URL_UPLOADED_FILE_WEB("event.api.file.uploaded.web"),
    EVENT_API_URL_DOWNLOADED_FILE_WEB("event.api.file.downloaded.web"),
    EVENT_API_URL_DOWNLOADED_FILE_LINK("event.api.file.downloaded.link"),

    EVENT_API_URL_DELETED_FILE("event.api.file.deleted"),

    /**
     * PFS-2427: Check if the client runs on a WDNAS device. Therefore setting POSIX file
     * permissions after sync is mandatory.
     */
    WDNAS_CLIENT("wdnas.client", false),

    LOCKING_CHANGES_FILE_PERMISSIONS("locking.changes.file_permissions", false);

    // Methods/Constructors ***************************************************

    private static final Logger LOG = Logger.getLogger(ConfigurationEntry.class
            .getName());

    private final String configKey;
    protected final String defaultValue;
    protected final boolean restartRequired;

    ConfigurationEntry(String aConfigKey) {
        this(aConfigKey, null);
    }

    ConfigurationEntry(String aConfigKey, String theDefaultValue) {
        this(aConfigKey, theDefaultValue, false);
    }

    ConfigurationEntry(String aConfigKey, String theDefaultValue, boolean restartRequired) {
        Reject.ifBlank(aConfigKey, "Config key is blank");
        Reject.ifTrue(
                aConfigKey.startsWith(FolderSettings.PREFIX_V4),
                "Config entries must not start with '"
                        + FolderSettings.PREFIX_V4 + '\'');
        configKey = aConfigKey;
        this.restartRequired = restartRequired;
        if (theDefaultValue != null) {
            defaultValue = theDefaultValue;
        } else {
            // Try harder. Use getter. might have been overridden
            defaultValue = getDefaultValue();
        }
    }

    ConfigurationEntry(String aConfigKey, boolean theDefaultValue) {
        this(aConfigKey, String.valueOf(theDefaultValue));
    }

    ConfigurationEntry(String aConfigKey, boolean theDefaultValue, boolean restartRequired) {
        this(aConfigKey, String.valueOf(theDefaultValue), restartRequired);
    }

    ConfigurationEntry(String aConfigKey, int theDefaultValue) {
        this(aConfigKey, String.valueOf(theDefaultValue));
    }

    ConfigurationEntry(String aConfigKey, int theDefaultValue, boolean restartRequired) {
        this(aConfigKey, String.valueOf(theDefaultValue), restartRequired);
    }

    /**
     * @param controller the controller to read the config from
     * @return If a value was set for this entry.
     */
    public boolean hasValue(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        return hasValue(controller.getConfig());
    }

    /**
     * @param config the config
     * @return If a value was set for this entry.
     */
    public boolean hasValue(Properties config) {
        Reject.ifNull(config, "Config is null");
        return config.getProperty(configKey) != null;
    }

    /**
     * @param controller the controller to read the config from
     * @return If a value was set for this entry and contains a non-blank
     * string.
     */
    public boolean hasNonBlankValue(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        return hasNonBlankValue(controller.getConfig());
    }

    /**
     * @param config the config
     * @return If a value was set for this entry and contains a non-blank
     * string.
     */
    public boolean hasNonBlankValue(Properties config) {
        return hasValue(config)
                && StringUtils.isNotBlank(getValue(config));
    }

    /**
     * @param controller the controller to read the config from
     * @return The current value from the configuration for this entry. or
     */
    public String getValue(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        return getValue(controller.getConfig());
    }

    /**
     * @param config the config to read the value from
     * @return The current value from the configuration for this entry. or
     */
    public String getValue(Properties config) {
        Reject.ifNull(config, "Config is null");
        String value = config.getProperty(configKey);
        if (value == null) {
            value = getDefaultValue();
        }
        return value;
    }

    /**
     * Parses the configuration entry into a Integer.
     *
     * @param controller the controller to read the config from
     * @return The current value from the configuration for this entry or the
     * default value if value not set/unparseable or {@code null} if no
     * default value was set.
     */
    public Integer getValueInt(Controller controller) {
        return getValueInt(controller.getConfig());
    }

    /**
     * Parses the configuration entry into a Integer.
     *
     * @param config the config to read from
     * @return The current value from the configuration for this entry or the
     * default value if value not set/unparseable or {@code null} if no
     * default value was set.
     */
    public Integer getValueInt(Properties config) {
        String value = getValue(config);
        if (value == null || StringUtils.isBlank(value)) {
            value = getDefaultValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return new Integer(value.trim());
        } catch (NumberFormatException e) {
            LOG.log(Level.WARNING, "Unable to parse configuration entry '"
                    + configKey + "' into a int. Value: " + value, e);
            return new Integer(getDefaultValue());
        }
    }

    /**
     * Parses the configuration entry into a Boolen.
     *
     * @param controller the controller to read the config from
     * @return The current value from the configuration for this entry or the
     * default value if value not set/unparseable or {@code null} if no
     * default value was set.
     */
    public Boolean getValueBoolean(Controller controller) {
        return getValueBoolean(controller.getConfig());
    }

    /**
     * Parses the configuration entry into a Boolen.
     *
     * @param config the config to read from
     * @return The current value from the configuration for this entry or the
     * default value if value not set/unparseable or {@code null} if no
     * default value was set.
     */
    public Boolean getValueBoolean(Properties config) {
        String value = getValue(config);
        if (value == null || StringUtils.isBlank(value)) {
            value = getDefaultValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return value.trim().equalsIgnoreCase("true");
        } catch (NumberFormatException e) {
            LOG.log(Level.WARNING, "Unable to parse configuration entry '"
                    + configKey + "' into a boolean. Value: " + value, e);
            return "true".equalsIgnoreCase(getDefaultValue());
        }
    }

    /**
     * Parses the configuration entry into an Array.
     *
     * @param controller the controller to read the config from
     * @return A string array of the parsed configuration entry.
     * If the configuration entry is null, the array contains one null element.
     */
    public String[] getValueArray(Controller controller) {
        String[] values = new String[1];
        String valuesString = getValue(controller.getConfig());
        if (valuesString != null) {
            values = valuesString.split("\\s*,\\s*");
        }
        return values;
    }

    /**
     * Creates a model containing the value of the configuration entry.
     * <p>
     * Changes from "below" won't be reflected.
     * <p>
     * TODO Resolve problem: Model not buffered!
     *
     * @param controller
     * @return a value model bound to the configuration entry.
     * @deprecated do not use util problems are resolved
     */
    @Deprecated
    public ValueModel getModel(final Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        ValueModel model = new ValueHolder(getValue(controller), false);
        model.addValueChangeListener(evt -> setValue(controller, (String) evt.getNewValue()));
        return model;
    }

    /**
     * Sets the value of this config entry.
     *
     * @param controller the controller of the config
     * @param value      the value to set
     */
    public void setValue(Controller controller, String value) {
        setValue(controller.getConfig(), value);
    }

    /**
     * Sets the value of this config entry.
     *
     * @param value the value to set
     */
    public void setValue(Properties config, String value) {
        Reject.ifNull(config, "config is null");
        config.setProperty(configKey, value);
    }

    /**
     * Sets the value of this config entry.
     *
     * @param controller the controller of the config
     * @param value      the value to set
     */
    public void setValue(Controller controller, boolean value) {
        setValue(controller, String.valueOf(value));
    }

    /**
     * Sets the value of this config entry.
     *
     * @param controller the controller of the config
     * @param value      the value to set
     */
    public void setValue(Controller controller, int value) {
        setValue(controller, String.valueOf(value));
    }

    /**
     * Sets the value of this config entry.
     *
     * @param config the config of the client
     * @param value  the value to set
     */
    public void setValue(Properties config, boolean value) {
        setValue(config, String.valueOf(value));
    }

    /**
     * Removes the entry from the configuration.
     *
     * @param controller the controller to use
     */
    public void removeValue(Controller controller) {
        removeValue(controller.getConfig());
    }

    /**
     * Removes the entry from the configuration.
     *
     * @param config the config to remove from
     */
    public void removeValue(Properties config) {
        Reject.ifNull(config, "config is null");
        config.remove(configKey);
    }

    /**
     * PFC-2969
     *
     * @param config
     * @param overwrite
     */
    public void setOverwrite(Properties config, Boolean overwrite) {
        if (overwrite != null) {
            config.setProperty(getConfigKey() + ConfigurationLoader.SUFFIX_OVERWRITE, String.valueOf(overwrite));
        } else {
            config.remove(getConfigKey() + ConfigurationLoader.SUFFIX_OVERWRITE);
        }
    }

    /**
     * PFC-2969: Copies the value from the current configuration to the target config.
     *
     * @param fromController
     * @param toConfig
     */
    public void copy(Controller fromController, Properties toConfig) {
        String val = this.getValue(fromController);
        if (val != null) {
            toConfig.setProperty(getConfigKey(), val);
        } else {
            toConfig.remove(getConfigKey());
        }
    }

    /**
     * PFC-2969: Copies the value from the current configuration to the target config if controller config is not default.
     *
     * @param fromController
     * @param toConfig
     */
    public void copyIfNotDefault(Controller fromController, Properties toConfig) {
        String val = getValue(fromController);
        if (!Util.equals(val, getDefaultValue())) {
            copy(fromController, toConfig);
        } else {
            // Default. Remove value.
            removeValue(toConfig);
        }
    }

    /**
     * @return the default value for this config entry.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return the key in config
     */
    public String getConfigKey() {
        return configKey;
    }

    private static final Map<String, ConfigurationEntry> LOOKUP = Maps.uniqueIndex(Arrays.asList(ConfigurationEntry.values()), ConfigurationEntry::getConfigKey);

    public static ConfigurationEntry getEnum(String key) {
        return LOOKUP.get(key);
    }

    /**
     * Indicates if by setting this configuration entry an server restart is required.
     *
     * @return true if an restart is required or false if not.
     */
    public boolean isRestartRequired() {
        return restartRequired;
    }
}
