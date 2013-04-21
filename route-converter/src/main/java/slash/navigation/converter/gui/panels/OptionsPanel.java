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
import slash.navigation.babel.BabelFormat;
import slash.navigation.common.DegreeFormat;
import slash.navigation.common.NumberPattern;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.helper.CheckBoxPreferencesSynchronizer;
import slash.navigation.converter.gui.mapview.TravelMode;
import slash.navigation.converter.gui.renderer.DegreeFormatListCellRenderer;
import slash.navigation.converter.gui.renderer.LocaleListCellRenderer;
import slash.navigation.converter.gui.renderer.NumberPatternListCellRenderer;
import slash.navigation.converter.gui.renderer.TravelModeListCellRenderer;
import slash.navigation.converter.gui.renderer.UnitSystemListCellRenderer;
import slash.navigation.gui.Application;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.gui.helpers.UIHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.prefs.Preferences;

import static java.awt.event.ItemEvent.SELECTED;
import static java.util.Arrays.sort;
import static java.util.Locale.CHINA;
import static java.util.Locale.FRANCE;
import static java.util.Locale.GERMANY;
import static java.util.Locale.ITALY;
import static java.util.Locale.ROOT;
import static java.util.Locale.US;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_ONLY;
import static slash.navigation.common.DegreeFormat.Degrees;
import static slash.navigation.common.DegreeFormat.Degrees_Minutes;
import static slash.navigation.common.DegreeFormat.Degrees_Minutes_Seconds;
import static slash.navigation.common.NumberPattern.Description_Only;
import static slash.navigation.common.NumberPattern.Number_Directly_Followed_By_Description;
import static slash.navigation.common.NumberPattern.Number_Only;
import static slash.navigation.common.NumberPattern.Number_Space_Then_Description;
import static slash.navigation.common.UnitSystem.Metric;
import static slash.navigation.common.UnitSystem.Nautic;
import static slash.navigation.common.UnitSystem.Statute;
import static slash.navigation.converter.gui.RouteConverter.AUTOMATIC_UPDATE_CHECK_PREFERENCE;
import static slash.navigation.converter.gui.RouteConverter.AVOID_HIGHWAYS_PREFERENCE;
import static slash.navigation.converter.gui.RouteConverter.AVOID_TOLLS_PREFERENCE;
import static slash.navigation.converter.gui.RouteConverter.RECENTER_AFTER_ZOOMING_PREFERENCE;
import static slash.navigation.converter.gui.RouteConverter.SHOW_COORDINATES_PREFERENCE;
import static slash.navigation.converter.gui.RouteConverter.SHOW_WAYPOINT_DESCRIPTION_PREFERENCE;
import static slash.navigation.converter.gui.mapview.TravelMode.Bicycling;
import static slash.navigation.converter.gui.mapview.TravelMode.Driving;
import static slash.navigation.converter.gui.mapview.TravelMode.Walking;
import static slash.navigation.gui.helpers.UIHelper.ARABIA;
import static slash.navigation.gui.helpers.UIHelper.CROATIA;
import static slash.navigation.gui.helpers.UIHelper.CZECH;
import static slash.navigation.gui.helpers.UIHelper.NEDERLANDS;
import static slash.navigation.gui.helpers.UIHelper.SERBIA;
import static slash.navigation.gui.helpers.UIHelper.SLOVAKIA;
import static slash.navigation.gui.helpers.UIHelper.SPAIN;

/**
 * The misc panel of the route converter user interface.
 *
 * @author Christian Pesch
 */

public class OptionsPanel {
    private static final Preferences preferences = Preferences.userNodeForPackage(OptionsPanel.class);

    private JPanel miscPanel;
    private JComboBox comboBoxLocale;
    private JTextField textFieldBabelPath;
    private JButton buttonChooseBabelPath;
    private JCheckBox checkBoxAutomaticUpdateCheck;
    private JCheckBox checkBoxAvoidHighways;
    private JCheckBox checkBoxAvoidTolls;
    private JCheckBox checkBoxRecenterAfterZooming;
    private JCheckBox checkBoxShowCoordinates;
    private JCheckBox checkBoxShowWaypointDescription;
    private JComboBox comboboxTravelMode;
    private JComboBox comboboxNumberPattern;
    private JComboBox comboBoxUnitSystem;
    private JComboBox comboBoxTimeZone;
    private JComboBox comboBoxDegreeFormat;

    public OptionsPanel() {
        initialize();
    }

    private void initialize() {
        final RouteConverter r = RouteConverter.getInstance();

        buttonChooseBabelPath.addActionListener(new FrameAction() {
            public void run() {
                chooseBabelPath();
            }
        });

        comboBoxLocale.setModel(new DefaultComboBoxModel(new Object[]{
                ARABIA, CHINA, CZECH, GERMANY, US, SPAIN, FRANCE, CROATIA, ITALY, NEDERLANDS, SLOVAKIA, SERBIA, ROOT}));
        comboBoxLocale.setRenderer(new LocaleListCellRenderer());
        comboBoxLocale.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED)
                    return;
                Locale locale = (Locale) e.getItem();
                Application.getInstance().setLocale(locale);
            }
        });
        comboBoxLocale.setSelectedItem(Application.getInstance().getLocale());

        textFieldBabelPath.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                BabelFormat.setBabelPathPreference(textFieldBabelPath.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                insertUpdate(e);
            }

            public void changedUpdate(DocumentEvent e) {
                insertUpdate(e);
            }
        });
        textFieldBabelPath.setText(BabelFormat.getBabelPathPreference());

        new CheckBoxPreferencesSynchronizer(checkBoxAutomaticUpdateCheck, preferences, AUTOMATIC_UPDATE_CHECK_PREFERENCE, true);

        new CheckBoxPreferencesSynchronizer(checkBoxRecenterAfterZooming, preferences, RECENTER_AFTER_ZOOMING_PREFERENCE, false);
        checkBoxRecenterAfterZooming.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.setRecenterAfterZooming(checkBoxRecenterAfterZooming.isSelected());
            }
        });

        new CheckBoxPreferencesSynchronizer(checkBoxShowCoordinates, preferences, SHOW_COORDINATES_PREFERENCE, false);
        checkBoxShowCoordinates.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.setShowCoordinates(checkBoxShowCoordinates.isSelected());
            }
        });

        new CheckBoxPreferencesSynchronizer(checkBoxShowWaypointDescription, preferences, SHOW_WAYPOINT_DESCRIPTION_PREFERENCE, false);
        checkBoxShowWaypointDescription.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.setShowWaypointDescription(checkBoxShowWaypointDescription.isSelected());
            }
        });

        List<Object> travelModes = new ArrayList<Object>();
        // bicycling currently (march 11) only available in the US
        if (Locale.getDefault().equals(US))
            travelModes.add(Bicycling);
        travelModes.add(Driving);
        travelModes.add(Walking);
        comboboxTravelMode.setModel(new DefaultComboBoxModel(travelModes.toArray()));
        comboboxTravelMode.setRenderer(new TravelModeListCellRenderer());
        comboboxTravelMode.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED)
                    return;
                TravelMode travelMode = (TravelMode) e.getItem();
                r.setTravelMode(travelMode);
            }
        });
        comboboxTravelMode.setSelectedItem(r.getTravelModePreference());

        new CheckBoxPreferencesSynchronizer(checkBoxAvoidHighways, preferences, AVOID_HIGHWAYS_PREFERENCE, true);
        checkBoxAvoidHighways.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.setAvoidHighways(checkBoxAvoidHighways.isSelected());
            }
        });
        new CheckBoxPreferencesSynchronizer(checkBoxAvoidTolls, preferences, AVOID_TOLLS_PREFERENCE, true);
        checkBoxAvoidTolls.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.setAvoidTolls(checkBoxAvoidTolls.isSelected());
            }
        });

        comboboxNumberPattern.setModel(new DefaultComboBoxModel(new Object[]{
                Description_Only, Number_Only, Number_Directly_Followed_By_Description, Number_Space_Then_Description
        }));
        comboboxNumberPattern.setRenderer(new NumberPatternListCellRenderer());
        comboboxNumberPattern.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED)
                    return;
                NumberPattern numberPattern = NumberPattern.class.cast(e.getItem());
                r.setNumberPatternPreference(numberPattern);
            }
        });
        comboboxNumberPattern.setSelectedItem(r.getNumberPatternPreference());

        comboBoxUnitSystem.setModel(new DefaultComboBoxModel(new Object[]{
                Metric, Statute, Nautic
        }));
        comboBoxUnitSystem.setRenderer(new UnitSystemListCellRenderer());
        comboBoxUnitSystem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED)
                    return;
                UnitSystem unitSystem = UnitSystem.class.cast(e.getItem());
                r.getUnitSystemModel().setUnitSystem(unitSystem);
            }
        });
        comboBoxUnitSystem.setSelectedItem(r.getUnitSystemModel().getUnitSystem());

        comboBoxDegreeFormat.setModel(new DefaultComboBoxModel(new Object[]{
                Degrees, Degrees_Minutes, Degrees_Minutes_Seconds
        }));
        comboBoxDegreeFormat.setRenderer(new DegreeFormatListCellRenderer());
        comboBoxDegreeFormat.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED)
                    return;
                DegreeFormat degreeFormat = DegreeFormat.class.cast(e.getItem());
                r.getUnitSystemModel().setDegreeFormat(degreeFormat);
            }
        });
        comboBoxDegreeFormat.setSelectedItem(r.getUnitSystemModel().getDegreeFormat());

        comboBoxTimeZone.setModel(new DefaultComboBoxModel(getTimeZoneIds()));
        comboBoxTimeZone.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED)
                    return;
                String timeZoneId = String.valueOf(e.getItem());
                r.setTimeZonePreference(timeZoneId);
            }
        });
        comboBoxTimeZone.setSelectedItem(r.getTimeZonePreference());
    }

    public Component getRootComponent() {
        return miscPanel;
    }

    private void chooseBabelPath() {
        JFileChooser chooser = UIHelper.createJFileChooser();
        chooser.setDialogTitle(RouteConverter.getBundle().getString("choose-gpsbabel-path"));
        chooser.setSelectedFile(new File(BabelFormat.getBabelPathPreference()));
        chooser.setFileSelectionMode(FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int open = chooser.showOpenDialog(RouteConverter.getInstance().getFrame());
        if (open != APPROVE_OPTION)
            return;

        File selected = chooser.getSelectedFile();
        if (selected == null || selected.getName().length() == 0)
            return;

        textFieldBabelPath.setText(selected.getAbsolutePath());
    }

    private String[] getTimeZoneIds() {
        String[] ids = TimeZone.getAvailableIDs();
        sort(ids);
        return ids;
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
        miscPanel = new JPanel();
        miscPanel.setLayout(new GridLayoutManager(3, 1, new Insets(3, 3, 3, 3), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 3, new Insets(3, 3, 3, 3), -1, -1));
        miscPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("preferred-locale"));
        panel1.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxLocale = new JComboBox();
        panel1.add(comboBoxLocale, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("gpsbabel-path"));
        panel1.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldBabelPath = new JTextField();
        panel1.add(textFieldBabelPath, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonChooseBabelPath = new JButton();
        buttonChooseBabelPath.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/select.png")));
        buttonChooseBabelPath.setText("");
        buttonChooseBabelPath.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("choose-gpsbabel-path"));
        panel1.add(buttonChooseBabelPath, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("automatic-update-check"));
        panel1.add(label3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAutomaticUpdateCheck = new JCheckBox();
        checkBoxAutomaticUpdateCheck.setHorizontalAlignment(11);
        checkBoxAutomaticUpdateCheck.setHorizontalTextPosition(11);
        panel1.add(checkBoxAutomaticUpdateCheck, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("program-options"));
        panel1.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel1.add(separator1, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        miscPanel.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 20), null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(22, 2, new Insets(3, 3, 3, 3), -1, -1));
        miscPanel.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("avoid-highways"));
        panel3.add(label5, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAvoidHighways = new JCheckBox();
        panel3.add(checkBoxAvoidHighways, new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("travel-mode"));
        panel3.add(label6, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        this.$$$loadLabelText$$$(label7, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("map-options"));
        panel3.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel3.add(separator2, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        this.$$$loadLabelText$$$(label8, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("recenter-after-zooming"));
        panel3.add(label8, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxRecenterAfterZooming = new JCheckBox();
        panel3.add(checkBoxRecenterAfterZooming, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        this.$$$loadLabelText$$$(label9, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("numbering-options"));
        panel3.add(label9, new GridConstraints(15, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator3 = new JSeparator();
        panel3.add(separator3, new GridConstraints(16, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(3, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(10, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JSeparator separator4 = new JSeparator();
        panel3.add(separator4, new GridConstraints(20, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        this.$$$loadLabelText$$$(label10, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-options"));
        panel3.add(label10, new GridConstraints(19, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(3, 0, 0, 0), -1, -1));
        panel3.add(panel5, new GridConstraints(18, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel6, new GridConstraints(17, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        this.$$$loadLabelText$$$(label11, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("number-pattern"));
        panel6.add(label11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboboxNumberPattern = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        comboboxNumberPattern.setModel(defaultComboBoxModel1);
        panel6.add(comboboxNumberPattern, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel7, new GridConstraints(21, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        this.$$$loadLabelText$$$(label12, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-times-with-timezone"));
        panel7.add(label12, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxTimeZone = new JComboBox();
        panel7.add(comboBoxTimeZone, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        this.$$$loadLabelText$$$(label13, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-measures-with-system-of-unit"));
        panel7.add(label13, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxUnitSystem = new JComboBox();
        panel7.add(comboBoxUnitSystem, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        this.$$$loadLabelText$$$(label14, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-degrees-with-format"));
        panel7.add(label14, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxDegreeFormat = new JComboBox();
        panel7.add(comboBoxDegreeFormat, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label15 = new JLabel();
        this.$$$loadLabelText$$$(label15, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("avoid-tolls"));
        panel3.add(label15, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAvoidTolls = new JCheckBox();
        panel3.add(checkBoxAvoidTolls, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboboxTravelMode = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        comboboxTravelMode.setModel(defaultComboBoxModel2);
        panel3.add(comboboxTravelMode, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label16 = new JLabel();
        this.$$$loadLabelText$$$(label16, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("routing-options"));
        panel3.add(label16, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator5 = new JSeparator();
        panel3.add(separator5, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(3, 0, 0, 0), -1, -1));
        panel3.add(panel8, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label17 = new JLabel();
        this.$$$loadLabelText$$$(label17, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("show-coordinates"));
        panel3.add(label17, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxShowCoordinates = new JCheckBox();
        panel3.add(checkBoxShowCoordinates, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label18 = new JLabel();
        this.$$$loadLabelText$$$(label18, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("waypoint-options"));
        panel3.add(label18, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator6 = new JSeparator();
        panel3.add(separator6, new GridConstraints(12, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label19 = new JLabel();
        this.$$$loadLabelText$$$(label19, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("show-waypoint-description"));
        panel3.add(label19, new GridConstraints(13, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 1, new Insets(3, 0, 0, 0), -1, -1));
        panel3.add(panel9, new GridConstraints(14, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        checkBoxShowWaypointDescription = new JCheckBox();
        panel3.add(checkBoxShowWaypointDescription, new GridConstraints(13, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        return miscPanel;
    }
}
