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

package slash.navigation.converter.gui.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.converter.gui.models.IntegerDocument;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.DialogAction;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.navigation.gui.helpers.JMenuHelper.setMnemonic;

/**
 * Dialog for inserting {@link BaseNavigationPosition}s into the current {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class InsertPositionsDialog extends SimpleDialog {
    private JPanel contentPane;

    private JLabel labelSelection;
    private JButton buttonSelectAll;
    private JButton buttonClearSelection;
    private JButton buttonInsertAllWaypoints;
    private JTextField textFieldStraightLineInterval;
    private JButton buttonInsertStraightLine;
    private IntegerDocument straightLineInterval;

    public InsertPositionsDialog() {
        super(BaseRouteConverter.getInstance().getFrame(), "insert-positions");
        setTitle(BaseRouteConverter.getBundle().getString("insert-positions-title"));
        setContentPane(contentPane);

        BaseRouteConverter r = BaseRouteConverter.getInstance();

        setMnemonic(buttonSelectAll, "select-all-action-mnemonic");
        buttonSelectAll.addActionListener(new DialogAction(this) {
            public void run() {
                selectAll();
            }
        });

        setMnemonic(buttonClearSelection, "clear-selection-action-mnemonic");
        buttonClearSelection.addActionListener(new DialogAction(this) {
            public void run() {
                clearSelection();
            }
        });

        setMnemonic(buttonInsertAllWaypoints, "insert-all-waypoints-mnemonic");
        buttonInsertAllWaypoints.addActionListener(new DialogAction(this) {
            public void run() {
                insertAllWaypoints();
            }
        });

        straightLineInterval = new IntegerDocument(r.getInsertStraightLineIntervalPreference());
        textFieldStraightLineInterval.setDocument(straightLineInterval);
        straightLineInterval.addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateInsertStraightLineEnabled();
            }

            public void removeUpdate(DocumentEvent e) {
                updateInsertStraightLineEnabled();
            }

            public void changedUpdate(DocumentEvent e) {
                updateInsertStraightLineEnabled();
            }
        });
        textFieldStraightLineInterval.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                textFieldStraightLineInterval.selectAll();
            }
        });

        setMnemonic(buttonInsertStraightLine, "insert-straight-line-action-mnemonic");
        buttonInsertStraightLine.addActionListener(new DialogAction(this) {
            public void run() {
                insertStraightLinePositions();
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
        }, getKeyStroke(VK_ESCAPE, 0), WHEN_IN_FOCUSED_WINDOW);

        final PositionsModel positionsModel = r.getConvertPanel().getPositionsModel();
        r.getConvertPanel().getPositionsView().getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting())
                return;
            if (positionsModel.isContinousRangeOperation())
                return;
            handlePositionsUpdate();
        });
        r.getRoutingServiceFacade().addPreferencesChangeListener(e -> handlePositionsUpdate());
        positionsModel.addTableModelListener(e -> {
            if (positionsModel.isContinousRangeOperation())
                return;
            handlePositionsUpdate();
        });

        handlePositionsUpdate();
    }

    private void handlePositionsUpdate() {
        BaseRouteConverter r = BaseRouteConverter.getInstance();
        int selectedRowCount = r.getConvertPanel().getPositionsView().getSelectedRowCount();
        labelSelection.setText(MessageFormat.format(BaseRouteConverter.getBundle().getString("selected-positions"), selectedRowCount));

        boolean existsSelectedPosition = selectedRowCount > 0;
        // both insert actions operate BETWEEN selected positions -> need a pair
        buttonInsertAllWaypoints.setEnabled(selectedRowCount >= 2);
        buttonClearSelection.setEnabled(existsSelectedPosition);
        updateInsertStraightLineEnabled();

        boolean notAllPositionsSelected = r.getConvertPanel().getPositionsView().getRowCount() > selectedRowCount;
        buttonSelectAll.setEnabled(notAllPositionsSelected);
    }

    private void selectAll() {
        BaseRouteConverter r = BaseRouteConverter.getInstance();
        r.getContext().getActionManager().run("select-all");
        int selectedRowCount = r.getConvertPanel().getPositionsView().getSelectedRowCount();
        labelSelection.setText(MessageFormat.format(BaseRouteConverter.getBundle().getString("selected-all-positions"), selectedRowCount));
    }

    private void clearSelection() {
        BaseRouteConverter.getInstance().clearSelection();
        handlePositionsUpdate();
    }

    private void insertAllWaypoints() {
        BaseRouteConverter.getInstance().getInsertPositionFacade().insertAllWaypoints();
    }

    private void updateInsertStraightLineEnabled() {
        int selectedRowCount = BaseRouteConverter.getInstance().getConvertPanel().getPositionsView().getSelectedRowCount();
        buttonInsertStraightLine.setEnabled(selectedRowCount >= 2 && straightLineInterval.getInt() > 0);
    }

    private void insertStraightLinePositions() {
        BaseRouteConverter r = BaseRouteConverter.getInstance();
        int intervalMetres = straightLineInterval.getInt();
        r.setInsertStraightLineIntervalPreference(intervalMetres);
        r.getInsertPositionFacade().insertStraightLinePositions(intervalMetres);
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
        contentPane.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panelSelection = new JPanel();
        panelSelection.setLayout(new GridLayoutManager(1, 2, new Insets(0, 5, 0, 0), -1, -1));
        panel1.add(panelSelection, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 14), null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "selection"));
        panelSelection.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelSelection = new JLabel();
        labelSelection.setText("-");
        panelSelection.add(labelSelection, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panelGap = new JPanel();
        panelGap.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panelGap, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 2), null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(5, 3, new Insets(0, 0, 10, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonSelectAll = new JButton();
        this.$$$loadButtonText$$$(buttonSelectAll, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "select-all-action"));
        panel2.add(buttonSelectAll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonClearSelection = new JButton();
        this.$$$loadButtonText$$$(buttonClearSelection, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "clear-selection-action"));
        panel2.add(buttonClearSelection, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panelSeparator1 = new JPanel();
        panelSeparator1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), 5, -1));
        panel2.add(panelSeparator1, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panelSeparator1.add(separator1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panelRouting = new JPanel();
        panelRouting.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel2.add(panelRouting, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "insert-positions"));
        panelRouting.add(label2);
        buttonInsertAllWaypoints = new JButton();
        this.$$$loadButtonText$$$(buttonInsertAllWaypoints, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "insert-all-waypoints"));
        panel2.add(buttonInsertAllWaypoints, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panelSeparator2 = new JPanel();
        panelSeparator2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), 5, -1));
        panel2.add(panelSeparator2, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panelSeparator2.add(separator2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panelStraightLine = new JPanel();
        panelStraightLine.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel2.add(panelStraightLine, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "insert-straight-line"));
        panelStraightLine.add(label3);
        textFieldStraightLineInterval = new JTextField();
        textFieldStraightLineInterval.setColumns(4);
        panelStraightLine.add(textFieldStraightLineInterval);
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "insert-straight-line-meter"));
        panelStraightLine.add(label4);
        buttonInsertStraightLine = new JButton();
        this.$$$loadButtonText$$$(buttonInsertStraightLine, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "insert-straight-line-action"));
        panel2.add(buttonInsertStraightLine, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
