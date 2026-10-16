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

import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.helpers.PoiCategorySelection;
import slash.navigation.converter.gui.helpers.PoiOverlayController;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.DialogAction;
import slash.navigation.pois.mapsforge.PoiCatalog;
import slash.navigation.pois.mapsforge.PoiCategory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

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
    private final JCheckBox checkBoxShowOnMap = new JCheckBox();
    private final JTree treeCategories = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode()));
    private final JLabel labelState = new JLabel(" ");
    private final JButton buttonClose = new JButton();
    private final ChangeListener stateListener = e -> updateState();
    private final PoiCategorySelection selection;
    private boolean loading = true;
    private boolean noCatalog;

    public ShowPoisDialog() {
        super(BaseRouteConverter.getInstance().getFrame(), "show-pois");
        setTitle(BaseRouteConverter.getBundle().getString("show-pois-title"));
        setContentPane(createContentPane());
        getRootPane().setDefaultButton(buttonClose);

        PoiOverlayController controller = getPoiOverlayController();
        selection = new PoiCategorySelection(controller.getSelectedKeys());

        checkBoxShowOnMap.setText(BaseRouteConverter.getBundle().getString("show-pois-on-map"));
        checkBoxShowOnMap.setSelected(controller.isEnabled());
        checkBoxShowOnMap.addActionListener(e -> controller.setEnabled(checkBoxShowOnMap.isSelected()));

        treeCategories.setRootVisible(false);
        treeCategories.setShowsRootHandles(true);
        treeCategories.setCellRenderer(new CategoryRenderer());
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

        buttonClose.setText(BaseRouteConverter.getBundle().getString("close"));
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
        getRootPane().registerKeyboardAction(new DialogAction(this) {
            public void run() {
                close();
            }
        }, getKeyStroke(VK_ESCAPE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        controller.addChangeListener(stateListener);
        updateState();
        controller.loadCatalog(this::showCatalog);
    }

    private JPanel createContentPane() {
        JPanel contentPane = new JPanel(new BorderLayout(0, 6));
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPane.add(checkBoxShowOnMap, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(treeCategories);
        scrollPane.setPreferredSize(new Dimension(320, 400));
        contentPane.add(scrollPane, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(6, 0));
        bottom.add(labelState, BorderLayout.CENTER);
        bottom.add(buttonClose, BorderLayout.EAST);
        contentPane.add(bottom, BorderLayout.SOUTH);
        return contentPane;
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
                case TRUNCATED -> MessageFormat.format(BaseRouteConverter.getBundle().getString("show-pois-truncated"), controller.getCount());
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
}
