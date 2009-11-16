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

import slash.navigation.catalog.domain.Route;
import slash.navigation.catalog.model.CategoryTreeNode;
import slash.navigation.converter.gui.dnd.CategorySelection;
import slash.navigation.converter.gui.dnd.RouteSelection;
import slash.navigation.converter.gui.panels.BrowsePanel;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The browse panel of the route converter user interface when
 * running under Java 5.
 *
 * @author Christian Pesch
 */

public class BrowsePanel5 extends BrowsePanel {
    protected void addDragAndDrop() {
        treeCategories.setDragEnabled(true);
        new DropTarget(treeCategories, new TreeDropListener());
        treeCategories.setTransferHandler(new TreeDragHandler());
    }

    private class TreeDragHandler extends TransferHandler {
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        protected Transferable createTransferable(JComponent c) {
            return new CategorySelection(getSelectedTreeNodes());
        }
    }

    private class TreeDropListener implements DropTargetListener {

        public void dragEnter(DropTargetDragEvent dropTargetDragEvent) {
        }

        public void dragExit(DropTargetEvent dropTargetEvent) {
        }

        public void dragOver(DropTargetDragEvent dropTargetDragEvent) {
        }

        public void dropActionChanged(DropTargetDragEvent dropTargetDragEvent) {
        }

        public void drop(DropTargetDropEvent dropTargetDropEvent) {
            CategoryTreeNode source = getSelectedTreeNode();
            Point location = dropTargetDropEvent.getLocation();
            TreePath path = treeCategories.getPathForLocation(location.x, location.y);
            CategoryTreeNode target = (CategoryTreeNode) path.getLastPathComponent();
            try {
                Transferable t = dropTargetDropEvent.getTransferable();
                if (t.isDataFlavorSupported(CategorySelection.categoryFlavor)) {
                    dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_MOVE);
                    Object data = t.getTransferData(CategorySelection.categoryFlavor);
                    if (data != null) {
                        List<CategoryTreeNode> categories = (List<CategoryTreeNode>) data;
                        moveCategory(categories, target);
                    }
                    dropTargetDropEvent.dropComplete(true);
                }

                if (t.isDataFlavorSupported(RouteSelection.routeFlavor)) {
                    dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_MOVE);
                    Object data = t.getTransferData(RouteSelection.routeFlavor);
                    if (data != null) {
                        List<Route> routes = (List<Route>) data;
                        moveRoute(routes, source, target);
                    }
                    dropTargetDropEvent.dropComplete(true);
                }

                if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_MOVE);
                    Object data = t.getTransferData(DataFlavor.javaFileListFlavor);
                    if (data != null) {
                        List<File> files = (List<File>) data;
                        addFilesToCatalog(target, files);
                    }
                    dropTargetDropEvent.dropComplete(true);
                }

                if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_MOVE);
                    Object data = t.getTransferData(DataFlavor.stringFlavor);
                    if (data != null) {
                        String url = (String) data;
                        addUrlToCatalog(target, url);
                    }
                    dropTargetDropEvent.dropComplete(true);
                }
            } catch (UnsupportedFlavorException e) {
                dropTargetDropEvent.rejectDrop();
            } catch (IOException e) {
                dropTargetDropEvent.rejectDrop();
            }
        }
    }

}
