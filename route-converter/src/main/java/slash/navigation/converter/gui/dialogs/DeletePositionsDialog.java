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
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.helper.DialogAction;
import slash.navigation.converter.gui.models.NumberDocument;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Dialog for selecting and deleting {@link slash.navigation.BaseNavigationPosition}s from the current {@link slash.navigation.BaseRoute}.
 *
 * @author Christian Pesch
 */

public class DeletePositionsDialog extends JDialog {
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
    private JLabel labelSelection;
    private NumberDocument duplicate;
    private NumberDocument distance;
    private NumberDocument order;
    private NumberDocument significance;

    public DeletePositionsDialog() {
        super(RouteConverter.getInstance().getFrame());
        setTitle(RouteConverter.getBundle().getString("delete-positions-title"));
        setContentPane(contentPane);

        buttonSelectDuplicates.addActionListener(new DialogAction(this) {
            public void run() {
                selectDuplicates();
            }
        });

        buttonSelectByDistance.addActionListener(new DialogAction(this) {
            public void run() {
                selectByDistance();
            }
        });

        buttonSelectByOrder.addActionListener(new DialogAction(this) {
            public void run() {
                selectByOrder();
            }
        });

        buttonSelectBySignificance.addActionListener(new DialogAction(this) {
            public void run() {
                selectBySignificance();
            }
        });

        buttonDeletePositions.addActionListener(new DialogAction(this) {
            public void run() {
                deletePositions();
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

    private void selectDuplicates() {
        int duplicate = this.duplicate.getNumber();
        if (duplicate >= 0) {
            int selectedRowCount = RouteConverter.getInstance().selectDuplicatesWithinDistance(duplicate);
            labelSelection.setText(MessageFormat.format(RouteConverter.getBundle().getString("filter-select-duplicates-result"), selectedRowCount, duplicate));
            savePreferences();
        }
    }

    private void selectByDistance() {
        int distance = this.distance.getNumber();
        if (distance >= 0) {
            int selectedRowCount = RouteConverter.getInstance().selectPositionsThatRemainingHaveDistance(distance);
            labelSelection.setText(MessageFormat.format(RouteConverter.getBundle().getString("filter-select-by-distance-result"), selectedRowCount, distance));
            savePreferences();
        }
    }

    private void selectByOrder() {
        int order = this.order.getNumber();
        if (order >= 0) {
            int[] selectedRowCount = RouteConverter.getInstance().selectAllButEveryNthPosition(order);
            labelSelection.setText(MessageFormat.format(RouteConverter.getBundle().getString("filter-select-by-order-result"), selectedRowCount[0], selectedRowCount[1]));
            savePreferences();
        }
    }

    private void selectBySignificance() {
        int significance = this.significance.getNumber();
        if (significance >= 0) {
            int selectedRowCount = RouteConverter.getInstance().selectInsignificantPositions(significance);
            labelSelection.setText(MessageFormat.format(RouteConverter.getBundle().getString("filter-select-by-significance-result"), selectedRowCount, significance));
            savePreferences();
        }
    }

    private void deletePositions() {
        RouteConverter.getInstance().deletePositions();
        close();
    }

    private void savePreferences() {
        RouteConverter r = RouteConverter.getInstance();
        r.setSelectDuplicatePreference(duplicate.getNumber());
        r.setSelectByDistancePreference(distance.getNumber());
        r.setSelectByOrderPreference(order.getNumber());
        r.setSelectBySignificancePreference(significance.getNumber());
    }

    private void close() {
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
        panel1.setLayout(new GridLayoutManager(7, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(7, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel2.add(panel3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("alle außer jeder");
        panel3.add(label1);
        textFieldOrder = new JTextField();
        textFieldOrder.setColumns(3);
        panel3.add(textFieldOrder);
        final JLabel label2 = new JLabel();
        label2.setText("ten Position");
        panel3.add(label2);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel2.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("doppelte innerhalb von");
        panel4.add(label3);
        textFieldDuplicates = new JTextField();
        textFieldDuplicates.setColumns(5);
        panel4.add(textFieldDuplicates);
        final JLabel label4 = new JLabel();
        label4.setMaximumSize(new Dimension(-1, 14));
        label4.setText("Metern zum Vorgänger");
        panel4.add(label4);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), 5, -1));
        panel2.add(panel5, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel5.add(separator1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel2.add(panel6, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("so daß mindestens");
        panel6.add(label5);
        textFieldDistance = new JTextField();
        textFieldDistance.setColumns(5);
        panel6.add(textFieldDistance);
        final JLabel label6 = new JLabel();
        label6.setMaximumSize(new Dimension(-1, 14));
        label6.setText("Meter zwischen den Positionen verbleiben");
        panel6.add(label6);
        buttonSelectDuplicates = new JButton();
        this.$$$loadButtonText$$$(buttonSelectDuplicates, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("select"));
        panel2.add(buttonSelectDuplicates, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonSelectByDistance = new JButton();
        this.$$$loadButtonText$$$(buttonSelectByDistance, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("select"));
        panel2.add(buttonSelectByDistance, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonSelectByOrder = new JButton();
        this.$$$loadButtonText$$$(buttonSelectByOrder, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("select"));
        panel2.add(buttonSelectByOrder, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), 5, -1));
        panel2.add(panel7, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel7.add(separator2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), 5, -1));
        panel2.add(panel8, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JSeparator separator3 = new JSeparator();
        panel8.add(separator3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel2.add(panel9, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("alle nach Douglas-Peucker redundanten Positionen mit einem Grenzwert von");
        panel9.add(label7);
        textFieldSignificance = new JTextField();
        textFieldSignificance.setColumns(3);
        panel9.add(textFieldSignificance);
        final JLabel label8 = new JLabel();
        label8.setText("Metern");
        panel9.add(label8);
        buttonSelectBySignificance = new JButton();
        this.$$$loadButtonText$$$(buttonSelectBySignificance, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("select"));
        panel2.add(buttonSelectBySignificance, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("1. Schritt:");
        panel1.add(label9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("Markieren der zu löschenden Positionen");
        panel1.add(label10, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("2. Schritt:");
        panel1.add(label11, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel10, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 15), null, null, 0, false));
        buttonDeletePositions = new JButton();
        buttonDeletePositions.setText("Löschen");
        panel1.add(buttonDeletePositions, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("Löschen der markierten Positionen");
        panel1.add(label12, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel11, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 30), null, null, 0, false));
        labelSelection = new JLabel();
        labelSelection.setText("-");
        panel11.add(labelSelection, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("Markierung:");
        panel1.add(label13, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel12, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 15), null, null, 0, false));
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
