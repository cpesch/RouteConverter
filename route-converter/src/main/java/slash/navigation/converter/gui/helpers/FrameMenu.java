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

package slash.navigation.converter.gui.helpers;

import javax.swing.*;

import static slash.common.system.Platform.isMac;
import static slash.navigation.gui.helpers.JMenuHelper.createItem;
import static slash.navigation.gui.helpers.JMenuHelper.createMenu;

/**
 * Creates a {@link JMenuBar} for RouteConverter.
 *
 * @author Christian Pesch
 */

public class FrameMenu {
    public JMenuBar createMenuBar() {
        JMenu fileMenu = createMenu("file");
        fileMenu.add(createItem("new-file"));
        fileMenu.add(createItem("open"));
        fileMenu.add(createMenu("reopen"));
        fileMenu.add(createItem("save"));
        fileMenu.add(createItem("save-as"));
        JMenu printMenu = createMenu("print");
        printMenu.add(createItem("print-map"));
        printMenu.add(createItem("print-profile"));
        fileMenu.add(printMenu);
        if(!isMac()) {
            fileMenu.addSeparator();
            fileMenu.add(createItem("exit"));
        }

        JMenu editMenu = createMenu("edit");
        editMenu.add(createItem("undo"));
        editMenu.add(createItem("redo"));
        editMenu.addSeparator();
        editMenu.add(createItem("cut"));
        editMenu.add(createItem("copy"));
        editMenu.add(createItem("paste"));
        editMenu.add(createItem("delete"));
        editMenu.addSeparator();
        editMenu.add(createItem("select-all"));
        editMenu.add(createItem("clear-selection"));

        JMenu positionMenu = createMenu("position");
        positionMenu.add(createItem("new-position"));
        positionMenu.add(createItem("delete-position"));
        positionMenu.addSeparator();
        positionMenu.add(createItem("top"));
        positionMenu.add(createItem("up"));
        positionMenu.add(createItem("down"));
        positionMenu.add(createItem("bottom"));
        positionMenu.addSeparator();
        positionMenu.add(createItem("find-place"));
        JMenu completeMenu = createMenu("complete");
        completeMenu.add(createItem("add-coordinates"));
        completeMenu.add(createItem("add-elevation"));
        completeMenu.add(createItem("add-address"));
        completeMenu.add(createItem("add-speed"));
        completeMenu.add(createItem("add-time"));
        completeMenu.add(createItem("add-number"));
        positionMenu.add(completeMenu);
        positionMenu.addSeparator();
        positionMenu.add(createItem("insert-positions"));
        positionMenu.add(createItem("delete-positions"));

        JMenu positionlistMenu = createMenu("positionlist");
        positionlistMenu.add(createItem("new-positionlist"));
        positionlistMenu.add(createItem("rename-positionlist"));
        positionlistMenu.add(createItem("delete-positionlist"));
        positionlistMenu.addSeparator();
        positionlistMenu.add(createItem("convert-route-to-track"));
        positionlistMenu.add(createItem("convert-track-to-route"));
        positionlistMenu.add(createItem("revert-positions"));
        positionlistMenu.add(createMenu("sort-positions"));
        positionlistMenu.addSeparator();
        positionlistMenu.add(createItem("split-positionlist"));
        positionlistMenu.add(createMenu("merge-positionlist"));
        positionlistMenu.add(createItem("import-positionlist"));
        positionlistMenu.add(createItem("export-positionlist"));

        JMenu viewMenu = createMenu("view");
        viewMenu.add(createItem("show-map-and-positionlist"));
        viewMenu.add(createItem("show-profile"));
        viewMenu.add(createItem("maximize-map"));
        viewMenu.add(createItem("maximize-positionlist"));
        viewMenu.add(createItem("show-all-positions-on-map"));
        viewMenu.addSeparator();
        viewMenu.add(createMenu("show-column"));
        viewMenu.add(createMenu("show-profile-x-axis"));
        viewMenu.add(createMenu("show-profile-y-axis"));

        JMenu extrasMenu = createMenu("extras");
        extrasMenu.add(createItem("complete-flight-plan"));
        extrasMenu.add(createItem("show-downloads"));
        if (!isMac())
            extrasMenu.add(createItem("show-options"));

        JMenu helpMenu = createMenu("help");
        helpMenu.add(createItem("help-topics"));
        helpMenu.add(createItem("check-for-update"));
        helpMenu.add(createItem("send-error-report"));
        if (!isMac())
            helpMenu.add(createItem("show-about"));

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(positionMenu);
        menuBar.add(positionlistMenu);
        menuBar.add(viewMenu);
        menuBar.add(extrasMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }
}
