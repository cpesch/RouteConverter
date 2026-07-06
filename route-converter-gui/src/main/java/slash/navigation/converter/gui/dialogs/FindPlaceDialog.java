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
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.converter.gui.models.FindPlaceResultsModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.renderer.AlternatingColorTableCellRenderer;
import slash.navigation.converter.gui.renderer.LatitudeColumnTableCellEditor;
import slash.navigation.converter.gui.renderer.LongitudeColumnTableCellEditor;
import slash.navigation.converter.gui.renderer.SimpleHeaderRenderer;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.DialogAction;

import javax.naming.ServiceUnavailableException;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import static java.awt.event.KeyEvent.*;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.navigation.common.BoundingBox.asBoundingBox;
import static slash.navigation.converter.gui.models.FindPlaceResultsModel.*;
import static slash.navigation.gui.helpers.JTableHelper.getDefaultRowHeight;
import static slash.navigation.gui.helpers.JMenuHelper.setMnemonic;
import static slash.navigation.gui.helpers.UIHelper.getMaxWidth;

/**
 * Dialog for finding and inserting {@link BaseNavigationPosition}s into the current {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class FindPlaceDialog extends SimpleDialog {
    private JPanel contentPane;
    private JTextField textFieldSearch;
    private JButton buttonSearchPositions;
    private JTable tableResult;
    private JButton buttonInsertPosition;
    private final FindPlaceResultsModel tableModel = new FindPlaceResultsModel();

    public FindPlaceDialog() {
        super(BaseRouteConverter.getInstance().getFrame(), "find-place");
        setTitle(BaseRouteConverter.getBundle().getString("find-place-title"));
        setContentPane(contentPane);
        // size the results table (the content), not the window: pack() then
        // yields a good default and WindowBounds derives the minimum from it
        tableResult.setPreferredScrollableViewportSize(new Dimension(840, 400));

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

        BaseRouteConverter r = BaseRouteConverter.getInstance();

        textFieldSearch.setText(r.getFindPlacePreference());
        textFieldSearch.registerKeyboardAction(new DialogAction(this) {
            public void run() throws IOException, ServiceUnavailableException {
                searchPositions();
            }
        }, getKeyStroke(VK_ENTER, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        tableResult.setModel(tableModel);
        tableResult.setDefaultRenderer(Object.class, new AlternatingColorTableCellRenderer());
        tableResult.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                handleSearchUpdate();
        });
        TableCellRenderer headerRenderer = new SimpleHeaderRenderer("description", "category", "longitude", "latitude", "service");
        TableColumnModel columns = tableResult.getColumnModel();
        for (int i = 0; i < columns.getColumnCount(); i++) {
            TableColumn column = columns.getColumn(i);
            column.setHeaderRenderer(headerRenderer);
            if (i == CATEGORY_COLUMN) {
                int width = getMaxWidth("charging_station", 4);
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            } else if (i == LONGITUDE_COLUMN) {
                column.setCellRenderer(new LongitudeColumnTableCellEditor());
                int width = getMaxWidth("-180.1234567", 5);
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            } else if (i == LATITUDE_COLUMN) {
                column.setCellRenderer(new LatitudeColumnTableCellEditor());
                int width = getMaxWidth("-180.1234567", 5);
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            } else if (i == GEOCODING_SERVICE_COLUMN) {
                int width = getMaxWidth("Openandromaps POI", 5);
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
            }
        }
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setSortsOnUpdates(true);
        sorter.setComparator(NAME_COLUMN, CASE_INSENSITIVE_ORDER);
        sorter.setComparator(CATEGORY_COLUMN, Comparator.nullsFirst(CASE_INSENSITIVE_ORDER));
        sorter.setComparator(LONGITUDE_COLUMN, Comparator.comparingDouble((NavigationPosition position) -> {
            Double longitude = position.getLongitude();
            return longitude != null ? longitude : Double.NEGATIVE_INFINITY;
        }));
        sorter.setComparator(LATITUDE_COLUMN, Comparator.comparingDouble((NavigationPosition position) -> {
            Double latitude = position.getLatitude();
            return latitude != null ? latitude : Double.NEGATIVE_INFINITY;
        }));
        sorter.setComparator(GEOCODING_SERVICE_COLUMN, CASE_INSENSITIVE_ORDER);
        tableResult.setRowSorter(sorter);
        tableResult.setRowHeight(getDefaultRowHeight(this));
        tableResult.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    insertPosition();
                }
            }
        });
        tableResult.registerKeyboardAction(new DialogAction(this) {
            public void run() {
                insertPosition();
            }
        }, getKeyStroke(VK_PLUS, 0), WHEN_IN_FOCUSED_WINDOW);
        tableResult.registerKeyboardAction(new DialogAction(this) {
            public void run() {
                insertPosition();
            }
        }, getKeyStroke(VK_ADD, 0), WHEN_IN_FOCUSED_WINDOW);

        handleSearchUpdate();
    }

    private void handleSearchUpdate() {
        BaseRouteConverter r = BaseRouteConverter.getInstance();
        boolean existsSelectedResult = tableResult.getSelectedRowCount() > 0;
        buttonInsertPosition.setEnabled(existsSelectedResult);
        List<GeocodingResult> selectedValues = getSelectedResults();
        if (selectedValues.isEmpty())
            return;

        List<NavigationPosition> positions = selectedValues.stream().<NavigationPosition>map(GeocodingResult::getPosition).toList();
        r.setCenter(asBoundingBox(positions).getCenter());
    }

    private void searchPositions() throws IOException, ServiceUnavailableException {
        BaseRouteConverter r = BaseRouteConverter.getInstance();
        String address = textFieldSearch.getText();
        List<GeocodingResult> results = r.getGeocodingServiceFacade().getPositionsFor(address);
        tableModel.setResults(results);
        if (tableModel.getRowCount() > 0) {
            tableResult.clearSelection();
            tableResult.scrollRectToVisible(tableResult.getCellRect(0, 0, true));
        } else {
            tableResult.clearSelection();
        }
        List<NavigationPosition> positions = tableModel.getResults().stream().<NavigationPosition>map(GeocodingResult::getPosition).toList();
        r.showPositionMagnifier(positions.isEmpty() ? null : positions);
        handleSearchUpdate();
        savePreferences();
    }

    private void insertPosition() {
        BaseRouteConverter r = BaseRouteConverter.getInstance();
        PositionsModel positionsModel = r.getConvertPanel().getPositionsModel();

        int[] selectedRows = r.getConvertPanel().getPositionsView().getSelectedRows();
        int row = selectedRows.length > 0 ? selectedRows[0] : positionsModel.getRowCount();
        int insertRow = row > positionsModel.getRowCount() - 1 ? row : row + 1;
        List<GeocodingResult> selectedValues = getSelectedResults();
        for (int i = selectedValues.size() - 1; i >= 0; i -= 1) {
            NavigationPosition position = selectedValues.get(i).getPosition();
            positionsModel.add(insertRow, position.getLongitude(), position.getLatitude(),
                    position.getElevation(), null, null, position.getDescription());

            int[] rows = new int[]{insertRow};
            r.getConvertPanel().getPositionsSelectionModel().setSelectedPositions(rows, true);
            r.getPositionAugmenter().addData(rows, false, true, true, true, false);
        }
    }

    private List<GeocodingResult> getSelectedResults() {
        int[] selectedRows = tableResult.getSelectedRows();
        List<GeocodingResult> selectedResults = new ArrayList<>(selectedRows.length);
        for (int selectedRow : selectedRows) {
            int modelRow = tableResult.convertRowIndexToModel(selectedRow);
            selectedResults.add(tableModel.getResult(modelRow));
        }
        return selectedResults;
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible)
            handleSearchUpdate();
    }

    private void savePreferences() {
        BaseRouteConverter r = BaseRouteConverter.getInstance();
        r.setFindPlacePreference(textFieldSearch.getText());
    }

    private void close() {
        BaseRouteConverter.getInstance().showPositionMagnifier(null);
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
        panel2.add(textFieldSearch, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(250, -1), null, 0, false));
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
        tableResult = new JTable();
        scrollPane1.setViewportView(tableResult);
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
