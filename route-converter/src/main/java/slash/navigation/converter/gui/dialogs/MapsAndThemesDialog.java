/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.converter.gui.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.renderer.MapsTableCellRenderer;
import slash.navigation.converter.gui.renderer.ResourcesTableCellRenderer;
import slash.navigation.converter.gui.renderer.SimpleHeaderRenderer;
import slash.navigation.converter.gui.renderer.ThemesTableCellRenderer;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.DialogAction;
import slash.navigation.maps.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.text.MessageFormat.format;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.io.Files.printArrayToDialogString;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startBrowserForHomepage;
import static slash.navigation.gui.helpers.UIHelper.getMaxWidth;

/**
 * Dialog to show maps, themes and resources of the program.
 *
 * @author Christian Pesch
 */

public class MapsAndThemesDialog extends SimpleDialog {
    private JPanel contentPane;
    private JTable tableAvailableMaps;
    private JButton buttonDisplay;
    private JTable tableAvailableThemes;
    private JButton buttonApply;
    private JTable tableResources;
    private JButton buttonDownload;
    private JButton buttonClose;
    private JLabel labelMessage;

    private ExecutorService executor = newCachedThreadPool();

    public MapsAndThemesDialog() {
        super(RouteConverter.getInstance().getFrame(), "maps");
        setTitle(RouteConverter.getBundle().getString("maps-title"));
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonClose);

        final RouteConverter r = RouteConverter.getInstance();

        tableAvailableMaps.setModel(getMapManager().getMapsModel());
        tableAvailableMaps.setDefaultRenderer(Object.class, new MapsTableCellRenderer());
        TableCellRenderer availableMapsHeaderRenderer = new SimpleHeaderRenderer("description", "offline");
        TableColumnModel mapsColumns = tableAvailableMaps.getColumnModel();
        for (int i = 0; i < mapsColumns.getColumnCount(); i++) {
            TableColumn column = mapsColumns.getColumn(i);
            column.setHeaderRenderer(availableMapsHeaderRenderer);
            if (i == 1) {
                int width = getMaxWidth("offline", 6);
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            }
        }
        TableRowSorter<TableModel> sorterAvailableMaps = new TableRowSorter<TableModel>(tableAvailableMaps.getModel());
        sorterAvailableMaps.setSortsOnUpdates(true);
        sorterAvailableMaps.setComparator(0, new Comparator<LocalMap>() {
            public int compare(LocalMap m1, LocalMap m2) {
                return m1.getDescription().compareToIgnoreCase(m2.getDescription());
            }
        });
        sorterAvailableMaps.setComparator(1, new Comparator<LocalMap>() {
            public int compare(LocalMap m1, LocalMap m2) {
                return m1.isRenderer() ? m2.isRenderer() ? 0 : -1 : 1;
            }
        });
        tableAvailableMaps.setRowSorter(sorterAvailableMaps);
        LocalMap selectedMap = getMapManager().getDisplayedMapModel().getItem();
        if (selectedMap != null) {
            int selectedMapIndex = getMapManager().getMapsModel().getIndex(selectedMap);
            if (selectedMapIndex != -1) {
                int selectedRow = tableAvailableMaps.convertRowIndexToView(selectedMapIndex);
                tableAvailableMaps.getSelectionModel().addSelectionInterval(selectedRow, selectedRow);
            }
        }
        tableAvailableMaps.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                int selectedRow = tableAvailableMaps.convertRowIndexToView(tableAvailableMaps.getSelectedRow());
                LocalMap map = getMapManager().getMapsModel().getMap(selectedRow);
                r.showMapBorder(map.isRenderer() ? map.getBoundingBox() : null);
            }
        });

        buttonDisplay.addActionListener(new DialogAction(this) {
            public void run() {
                display();
            }
        });

        tableAvailableThemes.setModel(getMapManager().getThemesModel());
        tableAvailableThemes.setDefaultRenderer(Object.class, new ThemesTableCellRenderer());
        TableCellRenderer availableThemesHeaderRenderer = new SimpleHeaderRenderer("description");
        TableColumnModel themesColumns = tableAvailableThemes.getColumnModel();
        for (int i = 0; i < themesColumns.getColumnCount(); i++) {
            TableColumn column = themesColumns.getColumn(i);
            column.setHeaderRenderer(availableThemesHeaderRenderer);
        }
        TableRowSorter<TableModel> sorterAvailableThemes = new TableRowSorter<TableModel>(tableAvailableThemes.getModel());
        sorterAvailableThemes.setSortsOnUpdates(true);
        sorterAvailableThemes.setComparator(0, new Comparator<Theme>() {
            public int compare(Theme t1, Theme t2) {
                return t1.getDescription().compareToIgnoreCase(t2.getDescription());
            }
        });
        tableAvailableThemes.setRowSorter(sorterAvailableThemes);
        Theme selectedTheme = getMapManager().getAppliedThemeModel().getItem();
        if (selectedTheme != null) {
            int selectedThemeIndex = getMapManager().getThemesModel().getIndex(selectedTheme);
            if (selectedThemeIndex != -1) {
                int selectedRow = tableAvailableThemes.convertRowIndexToView(selectedThemeIndex);
                tableAvailableThemes.getSelectionModel().addSelectionInterval(selectedRow, selectedRow);
            }
        }

        buttonApply.addActionListener(new DialogAction(this) {
            public void run() {
                apply();
            }
        });

        tableResources.setModel(getMapManager().getResourcesModel());
        tableResources.setDefaultRenderer(Object.class, new ResourcesTableCellRenderer());
        TableCellRenderer resourcesHeaderRenderer = new SimpleHeaderRenderer("datasource", "description", "size");
        TableColumnModel resourcesColumns = tableResources.getColumnModel();
        for (int i = 0; i < resourcesColumns.getColumnCount(); i++) {
            TableColumn column = resourcesColumns.getColumn(i);
            column.setHeaderRenderer(resourcesHeaderRenderer);
            if (i == 0) {
                int width = getMaxWidth("Openandromaps Themes", 13);
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            }
            if (i == 2) {
                int width = getMaxWidth("999 MB", 10);
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            }
        }
        TableRowSorter<TableModel> sorterResources = new TableRowSorter<TableModel>(tableResources.getModel());
        sorterResources.setSortsOnUpdates(true);
        sorterResources.setComparator(0, new Comparator<RemoteResource>() {
            public int compare(RemoteResource r1, RemoteResource r2) {
                return r1.getDataSource().compareToIgnoreCase(r2.getDataSource());
            }
        });
        sorterResources.setComparator(1, new Comparator<RemoteResource>() {
            public int compare(RemoteResource r1, RemoteResource r2) {
                return r1.getUrl().compareToIgnoreCase(r2.getUrl());
            }
        });
        sorterResources.setComparator(2, new Comparator<RemoteResource>() {
            public int compare(RemoteResource r1, RemoteResource r2) {
                return r1.getFile().getSize().intValue() - r2.getFile().getSize().intValue();
            }
        });
        tableResources.setRowSorter(sorterResources);
        tableResources.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                int selectedRow = tableResources.convertRowIndexToView(tableResources.getSelectedRow());
                RemoteResource resource = getMapManager().getResourcesModel().getResource(selectedRow);
                r.showMapBorder(resource instanceof RemoteMap ? ((RemoteMap) resource).getBoundingBox() : null);
            }
        });

        buttonDownload.addActionListener(new DialogAction(this) {
            public void run() {
                download();
            }
        });

        buttonClose.addActionListener(new DialogAction(this) {
            public void run() {
                close();
            }
        });

        labelMessage.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                r.getContext().getActionManager().run("show-downloads");
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        contentPane.registerKeyboardAction(new DialogAction(this) {
            public void run() {
                close();
            }
        }, getKeyStroke(VK_ESCAPE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private MapManager getMapManager() {
        return RouteConverter.getInstance().getMapManager();
    }

    private void display() {
        int selectedRow = tableAvailableMaps.convertRowIndexToView(tableAvailableMaps.getSelectedRow());
        LocalMap map = getMapManager().getMapsModel().getMap(selectedRow);
        getMapManager().getDisplayedMapModel().setItem(map);
        labelMessage.setText(MessageFormat.format(RouteConverter.getBundle().getString("map-displayed"), map.getDescription()));
    }

    private void apply() {
        int selectedRow = tableAvailableThemes.convertRowIndexToModel(tableAvailableThemes.getSelectedRow());
        Theme theme = getMapManager().getThemesModel().getTheme(selectedRow);
        getMapManager().getAppliedThemeModel().setItem(theme);
        labelMessage.setText(MessageFormat.format(RouteConverter.getBundle().getString("theme-applied"), theme.getDescription()));
    }

    private void download() {
        final List<RemoteResource> selectedResources = new ArrayList<RemoteResource>();
        List<String> selectedResourcesNames = new ArrayList<String>();
        for (int selectedRow : tableResources.getSelectedRows()) {
            RemoteResource resource = getMapManager().getResourcesModel().getResource(tableResources.convertRowIndexToModel(selectedRow));
            selectedResources.add(resource);
            selectedResourcesNames.add(resource.getUrl());
        }
        labelMessage.setText(MessageFormat.format(RouteConverter.getBundle().getString("download-started"), printArrayToDialogString(selectedResourcesNames.toArray())));

        executor.execute(new Runnable() {
            public void run() {
                getMapManager().queueForDownload(selectedResources);

                try {
                    getMapManager().scanDirectories();
                } catch (final IOException e) {
                    invokeLater(new Runnable() {
                        public void run() {
                            showMessageDialog(null,
                                    format(RouteConverter.getBundle().getString("scan-error"), e.getMessage()), "Error",
                                    ERROR_MESSAGE);
                        }
                    });
                }
            }
        });
    }

    private void close() {
        RouteConverter.getInstance().showMapBorder(null);
        dispose();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(6, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonDownload = new JButton();
        this.$$$loadButtonText$$$(buttonDownload, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("download"));
        panel2.add(buttonDownload, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(10, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("my-maps"));
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("resources"));
        panel3.add(label2, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonDisplay = new JButton();
        this.$$$loadButtonText$$$(buttonDisplay, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display"));
        panel5.add(buttonDisplay, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel5.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        panel3.add(panel6, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        panel3.add(panel7, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("my-themes"));
        panel3.add(label3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel8, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonApply = new JButton();
        this.$$$loadButtonText$$$(buttonApply, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("apply"));
        panel8.add(buttonApply, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel8.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableAvailableMaps = new JTable();
        tableAvailableMaps.setPreferredScrollableViewportSize(new Dimension(450, 100));
        scrollPane1.setViewportView(tableAvailableMaps);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel3.add(scrollPane2, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableAvailableThemes = new JTable();
        tableAvailableThemes.setPreferredScrollableViewportSize(new Dimension(450, 100));
        scrollPane2.setViewportView(tableAvailableThemes);
        final JScrollPane scrollPane3 = new JScrollPane();
        panel3.add(scrollPane3, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableResources = new JTable();
        tableResources.setPreferredScrollableViewportSize(new Dimension(450, 100));
        scrollPane3.setViewportView(tableResources);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 1, new Insets(5, 0, 0, 0), -1, -1));
        contentPane.add(panel9, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel10, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel10.add(spacer4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel10.add(panel11, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonClose = new JButton();
        this.$$$loadButtonText$$$(buttonClose, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("close"));
        panel11.add(buttonClose, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelMessage = new JLabel();
        contentPane.add(labelMessage, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(1, 1, new Insets(5, 0, 0, 0), -1, -1));
        contentPane.add(panel12, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
