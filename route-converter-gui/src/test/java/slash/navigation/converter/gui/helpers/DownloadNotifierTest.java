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

import org.junit.Test;
import slash.navigation.download.Action;
import slash.navigation.download.Download;
import slash.navigation.download.FileAndChecksum;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DownloadNotifierTest {
    private static final String URL = "https://download.geofabrik.de/europe/germany/hamburg-latest.osm.pbf";

    private final List<String> notifications = new ArrayList<>();

    private final DownloadNotifier notifier = new DownloadNotifier() {
        ResourceBundle getBundle() {
            return new ListResourceBundle() {
                protected Object[][] getContents() {
                    return new Object[][]{
                            {"download-started", "started {0}"},
                            {"download-progressed", "progressed {0} of {1} {2}"}
                    };
                }
            };
        }

        void showNotification(String message) {
            notifications.add(message);
        }
    };

    private static Download download(Action action) throws IOException {
        File target = File.createTempFile("download-notifier-test", ".pbf");
        target.deleteOnExit();
        return new Download("GraphHopper Routing Data: hamburg", URL, action, new FileAndChecksum(target, null), null);
    }

    // rc#247: a download that waits for its first byte must not be silent until it fails
    @Test
    public void initializedNotifiesBeforeTheFirstByte() throws IOException {
        Download download = download(Action.Copy);

        notifier.initialized(download);
        notifier.progressed(download);

        assertEquals(List.of("started " + URL), notifications);
    }

    @Test
    public void initializedIgnoresUpdateChecks() throws IOException {
        notifier.initialized(download(Action.Head));

        assertTrue(notifications.isEmpty());
    }
}
