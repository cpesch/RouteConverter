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
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.DoubleDocument;
import slash.navigation.converter.gui.models.IntegerDocument;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.Application;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.DialogAction;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startBrowserForDouglasPeucker;
import static slash.navigation.gui.helpers.JMenuHelper.setMnemonic;

/**
 * Dialog for selecting and deleting {@link BaseNavigationPosition}s from the current {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class DeletePositionsDialog extends SimpleDialog {
    private JPanel contentPane;

    private JLabel labelSelection;
    private JTextField textFieldDistance;
    private JTextField textFieldOrder;
    private JTextField textFieldSignificance;
    private JButton buttonSelectByDistance;
    private JButton buttonSelectByOrder;
    private JButton buttonSelectBySignificance;
    private JButton buttonDeletePositions;
    private JButton buttonClearSelection;
    private JLabel labelDouglasPeucker;
    private DoubleDocument distance;
    private IntegerDocument order;
    private DoubleDocument threshold;

    public DeletePositionsDialog() {
        super(RouteConverter.getInstance().getFrame(), "delete-positions");
        setTitle(RouteConverter.getBundle().getString("delete-positions-title"));
        setContentPane(contentPane);

        final RouteConverter r = RouteConverter.getInstance();

        labelDouglasPeucker.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                startBrowserForDouglasPeucker(DeletePositionsDialog.this);
            }
        });

        setMnemonic(buttonSelectByDistance, "select-mnemonic");
        buttonSelectByDistance.addActionListener(new DialogAction(this) {
            public void run() {
                selectByDistance();
            }
        });

        setMnemonic(buttonSelectByOrder, "select-mnemonic");
        buttonSelectByOrder.addActionListener(new DialogAction(this) {
            public void run() {
                selectByOrder();
            }
        });

        setMnemonic(buttonSelectBySignificance, "select-mnemonic");
        buttonSelectBySignificance.addActionListener(new DialogAction(this) {
            public void run() {
                selectBySignificance();
            }
        });

        setMnemonic(buttonClearSelection, "clear-selection-action-mnemonic");
        buttonClearSelection.addActionListener(new DialogAction(this) {
            public void run() {
                clearSelection();
            }
        });

        setMnemonic(buttonDeletePositions, "delete-selected-positions-mnemonic");
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
        }, getKeyStroke(VK_ESCAPE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        distance = new DoubleDocument(r.getSelectByDistancePreference());
        textFieldDistance.setDocument(distance);
        order = new IntegerDocument(r.getSelectByOrderPreference());
        textFieldOrder.setDocument(order);
        threshold = new DoubleDocument(r.getSelectBySignificancePreference());
        textFieldSignificance.setDocument(threshold);

        final PositionsModel positionsModel = r.getConvertPanel().getPositionsModel();
        r.getConvertPanel().getPositionsView().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                if (positionsModel.isContinousRange())
                    return;
                handlePositionsUpdate();
            }
        });
        positionsModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                if (positionsModel.isContinousRange())
                    return;
                handlePositionsUpdate();
            }
        });

        handlePositionsUpdate();
    }

    private void handlePositionsUpdate() {
        int selectedRowCount = RouteConverter.getInstance().getConvertPanel().getPositionsView().getSelectedRowCount();
        labelSelection.setText(MessageFormat.format(RouteConverter.getBundle().getString("selected-positions"), selectedRowCount));

        boolean existsSelectedPosition = selectedRowCount > 0;
        buttonDeletePositions.setEnabled(existsSelectedPosition);
        buttonClearSelection.setEnabled(existsSelectedPosition);
    }

    private void selectByDistance() {
        double distance = this.distance.getDouble();
        if (distance >= 0) {
            int selectedRowCount = RouteConverter.getInstance().selectPositionsWithinDistanceToPredecessor(distance);
            labelSelection.setText(MessageFormat.format(RouteConverter.getBundle().getString("delete-select-by-distance-result"), selectedRowCount, distance));
            savePreferences();
        }
    }

    private void selectByOrder() {
        int order = this.order.getInt();
        if (order >= 0) {
            int[] selectedRowCount = RouteConverter.getInstance().selectAllButEveryNthPosition(order);
            labelSelection.setText(MessageFormat.format(RouteConverter.getBundle().getString("delete-select-by-order-result"), selectedRowCount[0], selectedRowCount[1]));
            savePreferences();
        }
    }

    private void selectBySignificance() {
        double threshold = this.threshold.getDouble();
        if (threshold >= 0) {
            int selectedRowCount = RouteConverter.getInstance().selectInsignificantPositions(threshold);
            labelSelection.setText(MessageFormat.format(RouteConverter.getBundle().getString("delete-select-by-significance-result"), selectedRowCount, threshold));
            savePreferences();
        }
    }

    private void clearSelection() {
        RouteConverter.getInstance().clearSelection();
        handlePositionsUpdate();
    }

    private void deletePositions() {
        Application.getInstance().getContext().getActionManager().run("delete-position");
        handlePositionsUpdate();
    }

    private void savePreferences() {
        RouteConverter r = RouteConverter.getInstance();
        r.setSelectByDistancePreference(distance.getDouble());
        r.setSelectByOrderPreference(order.getInt());
        r.setSelectBySignificancePreference(threshold.getDouble());
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
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(7, 3, new Insets(0, 0, 10, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel2.add(panel3, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-select-by-order"));
        panel3.add(label1);
        textFieldOrder = new JTextField();
        textFieldOrder.setColumns(3);
        panel3.add(textFieldOrder);
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-select-by-order-meter"));
        panel3.add(label2);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel2.add(panel4, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-select-by-distance"));
        panel4.add(label3);
        textFieldDistance = new JTextField();
        textFieldDistance.setColumns(5);
        panel4.add(textFieldDistance);
        final JLabel label4 = new JLabel();
        label4.setMaximumSize(new Dimension(-1, 14));
        this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-select-by-distance-meter"));
        panel4.add(label4);
        buttonSelectByDistance = new JButton();
        this.$$$loadButtonText$$$(buttonSelectByDistance, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("select"));
        panel2.add(buttonSelectByDistance, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonSelectByOrder = new JButton();
        this.$$$loadButtonText$$$(buttonSelectByOrder, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("select"));
        panel2.add(buttonSelectByOrder, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), 5, -1));
        panel2.add(panel5, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel5.add(separator1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), 5, -1));
        panel2.add(panel6, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel6.add(separator2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel2.add(panel7, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-select-by-significance"));
        panel7.add(label5);
        textFieldSignificance = new JTextField();
        textFieldSignificance.setColumns(3);
        panel7.add(textFieldSignificance);
        labelDouglasPeucker = new JLabel();
        this.$$$loadLabelText$$$(labelDouglasPeucker, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-select-by-significance-meter"));
        panel7.add(labelDouglasPeucker);
        buttonSelectBySignificance = new JButton();
        this.$$$loadButtonText$$$(buttonSelectBySignificance, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("select"));
        panel2.add(buttonSelectBySignificance, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), 5, -1));
        panel2.add(panel8, new GridConstraints(5, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JSeparator separator3 = new JSeparator();
        panel8.add(separator3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        buttonClearSelection = new JButton();
        this.$$$loadButtonText$$$(buttonClearSelection, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("clear-selection-action"));
        panel2.add(buttonClearSelection, new GridConstraints(6, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonDeletePositions = new JButton();
        this.$$$loadButtonText$$$(buttonDeletePositions, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("delete-selected-positions"));
        panel2.add(buttonDeletePositions, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 2, new Insets(0, 5, 0, 0), -1, -1));
        panel1.add(panel9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 20), null, null, 0, false));
        labelSelection = new JLabel();
        labelSelection.setText("-");
        panel9.add(labelSelection, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("selection"));
        panel9.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel10, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
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
