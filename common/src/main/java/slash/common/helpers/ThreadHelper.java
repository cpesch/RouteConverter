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

package slash.common.helpers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static java.lang.String.format;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.SwingUtilities.isEventDispatchThread;

/**
 * Provides thread helpers
 *
 * @author Christian Pesch
 */

public class ThreadHelper {
    public static void safeJoin(Thread thread) throws InterruptedException {
        thread.join(500);
        thread.interrupt();
        thread.join();
    }

    public static void invokeInAwtEventQueue(final Runnable runnable) {
        if (!isEventDispatchThread())
            invokeLater(new Runnable() {
                public void run() {
                    runnable.run();
                }
            });
        else
            runnable.run();
    }

    public static ExecutorService createSingleThreadExecutor(String namePrefix) {
        return Executors.newSingleThreadExecutor(new NamedThreadFactory(namePrefix));
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private String namePrefix;
        private int number = 1;

        private NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, format("%s-%d", namePrefix, number++));
        }
    }
}
