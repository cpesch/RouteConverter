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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BaseMapViewProcessStreamTest {
    private EclipseSWTMapView view = new EclipseSWTMapView();
    private final Object LOCK = new Object();

    private void processStream(final String lines) throws InterruptedException {
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
                try {
                    Socket socket = new Socket("localhost", view.getCallbackPort());
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    writer.write(lines);
                    writer.close();
                    socket.close();
                } catch (IOException e) {
                    assertTrue("Cannot write to socket: " + e.getMessage(), false);
                }
            }
        }).start();

        synchronized (LOCK) {
            LOCK.wait(1000);
        }

        assertEquals(view.getCallbackPort(), portCallback[0]);
    }

    @Test
    public void testGetCallback() throws Exception {
        view.initializeCallbackListener();
        processStream("GET /0/callback-port/" + view.getCallbackPort() + " HTTP/1.1\nHost: 127.0.0.1:" + view.getCallbackPort() + "\n");
        view.dispose();
    }

    @Test
    public void testPostCallback() throws Exception {
        view.initializeCallbackListener();
        processStream("POST /0/generic-post-url/ HTTP/1.1\nHost: 127.0.0.1:" + view.getCallbackPort() + "\ncallback-port/" + view.getCallbackPort());
        view.dispose();
    }
}
