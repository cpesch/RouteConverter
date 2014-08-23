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

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class BaseMapViewProcessLinesTest {
    private EclipseSWTMapView view = new EclipseSWTMapView();
    private final Object notificationMutex = new Object();

    private void processLines(final List<String> lines) throws InterruptedException {
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
                view.processLines(lines);
            }
        }).start();

        synchronized (notificationMutex) {
            notificationMutex.wait(1000);
        }

        assertEquals(49632, portCallback[0]);
    }

    @Test
    public void testGetCallback() throws InterruptedException {
        processLines(asList("GET /0/callback-port/49632 HTTP/1.1"));
    }

    @Test
    public void testPostCallback() throws InterruptedException {
        processLines(asList("POST /0/generic-post-url/ HTTP/1.1", "callback-port/49632"));
    }

    private void processLinesSeparately(final List<String> lines, int expectedCallbackCount) throws InterruptedException {
        final int[] callbackCount = new int[1];
        callbackCount[0] = 0;
        final int[] portCallback = new int[1];
        portCallback[0] = -1;

        view.addMapViewListener(new AbstractMapViewListener() {
            public void receivedCallback(int port) {
                synchronized (notificationMutex) {
                    portCallback[0] = port;
                    callbackCount[0]++;
                }
            }
        });
        new Thread(new Runnable() {
            public void run() {
                for (String line : lines)
                    view.processLines(asList(line));
            }
        }).start();

        synchronized (notificationMutex) {
            notificationMutex.wait(1000);
        }

        assertEquals(49632, portCallback[0]);
        assertEquals(expectedCallbackCount, callbackCount[0]);
    }

    @Test
    public void testSubsequentCallbacks() throws InterruptedException {
        processLinesSeparately(asList("GET /0/callback-port/49634 HTTP/1.1",
                "GET /1/callback-port/49633 HTTP/1.1",
                "GET /2/callback-port/49632 HTTP/1.1"), 3);
    }

    @Test
    public void testSubsequentCallbacksWithJumpingCounter() throws InterruptedException {
        processLinesSeparately(asList("GET /1/callback-port/49634 HTTP/1.1",
                "GET /3/callback-port/49633 HTTP/1.1",
                "GET /5/callback-port/49632 HTTP/1.1"), 3);
    }

    @Test
    public void testTripleCallbacks() throws InterruptedException {
        processLinesSeparately(asList("GET /0/callback-port/49632 HTTP/1.1",
                "GET /0/callback-port/49633 HTTP/1.1",
                "GET /0/callback-port/49634 HTTP/1.1"), 1);
    }
}
