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

package slash.navigation.converter.gui.actions;

import slash.common.io.Files;
import slash.navigation.babel.BabelException;
import slash.navigation.base.NavigationFileParser;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.FrameAction;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * {@link ActionListener} that imports a file to the current position list of a {@link PositionsModel} at
 * the selected rows of a {@link JTable}.
 *
 * @author Christian Pesch
 */

public class ImportPositionList extends FrameAction {
    private static final Logger log = Logger.getLogger(ImportPositionList.class.getName());

    private final RouteConverter routeConverter;
    private final JTable table;
    private final PositionsModel model;

    public ImportPositionList(RouteConverter routeConverter, JTable table, PositionsModel model) {
        this.routeConverter = routeConverter;
        this.table = table;
        this.model = model;
    }

    public void run() {
        int selectedRow = table.getSelectedRow() + 1;

        File[] files = routeConverter.selectFilesToImport();
        if (files == null)
            return;

        importPositionList(selectedRow, Files.toUrls(files));

    }


    private List<URL> reverse(List<URL> urls) {
        List<URL> result = new ArrayList<URL>();
        for (URL url : urls)
            result.add(0, url);
        return result;
    }

    private void importPositionList(final int row, final List<URL> urls) { // TODO very similar to ConvertPanel#appendPositionList()
        new Thread(new Runnable() {
            public void run() {
                try {
                    for (URL url : reverse(urls)) {
                        String path = Files.createReadablePath(url);

                        final NavigationFileParser parser = new NavigationFileParser();
                        if (parser.read(url)) {
                            log.info("Imported: " + path);

                            final String finalPath = path;
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    try {
                                        model.add(row, parser.getTheRoute());
                                    } catch (FileNotFoundException e) {
                                        routeConverter.handleFileNotFound(finalPath);
                                    } catch (IOException e) {
                                        routeConverter.handleOpenError(e, finalPath);
                                    }
                                }
                            });

                        } else {
                            routeConverter.handleUnsupportedFormat(path);
                        }
                    }
                } catch (BabelException e) {
                    routeConverter.handleBabelError(e);
                } catch (OutOfMemoryError e) {
                    routeConverter.handleOutOfMemoryError();
                } catch (Throwable t) {
                    log.severe("Import error: " + t.getMessage());
                    routeConverter.handleOpenError(t, urls);
                }
            }
        }, "UrlImporter").start();
    }
}