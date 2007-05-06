/* $Id$
 * 
 * Copyright (c) 2007 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.ui.wizard;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;

import jwf.WizardPanel;

import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.FolderListPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.webservice.WebServiceClient;

public class MirrorFolderPanel extends PFWizardPanel {
    private boolean initalized = false;

    private SelectionInList foldersModel;
    private FolderListPanel folderList;

    public MirrorFolderPanel(Controller controller) {
        super(controller);
    }

    // From WizardPanel *******************************************************

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    public boolean hasNext() {
        return foldersModel.getSelection() != null;
    }

    public boolean validateNext(List list) {
        return true;
    }

    public WizardPanel next() {
        Folder folder = (Folder) foldersModel.getSelection();

        // Actually setup mirror
        getController().getWebServiceClient().mirrorFolder(folder);

        // Choose location...
        return new TextPanelPanel(getController(),
            "WebService Setup Successful",
            "You successfully setup the WebService\nto mirror folder "
                + folder.getName() + ".\n \n"
                + "Please keep in mind that the inital backup\n"
                + "may take some time on big folders.");
    }

    public boolean canFinish() {
        return false;
    }

    public void finish() {
    }

    // UI building ************************************************************

    /**
     * Builds the ui
     */
    private void buildUI() {
        // init
        initComponents();

        setBorder(Borders.EMPTY_BORDER);
        FormLayout layout = new FormLayout(
            "pref, 15dlu, right:pref, 3dlu, pref:grow",
            "pref, 15dlu, pref, 10dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout, this);
        builder.setBorder(Borders.createEmptyBorder("5dlu, 20dlu, 0, 0"));
        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(Icons.WEBSERVICE_PICTO), cc.xywh(1, 3, 1, 3,
            CellConstraints.DEFAULT, CellConstraints.TOP));
        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.webservice.mirrorsetup")), cc.xyw(3, 1, 3));

        builder.addLabel(Translation
            .getTranslation("wizard.webservice.mirrorchoosefolder"), cc.xyw(3,
            3, 3));

        builder.addLabel(Translation.getTranslation("general.folder"), cc.xy(3,
            5));
        builder.add(folderList.getUIComponent(), cc.xy(5, 5));

        // initalized
        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {
        WebServiceClient ws = getController().getWebServiceClient();
        List<Folder> folders = new ArrayList<Folder>();
        for (Folder folder : getController().getFolderRepository()
            .getFoldersAsCollection())
        {
            if (!ws.isMirrored(folder)) {
                folders.add(folder);
            }
        }
        foldersModel = new SelectionInList(folders);
        folderList = new FolderListPanel(foldersModel);
        foldersModel.getSelectionHolder().addValueChangeListener(
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    updateButtons();
                }
            });
        updateButtons();
    }
}
