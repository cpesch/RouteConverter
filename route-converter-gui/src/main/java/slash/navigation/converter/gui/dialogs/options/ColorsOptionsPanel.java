/*
    This file is part of BaseRouteConverter.

    BaseRouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    BaseRouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with BaseRouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.converter.gui.dialogs.options;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.converter.gui.models.ColorModel;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * The Colors tab of the {@link slash.navigation.converter.gui.dialogs.OptionsDialog}.
 *
 * @author Christian Pesch
 */

public class ColorsOptionsPanel {
    private JPanel colorsOptionsPanel;
    private JColorChooser colorChooserRoute;
    private JColorChooser colorChooserTrack;
    private JColorChooser colorChooserWaypoint;
    private JSpinner lineWidthSpinnerRoute;
    private JSpinner lineWidthSpinnerTrack;

    public ColorsOptionsPanel() {
        $$$setupUI$$$();

        BaseRouteConverter r = BaseRouteConverter.getInstance();

        setupColorChooser(colorChooserRoute, r.getMapPreferencesModel().getRouteColorModel());
        setupColorChooser(colorChooserTrack, r.getMapPreferencesModel().getTrackColorModel());
        setupColorChooser(colorChooserWaypoint, r.getMapPreferencesModel().getWaypointColorModel());
        lineWidthSpinnerRoute.setModel(new SpinnerNumberModel(r.getMapPreferencesModel().getRouteLineWidthModel().getInteger().intValue(), 1, 20, 1));
        lineWidthSpinnerRoute.addChangeListener(e -> r.getMapPreferencesModel().getRouteLineWidthModel().setInteger((Integer) lineWidthSpinnerRoute.getValue()));
        lineWidthSpinnerTrack.setModel(new SpinnerNumberModel(r.getMapPreferencesModel().getTrackLineWidthModel().getInteger().intValue(), 1, 20, 1));
        lineWidthSpinnerTrack.addChangeListener(e -> r.getMapPreferencesModel().getTrackLineWidthModel().setInteger((Integer) lineWidthSpinnerTrack.getValue()));
    }

    public JPanel getRootPanel() {
        return colorsOptionsPanel;
    }

    private static final Set<String> REMOVEABLE_COLOR_PANELS = new HashSet<>(
            asList("Swatches", "HSV", "HSL", "CMYK",
                    // German Mac OS X has different names
                    "Muster",
                    // French locale has different names
                    "Echantillons", "TSV", "TSL",
                    // Chinese locale has different names
                    "样本(S)", "HSV(H)", "HSL(L)"
            )
    );

    private void setupColorChooser(JColorChooser chooser, ColorModel colorModel) {
        chooser.setColor(colorModel.getColor());
        reducePanels(chooser);
        chooser.getSelectionModel().addChangeListener(e -> colorModel.setColor(chooser.getColor()));

        // add a right-aligned "reset to default color" button directly below the chooser:
        // wrap the chooser in place (keeping its original grid cell) so the button sits
        // beneath it. Clicking it sets the chooser to the model's built-in default, which
        // propagates to the model - and on to the map - via the selection listener above.
        JButton resetButton = new JButton(BaseRouteConverter.getBundle().getString("reset-color"));
        resetButton.addActionListener(e -> chooser.setColor(colorModel.getDefaultColor()));

        Container parent = chooser.getParent();
        GridConstraints constraints = ((GridLayoutManager) parent.getLayout()).getConstraintsForComponent(chooser);
        parent.remove(chooser);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.add(resetButton);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(chooser, BorderLayout.CENTER);
        wrapper.add(buttons, BorderLayout.SOUTH);
        parent.add(wrapper, constraints);
    }

    private void reducePanels(JColorChooser chooser) {
        chooser.setPreviewPanel(new JPanel());
        for (AbstractColorChooserPanel panel : chooser.getChooserPanels()) {
            String displayName = panel.getDisplayName();
            if (REMOVEABLE_COLOR_PANELS.contains(displayName)) {
                chooser.removeChooserPanel(panel);
            }
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        colorsOptionsPanel = new JPanel();
        colorsOptionsPanel.setLayout(new GridLayoutManager(1, 1, new Insets(5, 0, 0, 0), -1, -1));
        final JTabbedPane tabbedPane1 = new JTabbedPane();
        colorsOptionsPanel.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0,
                false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 2, new Insets(3, 3, 3, 3), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "route-tab"), panel1);
        colorChooserRoute = new JColorChooser();
        panel1.add(colorChooserRoute, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "route-color"));
        panel1.add(label1,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2,
                this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "route-line-width"));
        panel1.add(label2,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lineWidthSpinnerRoute = new JSpinner();
        panel1.add(lineWidthSpinnerRoute, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 2, new Insets(3, 3, 3, 3), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "track-tab"), panel2);
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        colorChooserTrack = new JColorChooser();
        panel2.add(colorChooserTrack, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "track-color"));
        panel2.add(label3,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4,
                this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "track-line-width"));
        panel2.add(label4,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lineWidthSpinnerTrack = new JSpinner();
        panel2.add(lineWidthSpinnerTrack, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 2, new Insets(3, 3, 3, 3), -1, -1));
        tabbedPane1.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "waypoint-tab"), panel3);
        final Spacer spacer3 = new Spacer();
        panel3.add(spacer3, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        colorChooserWaypoint = new JColorChooser();
        panel3.add(colorChooserWaypoint, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5,
                this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "waypoint-color"));
        panel3.add(label5,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    /**
     * @noinspection ALL
     */
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
                if (i == text.length()) {
                    break;
                }
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
        return colorsOptionsPanel;
    }

}
