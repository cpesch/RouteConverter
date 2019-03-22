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
package slash.navigation.routing;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;

/**
 * Counts seconds until stopped and calls a method once a second.
 *
 * @author Christian Pesch
 */

public abstract class SecondCounter {
    private static final int TIMEOUT = 1000;
    private Thread thread;
    private int seconds = 0;
    private boolean running = true;
    private final Object notificationMutex = new Object();

    public void start() {
        thread = new Thread(() -> {
            long lastEvent = currentTimeMillis();

            while (true) {
                synchronized (notificationMutex) {
                    if (!running) {
                        return;
                    }
                }

                if(currentTimeMillis() > lastEvent + TIMEOUT) {
                    second(seconds++);
                    lastEvent = currentTimeMillis();
                }

                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    // intentionally left empty
                }
            }
        }, "SecondCounter");
        thread.start();
    }

    public void stop() {
        synchronized (notificationMutex) {
            running = false;
            notificationMutex.notifyAll();
        }
    }

    protected abstract void second(int second);
}
