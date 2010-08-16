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

package slash.navigation.converter.gui;

import slash.navigation.converter.gui.panels.BrowsePanel;
import slash.navigation.converter.gui.dnd.CategorySelection;
import slash.navigation.converter.gui.dnd.RouteSelection;
import slash.navigation.catalog.model.CategoryTreeNode;
import slash.navigation.catalog.domain.Route;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.List;
import java.io.File;
import java.io.IOException;

/**
 * The browse panel of the route converter user interface when
 * running under Java 6.
 *
 * @author Christian Pesch
 */

class BrowsePanel6 extends BrowsePanel {
    protected void addDragAndDrop() {
        treeCategories.setDragEnabled(true);
        treeCategories.setDropMode(DropMode.ON);
        treeCategories.setTransferHandler(new TreeDragAndDropHandler());
    }

    private class TreeDragAndDropHandler extends TransferHandler {
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        protected Transferable createTransferable(JComponent c) {
            return new CategorySelection(getSelectedTreeNodes());
        }

        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(CategorySelection.categoryFlavor) ||
                    support.isDataFlavorSupported(RouteSelection.routeFlavor) ||
                    support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                    support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        public boolean importData(TransferSupport support) {
            JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
            TreePath path = dropLocation.getPath();
            CategoryTreeNode target = (CategoryTreeNode) path.getLastPathComponent();
            try {
                Transferable t = support.getTransferable();
                if (support.isDataFlavorSupported(CategorySelection.categoryFlavor)) {
                    Object data = t.getTransferData(CategorySelection.categoryFlavor);
                    if (data != null) {
                        List<CategoryTreeNode> categories = (List<CategoryTreeNode>) data;
                        moveCategory(categories, target);
                        return true;
                    }
                }

                if (support.isDataFlavorSupported(RouteSelection.routeFlavor)) {
                    Object data = t.getTransferData(RouteSelection.routeFlavor);
                    if (data != null) {
                        List<Route> routes = (List<Route>) data;
                        CategoryTreeNode source = getSelectedTreeNode();
                        moveRoute(routes, source, target);
                        return true;
                    }
                }

                if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    Object data = t.getTransferData(DataFlavor.javaFileListFlavor);
                    if (data != null) {
                        List<File> files = (List<File>) data;
                        addFilesToCatalog(target, files);
                        return true;
                    }
                }

                if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    Object data = t.getTransferData(DataFlavor.stringFlavor);
                    if (data != null) {
                        String url = (String) data;
                        addUrlToCatalog(target, url);
                        return true;
                    }
                }
            } catch (UnsupportedFlavorException e) {
                // intentionally left empty
            } catch (IOException e) {
                // intentionally left empty
            }
            return false;
        }
    }

}
