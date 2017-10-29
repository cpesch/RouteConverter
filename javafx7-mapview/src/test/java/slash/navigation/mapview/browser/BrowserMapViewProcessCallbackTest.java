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

package slash.navigation.mapview.browser;

import org.junit.Test;
import slash.navigation.mapview.AbstractMapViewListener;

import static org.junit.Assert.assertEquals;

public class BrowserMapViewProcessCallbackTest {
    private JavaFX7WebViewMapView view = new JavaFX7WebViewMapView();
    private final Object notificationMutex = new Object();

    private void processCallback() throws InterruptedException {
        final int[] portCallback = new int[1];
        portCallback[0] = -1;

        view.addMapViewListener(new AbstractMapViewListener() {
            public void receivedCallback(int port) {
                synchronized (notificationMutex) {
                    portCallback[0] = port;
                    notificationMutex.notify();
                }
            }
        });
        new Thread(new Runnable() {
            public void run() {
                view.processCallback("callback-port/49632");
            }
        }).start();

        synchronized (notificationMutex) {
            notificationMutex.wait(1000);
        }

        assertEquals(49632, portCallback[0]);
    }

    @Test
    public void testCallback() throws InterruptedException {
        processCallback();
    }
}
