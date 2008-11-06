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
package de.dal33t.powerfolder.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.uif_lite.panel.SimpleInternalFrame;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.chat.MemberChatPanel;
import de.dal33t.powerfolder.ui.folder.FolderPanel;
import de.dal33t.powerfolder.ui.friends.FriendsPanel;
import de.dal33t.powerfolder.ui.homeold.RootPanel;
import de.dal33t.powerfolder.ui.model.DirectoryModel;
import de.dal33t.powerfolder.ui.myfolders.MyFoldersPanel;
import de.dal33t.powerfolder.ui.navigation.ControlQuarter;
import de.dal33t.powerfolder.ui.navigation.NavigationToolBar;
import de.dal33t.powerfolder.ui.navigation.RootNode;
import de.dal33t.powerfolder.ui.recyclebin.RecycleBinPanel;
import de.dal33t.powerfolder.ui.transfer.DownloadsPanel;
import de.dal33t.powerfolder.ui.transfer.UploadsPanel;
import de.dal33t.powerfolder.ui.webservice.OnlineStoragePanel;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.UIPanel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * TODO #278 Rename to InformationWindow, Hold Window.
 * 
 * The information quarter right upper side of screen
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.114.2.1 $
 */
public class InformationQuarter extends PFUIComponent {

    private static final String ROOT_PANEL = "root";
    private static final String MY_FOLDER_PANEL = "my_folder";
    private static final String PREVIEW_FOLDER_PANEL = "preview_folder";
    private static final String MY_FOLDERS_PANEL = "myfolders";
    private static final String DOWNLOADS_PANEL = "downloads";
    private static final String UPLOADS_PANEL = "uploads";
    private static final String CHAT_PANEL = "chat";
    private static final String FRIENDS_PANEL = "friends";
    private static final String NETWORKSTATSISTICS_PANEL = "netstats";
    private static final String TEXT_PANEL = "text";
    private static final String RECYCLE_BIN_PANEL = "recycle";
    private static final String ONLINE_STORAGE_PANEL = "webservice";
    private static final String DEBUG_PANEL = "debug";
    private static final String DIALOG_TESINTG_PANEL = "dialogTesting";

    // the ui panel
    private JComponent uiPanel;

    // The frame around the panel
    private SimpleInternalFrame uiFrame;

    // The control quarter to act on
    private ControlQuarter controlQuarter;

    private CardLayout cardLayout;
    private JPanel cardPanel;

    // Root Panel
    private RootPanel rootPanel;

    // (My) Folder panel
    private FolderPanel myFolderPanel;

    // (Previre) Folder panel
    private FolderPanel previewFolderPanel;

    // MyFolders panel
    private MyFoldersPanel myFoldersPanel;

    // Down/uploads panel
    private DownloadsPanel downloadsPanel;
    private UploadsPanel uploadsPanel;

    private RecycleBinPanel recycleBinPanel;

    private OnlineStoragePanel osPanel;
    // chat
    private MemberChatPanel memberChatPanel;

    // friends
    private FriendsPanel friendsPanel;

    // netstats
    private NetworkStatisticsPanel networkStatisticsPanel;

    // Text
    private TextPanel textPanel;

    // debug
    private DebugPanel debugPanel;

    // debug
    private DialogTestingPanel dialogTestingPanel;

    // The uninitalized panels
    private Map<String, UIPanel> uninitializedPanels;

    /* The currently displayed item */
    private Object displayTarget;

    public InformationQuarter(ControlQuarter controlQuarter,
        Controller controller)
    {
        super(controller);
        this.controlQuarter = controlQuarter;
        this.uninitializedPanels = new HashMap<String, UIPanel>();

        // Add selection behavior
        controlQuarter.getSelectionModel().addSelectionChangeListener(
            new ControlQuarterSelectionListener());
    }

    // Selection code *********************************************************

    /**
     * Main class to act on selection changes in the control quarter
     */
    private class ControlQuarterSelectionListener implements
        SelectionChangeListener
    {
        public void selectionChanged(SelectionChangeEvent selectionChangeEvent)
        {
            Object selection = selectionChangeEvent.getSelection();
            if (selection != null) {
                // Call our selection method
                setSelected(selection);
            }
        }
    }

    private boolean showDebugReports() {
        if (!getController().isDebugReports()) {
            return false;
        }
        Preferences pref = getController().getPreferences();

        return pref.getBoolean(DebugPanel.SHOW_DEBUG_REPORTS_PREF_KEY, false);
    }

    /**
     * Sets the selected display component for info quarter
     * <p>
     * TODO #495
     */
    private void setSelected(Object selection) {
        // TODO #621 Refactor this
        if (selection instanceof DirectoryModel) {
            displayDirectory(((DirectoryModel) selection).getDirectory());
        } else if (selection instanceof Folder) {
            displayFolder((Folder) selection);
        } else if (selection instanceof Member) {
            // chat only if selection on Friends or Connected treenode and
            // not running in verbose mode (=displays debug info about node)
            Member member = (Member) selection;
            if (showDebugReports()) {
                displayNodeInformation((Member) selection);
            } else {
                displayChat(member);
            }

        } else if (selection instanceof RootNode) {
            displayRootPanel();
        } else if (selection == getUIController().getFolderRepositoryModel()
            .getMyFoldersTreeNode())
        {
            displayMyFolders();
        } else if (selection == RootNode.DOWNLOADS_NODE_LABEL) {
            displayDownloads();
        } else if (selection == RootNode.UPLOADS_NODE_LABEL) {
            displayUploads();
        } else if (selection == RootNode.RECYCLEBIN_NODE_LABEL) {
            displayRecycleBinPanel();
        } else if (selection == RootNode.WEBSERVICE_NODE_LABEL) {
            displayOnlineStoragePanel();
        } else if (selection == RootNode.DEBUG_NODE_LABEL) {
            displayDebugPanel();
        } else if (selection == RootNode.DIALOG_TESTING_NODE_LABEL) {
            displayDialogTestingPanel();
        } else if (selection == getUIController().getNodeManagerModel()
            .getFriendsTreeNode())
        {
            displayFriendsPanel();
        } else if (getController().isVerbose()
            && selection == getUIController().getNodeManagerModel()
                .getConnectedTreeNode())
        {
            displayStats();
        } else {
            // #621. Might be a TopLevelItem
            // displayNothing();
        }
    }

    // UI Building ************************************************************

    /**
     * @return the ui componennt and builds it lazily
     */
    public JComponent getUIComponent() {
        if (uiPanel == null) {
            initComponents();

            FormLayout layout = new FormLayout("max(0;pref):grow, pref",
                "pref, 0:grow, pref, pref, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            // Content
            builder.add(cardPanel, cc.xywh(1, 2, 2, 2));

            uiFrame = new SimpleInternalFrame(Translation
                .getTranslation("info_side.title"));
            uiFrame.add(builder.getPanel());
            uiFrame.setToolBar(new NavigationToolBar(getController(),
                getUIController().getControlQuarter().getNavigationModel())
                .getUIComponent());
            uiPanel = uiFrame;
        }

        return uiPanel;
    }

    /**
     * Initializes all ui components
     */
    private void initComponents() {
        textPanel = new TextPanel();

        // Root panel
        rootPanel = new RootPanel(getController());

        // Folder panel (my-type and preview-type)
        myFolderPanel = new FolderPanel(getController(), false);
        previewFolderPanel = new FolderPanel(getController(), true);

        // MyFolders panel
        myFoldersPanel = new MyFoldersPanel(getController());

        recycleBinPanel = new RecycleBinPanel(getController());
        osPanel = new OnlineStoragePanel(getController(), getUIController()
            .getServerClientModel());
        debugPanel = new DebugPanel(getController());
        dialogTestingPanel = new DialogTestingPanel(getController());
        // chat
        memberChatPanel = new MemberChatPanel(getController());

        // friends
        friendsPanel = new FriendsPanel(getController());

        // Down/uploads panel
        downloadsPanel = new DownloadsPanel(getController());
        uploadsPanel = new UploadsPanel(getController());

        networkStatisticsPanel = new NetworkStatisticsPanel(getController());

        cardLayout = new CardLayout();
        cardPanel = new JPanel();
        cardPanel.setLayout(cardLayout);
        cardPanel.add(ROOT_PANEL, rootPanel.getUIComponent());
        uninitializedPanels.put(MY_FOLDER_PANEL, myFolderPanel);
        uninitializedPanels.put(PREVIEW_FOLDER_PANEL, previewFolderPanel);
        uninitializedPanels.put(MY_FOLDERS_PANEL, myFoldersPanel);
        uninitializedPanels.put(DOWNLOADS_PANEL, downloadsPanel);
        uninitializedPanels.put(UPLOADS_PANEL, uploadsPanel);
        uninitializedPanels.put(CHAT_PANEL, memberChatPanel);
        uninitializedPanels.put(FRIENDS_PANEL, friendsPanel);
        uninitializedPanels.put(NETWORKSTATSISTICS_PANEL,
            networkStatisticsPanel);
        uninitializedPanels.put(TEXT_PANEL, textPanel);
        uninitializedPanels.put(RECYCLE_BIN_PANEL, recycleBinPanel);
        uninitializedPanels.put(ONLINE_STORAGE_PANEL, osPanel);
        uninitializedPanels.put(DEBUG_PANEL, debugPanel);
        uninitializedPanels.put(DIALOG_TESINTG_PANEL, dialogTestingPanel);
    }

    /**
     * @return the currently displayed target
     */
    public Object getDisplayTarget() {
        return displayTarget;
    }

    /**
     * Sets the current display target
     * 
     * @param target
     */
    private void setDisplayTarget(Object target) {
        Object oldValue = displayTarget;
        displayTarget = target;

        // Fire property change
        firePropertyChange("displayTarget", oldValue, displayTarget);
    }

    /**
     * Sets the title of the info frame. calling this method with
     * <code>null</code> will reset the title to default
     * 
     * @param title
     */
    public void setTitle(String title) {
        if (uiFrame != null) {
            if (title != null) {
                uiFrame.setTitle(title);
            } else {
                uiFrame.setTitle(Translation.getTranslation("info_side.title"));
            }
        }
    }

    // Display some really small statistics
    private void displayStats() {
        showCard(NETWORKSTATSISTICS_PANEL);
        setDisplayTarget(networkStatisticsPanel);
        setTitle(networkStatisticsPanel.getTitle());
    }

    public void displayDebugPanel() {
        showCard(DEBUG_PANEL);
        setDisplayTarget(debugPanel);
        setTitle(DebugPanel.getTitle());
    }

    public void displayRecycleBinPanel() {
        showCard(RECYCLE_BIN_PANEL);
        setDisplayTarget(recycleBinPanel);
        setTitle(recycleBinPanel.getTitle());
    }

    private void displayDialogTestingPanel() {
        showCard(DIALOG_TESINTG_PANEL);
        setDisplayTarget(dialogTestingPanel);
        setTitle(DialogTestingPanel.getTitle());
    }

    public void displayOnlineStoragePanel() {
        showCard(ONLINE_STORAGE_PANEL);
        setDisplayTarget(osPanel);
        if (getUIController().getServerClientModel().getClient().isConnected())
        {
            // Only popup if connected
            getUIController().getServerClientModel().checkAndSetupAccount(true);
        }
        setTitle(osPanel.getTitle());
    }

    public void displayRootPanel() {
        showCard(ROOT_PANEL);
        setDisplayTarget(rootPanel);
    }

    public void displayFolder(Folder folder) {
        Reject.ifNull(folder, "Folder is null");
        if (folder.isPreviewOnly()) {
            showCard(PREVIEW_FOLDER_PANEL);
            setDisplayTarget(folder);
            if (previewFolderPanel != null) { // fixes rare NPE on start
                previewFolderPanel.setFolder(folder);
                setTitle(previewFolderPanel.getTitle());
            }
        } else {
            showCard(MY_FOLDER_PANEL);
            setDisplayTarget(folder);
            if (myFolderPanel != null) { // fixes rare NPE on start
                myFolderPanel.setFolder(folder);
                setTitle(myFolderPanel.getTitle());
            }
        }
    }

    /**
     * Displays a Directory from a Folder
     * 
     * @param directory
     *            The Directory to display
     */
    public void displayDirectory(Directory directory) {
        Folder rootFolder = directory.getRootFolder();
        if (rootFolder.isPreviewOnly()) {
            showCard(PREVIEW_FOLDER_PANEL);
            controlQuarter.setSelected(directory);
            setDisplayTarget(directory);
            previewFolderPanel.setDirectory(directory);
            setTitle(previewFolderPanel.getTitle());
        } else {
            showCard(MY_FOLDER_PANEL);
            controlQuarter.setSelected(directory);
            setDisplayTarget(directory);
            myFolderPanel.setDirectory(directory);
            setTitle(myFolderPanel.getTitle());
        }
    }

    /**
     * Displays the downloads
     */
    public void displayDownloads() {
        showCard(DOWNLOADS_PANEL);
        setDisplayTarget(downloadsPanel);
        setTitle(DownloadsPanel.getTitle());
    }

    /**
     * Displays the uploads
     */
    public void displayUploads() {
        showCard(UPLOADS_PANEL);
        setDisplayTarget(uploadsPanel);
        setTitle(uploadsPanel.getTitle());
    }

    private void displayFriendsPanel() {
        showCard(FRIENDS_PANEL);
        setDisplayTarget(friendsPanel);
        setTitle(friendsPanel.getTitle());
    }

    /**
     * Displays the chat about a folder
     * 
     * @param folder
     */
    public void displayChat(Folder folder) {
        displayFolder(folder);
        if (folder.isPreviewOnly()) {
            previewFolderPanel.setTab(previewFolderPanel.getChatTabId());
        } else {
            myFolderPanel.setTab(myFolderPanel.getChatTabId());
        }
    }

    /**
     * Displays the chat about a (friend) member
     * 
     * @param member
     *            the member to chat with
     */
    public void displayChat(Member member) {
        showCard(CHAT_PANEL);
        memberChatPanel.setChatPartner(member);
        setDisplayTarget(memberChatPanel);
        setTitle(memberChatPanel.getTitle());
    }

    /**
     * Displays myFolders
     */
    public void displayMyFolders() {
        setDisplayTarget(myFoldersPanel);
        showCard(MY_FOLDERS_PANEL);
        setTitle(MyFoldersPanel.getTitle());
    }

    private void showCard(String panelName) {
        boolean cursorChanged = false;
        if (uninitializedPanels.containsKey(panelName)) {
            cursorChanged = true;
            getUIController().getMainFrame().getUIComponent().setCursor(
                new Cursor(Cursor.WAIT_CURSOR));
            cardPanel.add(panelName, uninitializedPanels.get(panelName)
                .getUIComponent());
            uninitializedPanels.remove(panelName);
        }
        cardLayout.show(cardPanel, panelName);
        if (cursorChanged) {
            getUIController().getMainFrame().getUIComponent().setCursor(
                new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    public void displayNodeInformation(Member node) {
        String debugReport = Debug.loadDebugReport(node.getInfo());
        if (debugReport != null) {
            setDisplayTarget(debugReport);
            displayText(debugReport);
        } else {
            String text = node.getNick() + " last seen online on "
                + Format.formatDate(node.getLastNetworkConnectTime());
            text += "\nfrom " + node.getHostName();
            setDisplayTarget(node);
            displayText(text);
        }
        setTitle(null);
    }

    /**
     * Displays nothing
     */
    public void displayNothing() {
        setDisplayTarget(null);
        displayText("");
        setTitle(null);
    }

    /**
     * Switches to text display and shows the document
     * 
     * @param doc
     * @param autoScroll
     *            if text area shoud scroll to the end automatically
     */
    private void displayText(StyledDocument doc, boolean autoScroll) {
        showCard(TEXT_PANEL);
        setDisplayTarget(textPanel);
        textPanel.setText(doc, autoScroll);
    }

    /**
     * Displays the text
     * 
     * @param text
     */
    public void displayText(String text) {
        StyledDocument doc = new DefaultStyledDocument();
        try {
            doc.insertString(0, text, null);
        } catch (BadLocationException e) {
            logFiner("BadLocationException", e);
        }
        displayText(doc, false);
    }

    public FolderPanel getMyFolderPanel() {
        return myFolderPanel;
    }

    public FolderPanel getPreviewFolderPanel() {
        return previewFolderPanel;
    }

    public MemberChatPanel getMemberChatPanel() {
        return memberChatPanel;
    }

    public DownloadsPanel getDownloadsPanel() {
        return downloadsPanel;
    }

    public UploadsPanel getUploadsPanel() {
        return uploadsPanel;
    }

}