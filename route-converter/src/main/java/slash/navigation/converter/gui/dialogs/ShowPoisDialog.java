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
import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.helpers.PoiCategorySelection;
import slash.navigation.converter.gui.helpers.PoiOverlayController;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.DialogAction;
import slash.navigation.pois.mapsforge.PoiCatalog;
import slash.navigation.pois.mapsforge.PoiCategory;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_SPACE;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JComponent.WHEN_FOCUSED;
import static javax.swing.KeyStroke.getKeyStroke;

/**
 * Dialog to select the POI categories the POI overlay shows on the map. Non-modal, and the
 * overlay stays after it is closed.
 *
 * @author Christian Pesch
 */

public class ShowPoisDialog extends SimpleDialog {
    private JPanel contentPane;
    private JCheckBox checkBoxShowOnMap;
    private JTree treeCategories;
    private JLabel labelState;
    private JButton buttonClose;
    private final ChangeListener stateListener = e -> updateState();
    private final PoiCategorySelection selection;
    private boolean loading = true;
    private boolean noCatalog;

    public ShowPoisDialog() {
        super(BaseRouteConverter.getInstance().getFrame(), "show-pois");
        setTitle(BaseRouteConverter.getBundle().getString("show-pois-title"));
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonClose);

        PoiOverlayController controller = getPoiOverlayController();
        selection = new PoiCategorySelection(controller.getSelectedKeys());

        checkBoxShowOnMap.setSelected(controller.isEnabled());
        checkBoxShowOnMap.addActionListener(e -> controller.setEnabled(checkBoxShowOnMap.isSelected()));

        treeCategories.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
        treeCategories.setCellRenderer(new CategoryRenderer());
        // the look and feel's fixed row height is made for a label and clips the check box
        treeCategories.setRowHeight(0);
        treeCategories.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // a click on the expand handle has no path, so it only expands
                TreePath path = treeCategories.getPathForLocation(e.getX(), e.getY());
                if (path != null)
                    toggle(path);
            }
        });
        treeCategories.registerKeyboardAction(new DialogAction(this) {
            public void run() {
                TreePath path = treeCategories.getSelectionPath();
                if (path != null)
                    toggle(path);
            }
        }, getKeyStroke(VK_SPACE, 0), WHEN_FOCUSED);

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

        controller.addChangeListener(stateListener);
        updateState();
        controller.loadCatalog(this::showCatalog);
    }

    private PoiOverlayController getPoiOverlayController() {
        return ((RouteConverter) BaseRouteConverter.getInstance()).getPoiOverlayController();
    }

    private void showCatalog(PoiCatalog catalog) {
        loading = false;
        noCatalog = catalog == null || catalog.categories().isEmpty();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        if (catalog != null) {
            for (PoiCategory category : catalog.categories())
                root.add(createNode(category));
        }
        treeCategories.setModel(new DefaultTreeModel(root));
        updateState();
    }

    private DefaultMutableTreeNode createNode(PoiCategory category) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(category);
        for (PoiCategory child : category.children())
            node.add(createNode(child));
        return node;
    }

    private static List<PoiCategory> toCategoryPath(TreePath path) {
        List<PoiCategory> result = new ArrayList<>();
        for (Object component : path.getPath()) {
            if (component instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof PoiCategory category)
                result.add(category);
        }
        return result;
    }

    private void toggle(TreePath path) {
        List<PoiCategory> categories = toCategoryPath(path);
        if (categories.isEmpty())
            return;
        selection.toggle(categories);

        PoiOverlayController controller = getPoiOverlayController();
        // checking a category means wanting to see it
        if (!controller.isEnabled() && selection.isSelected(categories)) {
            checkBoxShowOnMap.setSelected(true);
            controller.setEnabled(true);
        }
        controller.setSelectedKeys(selection.getKeys());
        treeCategories.repaint();
    }

    private void updateState() {
        PoiOverlayController controller = getPoiOverlayController();
        String text;
        if (loading)
            text = BaseRouteConverter.getBundle().getString("show-pois-loading");
        else if (noCatalog)
            text = BaseRouteConverter.getBundle().getString("show-pois-no-data");
        else {
            text = switch (controller.getState()) {
                case OFF -> " ";
                case ZOOM_IN -> BaseRouteConverter.getBundle().getString("show-pois-zoom-in");
                case NO_DATA -> BaseRouteConverter.getBundle().getString("show-pois-no-data");
                case SHOWN -> MessageFormat.format(BaseRouteConverter.getBundle().getString("show-pois-shown"), controller.getCount());
            };
        }
        labelState.setText(text);
    }

    private void close() {
        getPoiOverlayController().removeChangeListener(stateListener);
        dispose();
    }

    private class CategoryRenderer implements TreeCellRenderer {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        private final JCheckBox checkBox = new JCheckBox();
        private final JLabel label = new JLabel();

        CategoryRenderer() {
            panel.setOpaque(false);
            checkBox.setOpaque(false);
            panel.add(checkBox);
            panel.add(label);
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            if (!(node.getUserObject() instanceof PoiCategory category)) {
                label.setText("");
                label.setIcon(null);
                return panel;
            }

            List<PoiCategory> path = toCategoryPath(new TreePath(node.getPath()));
            checkBox.setSelected(selection.isSelected(path));
            // FlatLaf draws the third state, other look and feels ignore it
            checkBox.putClientProperty("JButton.selectedState", selection.isPartiallySelected(path) ? "indeterminate" : null);
            label.setText(category.label());
            // a top-level category shows the color of its markers
            label.setIcon(node.getParent() == tree.getModel().getRoot() ?
                    new ColorIcon(PoiOverlayController.getGroupColor(node.getParent().getIndex(node))) : null);
            label.setForeground(UIManager.getColor(selected ? "Tree.selectionForeground" : "Tree.textForeground"));
            panel.setOpaque(selected);
            panel.setBackground(UIManager.getColor("Tree.selectionBackground"));
            return panel;
        }
    }

    private record ColorIcon(Color color) implements Icon {
        private static final int SIZE = 10;

        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(color);
            graphics2D.fillOval(x, y, SIZE, SIZE);
            graphics2D.dispose();
        }

        public int getIconWidth() {
            return SIZE;
        }

        public int getIconHeight() {
            return SIZE;
        }
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
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        checkBoxShowOnMap = new JCheckBox();
        this.$$$loadButtonText$$$(checkBoxShowOnMap,
                this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "show-pois-on-map"));
        contentPane.add(checkBoxShowOnMap, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        contentPane.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW, null,
                new Dimension(320, 400), null, 0, false));
        treeCategories = new JTree();
        treeCategories.setRootVisible(false);
        treeCategories.setShowsRootHandles(true);
        scrollPane1.setViewportView(treeCategories);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        labelState = new JLabel();
        labelState.setText(" ");
        panel1.add(labelState, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                null, 0, false));
        buttonClose = new JButton();
        this.$$$loadButtonText$$$(buttonClose, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "close"));
        panel1.add(buttonClose, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
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
