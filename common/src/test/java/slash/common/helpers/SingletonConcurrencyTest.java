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

import org.junit.Test;
import slash.common.log.LoggingHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.Assert.assertSame;

public class SingletonConcurrencyTest {
    private static final int THREADS = 16;

    private static <T> void assertSameInstanceFromAllThreads(Supplier<T> getInstance) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return getInstance.get();
                }));
            }
            start.countDown();

            T expected = futures.get(0).get(10, TimeUnit.SECONDS);
            for (Future<T> future : futures)
                assertSame(expected, future.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void loggingHelperIsSingleton() throws Exception {
        assertSameInstanceFromAllThreads(LoggingHelper::getInstance);
    }

    @Test
    public void apiKeyRegistryIsSingleton() throws Exception {
        assertSameInstanceFromAllThreads(APIKeyRegistry::getInstance);
    }
}
