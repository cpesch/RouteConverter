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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.converter.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;
import java.text.MessageFormat;

import slash.navigation.converter.gui.models.NumberDocument;
import slash.navigation.converter.gui.helper.FrameAction;

/**
 * Dialog for selecting filter criteria
 *
 * @author Christian Pesch
 */

public class FilterDialog extends JDialog {
    private JPanel contentPane;

    private JTextField textFieldDuplicates;
    private JTextField textFieldDistance;
    private JTextField textFieldOrder;
    private JTextField textFieldSignificance;
    private JButton buttonSelectDuplicates;
    private JButton buttonSelectByDistance;
    private JButton buttonSelectByOrder;
    private JButton buttonSelectBySignificance;
    private JButton buttonDeletePositions;
    private JButton buttonClearSelection;
    private JLabel labelResult;
    private NumberDocument duplicate;
    private NumberDocument distance;
    private NumberDocument order;
    private NumberDocument significance;

    public FilterDialog() {
        super(RouteConverter.getInstance().getFrame());
        setTitle(RouteConverter.getBundle().getString("filter-title"));
        setContentPane(contentPane);

        buttonSelectDuplicates.addActionListener(new FrameAction() {
            public void run() {
                onSelectDuplicates();
            }
        });

        buttonSelectByDistance.addActionListener(new FrameAction() {
            public void run() {
                onSelectByDistance();
            }
        });

        buttonSelectByOrder.addActionListener(new FrameAction() {
            public void run() {
                onSelectByOrder();
            }
        });

        buttonSelectBySignificance.addActionListener(new FrameAction() {
            public void run() {
                onSelectBySignificance();
            }
        });

        buttonDeletePositions.addActionListener(new FrameAction() {
            public void run() {
                onDeletePositions();
            }
        });

        buttonClearSelection.addActionListener(new FrameAction() {
            public void run() {
                onRemoveSelection();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });

        contentPane.registerKeyboardAction(new FrameAction() {
            public void run() {
                onClose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        RouteConverter r = RouteConverter.getInstance();
        duplicate = new NumberDocument(r.getSelectDuplicatePreference());
        textFieldDuplicates.setDocument(duplicate);
        distance = new NumberDocument(r.getSelectByDistancePreference());
        textFieldDistance.setDocument(distance);
        order = new NumberDocument(r.getSelectByOrderPreference());
        textFieldOrder.setDocument(order);
        significance = new NumberDocument(r.getSelectBySignificancePreference());
        textFieldSignificance.setDocument(significance);
    }

    private void savePreferences() {
        RouteConverter r = RouteConverter.getInstance();
        r.setSelectDuplicatePreference(duplicate.getNumber());
        r.setSelectByDistancePreference(distance.getNumber());
        r.setSelectByOrderPreference(order.getNumber());
        r.setSelectBySignificancePreference(significance.getNumber());
    }

    private void onSelectDuplicates() {
        int duplicate = this.duplicate.getNumber();
        if (duplicate >= 0) {
            int selectedRowCount = RouteConverter.getInstance().selectDuplicatesWithinDistance(duplicate);
            labelResult.setText(MessageFormat.format(RouteConverter.getBundle().getString("filter-select-duplicates-result"), selectedRowCount, duplicate));
            savePreferences();
        }
    }

    private void onSelectByDistance() {
        int distance = this.distance.getNumber();
        if (distance >= 0) {
            int selectedRowCount = RouteConverter.getInstance().selectPositionsThatRemainingHaveDistance(distance);
            labelResult.setText(MessageFormat.format(RouteConverter.getBundle().getString("filter-select-by-distance-result"), selectedRowCount, distance));
            savePreferences();
        }
    }

    private void onSelectByOrder() {
        int order = this.order.getNumber();
        if (order >= 0) {
            int selectedRowCount = RouteConverter.getInstance().selectAllButEveryNthPosition(order);
            int unselectedRowCount = RouteConverter.getInstance().getPositionsModel().getRowCount() - selectedRowCount;
            labelResult.setText(MessageFormat.format(RouteConverter.getBundle().getString("filter-select-by-order-result"), selectedRowCount, unselectedRowCount));
            savePreferences();
        }
    }

    private void onSelectBySignificance() {
        int significance = this.significance.getNumber();
        if (significance >= 0) {
            int selectedRowCount = RouteConverter.getInstance().selectInsignificantPositions(significance);
            labelResult.setText(MessageFormat.format(RouteConverter.getBundle().getString("filter-select-by-significance-result"), selectedRowCount, significance));
            savePreferences();
        }
    }

    private void onDeletePositions() {
        RouteConverter.getInstance().onRemovePosition();
        onClose();
    }

    private void onRemoveSelection() {
        RouteConverter.getInstance().clearPositionSelection();
        labelResult.setText(RouteConverter.getBundle().getString("filter-remove-selection-result"));
    }

    private void onClose() {
        savePreferences();
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
        contentPane.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(10, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel1.add(panel2, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("filter-select-by-order"));
        panel2.add(label1);
        textFieldOrder = new JTextField();
        textFieldOrder.setColumns(3);
        panel2.add(textFieldOrder);
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("filter-select-by-order-meter"));
        panel2.add(label2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        buttonDeletePositions = new JButton();
        this.$$$loadButtonText$$$(buttonDeletePositions, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-positions"));
        panel3.add(buttonDeletePositions, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel1.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("filter-select-duplicates"));
        panel4.add(label3);
        textFieldDuplicates = new JTextField();
        textFieldDuplicates.setColumns(5);
        panel4.add(textFieldDuplicates);
        final JLabel label4 = new JLabel();
        label4.setMaximumSize(new Dimension(-1, 14));
        this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("filter-select-duplicates-meter"));
        panel4.add(label4);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel5, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel5.add(separator1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel1.add(panel6, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("filter-select-by-distance"));
        panel6.add(label5);
        textFieldDistance = new JTextField();
        textFieldDistance.setColumns(5);
        panel6.add(textFieldDistance);
        final JLabel label6 = new JLabel();
        label6.setMaximumSize(new Dimension(-1, 14));
        this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("filter-select-by-distance-meter"));
        panel6.add(label6);
        buttonSelectDuplicates = new JButton();
        this.$$$loadButtonText$$$(buttonSelectDuplicates, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("select"));
        panel1.add(buttonSelectDuplicates, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonSelectByDistance = new JButton();
        this.$$$loadButtonText$$$(buttonSelectByDistance, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("select"));
        panel1.add(buttonSelectByDistance, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonSelectByOrder = new JButton();
        this.$$$loadButtonText$$$(buttonSelectByOrder, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("select"));
        panel1.add(buttonSelectByOrder, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel7, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel7.add(separator2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel8, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JSeparator separator3 = new JSeparator();
        panel8.add(separator3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel9, new GridConstraints(8, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 30), null, null, 0, false));
        labelResult = new JLabel();
        labelResult.setText("");
        panel9.add(labelResult, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel1.add(panel10, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        this.$$$loadLabelText$$$(label7, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("filter-select-by-significance"));
        panel10.add(label7);
        textFieldSignificance = new JTextField();
        textFieldSignificance.setColumns(3);
        panel10.add(textFieldSignificance);
        final JLabel label8 = new JLabel();
        this.$$$loadLabelText$$$(label8, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("filter-select-by-significance-meter"));
        panel10.add(label8);
        buttonSelectBySignificance = new JButton();
        this.$$$loadButtonText$$$(buttonSelectBySignificance, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("select"));
        panel1.add(buttonSelectBySignificance, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel11, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JSeparator separator4 = new JSeparator();
        panel11.add(separator4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        buttonClearSelection = new JButton();
        this.$$$loadButtonText$$$(buttonClearSelection, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("filter-remove-selection"));
        panel1.add(buttonClearSelection, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
