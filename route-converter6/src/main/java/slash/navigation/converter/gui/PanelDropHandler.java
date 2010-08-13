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

import slash.common.io.Files;
import slash.navigation.converter.gui.dnd.DnDHelper;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reacts on drag and drop operation under Java 6.
 *
 * @author Christian Pesch
 */

class PanelDropHandler extends TransferHandler {
    private static final Logger log = Logger.getLogger(PanelDropHandler.class.getName());

    protected void handleDrop(List<File> files) {
        if (RouteConverter.getInstance().isConvertPanelSelected())
            RouteConverter.getInstance().openPositionList(Files.toUrls(files.toArray(new File[files.size()])));
        else if (RouteConverter.getInstance().isBrowsePanelSelected())
            RouteConverter.getInstance().addFilesToCatalog(files);
    }

    protected void handleDrop(String string) {
        if (RouteConverter.getInstance().isConvertPanelSelected()) {
            String url = DnDHelper.extractUrl(string);
            try {
                RouteConverter.getInstance().openPositionList(Arrays.asList(new URL(url)));
            }
            catch (MalformedURLException e) {
                log.severe("Could not create URL from '" + url + "'");
            }
        } else if (RouteConverter.getInstance().isBrowsePanelSelected()) {
            RouteConverter.getInstance().addUrlToCatalog(string);
        }
    }

    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    public boolean importData(TransferSupport support) {
        Transferable t = support.getTransferable();
        try {
            if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                Object data = t.getTransferData(DataFlavor.javaFileListFlavor);
                if (data != null) {
                    List<File> files = (List<File>) data;
                    handleDrop(files);
                    return true;
                }
            }

            if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                Object data = t.getTransferData(DataFlavor.stringFlavor);
                if (data != null) {
                    String url = (String) data;
                    handleDrop(url);
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
