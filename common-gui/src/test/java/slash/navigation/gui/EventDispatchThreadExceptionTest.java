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

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.swing.SwingUtilities.invokeLater;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the finding recorded in {@link Application#launch}: on Java 17+ an
 * exception thrown on the EDT reaches {@link Thread#getDefaultUncaughtExceptionHandler}
 * (via EventDispatchThread.processException). The modal-dialog secondary event pump
 * uses the same pumpOneEventForFilters -> processException path, so it is covered by
 * the same default handler and no custom EventQueue wrapping is required.
 */

public class EventDispatchThreadExceptionTest {

    @Test
    public void edtExceptionReachesDefaultHandler() throws InterruptedException {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        AtomicReference<Throwable> captured = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            captured.set(throwable);
            latch.countDown();
        });
        try {
            invokeLater(() -> {
                throw new RuntimeException("boom on EDT");
            });
            assertTrue("EDT exception did not reach the default handler within timeout",
                    latch.await(10, SECONDS));
            assertNotNull(captured.get());
            assertEquals("boom on EDT", captured.get().getMessage());
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous);
        }
    }
}
