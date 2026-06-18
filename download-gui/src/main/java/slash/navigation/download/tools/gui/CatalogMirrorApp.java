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
package slash.navigation.download.tools.gui;

import slash.navigation.gui.SingleFrameApplication;
import slash.navigation.gui.actions.ExitAction;

import javax.swing.*;
import java.awt.*;

import static java.util.Collections.emptyList;

/**
 * Starts the catalog mirror desktop app.
 *
 * @author Christian Pesch
 */
public class CatalogMirrorApp extends SingleFrameApplication {
    private CatalogMirrorFrame mirror;

    public static void main(String[] args) {
        launch(CatalogMirrorApp.class, emptyList(), args);
    }

    protected void startup() {
        getContext().getActionManager().register("exit", new ExitAction());

        mirror = new CatalogMirrorFrame();
        frame = new JFrame("RouteConverter Catalog Mirror");
        frame.setContentPane(mirror.getContentPane());
        frame.setMinimumSize(new Dimension(980, 720));
        openFrame(mirror.getContentPane());
        mirror.setFrame(frame);
        mirror.reloadData();
    }

    protected void shutdown() {
        mirror.savePreferences();
        super.shutdown();
    }
}

