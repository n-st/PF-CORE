package de.dal33t.powerfolder.ui.preferences;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.plugin.*;
import de.dal33t.powerfolder.ui.action.SelectionBaseAction;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

public class PluginSettingsTab extends PFUIComponent implements PreferenceTab, PluginManagerListener {

    private final static int PLUGIN_NAME_COL = 0;
    private final static int PLUGIN_DESCR_COL = 1;
    private final static int PLUGIN_CLASS_COL = 2;
    private final static int PLUGIN_STATUS_COL = 3;

    private PreferencesDialog preferencesDialog;

    private JPanel panel;
    private JTable pluginJTable;
    private JScrollPane pluginPane;
    private JButton settingsButton;
    private JButton enableButton;
    private SelectionModel selectionModel;

    public PluginSettingsTab(Controller controller,
        PreferencesDialog preferencesDialog)
    {
        super(controller);
        this.preferencesDialog = preferencesDialog;
        selectionModel = new SelectionModel();
        initComponents();
    }

    public boolean needsRestart() {
        return false;
    }

    public void save() {
    }

    public boolean validate() {
        return true;
    }

    public String getTabName() {
        return Translation
            .getTranslation("preferences.dialog.plugin.title");
    }

    public void undoChanges() {
    }

    /**
     * Creates the JPanel for plugin settings
     * 
     * @return the created panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout("3dlu, pref:grow, 3dlu",
                "3dlu, fill:pref:grow, 3dlu, pref, 3dlu");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.add(pluginPane, cc.xy(2, 2));
            builder.add(getButtonBar(), cc.xy(2, 4));
            panel = builder.getPanel();
        }        
        return panel;
    }

    private void initComponents() {
        pluginJTable = new JTable(new PluginTableModel());
        pluginJTable
            .setDefaultRenderer(Plugin.class, new PluginTableRenderer());
        pluginPane = new JScrollPane(pluginJTable);
        Util.whiteStripTable(pluginJTable);
        Util.removeBorder(pluginPane);
        Util.setZeroHeight(pluginPane);

        pluginJTable.getSelectionModel().addListSelectionListener(
            new PluginTableListSelectionListener());
        
        settingsButton = new JButton(new SettingsAction(getController(),
            selectionModel));
        enableButton = new JButton(new EnableAction(getController(),
            selectionModel));
        getController().getPluginManager().addPluginManagerListener(this);
    }

    private Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(enableButton, settingsButton);
    }

    private class PluginTableModel extends AbstractTableModel {

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return Plugin.class;
        }

        @Override
        public String getColumnName(int column)
        {
            switch (column) {
                case PLUGIN_NAME_COL : {
                    return Translation
                        .getTranslation("preferences.dialog.plugin.name");
                }
                case PLUGIN_DESCR_COL : {
                    return Translation
                        .getTranslation("preferences.dialog.plugin.description");
                }
                case PLUGIN_CLASS_COL : {
                    return Translation
                        .getTranslation("preferences.dialog.plugin.classname");
                }
                case PLUGIN_STATUS_COL : {
                    return Translation
                        .getTranslation("preferences.dialog.plugin.status");
                }
                default :
                    return null;
            }

        }

        public int getColumnCount() {
            return 4;
        }

        public int getRowCount() {
            PluginManager pluginManager = getController().getPluginManager();
            return pluginManager.countPlugins();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            PluginManager pluginManager = getController().getPluginManager();
            List<Plugin> plugins = pluginManager.getPlugins();
            return plugins.get(rowIndex);
        }
    }

    private class PluginTableRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            String newValue = "";
            PluginManager pluginManager = getController().getPluginManager();
            List<Plugin> plugins = pluginManager.getPlugins();
            Plugin plugin = plugins.get(row);
            boolean enabled = getController().getPluginManager().isEnabled(
                plugin);
            if (enabled) {
                setForeground(Color.BLACK);
            } else {
                setForeground(Color.LIGHT_GRAY);
            }
            
            int columnInModel = Util.toModel(table, column);
            switch (columnInModel) {
                case PLUGIN_NAME_COL : {
                    newValue = plugin.getName();
                    setToolTipText(plugin.getName());
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                }
                case PLUGIN_DESCR_COL : {
                    newValue = plugin.getDescription();
                    setToolTipText(plugin.getDescription());
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                }
                case PLUGIN_CLASS_COL : {
                    newValue = plugin.getClass().getName();
                    setToolTipText(plugin.getClass().getName());
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                }
                case PLUGIN_STATUS_COL : {
                    if (enabled) {
                        newValue = Translation
                            .getTranslation("preferences.dialog.plugin.status_enabled");
                    } else {
                        newValue = Translation
                            .getTranslation("preferences.dialog.plugin.status_disabled");
                    }
                    setToolTipText(newValue);
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                }
                default :
                    return null;
            }
            return super.getTableCellRendererComponent(table, newValue,
                isSelected, hasFocus, row, column);
        }
    }

    private class EnableAction extends SelectionBaseAction {

        public EnableAction(Controller controller, SelectionModel selectionModel)
        {
            super("plugin_enable", controller, selectionModel);
            setEnabled(false);
        }

        public void selectionChanged(SelectionChangeEvent event) {
            updateButton((Plugin) event.getSelection());
        }

        public void actionPerformed(ActionEvent e) {
            int index = pluginJTable.getSelectedRow();
            PluginManager pluginManager = getController().getPluginManager();
            Plugin plugin = pluginManager.getPlugins().get(index);
            boolean newStatus = !getController().getPluginManager().isEnabled(
                plugin);
            getController().getPluginManager().setEnabled(plugin, newStatus);
            updateButton(plugin);
        }

        private void updateButton(Plugin plugin) {
            if (plugin == null) {
                setEnabled(false);
            } else {
                setEnabled(true);
                if (getController().getPluginManager().isEnabled(plugin)) {
                    putValue(NAME, Translation
                        .getTranslation("plugin_disable.name"));
                    putValue(SHORT_DESCRIPTION, Translation
                        .getTranslation("plugin_disable.description"));
                    putValue(ACCELERATOR_KEY, Translation
                        .getTranslation("plugin_disable.key"));
                } else {
                    putValue(NAME, Translation
                        .getTranslation("plugin_enable.name"));
                    putValue(SHORT_DESCRIPTION, Translation
                        .getTranslation("plugin_enable.description"));
                    putValue(ACCELERATOR_KEY, Translation
                        .getTranslation("plugin_enable.key"));
                }
            }
        }

    }

    private class SettingsAction extends SelectionBaseAction {

        public SettingsAction(Controller controller,
            SelectionModel selectionModel)
        {
            super("pluginsettings", controller, selectionModel);
            setEnabled(false);
        }

        public void selectionChanged(SelectionChangeEvent event) {
            Plugin plugin = (Plugin) event.getSelection();
            if (plugin != null) {
                setEnabled(getController().getPluginManager().isEnabled(
                    plugin) && plugin.hasOptionsDialog());
            }
            
        }

        public void actionPerformed(ActionEvent e) {
            int index = pluginJTable.getSelectedRow();
            PluginManager pluginManager = getController().getPluginManager();
            Plugin plugin = pluginManager.getPlugins().get(index);
            if (plugin.hasOptionsDialog()) {
                plugin.showOptionsDialog(preferencesDialog.getDialog());
            }
        }

    }

    /**
     * updates the SelectionModel if some selection has changed in the plugin
     * table
     */
    private class PluginTableListSelectionListener implements
        ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e) {
            int[] selectedRows = pluginJTable.getSelectedRows();
            if (selectedRows.length != 0 && !e.getValueIsAdjusting()) {
                Object[] selectedObjects = new Object[selectedRows.length];
                for (int i = 0; i < selectedRows.length; i++) {
                    selectedObjects[i] = pluginJTable.getModel().getValueAt(
                        selectedRows[i], 0);
                }
                selectionModel.setSelections(selectedObjects);
            } else {
                selectionModel.setSelection(null);
            }
        }
    }
    
    public void pluginStatusChanged(PluginEvent pluginEvent) {
        Plugin plugin = pluginEvent.getPlugin();
        List<Plugin> plugins = getController().getPluginManager().getPlugins();
        int index = plugins.indexOf(plugin);
        ((PluginTableModel)pluginJTable.getModel()).fireTableRowsUpdated(index, index);
        settingsButton.setEnabled(getController().getPluginManager().isEnabled(plugin));
    }
}
