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
import slash.navigation.util.Platform;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
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

    protected void createFrame(String frameTitle, String iconName, JPanel contentPane, JButton defaultButton) {
        super.createFrame(frameTitle, iconName, contentPane, defaultButton);
        if (Platform.isMac()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");            
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", frameTitle);
        }
    }

    public ExternalPrograms createExternalPrograms() {
        return new ExternalPrograms5();
    }

    protected BrowsePanel createBrowsePanel() {
        return new BrowsePanel5();
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

    public static void main(String[] args) {
        launch(RouteConverter5.class, args);
    }
}
