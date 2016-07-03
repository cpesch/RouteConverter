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

import slash.navigation.converter.gui.actions.ReopenAction;
import slash.navigation.converter.gui.models.RecentUrlsModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.prefs.Preferences;

import static slash.common.io.Files.shortenPath;
import static slash.common.io.Files.toFile;
import static slash.navigation.gui.helpers.JMenuHelper.findMenu;

/**
 * Synchronizes the entries of a {@link JMenu}s with the {@link RecentUrlsModel}.
 *
 * @author Christian Pesch
 */

public class ReopenMenuSynchronizer {
    private static final String MAXIMUM_REOPEN_URL_MENU_TEXT_LENGTH_PREFERENCE = "maximumReopenUrlMenuTextLength";
    private static final Preferences preferences = Preferences.userNodeForPackage(ReopenMenuSynchronizer.class);

    private final RecentUrlsModel recentUrlsModel;

    public ReopenMenuSynchronizer(JMenuBar menuBar, RecentUrlsModel recentUrlsModel) {
        this.recentUrlsModel = recentUrlsModel;
        initializeMenu(findMenu(menuBar, "file", "reopen"));
    }

    private void initializeMenu(final JMenu reopenMenu) {
        fillMenu(reopenMenu);

        recentUrlsModel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                reopenMenu.removeAll();

                fillMenu(reopenMenu);
            }
        });
    }

    private void fillMenu(JMenu reopenMenu) {
        List<URL> urls = recentUrlsModel.getUrls();
        reopenMenu.setEnabled(urls.size() > 0);

        for (URL url : urls) {
            File file = toFile(url);
            JMenuItem menuItem = new JMenuItem();
            menuItem.setAction(new ReopenAction(url));
            String text = file != null ? file.getAbsolutePath() : url.toExternalForm();
            menuItem.setText(shortenPath(text, preferences.getInt(MAXIMUM_REOPEN_URL_MENU_TEXT_LENGTH_PREFERENCE, 80)));
            menuItem.setToolTipText(text);
            reopenMenu.add(menuItem);
        }
    }
}