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

package slash.navigation.converter.gui.mapview;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EclipseSWTMapViewTest {
    private EclipseSWTMapView view = new EclipseSWTMapView();
    private final Object LOCK = new Object();

    private void callback(final String line) throws InterruptedException {
        final int[] portCallback = new int[1];
        portCallback[0] = -1;

        view.addMapViewListener(new AbstractMapViewListener() {
            public void receivedCallback(int port) {
                synchronized (LOCK) {
                    portCallback[0] = port;
                    LOCK.notify();
                }
            }
        });
        new Thread(new Runnable() {
            public void run() {
                view.processCallback(line);
            }
        }).start();

        synchronized (LOCK) {
        LOCK.wait(1000);
        }

        assertEquals(49632, portCallback[0]);
    }

    @Test
    public void testGetCallback() throws InterruptedException {
        callback("GET /callback-port/49632 HTTP/1.1");

    }

    @Test
    public void testOptionsCallback() throws InterruptedException {
        callback("OPTIONS /callback-port/49632 HTTP/1.1");

    }
}
