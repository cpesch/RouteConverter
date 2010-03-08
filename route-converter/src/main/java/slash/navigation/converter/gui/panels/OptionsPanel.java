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
import com.intellij.uiDesigner.core.Spacer;
import slash.navigation.babel.BabelFormat;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.Updater;
import slash.navigation.converter.gui.helper.CheckBoxPreferencesSynchronizer;
import slash.navigation.converter.gui.helper.FrameAction;
import slash.navigation.converter.gui.renderer.LocaleListCellRenderer;
import slash.navigation.gui.Application;
import slash.navigation.gui.Constants;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The misc panel of the route converter user interface.
 *
 * @author Christian Pesch
 */

public class OptionsPanel {
    private JPanel miscPanel;
    private JLabel labelBrowse;
    private JLabel labelMail;
    private JLabel labelCp;
    private JLabel labelCredit;
    private JComboBox comboBoxLocale;
    private JTextField textFieldBabelPath;
    private JButton buttonChooseGPSBabel;
    private JCheckBox checkBoxAutomaticUpdateCheck;
    private JCheckBox checkBoxStartWithLastFile;
    private JButton buttonCheckForUpdate;
    private JButton buttonPrintMap;
    private JButton buttonPrintMapAndRoute;
    private JButton buttonPrintElevationProfile;

    public OptionsPanel() {
        initialize();
    }

    private void initialize() {
        final RouteConverter r = RouteConverter.getInstance();

        labelBrowse.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                r.createExternalPrograms().startBrowserForHomepage(r.getFrame());
            }
        });

        labelMail.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                r.createExternalPrograms().startBrowserForForum(r.getFrame());
            }
        });

        labelCredit.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                r.createExternalPrograms().startBrowserForHomepage(r.getFrame());
            }
        });

        labelCp.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                r.createExternalPrograms().startMail(r.getFrame());
            }
        });

        buttonChooseGPSBabel.addActionListener(new FrameAction() {
            public void run() {
                chooseBabelPath();
            }
        });

        comboBoxLocale.setModel(new DefaultComboBoxModel(new Object[]{Locale.CHINA, Locale.GERMANY, Locale.US, Locale.FRANCE, Constants.NL, Constants.ROOT_LOCALE}));
        comboBoxLocale.setRenderer(new LocaleListCellRenderer());
        comboBoxLocale.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED)
                    return;
                Locale locale = (Locale) e.getItem();
                Application.getInstance().setLocale(locale);
            }
        });
        comboBoxLocale.setSelectedItem(Locale.getDefault());

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

        new CheckBoxPreferencesSynchronizer(checkBoxAutomaticUpdateCheck, r.getPreferences(), RouteConverter.AUTOMATIC_UPDATE_CHECK_PREFERENCE, true);
        new CheckBoxPreferencesSynchronizer(checkBoxStartWithLastFile, r.getPreferences(), RouteConverter.START_WITH_LAST_FILE_PREFERENCE, true);

        buttonCheckForUpdate.addActionListener(new FrameAction() {
            public void run() {
                new Updater().explicitCheck(r.getFrame());
            }
        });

        buttonPrintMap.addActionListener(new FrameAction() {
            public void run() {
                if (r.isMapViewAvailable())
                    r.printMap(false);
            }
        });
        buttonPrintMap.setEnabled(r.isMapViewAvailable());

        buttonPrintMapAndRoute.addActionListener(new FrameAction() {
            public void run() {
                if (r.isMapViewAvailable())
                    r.printMap(true);
            }
        });
        buttonPrintMapAndRoute.setEnabled(r.isMapViewAvailable());

        buttonPrintElevationProfile.addActionListener(new FrameAction() {
            public void run() {
                r.printElevationProfile();
            }
        });
    }

    public Component getRootComponent() {
        return miscPanel;
    }

    private void chooseBabelPath() {
        JFileChooser chooser = Constants.createJFileChooser();
        chooser.setDialogTitle(RouteConverter.getBundle().getString("choose-gpsbabel-path"));
        chooser.setSelectedFile(new File(BabelFormat.getBabelPathPreference()));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int open = chooser.showOpenDialog(RouteConverter.getInstance().getFrame());
        if (open != JFileChooser.APPROVE_OPTION)
            return;

        File selected = chooser.getSelectedFile();
        if (selected == null || selected.getName().length() == 0)
            return;

        textFieldBabelPath.setText(selected.getAbsolutePath());
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
        miscPanel.setLayout(new GridLayoutManager(7, 1, new Insets(3, 3, 3, 3), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 3, new Insets(3, 3, 3, 3), -1, -1));
        miscPanel.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("preferred-locale"));
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxLocale = new JComboBox();
        panel1.add(comboBoxLocale, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("gpsbabel-path"));
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldBabelPath = new JTextField();
        panel1.add(textFieldBabelPath, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonChooseGPSBabel = new JButton();
        buttonChooseGPSBabel.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/select.png")));
        buttonChooseGPSBabel.setText("");
        buttonChooseGPSBabel.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("choose-gpsbabel-path"));
        panel1.add(buttonChooseGPSBabel, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("start-with-last-file"));
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxStartWithLastFile = new JCheckBox();
        checkBoxStartWithLastFile.setHorizontalAlignment(11);
        checkBoxStartWithLastFile.setSelected(false);
        checkBoxStartWithLastFile.setText("");
        panel1.add(checkBoxStartWithLastFile, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("automatic-update-check"));
        panel1.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAutomaticUpdateCheck = new JCheckBox();
        checkBoxAutomaticUpdateCheck.setHorizontalAlignment(11);
        checkBoxAutomaticUpdateCheck.setHorizontalTextPosition(11);
        checkBoxAutomaticUpdateCheck.setText("");
        panel1.add(checkBoxAutomaticUpdateCheck, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        miscPanel.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 20), null, null, 0, false));
        final JPanel panel3 = new JPanel();
        miscPanel.add(panel3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 20), null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(3, 2, new Insets(3, 3, 3, 3), -1, -1));
        miscPanel.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        labelBrowse = new JLabel();
        this.$$$loadLabelText$$$(labelBrowse, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("options-www"));
        panel4.add(labelBrowse, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelMail = new JLabel();
        this.$$$loadLabelText$$$(labelMail, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("options-mail"));
        panel4.add(labelMail, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelCp = new JLabel();
        labelCp.setForeground(UIManager.getColor("Label.background"));
        labelCp.setIcon(new ImageIcon(getClass().getResource("/slash/navigation/converter/gui/cp.png")));
        labelCp.setInheritsPopupMenu(true);
        labelCp.setOpaque(false);
        labelCp.setText("");
        panel4.add(labelCp, new GridConstraints(0, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        labelCredit = new JLabel();
        this.$$$loadLabelText$$$(labelCredit, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("options-credit"));
        panel4.add(labelCredit, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(3, 3, 3, 3), -1, -1));
        miscPanel.add(panel5, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCheckForUpdate = new JButton();
        this.$$$loadButtonText$$$(buttonCheckForUpdate, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("options-check-for-update"));
        panel5.add(buttonCheckForUpdate, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        miscPanel.add(panel6, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 4, new Insets(3, 3, 3, 3), -1, -1));
        miscPanel.add(panel7, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonPrintMap = new JButton();
        buttonPrintMap.setAlignmentY(0.0f);
        this.$$$loadButtonText$$$(buttonPrintMap, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("print-map"));
        buttonPrintMap.setVerifyInputWhenFocusTarget(false);
        panel7.add(buttonPrintMap, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel7.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonPrintMapAndRoute = new JButton();
        buttonPrintMapAndRoute.setAlignmentY(0.0f);
        this.$$$loadButtonText$$$(buttonPrintMapAndRoute, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("print-map-and-route"));
        buttonPrintMapAndRoute.setVerifyInputWhenFocusTarget(false);
        panel7.add(buttonPrintMapAndRoute, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonPrintElevationProfile = new JButton();
        this.$$$loadButtonText$$$(buttonPrintElevationProfile, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("print-elevation-profile"));
        panel7.add(buttonPrintElevationProfile, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        return miscPanel;
    }
}
