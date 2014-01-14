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
import slash.navigation.babel.BabelFormat;
import slash.navigation.common.DegreeFormat;
import slash.navigation.common.NumberPattern;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.actions.DialogAction;
import slash.navigation.converter.gui.helpers.CheckBoxPreferencesSynchronizer;
import slash.navigation.converter.gui.mapview.TravelMode;
import slash.navigation.converter.gui.renderer.*;
import slash.navigation.elevation.ElevationService;
import slash.navigation.gui.Application;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.*;
import java.util.List;

import static java.awt.event.ItemEvent.SELECTED;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.util.Arrays.sort;
import static java.util.Locale.*;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.navigation.common.DegreeFormat.*;
import static slash.navigation.common.NumberPattern.*;
import static slash.navigation.common.UnitSystem.*;
import static slash.navigation.converter.gui.RouteConverter.*;
import static slash.navigation.converter.gui.mapview.TravelMode.*;
import static slash.navigation.gui.helpers.UIHelper.*;

/**
 * Dialog to show options for the program.
 *
 * @author Christian Pesch
 */

public class OptionsDialog extends SimpleDialog {
    private JPanel contentPane;
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
    private JButton buttonClose;
    private JComboBox comboBoxElevationService;

    public OptionsDialog() {
        super(RouteConverter.getInstance().getFrame(), "options");
        setTitle(RouteConverter.getBundle().getString("options-title"));
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonClose);

        final RouteConverter r = RouteConverter.getInstance();

        buttonChooseBabelPath.addActionListener(new FrameAction() {
            public void run() {
                chooseBabelPath();
            }
        });

        comboBoxLocale.setModel(new DefaultComboBoxModel(new Object[]{
                ARABIA, CHINA, CZECH, GERMANY, US, SPAIN, FRANCE, CROATIA, ITALY, NEDERLANDS, POLAND,
                RUSSIA, SLOVAKIA, SERBIA, ROOT}));
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

        new CheckBoxPreferencesSynchronizer(checkBoxAutomaticUpdateCheck, getPreferences(), AUTOMATIC_UPDATE_CHECK_PREFERENCE, true);

        new CheckBoxPreferencesSynchronizer(checkBoxRecenterAfterZooming, getPreferences(), RECENTER_AFTER_ZOOMING_PREFERENCE, false);
        checkBoxRecenterAfterZooming.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.setRecenterAfterZooming(checkBoxRecenterAfterZooming.isSelected());
            }
        });

        new CheckBoxPreferencesSynchronizer(checkBoxShowCoordinates, getPreferences(), SHOW_COORDINATES_PREFERENCE, false);
        checkBoxShowCoordinates.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.setShowCoordinates(checkBoxShowCoordinates.isSelected());
            }
        });

        new CheckBoxPreferencesSynchronizer(checkBoxShowWaypointDescription, getPreferences(), SHOW_WAYPOINT_DESCRIPTION_PREFERENCE, false);
        checkBoxShowWaypointDescription.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.setShowWaypointDescription(checkBoxShowWaypointDescription.isSelected());
            }
        });

        List<Object> travelModes = new ArrayList<Object>();
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

        new CheckBoxPreferencesSynchronizer(checkBoxAvoidHighways, getPreferences(), AVOID_HIGHWAYS_PREFERENCE, true);
        checkBoxAvoidHighways.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.setAvoidHighways(checkBoxAvoidHighways.isSelected());
            }
        });
        new CheckBoxPreferencesSynchronizer(checkBoxAvoidTolls, getPreferences(), AVOID_TOLLS_PREFERENCE, true);
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

        DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
        for (ElevationService service : r.getCompletePositionService().getElevationServices())
            comboBoxModel.addElement(service);
        comboBoxElevationService.setModel(comboBoxModel);
        comboBoxElevationService.setRenderer(new ElevationServiceListCellRenderer());
        comboBoxElevationService.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED)
                    return;
                ElevationService service = ElevationService.class.cast(e.getItem());
                r.getCompletePositionService().setElevationService(service);
            }
        });
        comboBoxElevationService.setSelectedItem(r.getCompletePositionService().getElevationService());

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

    private void chooseBabelPath() {
        JFileChooser chooser = createJFileChooser();
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
        contentPane.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 3, new Insets(3, 3, 3, 3), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        panel2.setLayout(new GridLayoutManager(22, 2, new Insets(3, 3, 3, 3), -1, -1));
        contentPane.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("avoid-highways"));
        panel2.add(label5, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAvoidHighways = new JCheckBox();
        panel2.add(checkBoxAvoidHighways, new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("travel-mode"));
        panel2.add(label6, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        this.$$$loadLabelText$$$(label7, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("map-options"));
        panel2.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel2.add(separator2, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        this.$$$loadLabelText$$$(label8, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("recenter-after-zooming"));
        panel2.add(label8, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxRecenterAfterZooming = new JCheckBox();
        panel2.add(checkBoxRecenterAfterZooming, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        this.$$$loadLabelText$$$(label9, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("numbering-options"));
        panel2.add(label9, new GridConstraints(15, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator3 = new JSeparator();
        panel2.add(separator3, new GridConstraints(16, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(3, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(10, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JSeparator separator4 = new JSeparator();
        panel2.add(separator4, new GridConstraints(20, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        this.$$$loadLabelText$$$(label10, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-options"));
        panel2.add(label10, new GridConstraints(19, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(3, 0, 0, 0), -1, -1));
        panel2.add(panel4, new GridConstraints(18, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel5, new GridConstraints(17, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        comboBoxElevationService = new JComboBox();
        panel5.add(comboBoxElevationService, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboboxNumberPattern = new JComboBox();
        panel5.add(comboboxNumberPattern, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        this.$$$loadLabelText$$$(label11, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("number-pattern"));
        panel5.add(label11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        this.$$$loadLabelText$$$(label12, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("elevation-service"));
        panel5.add(label12, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel6, new GridConstraints(21, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        this.$$$loadLabelText$$$(label13, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-times-with-timezone"));
        panel6.add(label13, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxTimeZone = new JComboBox();
        panel6.add(comboBoxTimeZone, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        this.$$$loadLabelText$$$(label14, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-measures-with-system-of-unit"));
        panel6.add(label14, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxUnitSystem = new JComboBox();
        panel6.add(comboBoxUnitSystem, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label15 = new JLabel();
        this.$$$loadLabelText$$$(label15, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("display-degrees-with-format"));
        panel6.add(label15, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxDegreeFormat = new JComboBox();
        panel6.add(comboBoxDegreeFormat, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label16 = new JLabel();
        this.$$$loadLabelText$$$(label16, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("avoid-tolls"));
        panel2.add(label16, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAvoidTolls = new JCheckBox();
        panel2.add(checkBoxAvoidTolls, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboboxTravelMode = new JComboBox();
        panel2.add(comboboxTravelMode, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label17 = new JLabel();
        this.$$$loadLabelText$$$(label17, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("routing-options"));
        panel2.add(label17, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator5 = new JSeparator();
        panel2.add(separator5, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(3, 0, 0, 0), -1, -1));
        panel2.add(panel7, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label18 = new JLabel();
        this.$$$loadLabelText$$$(label18, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("show-coordinates"));
        panel2.add(label18, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxShowCoordinates = new JCheckBox();
        panel2.add(checkBoxShowCoordinates, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label19 = new JLabel();
        this.$$$loadLabelText$$$(label19, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("waypoint-options"));
        panel2.add(label19, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator6 = new JSeparator();
        panel2.add(separator6, new GridConstraints(12, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label20 = new JLabel();
        this.$$$loadLabelText$$$(label20, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("show-waypoint-description"));
        panel2.add(label20, new GridConstraints(13, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(3, 0, 0, 0), -1, -1));
        panel2.add(panel8, new GridConstraints(14, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        checkBoxShowWaypointDescription = new JCheckBox();
        panel2.add(checkBoxShowWaypointDescription, new GridConstraints(13, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 2, new Insets(3, 3, 0, 3), -1, -1));
        contentPane.add(panel9, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel9.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonClose = new JButton();
        this.$$$loadButtonText$$$(buttonClose, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("close"));
        panel9.add(buttonClose, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator7 = new JSeparator();
        contentPane.add(separator7, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
