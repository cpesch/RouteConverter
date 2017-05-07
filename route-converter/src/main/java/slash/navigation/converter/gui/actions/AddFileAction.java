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

import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.CatalogModel;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.gui.helpers.WindowHelper;
import slash.navigation.routes.Category;

import javax.swing.*;
import java.io.File;

import static java.util.Arrays.asList;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_ONLY;
import static slash.navigation.gui.helpers.UIHelper.createJFileChooser;

/**
 * {@link Action} that adds a {@link File} to a {@link Category} to the {@link CatalogModel}.
 *
 * @author Christian Pesch
 */

public class AddFileAction extends FrameAction {
    public void run() {
        RouteConverter r = RouteConverter.getInstance();

        JFileChooser chooser = createJFileChooser();
        chooser.setDialogTitle(getBundle().getString("add-file"));
        chooser.setSelectedFile(r.getUploadRoutePreference());
        chooser.setFileSelectionMode(FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        int open = chooser.showOpenDialog(WindowHelper.getFrame());
        if (open != APPROVE_OPTION)
            return;

        File[] selected = chooser.getSelectedFiles();
        if (selected == null || selected.length == 0)
            return;

        r.setUploadRoutePreference(selected[0]);
        r.getBrowsePanel().addFilesToCatalog(asList(selected));
    }
}