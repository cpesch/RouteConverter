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
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.actions.RestartDownloadsAction;
import slash.navigation.converter.gui.actions.StopDownloadsAction;
import slash.navigation.converter.gui.helpers.DownloadsTablePopupMenu;
import slash.navigation.converter.gui.renderer.DownloadsTableCellRenderer;
import slash.navigation.converter.gui.renderer.SimpleHeaderRenderer;
import slash.navigation.download.Download;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.DialogAction;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Comparator;
import java.util.ResourceBundle;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.navigation.download.DownloadTableModel.*;
import static slash.navigation.gui.helpers.JMenuHelper.registerAction;
import static slash.navigation.gui.helpers.JMenuHelper.setMnemonic;
import static slash.navigation.gui.helpers.JTableHelper.calculateRowHeight;
import static slash.navigation.gui.helpers.UIHelper.getMaxWidth;

/**
 * Dialog to show downloads of the program.
 *
 * @author Christian Pesch
 */

public class DownloadsDialog extends SimpleDialog {
    private JPanel contentPane;
    private JTable tableDownloads;
    private JButton buttonRestart;
    private JButton buttonStop;
    private JButton buttonClose;

    public DownloadsDialog() {
        super(RouteConverter.getInstance().getFrame(), "downloads");
        setTitle(RouteConverter.getBundle().getString("downloads-title"));
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonClose);

        final RouteConverter r = RouteConverter.getInstance();

        tableDownloads.setModel(r.getDownloadManager().getModel());
        tableDownloads.setDefaultRenderer(Object.class, new DownloadsTableCellRenderer());
        TableCellRenderer headerRenderer = new SimpleHeaderRenderer("description", "state", "size", "date");
        TableColumnModel columns = tableDownloads.getColumnModel();
        for (int i = 0; i < columns.getColumnCount(); i++) {
            TableColumn column = columns.getColumn(i);
            column.setHeaderRenderer(headerRenderer);
            if (i == SIZE_COLUMN) {
                int width = getMaxWidth("9999 MByte", 10);
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            } else if (i == DATE_COLUMN) {
                int width = getMaxWidth("06.08.15", 10);
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            } else if (i == STATE_COLUMN) {
                int width = getMaxWidth("Downloading (1000 kByte)", 2);
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            }
        }
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableDownloads.getModel());
        sorter.setSortsOnUpdates(true);
        sorter.setComparator(DESCRIPTION_COLUMN, new Comparator<Download>() {
            public int compare(Download d1, Download d2) {
                return d1.getDescription().compareToIgnoreCase(d2.getDescription());
            }
        });
        sorter.setComparator(STATE_COLUMN, new Comparator<Download>() {
            public int compare(Download d1, Download d2) {
                return d1.getState().compareTo(d2.getState());
            }
        });
        sorter.setComparator(SIZE_COLUMN, new Comparator<Download>() {
            private long getSize(Download download) {
                return download.getSize() != null ? download.getSize() : 0L;
            }

            public int compare(Download d1, Download d2) {
                return (int) (getSize(d1) - getSize(d2));
            }
        });
        sorter.setComparator(DATE_COLUMN, new Comparator<Download>() {
            private long getLastModified(Download download) {
                return download.getLastModified() != null ? download.getLastModified().getTimeInMillis() : 0L;
            }

            public int compare(Download d1, Download d2) {
                return (int) (getLastModified(d1) - getLastModified(d2));
            }
        });
        tableDownloads.setRowSorter(sorter);
        tableDownloads.setRowHeight(getDefaultRowHeight());

        final ActionManager actionManager = r.getContext().getActionManager();
        actionManager.register("restart-download", new RestartDownloadsAction(tableDownloads, r.getDownloadManager()));
        actionManager.register("stop-download", new StopDownloadsAction(tableDownloads, r.getDownloadManager()));

        new DownloadsTablePopupMenu(tableDownloads).createPopupMenu();
        registerAction(buttonRestart, "restart-download");
        registerAction(buttonStop, "stop-download");

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
    }

    private int getDefaultRowHeight() {
        return calculateRowHeight(this, new DefaultCellEditor(new JTextField()), "Value");
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
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("downloads-colon"));
        contentPane.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        contentPane.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableDownloads = new JTable();
        tableDownloads.setShowHorizontalLines(false);
        tableDownloads.setShowVerticalLines(false);
        scrollPane1.setViewportView(tableDownloads);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(2, 0, 1, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonClose = new JButton();
        this.$$$loadButtonText$$$(buttonClose, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("close"));
        panel1.add(buttonClose, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonRestart = new JButton();
        this.$$$loadButtonText$$$(buttonRestart, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("restart-download-action"));
        buttonRestart.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("restart-download-action-tooltip"));
        panel2.add(buttonRestart, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonStop = new JButton();
        this.$$$loadButtonText$$$(buttonStop, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("stop-download-action"));
        buttonStop.setToolTipText(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("stop-download-action-tooltip"));
        panel2.add(buttonStop, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
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
