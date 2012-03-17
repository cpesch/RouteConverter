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

package slash.navigation.converter.gui.dnd;

import slash.common.io.Files;
import slash.navigation.converter.gui.RouteConverter;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import static java.awt.datatransfer.DataFlavor.javaFileListFlavor;
import static java.awt.datatransfer.DataFlavor.stringFlavor;
import static java.util.Arrays.asList;
import static slash.navigation.converter.gui.dnd.DnDHelper.extractUrl;

/**
 * Reacts on drop operations on a panel to open a file or addChild it to the catalog.
 *
 * @author Christian Pesch
 */

public class PanelDropHandler extends TransferHandler {
    private static final Logger log = Logger.getLogger(PanelDropHandler.class.getName());

    private void openOrAdd(List<File> files) {
        RouteConverter r = RouteConverter.getInstance();
        if (r.isConvertPanelSelected())
            r.openPositionList(Files.toUrls(files.toArray(new File[files.size()])));
        else if (r.isBrowsePanelSelected())
            r.addFilesToCatalog(files);
    }

    private void openOrAdd(String string) {
        RouteConverter r = RouteConverter.getInstance();
        if (r.isConvertPanelSelected()) {
            String url = extractUrl(string);
            try {
                r.openPositionList(asList(new URL(url)));
            }
            catch (MalformedURLException e) {
                log.severe("Could not create URL from '" + url + "'");
            }
        } else if (r.isBrowsePanelSelected()) {
            r.addUrlToCatalog(string);
        }
    }

    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(javaFileListFlavor) ||
                support.isDataFlavorSupported(stringFlavor);
    }

    @SuppressWarnings("unchecked")
    public boolean importData(TransferSupport support) {
        Transferable transferable = support.getTransferable();
        try {
            if (support.isDataFlavorSupported(javaFileListFlavor)) {
                Object data = transferable.getTransferData(javaFileListFlavor);
                if (data != null) {
                    List<File> files = (List<File>) data;
                    openOrAdd(files);
                    return true;
                }
            }

            if (support.isDataFlavorSupported(stringFlavor)) {
                Object data = transferable.getTransferData(stringFlavor);
                if (data != null) {
                    String url = (String) data;
                    openOrAdd(url);
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
