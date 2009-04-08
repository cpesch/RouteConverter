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
import slash.navigation.util.Platform;

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
 * A small graphical user interface for the route conversion
 * running under Java 5.
 *
 * @author Christian Pesch, Michel
 */

public class RouteConverter5 extends RouteConverter {

    protected void addDragAndDropToConvertPane() {
        FrameDropHandler dropHandler = new FrameDropHandler();
        textFieldSource.setTransferHandler(dropHandler);
        getPositionsTable().setTransferHandler(dropHandler);
        contentPane.setTransferHandler(dropHandler);
    }

    protected void addDragAndDropToBrowsePane() {
        treeCategories.setDragEnabled(true);
        new DropTarget(treeCategories, new TreeDropListener());
        treeCategories.setTransferHandler(new TreeDragHandler());
    }

    protected void createFrame(String frameTitle, String iconName, JPanel contentPane, JButton defaultButton) {
        super.createFrame(frameTitle, iconName, contentPane, defaultButton);
        if (Platform.isMac()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");            
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", frameTitle);
        }
    }

    protected FilterDialog createFilterDialog() {
        return new FilterDialog(this);
    }

    public ExternalPrograms createExternalPrograms() {
        return new ExternalPrograms5();
    }

    private class FrameDropHandler extends TransferHandler {
        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            if (transferFlavors != null) {
                for (DataFlavor transferFlavor : transferFlavors) {
                    if (transferFlavor.equals(DataFlavor.javaFileListFlavor) ||
                            transferFlavor.equals(DataFlavor.stringFlavor)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean importData(JComponent comp, Transferable t) {
            try {
                Object data = t.getTransferData(DataFlavor.javaFileListFlavor);
                List<File> files = (List<File>) data;
                onDrop(files);
                return true;
            } catch (UnsupportedFlavorException e) {
                // intentionally left empty
            } catch (IOException e) {
                // intentionally left empty
            }

            try {
                Object data = t.getTransferData(DataFlavor.stringFlavor);
                String url = (String) data;
                onDrop(url);
                return true;
            } catch (UnsupportedFlavorException e) {
                // intentionally left empty
            } catch (IOException e) {
                // intentionally left empty
            }

            return false;
        }
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
                        onMove(categories, target);
                    }
                    dropTargetDropEvent.dropComplete(true);
                }

                if (t.isDataFlavorSupported(RouteSelection.routeFlavor)) {
                    dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_MOVE);
                    Object data = t.getTransferData(RouteSelection.routeFlavor);
                    if (data != null) {
                        List<Route> routes = (List<Route>) data;
                        onMove(routes, source, target);
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

    public static void main(String[] args) {
        launch(RouteConverter6.class, args);
    }
}
