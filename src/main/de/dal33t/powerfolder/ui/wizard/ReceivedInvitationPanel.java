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
 * $Id: ReceivedInvitationPanel.java 20999 2013-03-11 13:19:11Z glasgow $
 */
package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.panel.SyncProfileSelectorPanel;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ConfigurationLoader;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import jwf.WizardPanel;

import javax.swing.*;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.*;

/**
 * Class to do folder creation for a specified invite.
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
@SuppressWarnings("serial")
public class ReceivedInvitationPanel extends PFWizardPanel {

    private static final Logger log = Logger
        .getLogger(ReceivedInvitationPanel.class.getName());

    private final Invitation invitation;

    private JLabel folderHintLabel;
    private JLabel folderNameLabel;
    private JLabel invitorHintLabel;
    private JLabel invitorLabel;
    private JLabel invitationMessageHintLabel;
    private JTextField invitationMessageLabel;
    private JLabel syncProfileHintLabel;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;

    public ReceivedInvitationPanel(Controller controller, Invitation invitation)
    {
        super(controller);
        this.invitation = invitation;
    }

    /**
     * Can procede if an invitation exists.
     */
    @Override
    public boolean hasNext() {
        return invitation != null;
    }

    @Override
    public WizardPanel next() {

        // Set sync profile
        getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
                syncProfileSelectorPanel.getSyncProfile());

        // Set folder info
        getWizardContext()
                .setAttribute(FOLDERINFO_ATTRIBUTE, invitation.folder);

        // Set folder permission
        getWizardContext().setAttribute(FOLDER_PERMISSION_ATTRIBUTE,
                invitation.getPermission());

        // Do not prompt for send invitation afterwards
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, false);

        // Do not prompt for send invitation afterwards
        getWizardContext().setAttribute(FOLDER_IS_INVITE, true);

        // Setup choose disk location panel
        getWizardContext()
                .setAttribute(
                        PROMPT_TEXT_ATTRIBUTE,
                        Translation
                                .get("wizard.what_to_do.invite.select_local"));

        // Setup success panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
                Translation.get("wizard.setup_success"),
                Translation.get("wizard.success_join"));
        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        getWizardContext().setAttribute(MAKE_FRIEND_AFTER,
                invitation.getSenderDevice());

        WizardPanel next = new ChooseDiskLocationPanel(getController(), invitation
                .getSuggestedLocalBase(getController()).toAbsolutePath().toString(),
                new FolderCreatePanel(getController()));

        if (serverAgreeInvitationEnabled(invitation)) {
            return new SwingWorkerPanel(getController(), new AcceptInviteTask(),
                    Translation.get(""), Translation.get(""),
                    next);
        } else {
            return next;
        }
    }

    /**
     * PF-164: Support federation invites:
     * <p>
     * If we're in a federation environment, we have to ask the service of the invitation if
     * FOLDER_AGREE_INVITATION_ENABLED is enabled.
     *
     * @param invitation The invitation.
     */
    private boolean serverAgreeInvitationEnabled(Invitation invitation) {

        boolean serverAgreeInvitationsEnabled =
                ConfigurationEntry.FOLDER_AGREE_INVITATION_ENABLED.getValueBoolean(getController());

        if (invitation.getServer() != null &&
                ConfigurationEntry.SERVER_FEDERATION_ENABLED.getValueBoolean(getController())) {

            try {
                Properties props = ConfigurationLoader
                        .loadPreConfiguration(invitation.getServer().getWebUrl());
                String agreeInvitations = (String) props.get(ConfigurationEntry.FOLDER_AGREE_INVITATION_ENABLED
                        .getConfigKey());

                if (StringUtils.isNotBlank(agreeInvitations)) {
                    serverAgreeInvitationsEnabled = Boolean.parseBoolean(agreeInvitations);
                }

            } catch (IOException e) {
                log.warning("Failed to get config from federation server "
                        + invitation.getServer().getWebUrl());
                return serverAgreeInvitationsEnabled;
            }
        }

        return serverAgreeInvitationsEnabled;
    }

    @Override
    protected JPanel buildContent() {

        FormLayout layout = new FormLayout("right:pref, 3dlu, pref, pref:grow",
            "pref, 6dlu, pref, 3dlu, pref, 3dlu, pref, "
                + "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        // Invite info

        builder.addLabel(Translation.get(
            "wizard.folder_invitation.intro", invitation.getSender(),
            invitation.folder.getLocalizedName()), cc.xyw(1, 1, 4));

        // Message

        int row = 3;
        String message = invitation.getInvitationText();
        if (message != null && message.trim().length() > 0) {
            builder.add(invitationMessageHintLabel, cc.xy(1, row));
            builder.add(invitationMessageLabel, cc.xy(3, row));
            row += 2;
        }

        // Sync
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            builder.add(syncProfileHintLabel, cc.xy(1, row));
            JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
            p.setOpaque(false);
            builder.add(p, cc.xyw(3, row, 2));
        }
        row += 2;

        return builder.getPanel();
    }

    /**
     * Initalizes all nessesary components
     */
    @Override
    protected void initComponents() {
        // Folder name label
        folderHintLabel = new JLabel(
            Translation.get("general.folder"));
        folderHintLabel.setEnabled(false);
        folderNameLabel = SimpleComponentFactory.createLabel();

        // Invitor label
        invitorHintLabel = new JLabel(
            Translation.get("general.inviter"));
        invitorHintLabel.setEnabled(false);
        invitorLabel = SimpleComponentFactory.createLabel();

        // Invitation messages
        invitationMessageHintLabel = new JLabel(
            Translation.get("general.message"));
        invitationMessageHintLabel.setEnabled(false);
        invitationMessageLabel = new JTextField();
        invitationMessageLabel.setEditable(false);

        // Sync profile
        syncProfileHintLabel = new JLabel(
            Translation.get("general.synchonisation"));
        syncProfileHintLabel.setEnabled(false);
        syncProfileSelectorPanel = new SyncProfileSelectorPanel(getController());
        syncProfileSelectorPanel.setEnabled(false);

        loadInvitation();
    }

    @Override
    protected String getTitle() {
        return Translation.get("wizard.folder_invitation.title");
    }

    private void loadInvitation() {
        log.info("Loaded invitation " + invitation);
        if (invitation != null) {
            folderHintLabel.setEnabled(true);
            folderNameLabel.setText(invitation.folder.getLocalizedName());

            invitorHintLabel.setEnabled(true);
            invitorLabel.setText(invitation.getSender());

            invitationMessageHintLabel.setEnabled(true);
            invitationMessageLabel
                .setText(invitation.getInvitationText() == null
                    ? ""
                    : invitation.getInvitationText());

            syncProfileHintLabel.setEnabled(true);
            syncProfileSelectorPanel.setEnabled(true);
            SyncProfile suggestedProfile = invitation.getSuggestedSyncProfile();
            syncProfileSelectorPanel.setSyncProfile(suggestedProfile, false);
        } else {
            folderHintLabel.setEnabled(false);
            folderNameLabel.setText("");
            invitorHintLabel.setEnabled(false);
            invitorLabel.setText("");
            invitationMessageHintLabel.setEnabled(false);
            invitationMessageLabel.setText("");
            syncProfileHintLabel.setEnabled(false);
            syncProfileSelectorPanel.setEnabled(false);
        }
    }

    private class AcceptInviteTask implements Runnable {
        @Override
        public void run() {
            getController().getOSClient().getSecurityService().acceptInvitation(invitation);
        }

    }
}