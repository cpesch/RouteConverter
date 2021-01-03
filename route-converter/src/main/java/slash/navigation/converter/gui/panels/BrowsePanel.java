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

package slash.navigation.converter.gui.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import slash.navigation.babel.BabelException;
import slash.navigation.base.*;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.actions.*;
import slash.navigation.converter.gui.dialogs.AddFileDialog;
import slash.navigation.converter.gui.dialogs.AddUrlDialog;
import slash.navigation.converter.gui.dnd.CategorySelection;
import slash.navigation.converter.gui.dnd.PanelDropHandler;
import slash.navigation.converter.gui.dnd.RouteSelection;
import slash.navigation.converter.gui.helpers.RouteServiceOperator;
import slash.navigation.converter.gui.helpers.RoutesTablePopupMenu;
import slash.navigation.converter.gui.helpers.TreePathStringConversion;
import slash.navigation.converter.gui.models.CatalogModel;
import slash.navigation.converter.gui.renderer.CategoryTreeCellRenderer;
import slash.navigation.converter.gui.renderer.RoutesTableCellRenderer;
import slash.navigation.converter.gui.renderer.SimpleHeaderRenderer;
import slash.navigation.converter.gui.undo.UndoCatalogModel;
import slash.navigation.gui.Application;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.routes.Catalog;
import slash.navigation.routes.impl.*;
import slash.navigation.routes.local.LocalCatalog;
import slash.navigation.routes.remote.RemoteCatalog;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static java.awt.datatransfer.DataFlavor.javaFileListFlavor;
import static java.awt.datatransfer.DataFlavor.stringFlavor;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.Collections.singletonList;
import static javax.help.CSH.setHelpIDString;
import static javax.swing.DropMode.ON;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.tree.TreeSelectionModel.CONTIGUOUS_TREE_SELECTION;
import static slash.common.io.Directories.getRoutesDirectory;
import static slash.common.io.Files.createReadablePath;
import static slash.navigation.base.RouteComments.createRouteDescription;
import static slash.navigation.converter.gui.dnd.CategorySelection.categoryFlavor;
import static slash.navigation.converter.gui.dnd.DnDHelper.extractDescription;
import static slash.navigation.converter.gui.dnd.DnDHelper.extractUrl;
import static slash.navigation.converter.gui.dnd.RouteSelection.routeFlavor;
import static slash.navigation.converter.gui.helpers.RouteModelHelper.*;
import static slash.navigation.converter.gui.models.LocalActionConstants.CATEGORIES;
import static slash.navigation.converter.gui.models.LocalActionConstants.ROUTES;
import static slash.navigation.gui.helpers.JMenuHelper.registerAction;
import static slash.navigation.gui.helpers.JTableHelper.calculateRowHeight;
import static slash.navigation.gui.helpers.JTableHelper.selectAndScrollToPosition;
import static slash.navigation.gui.helpers.UIHelper.startWaitCursor;
import static slash.navigation.gui.helpers.UIHelper.stopWaitCursor;
import static slash.navigation.gui.helpers.WindowHelper.handleOutOfMemoryError;

/**
 * The browse panel of the route converter user interface.
 *
 * @author Christian Pesch
 */

public class BrowsePanel implements PanelInTab {
    private static final Logger log = Logger.getLogger(BrowsePanel.class.getName());

    private JPanel browsePanel;
    private JTree treeCategories;
    private JTable tableRoutes;
    private JButton buttonAddCategory;
    private JButton buttonRenameCategory;
    private JButton buttonDeleteCategory;
    private JButton buttonAddRouteFromFile;
    private JButton buttonAddRouteFromUrl;
    private JButton buttonRenameRoute;
    private JButton buttonDeleteRoute;
    private JButton buttonLogin;

    private CatalogModel catalogModel;

    public BrowsePanel() {
        initialize();
    }

    private void initialize() {
        final RouteConverter r = RouteConverter.getInstance();

        Catalog localCatalog = new LocalCatalog(System.getProperty("root", createRootFolder()));
        CategoryTreeNode localRoot = new CategoryTreeNodeImpl(localCatalog.getRootCategory(), true, false);
        Catalog remoteCatalog = new RemoteCatalog(r.getApiUrl(), r.getCredentials());
        final CategoryTreeNodeImpl remoteRoot = new CategoryTreeNodeImpl(remoteCatalog.getRootCategory(), false, true);
        final RootTreeNode root = new RootTreeNode(localRoot, remoteRoot);
        catalogModel = new UndoCatalogModel(r.getContext().getUndoManager(), root, getOperator());

        final ActionManager actionManager = r.getContext().getActionManager();
        actionManager.register("add-category", new AddCategoryAction(treeCategories, catalogModel));
        actionManager.register("add-route-from-file", new AddFileAction());
        actionManager.register("add-route-from-url", new AddUrlAction());
        actionManager.register("rename-category", new RenameCategoryAction(treeCategories, catalogModel));
        actionManager.register("delete-category", new DeleteCategoriesAction(treeCategories, catalogModel));
        actionManager.registerLocal("delete", CATEGORIES, "delete-category");
        actionManager.register("rename-route", new RenameRoutesAction(tableRoutes, catalogModel));
        actionManager.register("delete-route", new DeleteRoutesAction(tableRoutes, catalogModel));
        actionManager.registerLocal("delete", ROUTES, "delete-route");

        registerAction(buttonAddCategory, "add-category");
        registerAction(buttonRenameCategory, "rename-category");
        registerAction(buttonDeleteCategory, "delete-category");
        registerAction(buttonAddRouteFromFile, "add-route-from-file");
        registerAction(buttonAddRouteFromUrl, "add-route-from-url");
        registerAction(buttonRenameRoute, "rename-route");
        registerAction(buttonDeleteRoute, "delete-route");

        setHelpIDString(treeCategories, "category-tree");
        setHelpIDString(tableRoutes, "route-list");

        buttonLogin.addActionListener(new FrameAction() {
            public void run() {
                getOperator().showLogin();
            }
        });

        treeCategories.setModel(catalogModel.getCategoryTreeModel());
        treeCategories.addTreeSelectionListener(e -> {
            handleCategoryTreeUpdate();
            selectTreePath(e.getPath(), false);
        });
        treeCategories.getModel().addTreeModelListener(new TreeModelListener() {
            public void treeNodesChanged(TreeModelEvent e) {
                selectTreePath(treeCategories.getSelectionModel().getSelectionPath(), true);
            }

            public void treeNodesInserted(TreeModelEvent e) {
            }

            public void treeNodesRemoved(TreeModelEvent e) {
            }

            public void treeStructureChanged(TreeModelEvent e) {
            }
        });
        treeCategories.setCellRenderer(new CategoryTreeCellRenderer());
        treeCategories.setDragEnabled(true);
        treeCategories.setDropMode(ON);
        treeCategories.setTransferHandler(new TreeDragAndDropHandler());
        treeCategories.getSelectionModel().setSelectionMode(CONTIGUOUS_TREE_SELECTION);

        tableRoutes.setModel(catalogModel.getRoutesTableModel());
        tableRoutes.registerKeyboardAction(new FrameAction() {
            public void run() {
                actionManager.run("delete-route");
            }
        }, getKeyStroke(VK_DELETE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        tableRoutes.registerKeyboardAction(new FrameAction() {
            public void run() {
                selectAndScrollToPosition(tableRoutes, 0, 0);
            }
        }, getKeyStroke(VK_HOME, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        tableRoutes.registerKeyboardAction(new FrameAction() {
            public void run() {
                selectAndScrollToPosition(tableRoutes, 0, tableRoutes.getSelectedRow());
            }
        }, getKeyStroke(VK_HOME, SHIFT_DOWN_MASK), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        tableRoutes.registerKeyboardAction(new FrameAction() {
            public void run() {
                int lastRow = tableRoutes.getRowCount() - 1;
                selectAndScrollToPosition(tableRoutes, lastRow, lastRow);
            }
        }, getKeyStroke(VK_END, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        tableRoutes.registerKeyboardAction(new FrameAction() {
            public void run() {
                selectAndScrollToPosition(tableRoutes, tableRoutes.getRowCount() - 1, tableRoutes.getSelectedRow());
            }
        }, getKeyStroke(VK_END, SHIFT_DOWN_MASK), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        tableRoutes.setDragEnabled(true);
        tableRoutes.setTransferHandler(new TableDragHandler());
        tableRoutes.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting())
                return;
            handleRouteListUpdate();
            openRoute();
        });
        TableCellRenderer headerRenderer = new SimpleHeaderRenderer("description", "creator");
        TableCellRenderer cellRenderer = new RoutesTableCellRenderer();
        TableColumnModel columns = tableRoutes.getColumnModel();
        for (int i = 0; i < columns.getColumnCount(); i++) {
            TableColumn column = columns.getColumn(i);
            column.setHeaderRenderer(headerRenderer);
            if (i == 1) {
                column.setPreferredWidth(80);
                column.setMaxWidth(100);
            }
            column.setCellRenderer(cellRenderer);
        }
        tableRoutes.setRowHeight(getDefaultRowHeight());

        browsePanel.setTransferHandler(new PanelDropHandler());

        new RoutesTablePopupMenu(tableRoutes).createPopupMenu();

        handleRouteListUpdate();
        handleCategoryTreeUpdate();

        new Thread(() -> {
            String selected = r.getCategoryPreference();
            if (TreePathStringConversion.isRemote(selected)) {
                // do the loading in a separate thread since treeCategories.setModel(categoryTreeModel)
                // would do it in the AWT EventQueue
                catalogModel.getCategoryTreeModel().getChildCount(remoteRoot);
            }

            invokeLater(() -> {
                startWaitCursor(r.getFrame().getRootPane());
                try {
                    selectTreePath(TreePathStringConversion.fromString(root, selected), true);
                    // make sure the subcategories of the remote catalog are visible, too
                    treeCategories.expandPath(new TreePath(new Object[]{root, remoteRoot}));
                } finally {
                    stopWaitCursor(r.getFrame().getRootPane());
                }
            });
        }, "CategoryTreeInitializer").start();
    }

    private int getDefaultRowHeight() {
        return calculateRowHeight(this, new DefaultCellEditor(new JTextField()), "Value");
    }

    private String createRootFolder() {
        return getRoutesDirectory().getAbsolutePath();
    }

    public Component getRootComponent() {
        return browsePanel;
    }

    public JComponent getFocusComponent() {
        return tableRoutes;
    }

    public String getLocalName() {
        return CATEGORIES;
    }

    public JButton getDefaultButton() {
        return buttonAddRouteFromFile;
    }

    public void initializeSelection() {
        handleCategoryTreeUpdate();
        handleRouteListUpdate();
    }

    private void selectTreePath(TreePath treePath, boolean selectCategoryTreePath) {
        RouteConverter r = RouteConverter.getInstance();
        startWaitCursor(r.getFrame().getRootPane());
        try {
            Object selectedObject = treePath.getLastPathComponent();
            if (!(selectedObject instanceof CategoryTreeNode))
                return;
            if (selectCategoryTreePath)
                selectCategoryTreePath(treeCategories, treePath);
            CategoryTreeNode selectedCategoryTreeNode = (CategoryTreeNode) selectedObject;
            catalogModel.setCurrentCategory(selectedCategoryTreeNode);
            RouteConverter.getInstance().setCategoryPreference(TreePathStringConversion.toString(treePath));
        } finally {
            stopWaitCursor(r.getFrame().getRootPane());
        }
    }

    private void openRoute() {
        int[] selectedRows = tableRoutes.getSelectedRows();
        if (selectedRows.length == 0)
            return;

        RouteModel route = getRoutesListModel().getRoute(selectedRows[0]);
        URL url;
        try {
            String urlString = route.getRoute().getUrl();
            if (urlString == null)
                return;
            url = new URL(urlString);
        } catch (Throwable t) {
            getOperator().handleServiceError(t);
            return;
        }
        RouteConverter.getInstance().openPositionList(singletonList(url), false);
    }

    private void handleRouteListUpdate() {
        int[] selectedRows = tableRoutes.getSelectedRows();

        boolean existsASelectedRoute = selectedRows.length > 0;

        ActionManager actionManager = Application.getInstance().getContext().getActionManager();
        actionManager.enable("delete-route", existsASelectedRoute);
        actionManager.enableLocal("delete", ROUTES, existsASelectedRoute);
    }

    private void handleCategoryTreeUpdate() {
        int[] selectedRows = treeCategories.getSelectionRows();

        boolean existsASelectedCategory = selectedRows != null && selectedRows.length > 0;

        ActionManager actionManager = Application.getInstance().getContext().getActionManager();
        actionManager.enable("delete-category", existsASelectedCategory);
        actionManager.enableLocal("delete", CATEGORIES, existsASelectedCategory);
    }

    private RoutesTableModel getRoutesListModel() {
        return (RoutesTableModel) tableRoutes.getModel();
    }

    private RouteServiceOperator getOperator() {
        return RouteConverter.getInstance().getRouteServiceOperator();
    }


    private void showAddFileToCatalog(CategoryTreeNode categoryTreeNode, String description, Double length, File file) {
        AddFileDialog addFileDialog = new AddFileDialog(catalogModel, categoryTreeNode, description, length, file);
        addFileDialog.pack();
        addFileDialog.restoreLocation();
        addFileDialog.setVisible(true);
    }

    private void addFileToCatalog(CategoryTreeNode categoryTreeNode, File file) {
        RouteConverter r = RouteConverter.getInstance();
        String path = createReadablePath(file);
        String description = null;
        Double length = null;
        try {
            NavigationFormatParser parser = new NavigationFormatParser(new NavigationFormatRegistry());
            ParserResult result = parser.read(file);
            if (result.isSuccessful()) {
                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = result.getTheRoute();
                if (route != null) {
                    description = createRouteDescription(route);
                    length = route.getDistance();
                }
                showAddFileToCatalog(categoryTreeNode, description, length, file);
            } else
                r.handleUnsupportedFormat(path);
        } catch (BabelException e) {
            r.handleBabelError(e);
        } catch (OutOfMemoryError e) {
            handleOutOfMemoryError(e);
        } catch (FileNotFoundException e) {
            r.handleFileNotFound(path);
        } catch (Throwable t) {
            log.severe(String.format("Cannot parse description from route %s: %s", path, t));
            r.handleOpenError(t, path);
        }
    }

    private void addFilesToCatalog(CategoryTreeNode category, List<File> files) {
        if (category == null || category.getParent() == null) {
            RouteConverter r = RouteConverter.getInstance();
            showMessageDialog(r.getFrame(),
                    r.getContext().getBundle().getString("add-file-category-missing"),
                    r.getFrame().getTitle(), ERROR_MESSAGE);
            return;
        }

        for (File file : files) {
            addFileToCatalog(category, file);
        }
    }

    public void addFilesToCatalog(List<File> files) {
        addFilesToCatalog(getSelectedCategoryTreeNode(treeCategories), files);
    }

    private void showAddUrlToCatalog(CategoryTreeNode categoryTreeNode, String description, String url) {
        AddUrlDialog addUrlDialog = new AddUrlDialog(catalogModel, categoryTreeNode, description, url);
        addUrlDialog.pack();
        addUrlDialog.restoreLocation();
        addUrlDialog.setVisible(true);
    }

    private void addUrlToCatalog(CategoryTreeNode category, String url) {
        if (category == null || category.getParent() == null) {
            RouteConverter r = RouteConverter.getInstance();
            showMessageDialog(r.getFrame(),
                    r.getContext().getBundle().getString("add-url-category-missing"),
                    r.getFrame().getTitle(), ERROR_MESSAGE);
            return;
        }

        showAddUrlToCatalog(category, extractDescription(url), extractUrl(url));
    }

    public void addUrlToCatalog(String url) {
        addUrlToCatalog(getSelectedCategoryTreeNode(treeCategories), url);
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
        browsePanel = new JPanel();
        browsePanel.setLayout(new GridLayoutManager(4, 2, new Insets(3, 3, 3, 3), -1, -1));
        browsePanel.setMinimumSize(new Dimension(-1, -1));
        browsePanel.setPreferredSize(new Dimension(560, 560));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        browsePanel.add(panel1, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonAddCategory = new JButton();
        this.$$$loadButtonText$$$(buttonAddCategory, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "add-category-action"));
        buttonAddCategory.setToolTipText(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "add-category-action-tooltip"));
        panel1.add(buttonAddCategory, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonDeleteCategory = new JButton();
        this.$$$loadButtonText$$$(buttonDeleteCategory, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "delete-category-action"));
        buttonDeleteCategory.setToolTipText(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "delete-category-action-tooltip"));
        panel1.add(buttonDeleteCategory, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRenameCategory = new JButton();
        this.$$$loadButtonText$$$(buttonRenameCategory, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "rename-category-action"));
        buttonRenameCategory.setToolTipText(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "rename-category-action-tooltip"));
        panel1.add(buttonRenameCategory, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "categories"));
        browsePanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "routes"));
        browsePanel.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        browsePanel.add(panel2, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonAddRouteFromFile = new JButton();
        this.$$$loadButtonText$$$(buttonAddRouteFromFile, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "add-route-by-file-action"));
        buttonAddRouteFromFile.setToolTipText(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "add-route-by-file-action-tooltip"));
        panel2.add(buttonAddRouteFromFile, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        buttonDeleteRoute = new JButton();
        this.$$$loadButtonText$$$(buttonDeleteRoute, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "delete-route-action"));
        buttonDeleteRoute.setToolTipText(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "delete-route-action-tooltip"));
        panel2.add(buttonDeleteRoute, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRenameRoute = new JButton();
        buttonRenameRoute.setHideActionText(false);
        this.$$$loadButtonText$$$(buttonRenameRoute, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "rename-route-action"));
        buttonRenameRoute.setToolTipText(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "rename-route-action-tooltip"));
        panel2.add(buttonRenameRoute, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonAddRouteFromUrl = new JButton();
        this.$$$loadButtonText$$$(buttonAddRouteFromUrl, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "add-route-by-url-action"));
        buttonAddRouteFromUrl.setToolTipText(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "add-route-by-url-action-tooltip"));
        panel2.add(buttonAddRouteFromUrl, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonLogin = new JButton();
        this.$$$loadButtonText$$$(buttonLogin, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "login"));
        buttonLogin.setVisible(false);
        panel2.add(buttonLogin, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        browsePanel.add(scrollPane1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableRoutes = new JTable();
        tableRoutes.setShowHorizontalLines(false);
        tableRoutes.setShowVerticalLines(false);
        scrollPane1.setViewportView(tableRoutes);
        final JScrollPane scrollPane2 = new JScrollPane();
        browsePanel.add(scrollPane2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        treeCategories = new JTree();
        treeCategories.setLargeModel(true);
        treeCategories.setRootVisible(false);
        scrollPane2.setViewportView(treeCategories);
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
        return browsePanel;
    }


    private static class TableDragHandler extends TransferHandler {
        public int getSourceActions(JComponent comp) {
            return MOVE;
        }

        private List<RouteModel> toModels(int[] rowIndices, RoutesTableModel model) {
            List<RouteModel> selectedRoutes = new ArrayList<>();
            for (int selectedRow : rowIndices) {
                RouteModel route = model.getRoute(selectedRow);
                selectedRoutes.add(route);
            }
            return selectedRoutes;
        }

        protected Transferable createTransferable(JComponent c) {
            JTable table = (JTable) c;
            RoutesTableModel model = (RoutesTableModel) table.getModel();
            int[] selectedRows = table.getSelectedRows();
            List<RouteModel> selectedRoutes = toModels(selectedRows, model);
            return new RouteSelection(selectedRoutes);
        }
    }

    private class TreeDragAndDropHandler extends TransferHandler {
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        protected Transferable createTransferable(JComponent c) {
            JTree tree = (JTree) c;
            return new CategorySelection(getSelectedCategoryTreeNodes(tree));
        }

        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(categoryFlavor) ||
                    support.isDataFlavorSupported(routeFlavor) ||
                    support.isDataFlavorSupported(javaFileListFlavor) ||
                    support.isDataFlavorSupported(stringFlavor);
        }

        private void moveCategories(final List<CategoryTreeNode> categories, final CategoryTreeNode target) {
            catalogModel.moveCategories(categories, target, () -> {
                for (CategoryTreeNode category : categories)
                    selectCategory(treeCategories, category);
            });
        }


        private void moveRoutes(final List<RouteModel> routes, final CategoryTreeNode target) {
            catalogModel.moveRoutes(routes, target, () -> {
                selectCategory(treeCategories, target);
                invokeLater(() -> {
                    for (RouteModel route : routes)
                        selectRoute(tableRoutes, route);
                });
            });
        }

        @SuppressWarnings("unchecked")
        public boolean importData(TransferSupport support) {
            JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
            TreePath path = dropLocation.getPath();
            CategoryTreeNode target = (CategoryTreeNode) path.getLastPathComponent();
            try {
                Transferable t = support.getTransferable();
                if (support.isDataFlavorSupported(categoryFlavor)) {
                    Object data = t.getTransferData(categoryFlavor);
                    List<CategoryTreeNode> categories = (List<CategoryTreeNode>) data;
                    moveCategories(categories, target);
                    return true;
                }

                if (support.isDataFlavorSupported(routeFlavor)) {
                    Object data = t.getTransferData(routeFlavor);
                    List<RouteModel> routes = (List<RouteModel>) data;
                    moveRoutes(routes, target);
                    return true;
                }

                if (support.isDataFlavorSupported(javaFileListFlavor)) {
                    Object data = t.getTransferData(javaFileListFlavor);
                    List<File> files = (List<File>) data;
                    addFilesToCatalog(target, files);
                    return true;
                }

                if (support.isDataFlavorSupported(stringFlavor)) {
                    Object data = t.getTransferData(stringFlavor);
                    String url = (String) data;
                    addUrlToCatalog(target, url);
                    return true;
                }
            } catch (UnsupportedFlavorException | IOException e) {
                // intentionally left empty
            }
            return false;
        }
    }
}
