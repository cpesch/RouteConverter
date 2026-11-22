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

import java.io.InterruptedIOException;

import static java.lang.Math.max;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Spaces the requests to a public web service at least a given interval apart, across threads,
 * as usage policies like Nominatim's ask for (at most one request per second).
 *
 * @author Christian Pesch
 */

public class RequestThrottle {
    private final long intervalNanos;
    private long nextSlotNanos;
    private boolean reserved;

    public RequestThrottle(long intervalMillis) {
        this.intervalNanos = MILLISECONDS.toNanos(intervalMillis);
    }

    /**
     * Blocks until the interval since the previous request has passed.
     *
     * @throws InterruptedIOException if the waiting thread is interrupted, e.g. when the user cancels
     */
    public void acquire() throws InterruptedIOException {
        long waitNanos = reserveSlot();
        if (waitNanos > 0) {
            try {
                Thread.sleep(NANOSECONDS.toMillis(waitNanos) + 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Interrupted while waiting for the request interval");
            }
        }
    }

    // reserves the next free slot, so concurrent callers queue up one interval apart without sleeping under the lock
    private synchronized long reserveSlot() {
        long now = System.nanoTime();
        long slot = reserved ? max(now, nextSlotNanos) : now;
        nextSlotNanos = slot + intervalNanos;
        reserved = true;
        return slot - now;
    }
}
