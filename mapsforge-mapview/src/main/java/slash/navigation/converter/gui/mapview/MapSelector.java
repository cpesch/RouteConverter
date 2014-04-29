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
package slash.navigation.converter.gui.mapview;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import slash.navigation.converter.gui.mapview.models.JoinedListComboBoxModel;
import slash.navigation.converter.gui.mapview.models.TableModelToComboBoxModelAdapter;
import slash.navigation.converter.gui.mapview.renderer.MapListCellRenderer;
import slash.navigation.converter.gui.mapview.renderer.ThemeListCellRenderer;
import slash.navigation.gui.Application;
import slash.navigation.maps.LocalMap;
import slash.navigation.maps.MapManager;
import slash.navigation.maps.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import static com.intellij.uiDesigner.core.GridConstraints.*;
import static java.awt.event.ItemEvent.SELECTED;
import static java.util.Arrays.asList;
import static slash.navigation.converter.gui.mapview.renderer.MapListCellRenderer.DOWNLOAD_MAP;
import static slash.navigation.converter.gui.mapview.renderer.MapListCellRenderer.SEPARATOR_TO_DOWNLOAD_MAP;
import static slash.navigation.converter.gui.mapview.renderer.ThemeListCellRenderer.DOWNLOAD_THEME;
import static slash.navigation.converter.gui.mapview.renderer.ThemeListCellRenderer.SEPARATOR_TO_DOWNLOAD_THEME;

/**
 * The map and theme chooser panel of the mapsforge map view.
 *
 * @author Christian Pesch
 */

public class MapSelector {
    private JPanel contentPane;
    private JLabel labelZoom;
    private JComboBox<LocalMap> comboBoxMap;
    private JComboBox<Theme> comboBoxTheme;
    private JPanel mapViewPanel;

    public MapSelector(final MapManager mapManager, AwtGraphicMapView mapView) {
        $$$setupUI$$$();
        mapViewPanel.add(mapView, new GridConstraints(0, 0, 1, 1, ANCHOR_CENTER, FILL_BOTH,
                SIZEPOLICY_CAN_SHRINK | SIZEPOLICY_CAN_GROW, SIZEPOLICY_CAN_SHRINK | SIZEPOLICY_CAN_GROW,
                new Dimension(0, 0), new Dimension(0, 0), new Dimension(2000, 2640), 0, false));

        comboBoxMap.setModel(new JoinedListComboBoxModel<LocalMap>(
                        new TableModelToComboBoxModelAdapter<LocalMap>(mapManager.getMapsModel(), mapManager.getDisplayedMapModel()),
                        asList(SEPARATOR_TO_DOWNLOAD_MAP, DOWNLOAD_MAP))
        );
        comboBoxMap.setPrototypeDisplayValue(mapManager.getMapsModel().getMap(1));
        comboBoxMap.setRenderer(new MapListCellRenderer());
        comboBoxMap.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED)
                    return;
                LocalMap map = (LocalMap) e.getItem();
                comboBoxTheme.setEnabled(map.isVector());
            }
        });

        comboBoxTheme.setModel(new JoinedListComboBoxModel<Theme>(
                        new TableModelToComboBoxModelAdapter<Theme>(mapManager.getThemesModel(), mapManager.getAppliedThemeModel()),
                        asList(SEPARATOR_TO_DOWNLOAD_THEME, DOWNLOAD_THEME))
        );
        comboBoxTheme.setPrototypeDisplayValue(mapManager.getThemesModel().getTheme(0));
        comboBoxTheme.setRenderer(new ThemeListCellRenderer());
        comboBoxTheme.setEnabled(((LocalMap) comboBoxMap.getSelectedItem()).isVector());
    }

    public void zoomChanged(int zoomLevel) {
        labelZoom.setText(Integer.toString(zoomLevel));
    }

    public Component getComponent() {
        return contentPane;
    }

    private void createUIComponents() {
        comboBoxMap = new JComboBox<LocalMap>() {
            public void setSelectedItem(Object anObject) {
                if (DOWNLOAD_MAP.equals(anObject)) {
                    Application.getInstance().getContext().getActionManager().run("select-maps");
                    return;
                }
                super.setSelectedItem(anObject);
            }
        };
        comboBoxTheme = new JComboBox<Theme>() {
            public void setSelectedItem(Object anObject) {
                if (DOWNLOAD_THEME.equals(anObject)) {
                    Application.getInstance().getContext().getActionManager().run("select-maps");
                    return;
                }
                super.setSelectedItem(anObject);
            }
        };
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 6, new Insets(2, 2, 0, 4), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/mapview/MapsforgeMapView").getString("map-colon"));
        panel1.add(label1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        panel1.add(comboBoxMap, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/mapview/MapsforgeMapView").getString("theme-colon"));
        panel1.add(label2, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        panel1.add(comboBoxTheme, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/mapview/MapsforgeMapView").getString("zoom-colon"));
        panel1.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelZoom = new JLabel();
        labelZoom.setText("");
        panel1.add(labelZoom, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mapViewPanel = new JPanel();
        mapViewPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(mapViewPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
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
        return contentPane;
    }
}
