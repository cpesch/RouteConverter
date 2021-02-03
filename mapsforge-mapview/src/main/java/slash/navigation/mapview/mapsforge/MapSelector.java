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
package slash.navigation.mapview.mapsforge;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.mapsforge.map.model.IMapViewPosition;
import slash.navigation.gui.Application;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.LocalTheme;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.mapsforge.impl.MapsforgeFileMap;
import slash.navigation.mapview.mapsforge.models.JoinedListComboBoxModel;
import slash.navigation.mapview.mapsforge.models.TableModelToComboBoxModelAdapter;
import slash.navigation.mapview.mapsforge.renderer.LocalMapListCellRenderer;
import slash.navigation.mapview.mapsforge.renderer.LocalThemeListCellRenderer;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Locale;
import java.util.ResourceBundle;

import static com.intellij.uiDesigner.core.GridConstraints.*;
import static java.awt.Desktop.getDesktop;
import static java.awt.event.ItemEvent.SELECTED;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static slash.common.io.Transfer.toArray;
import static slash.navigation.gui.events.Range.asRange;
import static slash.navigation.gui.helpers.UIHelper.getMaxWidth;
import static slash.navigation.maps.tileserver.TileServerMapManager.extractCopyrightHref;
import static slash.navigation.mapview.mapsforge.renderer.LocalMapListCellRenderer.DOWNLOAD_MAP;
import static slash.navigation.mapview.mapsforge.renderer.LocalMapListCellRenderer.SEPARATOR_TO_DOWNLOAD_MAP;
import static slash.navigation.mapview.mapsforge.renderer.LocalThemeListCellRenderer.DOWNLOAD_THEME;
import static slash.navigation.mapview.mapsforge.renderer.LocalThemeListCellRenderer.SEPARATOR_TO_DOWNLOAD_THEME;

/**
 * The map and theme chooser panel of the mapsforge map view.
 *
 * @author Christian Pesch
 */

public class MapSelector {
    private static final GridConstraints MAP_SELECTOR_CONSTRAINTS = new GridConstraints(0, 0, 1, 1, ANCHOR_CENTER,
            FILL_BOTH, SIZEPOLICY_CAN_SHRINK | SIZEPOLICY_CAN_GROW, SIZEPOLICY_CAN_SHRINK | SIZEPOLICY_CAN_GROW,
            new Dimension(0, 0), new Dimension(0, 0), new Dimension(MAX_VALUE, MAX_VALUE), 0, false);
    private JPanel contentPane;
    private JComboBox<Integer> comboBoxZoom;
    private JComboBox<LocalMap> comboBoxMap;
    private JComboBox<LocalTheme> comboBoxTheme;
    private JPanel mapViewPanel;
    private JLabel labelCopyrightText;

    public MapSelector(MapsforgeMapManager mapManager, final AwtGraphicMapView mapView) {
        $$$setupUI$$$();
        mapViewPanel.add(mapView, MAP_SELECTOR_CONSTRAINTS);

        comboBoxZoom.setModel(new DefaultComboBoxModel<>());
        comboBoxZoom.addItemListener(e -> {
            if (e.getStateChange() != SELECTED) {
                return;
            }
            Integer zoom = (Integer) e.getItem();
            mapView.setZoomLevel(zoom.byteValue());
        });
        int width = getMaxWidth("19", 0);
        comboBoxZoom.setMaximumSize(new Dimension(width, comboBoxZoom.getMaximumSize().height));
        comboBoxZoom.setPreferredSize(new Dimension(width, comboBoxZoom.getPreferredSize().height));

        IMapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;
        mapViewPosition.addObserver(() -> zoomChanged(mapViewPosition.getZoomLevelMin(), mapViewPosition.getZoomLevelMax(), mapViewPosition.getZoomLevel()));

        comboBoxMap.setModel(new JoinedListComboBoxModel<>(
                new TableModelToComboBoxModelAdapter<>(mapManager.getAvailableMapsModel(), mapManager.getDisplayedMapModel()),
                asList(SEPARATOR_TO_DOWNLOAD_MAP, DOWNLOAD_MAP))
        );
        comboBoxMap.setPrototypeDisplayValue(new MapsforgeFileMap("Map", "http://mal.url", null, null, null));
        comboBoxMap.setRenderer(new LocalMapListCellRenderer());
        comboBoxMap.addItemListener(e -> {
            if (e.getStateChange() != SELECTED) {
                return;
            }
            LocalMap map = (LocalMap) e.getItem();
            comboBoxTheme.setEnabled(map.getType().isThemed());

            handleMapUpdate(map);
        });

        comboBoxTheme.setModel(new JoinedListComboBoxModel<>(
                new TableModelToComboBoxModelAdapter<>(mapManager.getAvailableThemesModel(), mapManager.getAppliedThemeModel()),
                asList(SEPARATOR_TO_DOWNLOAD_THEME, DOWNLOAD_THEME))
        );
        comboBoxTheme.setPrototypeDisplayValue(mapManager.getAvailableThemesModel().getItem(0));
        comboBoxTheme.setRenderer(new LocalThemeListCellRenderer());
        LocalMap selectedItem = (LocalMap) comboBoxMap.getSelectedItem();
        comboBoxTheme.setEnabled(selectedItem != null && selectedItem.getType().isThemed());

        labelCopyrightText.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                String uri = extractCopyrightHref(labelCopyrightText.getText());
                if (uri != null) {
                    try {
                        // should be ExternalPrograms#startBrowser
                        getDesktop().browse(new URI(uri));
                    } catch (Exception e) {
                        // intentionally left empty
                    }
                }
            }
        });

        if (selectedItem != null)
            handleMapUpdate(selectedItem);
    }

    private void handleMapUpdate(LocalMap map) {
        String copyrightText = map.getCopyrightText();
        labelCopyrightText.setText("<html>" + copyrightText);
    }

    private void zoomChanged(byte zoomLevelMin, byte zoomLevelMax, int zoomLevel) {
        ComboBoxModel<Integer> model = comboBoxZoom.getModel();
        if (model.getSize() == 0 ||
                model.getElementAt(0) != zoomLevelMin ||
                model.getElementAt(model.getSize() - 1) != zoomLevelMax)
            comboBoxZoom.setModel(new DefaultComboBoxModel<>(toArray(asRange(zoomLevelMin, zoomLevelMax))));

        try {
            comboBoxZoom.setSelectedItem(zoomLevel);
        } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
            // work around exceptions deep in Swings BasicListUI on Mac OS X
            comboBoxZoom.setSelectedIndex(min(zoomLevel - 1, zoomLevelMax));
        }
    }

    public Component getComponent() {
        return contentPane;
    }

    JPanel getMapViewPanel() {
        return mapViewPanel;
    }

    private void createUIComponents() {
        comboBoxMap = new JComboBox<LocalMap>() {
            public void setSelectedItem(Object anObject) {
                if (DOWNLOAD_MAP.equals(anObject)) {
                    Application.getInstance().getContext().getActionManager().run("show-maps");
                    return;
                }
                super.setSelectedItem(anObject);
            }
        };
        comboBoxTheme = new JComboBox<LocalTheme>() {
            public void setSelectedItem(Object anObject) {
                if (DOWNLOAD_THEME.equals(anObject)) {
                    Application.getInstance().getContext().getActionManager().run("show-themes");
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
        panel1.setLayout(new GridLayoutManager(1, 7, new Insets(2, 2, 0, 4), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "map-colon"));
        panel1.add(label1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel1.add(comboBoxMap, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(200, -1), null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "theme-colon"));
        panel1.add(label2, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        panel1.add(comboBoxTheme, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "zoom-colon"));
        panel1.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxZoom = new JComboBox();
        comboBoxZoom.setFocusable(false);
        panel1.add(comboBoxZoom, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelCopyrightText = new JLabel();
        labelCopyrightText.setFocusable(false);
        Font labelCopyrightTextFont = this.$$$getFont$$$(null, -1, 7, labelCopyrightText.getFont());
        if (labelCopyrightTextFont != null) labelCopyrightText.setFont(labelCopyrightTextFont);
        labelCopyrightText.setText("");
        panel1.add(labelCopyrightText, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mapViewPanel = new JPanel();
        mapViewPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(mapViewPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
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
