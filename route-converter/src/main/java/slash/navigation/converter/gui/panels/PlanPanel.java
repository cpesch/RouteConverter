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
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.dialogs.ComplementPositionsDialog;
import slash.navigation.converter.gui.dialogs.DeletePositionsDialog;
import slash.navigation.converter.gui.dialogs.GeocodePositionDialog;
import slash.navigation.converter.gui.dialogs.InsertPositionsDialog;
import slash.navigation.converter.gui.helper.CheckBoxPreferencesSynchronizer;
import slash.navigation.converter.gui.helper.FrameAction;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

/**
 * The plan panel of the route converter user interface.
 *
 * @author Christian Pesch
 */

public class PlanPanel {
    private JPanel planPanel;
    private JCheckBox checkBoxPedestrians;
    private JCheckBox checkBoxAvoidHighways;
    private JCheckBox checkBoxPrefixNumberWithZeros;
    private JCheckBox checkBoxSpaceBetweenNumberAndComment;
    private JButton buttonInsertIntoPositionList;
    private JButton buttonGeocodePosition;
    private JButton buttonComplementPositionList;
    private JButton buttonDeleteFromPositionList;
    private JButton buttonRevertPositionList;

    public PlanPanel() {
        initialize();
    }

    private void initialize() {
        final RouteConverter r = RouteConverter.getInstance();

        buttonInsertIntoPositionList.addActionListener(new FrameAction() {
            public void run() {
                InsertPositionsDialog options = new InsertPositionsDialog();
                options.pack();
                options.restoreLocation();
                options.setVisible(true);
            }
        });

        buttonGeocodePosition.addActionListener(new FrameAction() {
            public void run() {
                GeocodePositionDialog options = new GeocodePositionDialog();
                options.pack();
                options.restoreLocation();
                options.setVisible(true);
            }
        });

        buttonComplementPositionList.addActionListener(new FrameAction() {
            public void run() {
                ComplementPositionsDialog options = new ComplementPositionsDialog();
                options.pack();
                options.restoreLocation();
                options.setVisible(true);
            }
        });

        buttonDeleteFromPositionList.addActionListener(new FrameAction() {
            public void run() {
                DeletePositionsDialog options = new DeletePositionsDialog();
                options.pack();
                options.restoreLocation();
                options.setVisible(true);
            }
        });

        buttonRevertPositionList.addActionListener(new FrameAction() {
            public void run() {
                r.revertPositions();
            }
        });

        r.getPositionsModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                handlePositionsUpdate();
            }
        });

        new CheckBoxPreferencesSynchronizer(checkBoxPedestrians, r.getPreferences(), RouteConverter.PEDESTRIANS_PREFERENCE, false);
        checkBoxPedestrians.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.setPedestrians(checkBoxPedestrians.isSelected());
            }
        });

        new CheckBoxPreferencesSynchronizer(checkBoxAvoidHighways, r.getPreferences(), RouteConverter.AVOID_HIGHWAYS_PREFERENCE, true);
        checkBoxAvoidHighways.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                r.setAvoidHighways(checkBoxAvoidHighways.isSelected());
            }
        });

        new CheckBoxPreferencesSynchronizer(checkBoxPrefixNumberWithZeros, r.getPreferences(), RouteConverter.PREFIX_NUMBER_WITH_ZEROS, false);
        new CheckBoxPreferencesSynchronizer(checkBoxSpaceBetweenNumberAndComment, r.getPreferences(), RouteConverter.SPACE_BETWEEN_NUMBER_AND_COMMENT_PREFERENCE, false);
    }

    public Component getRootComponent() {
        return planPanel;
    }

    private void handlePositionsUpdate() {
        final RouteConverter r = RouteConverter.getInstance();
        boolean existsAPosition = r.getPositionsModel().getRowCount() > 0;
        boolean existsMoreThanOnePosition = r.getPositionsModel().getRowCount() > 1;

        buttonComplementPositionList.setEnabled(existsAPosition);
        buttonDeleteFromPositionList.setEnabled(existsAPosition);
        buttonRevertPositionList.setEnabled(existsMoreThanOnePosition);
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
        planPanel = new JPanel();
        planPanel.setLayout(new GridLayoutManager(4, 1, new Insets(3, 3, 3, 3), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(6, 3, new Insets(3, 3, 3, 3), -1, -1));
        planPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("avoid-highways"));
        panel1.add(label1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAvoidHighways = new JCheckBox();
        checkBoxAvoidHighways.setSelected(false);
        checkBoxAvoidHighways.setText("");
        panel1.add(checkBoxAvoidHighways, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("space-between-number-and-comment"));
        panel1.add(label2, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxSpaceBetweenNumberAndComment = new JCheckBox();
        checkBoxSpaceBetweenNumberAndComment.setText("");
        panel1.add(checkBoxSpaceBetweenNumberAndComment, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("pedestrians"));
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxPedestrians = new JCheckBox();
        checkBoxPedestrians.setText("");
        panel1.add(checkBoxPedestrians, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("prefix-number-with-zeros"));
        panel1.add(label4, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxPrefixNumberWithZeros = new JCheckBox();
        checkBoxPrefixNumberWithZeros.setText("");
        panel1.add(checkBoxPrefixNumberWithZeros, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("options"));
        panel1.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel1.add(separator1, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(7, 2, new Insets(3, 3, 3, 3), -1, -1));
        planPanel.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonInsertIntoPositionList = new JButton();
        this.$$$loadButtonText$$$(buttonInsertIntoPositionList, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("insert-into-positionlist"));
        buttonInsertIntoPositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("insert-into-positionlist-tooltip"));
        panel2.add(buttonInsertIntoPositionList, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("plan-tab"));
        panel2.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel2.add(separator2, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        buttonGeocodePosition = new JButton();
        this.$$$loadButtonText$$$(buttonGeocodePosition, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("geocode-position"));
        buttonGeocodePosition.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("geocode-position-tooltip"));
        panel2.add(buttonGeocodePosition, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRevertPositionList = new JButton();
        this.$$$loadButtonText$$$(buttonRevertPositionList, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("revert"));
        buttonRevertPositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("revert-tooltip"));
        panel2.add(buttonRevertPositionList, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonComplementPositionList = new JButton();
        this.$$$loadButtonText$$$(buttonComplementPositionList, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("complement-positionlist"));
        buttonComplementPositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("complement-positionlist-tooltip"));
        panel2.add(buttonComplementPositionList, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonDeleteFromPositionList = new JButton();
        this.$$$loadButtonText$$$(buttonDeleteFromPositionList, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-from-positionlist"));
        buttonDeleteFromPositionList.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-from-positionlist-tooltip"));
        panel2.add(buttonDeleteFromPositionList, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        planPanel.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 20), null, null, 0, false));
        final JPanel panel4 = new JPanel();
        planPanel.add(panel4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
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
        return planPanel;
    }
}
