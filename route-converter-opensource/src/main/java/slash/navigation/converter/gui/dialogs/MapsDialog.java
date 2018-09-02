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
import slash.navigation.converter.gui.RouteConverterOpenSource;
import slash.navigation.converter.gui.actions.DisplayMapAction;
import slash.navigation.converter.gui.actions.DownloadMapsAction;
import slash.navigation.converter.gui.helpers.AutomaticElevationService;
import slash.navigation.converter.gui.helpers.AvailableOfflineMapsTablePopupMenu;
import slash.navigation.converter.gui.helpers.AvailableOnlineMapsTablePopupMenu;
import slash.navigation.converter.gui.helpers.DownloadableMapsTablePopupMenu;
import slash.navigation.converter.gui.renderer.LocalMapTableCellRenderer;
import slash.navigation.converter.gui.renderer.RemoteMapTableCellRenderer;
import slash.navigation.converter.gui.renderer.SimpleHeaderRenderer;
import slash.navigation.converter.gui.renderer.TileMapTableCellRenderer;
import slash.navigation.download.Checksum;
import slash.navigation.elevation.ElevationService;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.DialogAction;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.mapsforge.RemoteMap;
import slash.navigation.maps.mapsforge.impl.TileMap;
import slash.navigation.maps.mapsforge.models.TileMapTableModel;
import slash.navigation.routing.RoutingService;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
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

/**
 * Dialog to show available and downloadable maps of the program.
 *
 * @author Christian Pesch
 */

public class MapsDialog extends SimpleDialog {
    private JPanel contentPane;
    private JTable tableAvailableOnlineMaps;
    private JButton buttonDisplayOnlineMap;
    private JTable tableAvailableOfflineMaps;
    private JButton buttonDisplayOfflineMap;
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

        tableAvailableOnlineMaps.setModel(getMapsforgeMapManager().getAvailableOnlineMapsModel());
        tableAvailableOnlineMaps.setDefaultRenderer(Object.class, new TileMapTableCellRenderer());
        TableCellRenderer availableMapsHeaderRenderer = new SimpleHeaderRenderer("description", "active");
        TableColumnModel onlineMapsColumns = tableAvailableOnlineMaps.getColumnModel();
        for (int i = 0; i < onlineMapsColumns.getColumnCount(); i++) {
            TableColumn column = onlineMapsColumns.getColumn(i);
            column.setHeaderRenderer(availableMapsHeaderRenderer);
            if (i == 1) {
                int width = 30;
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            }
        }
        TableRowSorter<TableModel> sorterAvailableMaps = new TableRowSorter<>(tableAvailableOnlineMaps.getModel());
        sorterAvailableMaps.setSortsOnUpdates(true);
        sorterAvailableMaps.setComparator(TileMapTableModel.DESCRIPTION_COLUMN, new Comparator<TileMap>() {
            public int compare(TileMap m1, TileMap m2) {
                return m1.getDescription().compareToIgnoreCase(m2.getDescription());
            }
        });
        sorterAvailableMaps.setComparator(TileMapTableModel.ACTIVE_COLUMN, new Comparator<Boolean>() {
            public int compare(Boolean b1, Boolean b2) {
                return b1.compareTo(b2);
            }
        });
        tableAvailableOnlineMaps.setRowSorter(sorterAvailableMaps);
        tableAvailableOnlineMaps.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                int selectedRow = tableAvailableOnlineMaps.getSelectedRow();
                if (selectedRow == -1)
                    return;
                int row = tableAvailableOnlineMaps.convertRowIndexToView(selectedRow);
                LocalMap map = getMapsforgeMapManager().getAvailableOnlineMapsModel().getItem(row);
                r.showMapBorder(map.isVector() ? map.getBoundingBox() : null);
            }
        });

        tableAvailableOfflineMaps.setModel(getMapsforgeMapManager().getAvailableOfflineMapsModel());
        tableAvailableOfflineMaps.setDefaultRenderer(Object.class, new LocalMapTableCellRenderer());
        TableColumnModel offlineMapsColumns = tableAvailableOfflineMaps.getColumnModel();
        for (int i = 0; i < offlineMapsColumns.getColumnCount(); i++) {
            TableColumn column = offlineMapsColumns.getColumn(i);
            column.setHeaderRenderer(availableMapsHeaderRenderer);
        }
        TableRowSorter<TableModel> sorterAvailableOfflineMaps = new TableRowSorter<>(tableAvailableOfflineMaps.getModel());
        sorterAvailableOfflineMaps.setSortsOnUpdates(true);
        sorterAvailableOfflineMaps.setComparator(LocalMapTableCellRenderer.DESCRIPTION_COLUMN, new Comparator<LocalMap>() {
            public int compare(LocalMap m1, LocalMap m2) {
                return m1.getDescription().compareToIgnoreCase(m2.getDescription());
            }
        });
        tableAvailableOfflineMaps.setRowSorter(sorterAvailableOfflineMaps);
        final LocalMap selectedMap = getMapsforgeMapManager().getDisplayedMapModel().getItem();
        if (selectedMap != null) {
            int selectedMapIndex = getMapsforgeMapManager().getAvailableOfflineMapsModel().getIndex(selectedMap);
            if (selectedMapIndex != -1) {
                int selectedRow = tableAvailableOfflineMaps.convertRowIndexToView(selectedMapIndex);
                tableAvailableOfflineMaps.getSelectionModel().addSelectionInterval(selectedRow, selectedRow);
                scrollToPosition(tableAvailableOfflineMaps, selectedRow);
            }
        }
        tableAvailableOfflineMaps.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                int selectedRow = tableAvailableOfflineMaps.getSelectedRow();
                if (selectedRow == -1)
                    return;
                int row = tableAvailableOfflineMaps.convertRowIndexToView(selectedRow);
                LocalMap map = getMapsforgeMapManager().getAvailableOfflineMapsModel().getItem(row);
                r.showMapBorder(map.isVector() ? map.getBoundingBox() : null);
            }
        });

        tableDownloadableMaps.setModel(getMapsforgeMapManager().getDownloadableMapsModel());
        tableDownloadableMaps.setDefaultRenderer(Object.class, new RemoteMapTableCellRenderer());
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
        sorterDownloadableMaps.setComparator(RemoteMapTableCellRenderer.DATASOURCE_COLUMN, new Comparator<RemoteMap>() {
            public int compare(RemoteMap m1, RemoteMap m2) {
                return m1.getDataSource().getName().compareToIgnoreCase(m2.getDataSource().getName());
            }
        });
        sorterDownloadableMaps.setComparator(RemoteMapTableCellRenderer.DESCRIPTION_COLUMN, new Comparator<RemoteMap>() {
            public int compare(RemoteMap m1, RemoteMap m2) {
                return m1.getDescription().compareToIgnoreCase(m2.getDescription());
            }
        });
        sorterDownloadableMaps.setComparator(RemoteMapTableCellRenderer.SIZE_COLUMN, new Comparator<RemoteMap>() {
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
                RemoteMap map = getMapsforgeMapManager().getDownloadableMapsModel().getItem(row);
                r.showMapBorder(map.getBoundingBox());
                updateLabel();
            }
        });

        updateLabel();

        final ActionManager actionManager = r.getContext().getActionManager();
        actionManager.register("display-online-map", new DisplayMapAction(tableAvailableOnlineMaps, getMapsforgeMapManager()));
        new AvailableOnlineMapsTablePopupMenu(tableAvailableOnlineMaps).createPopupMenu();
        registerAction(buttonDisplayOnlineMap, "display-online-map");

        actionManager.register("display-offline-map", new DisplayMapAction(tableAvailableOfflineMaps, getMapsforgeMapManager()));
        actionManager.register("download-maps", new DownloadMapsAction(tableDownloadableMaps, getMapsforgeMapManager(),
                checkBoxDownloadRoutingData, checkBoxDownloadElevationData));
        new AvailableOfflineMapsTablePopupMenu(tableAvailableOfflineMaps).createPopupMenu();
        new DownloadableMapsTablePopupMenu(tableDownloadableMaps).createPopupMenu();
        registerAction(buttonDisplayOfflineMap, "display-offline-map");
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
            RemoteMap map = getMapsforgeMapManager().getDownloadableMapsModel().getItem(row);
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

    private MapsforgeMapManager getMapsforgeMapManager() {
        return ((RouteConverterOpenSource) RouteConverter.getInstance()).getMapsforgeMapManager();
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
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonClose = new JButton();
        this.$$$loadButtonText$$$(buttonClose, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("close"));
        panel2.add(buttonClose, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JTabbedPane tabbedPane1 = new JTabbedPane();
        contentPane.add(tabbedPane1, new GridConstraints(0, 0, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("offline-tab"), panel3);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonDownload = new JButton();
        this.$$$loadButtonText$$$(buttonDownload, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("download-maps-action"));
        buttonDownload.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("download-maps-action-tooltip"));
        panel5.add(buttonDownload, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel5.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        checkBoxDownloadElevationData = new JCheckBox();
        checkBoxDownloadElevationData.setSelected(true);
        panel4.add(checkBoxDownloadElevationData, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxDownloadRoutingData = new JCheckBox();
        checkBoxDownloadRoutingData.setSelected(true);
        panel4.add(checkBoxDownloadRoutingData, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("download-complete-coverage"));
        panel4.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel4.add(separator1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(6, 0, 0, 0), -1, -1));
        panel4.add(panel6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 1, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("my-maps"));
        panel7.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("available-download-maps"));
        panel7.add(label3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel8, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonDisplayOfflineMap = new JButton();
        this.$$$loadButtonText$$$(buttonDisplayOfflineMap, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-offline-map-action"));
        buttonDisplayOfflineMap.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-offline-map-action-tooltip"));
        panel9.add(buttonDisplayOfflineMap, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel9.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        panel7.add(panel10, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel7.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableAvailableOfflineMaps = new JTable();
        tableAvailableOfflineMaps.setPreferredScrollableViewportSize(new Dimension(400, 120));
        tableAvailableOfflineMaps.setShowHorizontalLines(false);
        tableAvailableOfflineMaps.setShowVerticalLines(false);
        scrollPane1.setViewportView(tableAvailableOfflineMaps);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel7.add(scrollPane2, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableDownloadableMaps = new JTable();
        tableDownloadableMaps.setPreferredScrollableViewportSize(new Dimension(400, 200));
        tableDownloadableMaps.setShowHorizontalLines(false);
        tableDownloadableMaps.setShowVerticalLines(false);
        scrollPane2.setViewportView(tableDownloadableMaps);
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 1, new Insets(3, 0, 0, 0), -1, -1));
        panel3.add(panel11, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("online-tab"), panel12);
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("available-online-maps"));
        panel12.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        panel12.add(scrollPane3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableAvailableOnlineMaps = new JTable();
        tableAvailableOnlineMaps.setPreferredScrollableViewportSize(new Dimension(400, 120));
        tableAvailableOnlineMaps.setShowHorizontalLines(false);
        tableAvailableOnlineMaps.setShowVerticalLines(false);
        scrollPane3.setViewportView(tableAvailableOnlineMaps);
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel12.add(panel13, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonDisplayOnlineMap = new JButton();
        this.$$$loadButtonText$$$(buttonDisplayOnlineMap, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-online-map-action"));
        buttonDisplayOnlineMap.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-online-map-action-tooltip"));
        panel13.add(buttonDisplayOnlineMap, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel13.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
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
