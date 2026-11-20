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
import slash.navigation.base.BaseRoute;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.renderer.RouteCharacteristicsListCellRenderer;
import slash.navigation.converter.gui.renderer.RouteListCellRenderer;
import slash.navigation.converter.gui.undo.UndoFormatAndRoutesModel;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.DialogAction;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

import static java.awt.event.KeyEvent.VK_DELETE;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JComponent.WHEN_FOCUSED;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.navigation.converter.gui.helpers.RowMoves.*;
import static slash.navigation.gui.helpers.IconHelper.loadIcon;
import static slash.navigation.gui.helpers.JMenuHelper.registerAction;
import static slash.navigation.gui.helpers.JMenuHelper.setMnemonic;

/**
 * Dialog to reorder, rename, retype and delete the position lists of the current file.
 *
 * @author Christian Pesch
 */

public class ManagePositionListsDialog extends SimpleDialog {
    private JPanel contentPane;
    private JList<BaseRoute<?, ?>> listPositionLists;
    private JButton buttonTop;
    private JButton buttonUp;
    private JButton buttonDown;
    private JButton buttonBottom;
    private JButton buttonNew;
    private JButton buttonRename;
    private JButton buttonDelete;
    private JButton buttonClose;
    private JComboBox<RouteCharacteristics> comboBoxCharacteristics;

    private final UndoFormatAndRoutesModel formatAndRoutesModel;
    private final ListDataListener modelListener;
    // true while this dialog changes the model or the marking itself
    private boolean updating;
    // true from a model change until the marking is reset to the current position list
    private boolean modelChanging;

    public ManagePositionListsDialog(UndoFormatAndRoutesModel formatAndRoutesModel, CharacteristicsModel characteristicsModel) {
        super(BaseRouteConverter.getInstance().getFrame(), "manage-position-lists");
        this.formatAndRoutesModel = formatAndRoutesModel;
        setTitle(BaseRouteConverter.getBundle().getString("manage-positionlist-title"));
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonClose);

        listPositionLists.setModel(formatAndRoutesModel);
        listPositionLists.setCellRenderer(new RouteListCellRenderer());
        listPositionLists.setSelectionMode(MULTIPLE_INTERVAL_SELECTION);
        // the type of the current position list, which is the marked one when exactly one is marked
        comboBoxCharacteristics.setModel(characteristicsModel);
        comboBoxCharacteristics.setRenderer(new RouteCharacteristicsListCellRenderer());
        listPositionLists.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updating || modelChanging)
                return;
            selectLeadAsCurrent();
            updateButtons();
        });

        // added after the JList's own listener: the model notifies listeners last-added first, so
        // modelChanging is set before the JList adjusts its marking to the change
        modelListener = new ListDataListener() {
            public void intervalAdded(ListDataEvent e) {
                modelChanged();
            }

            public void intervalRemoved(ListDataEvent e) {
                modelChanged();
            }

            public void contentsChanged(ListDataEvent e) {
                modelChanged();
            }
        };
        formatAndRoutesModel.addListDataListener(modelListener);

        buttonTop.setIcon(loadIcon("/slash/navigation/converter/gui/24/top.png"));
        buttonUp.setIcon(loadIcon("/slash/navigation/converter/gui/24/up.png"));
        buttonDown.setIcon(loadIcon("/slash/navigation/converter/gui/24/down.png"));
        buttonBottom.setIcon(loadIcon("/slash/navigation/converter/gui/24/bottom.png"));

        buttonTop.addActionListener(new DialogAction(this) {
            public void run() {
                move(top(getMarked()));
            }
        });
        buttonUp.addActionListener(new DialogAction(this) {
            public void run() {
                move(up(getMarked(), 1));
            }
        });
        buttonDown.addActionListener(new DialogAction(this) {
            public void run() {
                move(down(getMarked(), 1, formatAndRoutesModel.getSize()));
            }
        });
        buttonBottom.addActionListener(new DialogAction(this) {
            public void run() {
                move(bottom(getMarked(), formatAndRoutesModel.getSize()));
            }
        });
        // the existing action, so the button follows its enabled state (formats with several position lists)
        registerAction(buttonNew, "new-positionlist");
        buttonRename.addActionListener(new DialogAction(this) {
            public void run() {
                rename();
            }
        });
        DialogAction deleteAction = new DialogAction(this) {
            public void run() {
                delete();
            }
        };
        buttonDelete.addActionListener(deleteAction);
        listPositionLists.registerKeyboardAction(deleteAction, getKeyStroke(VK_DELETE, 0), WHEN_FOCUSED);

        setMnemonic(buttonClose, "close-mnemonic");
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

        markCurrent();
    }

    private int[] getMarked() {
        return listPositionLists.getSelectedIndices();
    }

    private void modelChanged() {
        if (updating)
            return;
        // the JList adjusts its marking to the change after this listener: reset it afterwards
        modelChanging = true;
        invokeLater(() -> {
            modelChanging = false;
            markCurrent();
        });
    }

    private void markCurrent() {
        updating = true;
        try {
            BaseRoute<?, ?> current = formatAndRoutesModel.getSelectedRoute();
            int index = current != null ? formatAndRoutesModel.getIndex(current) : -1;
            if (index != -1) {
                listPositionLists.setSelectedIndex(index);
                listPositionLists.ensureIndexIsVisible(index);
            } else
                listPositionLists.clearSelection();
        } finally {
            updating = false;
        }
        updateButtons();
    }

    private void selectLeadAsCurrent() {
        int lead = listPositionLists.getLeadSelectionIndex();
        if (lead == -1 || !listPositionLists.isSelectedIndex(lead))
            lead = listPositionLists.getMinSelectionIndex();
        if (lead == -1 || lead >= formatAndRoutesModel.getSize())
            return;

        updating = true;
        try {
            formatAndRoutesModel.setSelectedRoute(formatAndRoutesModel.getRoute(lead), false);
        } finally {
            updating = false;
        }
    }

    private void updateButtons() {
        int[] marked = getMarked();
        int size = formatAndRoutesModel.getSize();
        boolean valid = marked.length == 0 || marked[marked.length - 1] < size;
        buttonTop.setEnabled(valid && canMoveUp(marked));
        buttonUp.setEnabled(valid && canMoveUp(marked));
        buttonDown.setEnabled(valid && canMoveDown(marked, size));
        buttonBottom.setEnabled(valid && canMoveDown(marked, size));
        buttonRename.setEnabled(valid && marked.length == 1);
        buttonDelete.setEnabled(valid && marked.length > 0);
        comboBoxCharacteristics.setEnabled(valid && marked.length == 1);
    }

    private void move(int[] movedRows) {
        int[] marked = getMarked();
        if (marked.length == 0)
            return;

        updating = true;
        try {
            formatAndRoutesModel.movePositionLists(order(formatAndRoutesModel.getSize(), marked, movedRows));
            listPositionLists.setSelectedIndices(movedRows);
            listPositionLists.ensureIndexIsVisible(movedRows[0]);
        } finally {
            updating = false;
        }
        updateButtons();
    }

    private void rename() {
        if (getMarked().length != 1)
            return;
        // the marked position list is the current one, which the rename action renames
        BaseRouteConverter.getInstance().getContext().getActionManager().run("rename-positionlist");
    }

    private void delete() {
        int[] marked = getMarked();
        if (marked.length == 0)
            return;

        updating = true;
        try {
            for (int i = marked.length - 1; i >= 0; i--)
                formatAndRoutesModel.removePositionList(formatAndRoutesModel.getRoute(marked[i]));
        } finally {
            updating = false;
        }
        markCurrent();
    }

    private void close() {
        formatAndRoutesModel.removeListDataListener(modelListener);
        // detach the list and the combo box from the models they share with the main window
        updating = true;
        listPositionLists.setModel(new DefaultListModel<>());
        comboBoxCharacteristics.setModel(new DefaultComboBoxModel<>());
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
        contentPane.setLayout(new GridLayoutManager(2, 2, new Insets(10, 10, 10, 10), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        contentPane.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW, null,
                new Dimension(320, 240), null, 0, false));
        listPositionLists = new JList<>();
        scrollPane1.setViewportView(listPositionLists);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(10, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonTop = new JButton();
        this.$$$loadButtonText$$$(buttonTop, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "top-action"));
        panel1.add(buttonTop, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                null, 0, false));
        buttonUp = new JButton();
        this.$$$loadButtonText$$$(buttonUp, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "up-action"));
        panel1.add(buttonUp, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                null, 0, false));
        buttonDown = new JButton();
        this.$$$loadButtonText$$$(buttonDown, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "down-action"));
        panel1.add(buttonDown, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                null, 0, false));
        buttonBottom = new JButton();
        this.$$$loadButtonText$$$(buttonBottom,
                this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "bottom-action"));
        panel1.add(buttonBottom, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        buttonNew = new JButton();
        this.$$$loadButtonText$$$(buttonNew,
                this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "new-positionlist-action"));
        panel1.add(buttonNew, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                null, 0, false));
        buttonRename = new JButton();
        this.$$$loadButtonText$$$(buttonRename,
                this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "rename-positionlist-action"));
        panel1.add(buttonRename, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                null, 0, false));
        buttonDelete = new JButton();
        this.$$$loadButtonText$$$(buttonDelete,
                this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "delete-positionlist-action"));
        panel1.add(buttonDelete, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "characteristics"));
        panel1.add(label1, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 28), null, 0, false));
        comboBoxCharacteristics = new JComboBox<>();
        panel1.add(comboBoxCharacteristics, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(2, 0, 1, 0), -1, -1));
        contentPane.add(panel2, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonClose = new JButton();
        this.$$$loadButtonText$$$(buttonClose, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "close"));
        panel2.add(buttonClose, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                null, 0, false));
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
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
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
