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
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.renderer.NavigationPositionListCellRenderer;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.DialogAction;

import javax.naming.ServiceUnavailableException;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ResourceBundle;

import static java.awt.event.KeyEvent.*;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.navigation.gui.helpers.JMenuHelper.setMnemonic;

/**
 * Dialog for finding and inserting {@link BaseNavigationPosition}s into the current {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class FindPlaceDialog extends SimpleDialog {
    private JPanel contentPane;
    private JTextField textFieldSearch;
    private JButton buttonSearchPositions;
    private JList<NavigationPosition> listResult;
    private JButton buttonInsertPosition;

    public FindPlaceDialog() {
        super(RouteConverter.getInstance().getFrame(), "find-place");
        setTitle(RouteConverter.getBundle().getString("find-place-title"));
        setContentPane(contentPane);

        setMnemonic(buttonSearchPositions, "search-position-mnemonic");
        buttonSearchPositions.addActionListener(new DialogAction(this) {
            public void run() throws IOException, ServiceUnavailableException {
                searchPositions();
            }
        });

        setMnemonic(buttonInsertPosition, "insert-action-mnemonic");
        buttonInsertPosition.addActionListener(new DialogAction(this) {
            public void run() {
                insertPosition();
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

        RouteConverter r = RouteConverter.getInstance();

        textFieldSearch.setText(r.getFindPlacePreference());
        textFieldSearch.registerKeyboardAction(new DialogAction(this) {
            public void run() throws IOException, ServiceUnavailableException {
                searchPositions();
            }
        }, getKeyStroke(VK_ENTER, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        listResult.setCellRenderer(new NavigationPositionListCellRenderer());
        listResult.setSelectionForeground(new Color(232, 232, 232));
        listResult.setSelectionBackground(new Color(0, 9 * 16 + 9, 255));
        listResult.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                handleSearchUpdate();
            }
        });
        listResult.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    insertPosition();
                }
            }
        });
        listResult.registerKeyboardAction(new DialogAction(this) {
            public void run() {
                insertPosition();
            }
        }, getKeyStroke(VK_PLUS, 0), WHEN_IN_FOCUSED_WINDOW);
        listResult.registerKeyboardAction(new DialogAction(this) {
            public void run() {
                insertPosition();
            }
        }, getKeyStroke(VK_ADD, 0), WHEN_IN_FOCUSED_WINDOW);

        handleSearchUpdate();
    }

    private void handleSearchUpdate() {
        boolean existsSelectedResult = listResult.getSelectedIndices().length > 0;
        buttonInsertPosition.setEnabled(existsSelectedResult);
        if (existsSelectedResult) {
            List<NavigationPosition> selectedValues = listResult.getSelectedValuesList();
            RouteConverter.getInstance().showPositionMagnifier(selectedValues);
        }
    }

    private void searchPositions() throws IOException, ServiceUnavailableException {
        RouteConverter r = RouteConverter.getInstance();

        DefaultListModel<NavigationPosition> listModel = new DefaultListModel<>();
        listResult.setModel(listModel);
        String address = textFieldSearch.getText();

        List<NavigationPosition> positions = r.getGeocodingServiceFacade().getPositionsFor(address);
        if (positions != null) {
            for (NavigationPosition position : positions)
                listModel.addElement(position);

            if (listModel.getSize() > 0) {
                listResult.setSelectedIndex(0);
                listResult.scrollRectToVisible(listResult.getCellBounds(0, 0));
            }
        }

        savePreferences();
    }

    private void insertPosition() {
        RouteConverter r = RouteConverter.getInstance();
        PositionsModel positionsModel = r.getConvertPanel().getPositionsModel();

        int[] selectedRows = r.getConvertPanel().getPositionsView().getSelectedRows();
        int row = selectedRows.length > 0 ? selectedRows[0] : positionsModel.getRowCount();
        int insertRow = row > positionsModel.getRowCount() - 1 ? row : row + 1;
        List<NavigationPosition> selectedValues = listResult.getSelectedValuesList();
        for (int i = selectedValues.size() - 1; i >= 0; i -= 1) {
            NavigationPosition position = selectedValues.get(i);
            positionsModel.add(insertRow, position.getLongitude(), position.getLatitude(),
                    position.getElevation(), null, null, position.getDescription());

            int[] rows = new int[]{insertRow};
            r.getConvertPanel().getPositionsSelectionModel().setSelectedPositions(rows, true);
            r.getPositionAugmenter().addData(rows, false, true, true, true, false);
        }
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible)
            handleSearchUpdate();
    }

    private void savePreferences() {
        RouteConverter r = RouteConverter.getInstance();
        r.setFindPlacePreference(textFieldSearch.getText());
    }

    private void close() {
        RouteConverter.getInstance().showPositionMagnifier(null);
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
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "search-term"));
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldSearch = new JTextField();
        panel2.add(textFieldSearch, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonSearchPositions = new JButton();
        this.$$$loadButtonText$$$(buttonSearchPositions, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "search-position"));
        panel2.add(buttonSearchPositions, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonInsertPosition = new JButton();
        this.$$$loadButtonText$$$(buttonInsertPosition, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "insert-action"));
        panel3.add(buttonInsertPosition, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel4.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        listResult = new JList();
        listResult.setMinimumSize(new Dimension(300, 0));
        scrollPane1.setViewportView(listResult);
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

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
