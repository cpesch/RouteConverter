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

package slash.navigation.converter.gui.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import slash.navigation.elevation.ElevationService;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.renderer.DownloadsTableCellHeaderRenderer;
import slash.navigation.converter.gui.renderer.DownloadsTableCellRenderer;
import slash.navigation.converter.gui.renderer.ElevationLookupServiceListCellRenderer;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import static java.awt.event.ItemEvent.SELECTED;

/**
 * The download panel of the route converter user interface.
 *
 * @author Christian Pesch
 */

public class DownloadsPanel {
    private JPanel rootPanel;
    private JComboBox comboBoxElevationService;
    private JTable tableDownloads;

    public DownloadsPanel() {
        initialize();
    }

    private void initialize() {
        final RouteConverter r = RouteConverter.getInstance();

        DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
        for (ElevationService service : r.getCompletePositionService().getElevationServices())
            comboBoxModel.addElement(service);
        comboBoxElevationService.setModel(comboBoxModel);
        comboBoxElevationService.setRenderer(new ElevationLookupServiceListCellRenderer());
        comboBoxElevationService.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED)
                    return;
                ElevationService service = ElevationService.class.cast(e.getItem());
                r.getCompletePositionService().setElevationLookupService(service);
            }
        });
        comboBoxElevationService.setSelectedItem(r.getCompletePositionService().getElevationService());

        tableDownloads.setModel(r.getDownloadManager().getModel());
        tableDownloads.setDefaultRenderer(Object.class, new DownloadsTableCellRenderer());
        TableCellRenderer routesHeaderRenderer = new DownloadsTableCellHeaderRenderer();
        TableColumnModel routeColumns = tableDownloads.getColumnModel();
        for (int i = 0; i < routeColumns.getColumnCount(); i++) {
            TableColumn column = routeColumns.getColumn(i);
            column.setHeaderRenderer(routesHeaderRenderer);
            if (i == 1) {
                column.setPreferredWidth(100);
                column.setMaxWidth(140);
            }
        }

        new Thread(new Runnable() {
            public void run() {
                /*
                try {
                    File mapFile = new File(createTempFile("germany", ".map").getParentFile(), "germany.map");
                    r.getDownloadManager().queueForDownload("Germany Map", "http://download.mapsforge.org/maps/europe/germany.map", mapFile);
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                */
                /*
                int i = 1;
                while (true) {
                    try {
                        File tempFile = createTempFile("447bytes", ".test");
                        r.getDownloadManager().queueForDownload("447 Bytes " + (i++), "http://static.routeconverter.com/download/test/447bytes.txt", tempFile);
                        sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                */
                /*
                List<LongitudeAndLatitude> list = new ArrayList<LongitudeAndLatitude>();
                for(int j=0; j < 180; j++) {
                    list.add(new LongitudeAndLatitude(j, j));
                }
                r.getCompletePositionService().downloadElevationFor(list);
                */
            }
        }).start();
    }

    public Component getRootComponent() {
        return rootPanel;
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
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("elevation-service"));
        rootPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("downloads-colon"));
        rootPanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxElevationService = new JComboBox();
        rootPanel.add(comboBoxElevationService, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootPanel.add(scrollPane1, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableDownloads = new JTable();
        scrollPane1.setViewportView(tableDownloads);
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
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }
}
