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
import slash.navigation.common.BoundingBox;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.RouteConverterOffline;
import slash.navigation.converter.gui.actions.DisplayMapAction;
import slash.navigation.converter.gui.actions.DownloadMapsAction;
import slash.navigation.converter.gui.helpers.AutomaticElevationService;
import slash.navigation.converter.gui.helpers.AvailableMapsTablePopupMenu;
import slash.navigation.converter.gui.helpers.DownloadableMapsTablePopupMenu;
import slash.navigation.converter.gui.renderer.LocalMapsTableCellRenderer;
import slash.navigation.converter.gui.renderer.RemoteMapsTableCellRenderer;
import slash.navigation.converter.gui.renderer.SimpleHeaderRenderer;
import slash.navigation.download.Checksum;
import slash.navigation.elevation.ElevationService;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.DialogAction;
import slash.navigation.maps.LocalMap;
import slash.navigation.maps.MapManager;
import slash.navigation.maps.RemoteMap;
import slash.navigation.maps.impl.LocalMapsTableModel;
import slash.navigation.routing.RoutingService;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.text.MessageFormat.format;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatSize;
import static slash.navigation.gui.helpers.JMenuHelper.registerAction;
import static slash.navigation.gui.helpers.JTableHelper.scrollToPosition;
import static slash.navigation.gui.helpers.UIHelper.getMaxWidth;
import static slash.navigation.maps.impl.RemoteMapsTableModel.*;

/**
 * Dialog to show available and downloadable maps of the program.
 *
 * @author Christian Pesch
 */

public class MapsDialog extends SimpleDialog {
    private JPanel contentPane;
    private JTable tableAvailableMaps;
    private JButton buttonDisplay;
    private JTable tableDownloadableMaps;
    private JButton buttonDownload;
    private JButton buttonClose;
    private JCheckBox checkBoxDownloadElevationData;
    private JCheckBox checkBoxDownloadRoutingData;

    public MapsDialog() {
        super(RouteConverter.getInstance().getFrame(), "maps");
        setTitle(RouteConverter.getBundle().getString("maps-title"));
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonClose);

        final RouteConverter r = RouteConverter.getInstance();

        tableAvailableMaps.setModel(getMapManager().getAvailableMapsModel());
        tableAvailableMaps.setDefaultRenderer(Object.class, new LocalMapsTableCellRenderer());
        TableCellRenderer availableMapsHeaderRenderer = new SimpleHeaderRenderer("description");
        TableColumnModel mapsColumns = tableAvailableMaps.getColumnModel();
        for (int i = 0; i < mapsColumns.getColumnCount(); i++) {
            TableColumn column = mapsColumns.getColumn(i);
            column.setHeaderRenderer(availableMapsHeaderRenderer);
        }
        TableRowSorter<TableModel> sorterAvailableMaps = new TableRowSorter<>(tableAvailableMaps.getModel());
        sorterAvailableMaps.setSortsOnUpdates(true);
        sorterAvailableMaps.setComparator(LocalMapsTableModel.DESCRIPTION_COLUMN, new Comparator<LocalMap>() {
            public int compare(LocalMap m1, LocalMap m2) {
                return m1.getDescription().compareToIgnoreCase(m2.getDescription());
            }
        });
        tableAvailableMaps.setRowSorter(sorterAvailableMaps);
        final LocalMap selectedMap = getMapManager().getDisplayedMapModel().getItem();
        if (selectedMap != null) {
            int selectedMapIndex = getMapManager().getAvailableMapsModel().getIndex(selectedMap);
            if (selectedMapIndex != -1) {
                int selectedRow = tableAvailableMaps.convertRowIndexToView(selectedMapIndex);
                tableAvailableMaps.getSelectionModel().addSelectionInterval(selectedRow, selectedRow);
                scrollToPosition(tableAvailableMaps, selectedRow);
            }
        }
        tableAvailableMaps.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                int selectedRow = tableAvailableMaps.getSelectedRow();
                if (selectedRow == -1)
                    return;
                int row = tableAvailableMaps.convertRowIndexToView(selectedRow);
                LocalMap map = getMapManager().getAvailableMapsModel().getMap(row);
                r.showMapBorder(map.isVector() ? map.getBoundingBox() : null);
            }
        });

        tableDownloadableMaps.setModel(getMapManager().getDownloadableMapsModel());
        tableDownloadableMaps.setDefaultRenderer(Object.class, new RemoteMapsTableCellRenderer());
        TableCellRenderer downloadableMapsHeaderRenderer = new SimpleHeaderRenderer("datasource", "description", "size");
        TableColumnModel downloadableMapsColumns = tableDownloadableMaps.getColumnModel();
        for (int i = 0; i < downloadableMapsColumns.getColumnCount(); i++) {
            TableColumn column = downloadableMapsColumns.getColumn(i);
            column.setHeaderRenderer(downloadableMapsHeaderRenderer);
            if (i == 0) {
                int width = getMaxWidth("RouteConverter Maps", 13);
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            }
            if (i == 2) {
                int width = getMaxWidth("9999 MByte", 10);
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            }
        }
        TableRowSorter<TableModel> sorterDownloadableMaps = new TableRowSorter<>(tableDownloadableMaps.getModel());
        sorterDownloadableMaps.setSortsOnUpdates(true);
        sorterDownloadableMaps.setComparator(DATASOURCE_COLUMN, new Comparator<RemoteMap>() {
            public int compare(RemoteMap m1, RemoteMap m2) {
                return m1.getDataSource().getName().compareToIgnoreCase(m2.getDataSource().getName());
            }
        });
        sorterDownloadableMaps.setComparator(DESCRIPTION_COLUMN, new Comparator<RemoteMap>() {
            public int compare(RemoteMap m1, RemoteMap m2) {
                return m1.getDownloadable().getUri().compareToIgnoreCase(m2.getDownloadable().getUri());
            }
        });
        sorterDownloadableMaps.setComparator(SIZE_COLUMN, new Comparator<RemoteMap>() {
            private long getSize(RemoteMap map) {
                Checksum checksum = map.getDownloadable().getLatestChecksum();
                return checksum != null && checksum.getContentLength() != null ? checksum.getContentLength() : 0L;
            }

            public int compare(RemoteMap m1, RemoteMap m2) {
                return (int) (getSize(m1) - getSize(m2));
            }
        });
        tableDownloadableMaps.setRowSorter(sorterDownloadableMaps);
        tableDownloadableMaps.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                int selectedRow = tableDownloadableMaps.getSelectedRow();
                if (selectedRow == -1)
                    return;
                int row = tableDownloadableMaps.convertRowIndexToView(selectedRow);
                RemoteMap map = getMapManager().getDownloadableMapsModel().getMap(row);
                r.showMapBorder(map.getBoundingBox());
                updateLabel();
            }
        });

        updateLabel();

        final ActionManager actionManager = r.getContext().getActionManager();
        actionManager.register("display-map", new DisplayMapAction(tableAvailableMaps, getMapManager()));
        actionManager.register("download-maps", new DownloadMapsAction(tableDownloadableMaps, getMapManager(),
                checkBoxDownloadRoutingData, checkBoxDownloadElevationData));

        new AvailableMapsTablePopupMenu(tableAvailableMaps).createPopupMenu();
        new DownloadableMapsTablePopupMenu(tableDownloadableMaps).createPopupMenu();
        registerAction(buttonDisplay, "display-map");
        registerAction(buttonDownload, "download-maps");

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

    private List<BoundingBox> getSelectedBoundingBoxes() {
        List<BoundingBox> result = new ArrayList<>();
        int[] selectedRows = tableDownloadableMaps.getSelectedRows();
        for (int selectedRow : selectedRows) {
            int row = tableDownloadableMaps.convertRowIndexToModel(selectedRow);
            RemoteMap map = getMapManager().getDownloadableMapsModel().getMap(row);
            BoundingBox boundingBox = map.getBoundingBox();
            if (boundingBox != null)
                result.add(boundingBox);
        }
        return result;
    }

    private void updateLabel() {
        RouteConverter r = RouteConverter.getInstance();
        List<BoundingBox> selectedBoundingBoxes = getSelectedBoundingBoxes();

        RoutingService routingService = r.getRoutingServiceFacade().getRoutingService();
        long routingServiceDownloadSize = routingService.isDownload() ?
                routingService.calculateRemainingDownloadSize(selectedBoundingBoxes) : 0;
        checkBoxDownloadRoutingData.setEnabled(routingServiceDownloadSize > 0);
        checkBoxDownloadRoutingData.setSelected(checkBoxDownloadRoutingData.isEnabled());
        checkBoxDownloadRoutingData.setText(format(RouteConverter.getBundle().getString("download-routing-data"),
                formatSize(routingServiceDownloadSize), routingService.getName()));

        ElevationService elevationService = r.getElevationServiceFacade().getElevationService();
        long elevationServiceDownloadSize = elevationService.isDownload() ?
                elevationService.calculateRemainingDownloadSize(selectedBoundingBoxes) : 0;
        checkBoxDownloadElevationData.setEnabled(elevationServiceDownloadSize > 0);
        checkBoxDownloadElevationData.setSelected(checkBoxDownloadElevationData.isEnabled());
        String elevationServiceName = elevationService instanceof AutomaticElevationService ?
                AutomaticElevationService.class.cast(elevationService).getPreferredDownloadName() :
                elevationService.getName();
        checkBoxDownloadElevationData.setText(format(RouteConverter.getBundle().getString("download-elevation-data"),
                formatSize(elevationServiceDownloadSize), elevationServiceName));
    }

    private MapManager getMapManager() {
        return ((RouteConverterOffline) RouteConverter.getInstance()).getMapManager();
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
        contentPane.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonDownload = new JButton();
        this.$$$loadButtonText$$$(buttonDownload, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("download-maps-action"));
        buttonDownload.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("download-maps-action-tooltip"));
        panel2.add(buttonDownload, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        checkBoxDownloadElevationData = new JCheckBox();
        checkBoxDownloadElevationData.setSelected(true);
        panel1.add(checkBoxDownloadElevationData, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxDownloadRoutingData = new JCheckBox();
        checkBoxDownloadRoutingData.setSelected(true);
        panel1.add(checkBoxDownloadRoutingData, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("download-complete-coverage"));
        panel1.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel1.add(separator1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 1, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("my-maps"));
        panel4.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("downloadable-maps"));
        panel4.add(label3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonDisplay = new JButton();
        this.$$$loadButtonText$$$(buttonDisplay, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-map-action"));
        buttonDisplay.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-map-action-tooltip"));
        panel6.add(buttonDisplay, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel6.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        panel4.add(panel7, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel4.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableAvailableMaps = new JTable();
        tableAvailableMaps.setPreferredScrollableViewportSize(new Dimension(400, 120));
        tableAvailableMaps.setShowHorizontalLines(false);
        tableAvailableMaps.setShowVerticalLines(false);
        scrollPane1.setViewportView(tableAvailableMaps);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel4.add(scrollPane2, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableDownloadableMaps = new JTable();
        tableDownloadableMaps.setPreferredScrollableViewportSize(new Dimension(400, 200));
        tableDownloadableMaps.setShowHorizontalLines(false);
        tableDownloadableMaps.setShowVerticalLines(false);
        scrollPane2.setViewportView(tableDownloadableMaps);
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(3, 0, 0, 0), -1, -1));
        contentPane.add(panel8, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel9, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel9.add(spacer3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel9.add(panel10, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonClose = new JButton();
        this.$$$loadButtonText$$$(buttonClose, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("close"));
        panel10.add(buttonClose, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
