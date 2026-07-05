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

package slash.navigation.gui;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Verifies the crash dispatch installed by {@link Application#installDefaultUncaughtExceptionHandler}:
 * a {@link CrashHandler} that itself throws must be caught (no infinite recursion) and the
 * re-entry latch must be reset afterwards so the next crash still reaches the handler.
 * Headless in the style of {@link EventDispatchThreadExceptionTest}: it drives
 * {@code Thread.getDefaultUncaughtExceptionHandler().uncaughtException(...)} directly.
 */

public class ApplicationCrashDispatchTest {

    @After
    public void tearDown() {
        Application.setCrashHandler(null);
    }

    @Test
    public void crashHandlerInvokedOncePerDispatchAndLatchResetAfterThrow() {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        try {
            Application.installDefaultUncaughtExceptionHandler();

            AtomicInteger calls = new AtomicInteger();
            Application.setCrashHandler((thread, throwable) -> {
                calls.incrementAndGet();
                throw new RuntimeException("crash handler itself blows up");
            });

            Thread thread = Thread.currentThread();
            RuntimeException crash = new RuntimeException("boom");

            // the handler throws, but the dispatch must swallow it: invoked exactly once,
            // no re-entry on the same thread, no infinite recursion
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(thread, crash);
            assertEquals("handler must be invoked exactly once, without recursing", 1, calls.get());

            // the handlingCrash latch must have been cleared in the finally block, so a
            // later crash on the same thread reaches the handler again rather than being
            // permanently suppressed
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(thread, crash);
            assertEquals("latch must reset so a subsequent crash is handled", 2, calls.get());
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous);
        }
    }
}
