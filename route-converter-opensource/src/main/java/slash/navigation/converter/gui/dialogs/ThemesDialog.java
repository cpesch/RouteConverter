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
import slash.navigation.converter.gui.RouteConverterOpenSource;
import slash.navigation.converter.gui.actions.ApplyThemeAction;
import slash.navigation.converter.gui.actions.DownloadThemesAction;
import slash.navigation.converter.gui.helpers.AvailableThemesTablePopupMenu;
import slash.navigation.converter.gui.helpers.DownloadableThemesTablePopupMenu;
import slash.navigation.converter.gui.renderer.LocalThemeTableCellRenderer;
import slash.navigation.converter.gui.renderer.RemoteThemeTableCellRenderer;
import slash.navigation.converter.gui.renderer.SimpleHeaderRenderer;
import slash.navigation.download.Checksum;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.DialogAction;
import slash.navigation.maps.mapsforge.LocalTheme;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.mapsforge.RemoteTheme;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Comparator;
import java.util.ResourceBundle;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.navigation.gui.helpers.JMenuHelper.registerAction;
import static slash.navigation.gui.helpers.JTableHelper.scrollToPosition;
import static slash.navigation.gui.helpers.UIHelper.getMaxWidth;

/**
 * Dialog to show available and downloadable themes of the program.
 *
 * @author Christian Pesch
 */

public class ThemesDialog extends SimpleDialog {
    private JPanel contentPane;
    private JTable tableAvailableThemes;
    private JButton buttonApply;
    private JTable tableDownloadableThemes;
    private JButton buttonDownload;
    private JButton buttonClose;

    public ThemesDialog() {
        super(RouteConverter.getInstance().getFrame(), "themes");
        setTitle(RouteConverter.getBundle().getString("themes-title"));
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonClose);

        final RouteConverter r = RouteConverter.getInstance();

        tableAvailableThemes.setModel(getMapsforgeMapManager().getAvailableThemesModel());
        tableAvailableThemes.setDefaultRenderer(Object.class, new LocalThemeTableCellRenderer());
        TableCellRenderer availableThemesHeaderRenderer = new SimpleHeaderRenderer("description");
        TableColumnModel themesColumns = tableAvailableThemes.getColumnModel();
        for (int i = 0; i < themesColumns.getColumnCount(); i++) {
            TableColumn column = themesColumns.getColumn(i);
            column.setHeaderRenderer(availableThemesHeaderRenderer);
        }
        TableRowSorter<TableModel> sorterAvailableThemes = new TableRowSorter<>(tableAvailableThemes.getModel());
        sorterAvailableThemes.setSortsOnUpdates(true);
        sorterAvailableThemes.setComparator(LocalThemeTableCellRenderer.DESCRIPTION_COLUMN, new Comparator<LocalTheme>() {
            public int compare(LocalTheme t1, LocalTheme t2) {
                return t1.getDescription().compareToIgnoreCase(t2.getDescription());
            }
        });
        tableAvailableThemes.setRowSorter(sorterAvailableThemes);
        LocalTheme selectedTheme = getMapsforgeMapManager().getAppliedThemeModel().getItem();
        if (selectedTheme != null) {
            int selectedThemeIndex = getMapsforgeMapManager().getAvailableThemesModel().getIndex(selectedTheme);
            if (selectedThemeIndex != -1) {
                int selectedRow = tableAvailableThemes.convertRowIndexToView(selectedThemeIndex);
                tableAvailableThemes.getSelectionModel().addSelectionInterval(selectedRow, selectedRow);
                scrollToPosition(tableAvailableThemes, selectedRow);
            }
        }

        tableDownloadableThemes.setModel(getMapsforgeMapManager().getDownloadableThemesModel());
        tableDownloadableThemes.setDefaultRenderer(Object.class, new RemoteThemeTableCellRenderer());
        TableCellRenderer downloadableThemesHeaderRenderer = new SimpleHeaderRenderer("datasource", "description", "size");
        TableColumnModel downloadableThemesColumns = tableDownloadableThemes.getColumnModel();
        for (int i = 0; i < downloadableThemesColumns.getColumnCount(); i++) {
            TableColumn column = downloadableThemesColumns.getColumn(i);
            column.setHeaderRenderer(downloadableThemesHeaderRenderer);
            if (i == 0) {
                int width = getMaxWidth("Openandromaps Themes", 13);
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            }
            if (i == 2) {
                int width = getMaxWidth("9999 MByte", 10);
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            }
        }
        TableRowSorter<TableModel> sorterResources = new TableRowSorter<>(tableDownloadableThemes.getModel());
        sorterResources.setSortsOnUpdates(true);
        sorterResources.setComparator(RemoteThemeTableCellRenderer.DATASOURCE_COLUMN, new Comparator<RemoteTheme>() {
            public int compare(RemoteTheme t1, RemoteTheme t2) {
                return t1.getDataSource().getName().compareToIgnoreCase(t2.getDataSource().getName());
            }
        });
        sorterResources.setComparator(RemoteThemeTableCellRenderer.DESCRIPTION_COLUMN, new Comparator<RemoteTheme>() {
            public int compare(RemoteTheme t1, RemoteTheme t2) {
                return t1.getDescription().compareToIgnoreCase(t2.getDescription());
            }
        });
        sorterResources.setComparator(RemoteThemeTableCellRenderer.SIZE_COLUMN, new Comparator<RemoteTheme>() {
            private long getSize(RemoteTheme theme) {
                Checksum checksum = theme.getDownloadable().getLatestChecksum();
                return checksum != null && checksum.getContentLength() != null ? checksum.getContentLength() : 0L;
            }

            public int compare(RemoteTheme t1, RemoteTheme t2) {
                return (int) (getSize(t1) - getSize(t2));
            }
        });
        tableDownloadableThemes.setRowSorter(sorterResources);

        final ActionManager actionManager = r.getContext().getActionManager();
        actionManager.register("apply-theme", new ApplyThemeAction(tableAvailableThemes, getMapsforgeMapManager()));
        actionManager.register("download-themes", new DownloadThemesAction(tableDownloadableThemes, getMapsforgeMapManager()));

        new AvailableThemesTablePopupMenu(tableAvailableThemes).createPopupMenu();
        new DownloadableThemesTablePopupMenu(tableDownloadableThemes).createPopupMenu();
        registerAction(buttonApply, "apply-theme");
        registerAction(buttonDownload, "download-themes");

        buttonClose.addActionListener(new DialogAction(this) {
            public void run() {
                close();
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

    private MapsforgeMapManager getMapsforgeMapManager() {
        return ((RouteConverterOpenSource) RouteConverter.getInstance()).getMapsforgeMapManager();
    }

    private void close() {
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
        contentPane.setLayout(new GridLayoutManager(5, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonClose = new JButton();
        this.$$$loadButtonText$$$(buttonClose, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("close"));
        panel2.add(buttonClose, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("my-themes"));
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableAvailableThemes = new JTable();
        tableAvailableThemes.setPreferredScrollableViewportSize(new Dimension(400, 120));
        tableAvailableThemes.setShowHorizontalLines(false);
        tableAvailableThemes.setShowVerticalLines(false);
        scrollPane1.setViewportView(tableAvailableThemes);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonApply = new JButton();
        this.$$$loadButtonText$$$(buttonApply, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("apply-theme-action"));
        buttonApply.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("apply-theme-action-tooltip"));
        panel5.add(buttonApply, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel5.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("downloadable-themes"));
        contentPane.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        contentPane.add(scrollPane2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableDownloadableThemes = new JTable();
        tableDownloadableThemes.setPreferredScrollableViewportSize(new Dimension(400, 200));
        tableDownloadableThemes.setShowHorizontalLines(false);
        tableDownloadableThemes.setShowVerticalLines(false);
        scrollPane2.setViewportView(tableDownloadableThemes);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel6, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonDownload = new JButton();
        this.$$$loadButtonText$$$(buttonDownload, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("download-maps-action"));
        buttonDownload.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("download-themes-action-tooltip"));
        panel6.add(buttonDownload, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel6.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
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
