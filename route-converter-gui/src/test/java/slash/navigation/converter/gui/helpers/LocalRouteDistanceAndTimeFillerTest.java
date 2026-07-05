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
package slash.navigation.converter.gui.helpers;

import org.junit.Test;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.converter.gui.models.RouteDistanceAndTimeCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link LocalRouteDistanceAndTimeFiller}: pending-URL dedup, queue-full drop,
 * cache-hit short-circuit and coalescing of per-file results into a single batch.
 *
 * @author Christian Pesch
 */
public class LocalRouteDistanceAndTimeFillerTest {
    private static final DistanceAndTime RESULT = new DistanceAndTime(1000.0, 60_000L);

    private final RouteDistanceAndTimeCache cache = new RouteDistanceAndTimeCache();
    private final List<Collection<String>> batches = new ArrayList<>();

    /**
     * An executor that captures submitted tasks instead of running them, so a test can decide
     * when (or whether) the parse runs - keeping the concurrency deterministic.
     */
    private static class ManualExecutor extends AbstractExecutorService {
        private final List<Runnable> tasks = new ArrayList<>();
        private boolean rejecting;

        public void execute(Runnable command) {
            if (rejecting)
                throw new java.util.concurrent.RejectedExecutionException("queue full");
            tasks.add(command);
        }

        void runAll() {
            List<Runnable> copy = new ArrayList<>(tasks);
            tasks.clear();
            for (Runnable task : copy)
                task.run();
        }

        int taskCount() {
            return tasks.size();
        }

        public void shutdown() {
        }

        public List<Runnable> shutdownNow() {
            return new ArrayList<>();
        }

        public boolean isShutdown() {
            return false;
        }

        public boolean isTerminated() {
            return false;
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }

    private LocalRouteDistanceAndTimeFiller filler(Function<String, DistanceAndTime> parser, ManualExecutor executor) {
        // coalesceMillis = 0 disables the Swing timer so the test drives flush() directly
        return new LocalRouteDistanceAndTimeFiller(cache, parser, executor, batches::add, 0);
    }

    @Test
    public void pendingUrlIsParsedOnlyOnce() {
        AtomicInteger parseCount = new AtomicInteger();
        Function<String, DistanceAndTime> parser = url -> {
            parseCount.incrementAndGet();
            return RESULT;
        };
        ManualExecutor executor = new ManualExecutor();
        LocalRouteDistanceAndTimeFiller filler = filler(parser, executor);

        // while the first parse is still pending (task captured, not run), the duplicate is dropped
        filler.fill("url");
        filler.fill("url");
        assertEquals(1, executor.taskCount());

        executor.runAll();
        assertEquals(1, parseCount.get());
    }

    @Test
    public void queueFullDropRemovesPendingMarkSoRetryIsPossible() {
        AtomicInteger parseCount = new AtomicInteger();
        Function<String, DistanceAndTime> parser = url -> {
            parseCount.incrementAndGet();
            return RESULT;
        };
        ManualExecutor executor = new ManualExecutor();
        LocalRouteDistanceAndTimeFiller filler = filler(parser, executor);

        executor.rejecting = true;
        filler.fill("url"); // rejected -> pending mark must be cleared
        assertEquals(0, executor.taskCount());

        // a later fill (queue has room again) must be accepted, proving the mark was cleared
        executor.rejecting = false;
        filler.fill("url");
        assertEquals(1, executor.taskCount());
        executor.runAll();
        assertEquals(1, parseCount.get());
    }

    @Test
    public void cacheHitShortCircuitsAndSkipsParser() {
        AtomicInteger parseCount = new AtomicInteger();
        Function<String, DistanceAndTime> parser = url -> {
            parseCount.incrementAndGet();
            return RESULT;
        };
        ManualExecutor executor = new ManualExecutor();
        LocalRouteDistanceAndTimeFiller filler = filler(parser, executor);

        cache.put("url", RESULT);
        filler.fill("url");

        assertEquals(0, executor.taskCount());
        assertEquals(0, parseCount.get());
    }

    @Test
    public void resultsAreCoalescedIntoASingleBatch() {
        Function<String, DistanceAndTime> parser = url -> RESULT;
        ManualExecutor executor = new ManualExecutor();
        LocalRouteDistanceAndTimeFiller filler = filler(parser, executor);

        filler.fill("a");
        filler.fill("b");
        filler.fill("c");
        executor.runAll();

        // nothing is delivered until the (coalescing) flush runs
        assertTrue(batches.isEmpty());

        filler.flush();
        assertEquals(1, batches.size());
        assertEquals(List.of("a", "b", "c"), new ArrayList<>(batches.get(0)));

        // a second flush without new results delivers nothing
        filler.flush();
        assertEquals(1, batches.size());
    }
}
