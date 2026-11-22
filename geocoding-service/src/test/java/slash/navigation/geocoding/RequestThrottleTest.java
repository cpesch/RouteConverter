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
package slash.navigation.geocoding;

import org.junit.Test;

import java.io.InterruptedIOException;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.*;

public class RequestThrottleTest {

    @Test
    public void testFirstRequestDoesNotWait() throws InterruptedIOException {
        RequestThrottle throttle = new RequestThrottle(5000);
        long start = System.nanoTime();
        throttle.acquire();
        assertTrue(NANOSECONDS.toMillis(System.nanoTime() - start) < 1000);
    }

    @Test
    public void testRequestsAreSpacedByInterval() throws InterruptedIOException {
        RequestThrottle throttle = new RequestThrottle(100);
        long start = System.nanoTime();
        throttle.acquire();
        throttle.acquire();
        throttle.acquire();
        assertTrue(NANOSECONDS.toMillis(System.nanoTime() - start) >= 200);
    }

    @Test
    public void testNoWaitOnceIntervalHasPassed() throws Exception {
        RequestThrottle throttle = new RequestThrottle(50);
        throttle.acquire();
        Thread.sleep(120);
        long start = System.nanoTime();
        throttle.acquire();
        assertTrue(NANOSECONDS.toMillis(System.nanoTime() - start) < 40);
    }

    @Test
    public void testInterruptWhileWaitingThrowsAndKeepsFlag() throws InterruptedIOException {
        RequestThrottle throttle = new RequestThrottle(5000);
        throttle.acquire();
        Thread.currentThread().interrupt();
        try {
            throttle.acquire();
            fail("expected InterruptedIOException");
        } catch (InterruptedIOException e) {
            assertTrue(Thread.interrupted()); // also clears the flag for the next test
        }
    }
}
