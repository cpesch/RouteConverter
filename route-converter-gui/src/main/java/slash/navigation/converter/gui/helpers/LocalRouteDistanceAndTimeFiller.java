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

import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserResult;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.converter.gui.models.RouteDistanceAndTimeCache;

import javax.swing.Timer;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static slash.common.io.Files.toFile;

/**
 * Fills the {@link RouteDistanceAndTimeCache} for routes of the local catalog by
 * parsing their files in a background thread - one file at a time, bounded queue,
 * rows fill progressively as results arrive.
 *
 * Length is the beeline distance between the positions, duration is taken from
 * the timestamps of the positions.
 *
 * Per-file results are coalesced: rather than notifying the table once per parsed
 * file (which, with {@code setSortsOnUpdates}, re-sorts the whole table on every
 * file and drifts the scroll position on big folders), the URLs are collected and
 * flushed as a single batch on a Swing timer, so the table re-sorts once per flush.
 *
 * @author Christian Pesch
 */

public class LocalRouteDistanceAndTimeFiller {
    private static final Logger log = Logger.getLogger(LocalRouteDistanceAndTimeFiller.class.getName());
    private static final int MAXIMUM_QUEUE_SIZE = 1000;
    private static final int COALESCE_MILLIS = 500;

    private final RouteDistanceAndTimeCache cache;
    private final Function<String, DistanceAndTime> parser;
    private final Consumer<Collection<String>> urlsUpdated;
    private final Set<String> pendingUrls = ConcurrentHashMap.newKeySet();
    private final Set<String> updatedUrls = new LinkedHashSet<>();
    private final ExecutorService executor;
    private final Timer flushTimer;

    public LocalRouteDistanceAndTimeFiller(RouteDistanceAndTimeCache cache, Consumer<Collection<String>> urlsUpdated) {
        this(cache, LocalRouteDistanceAndTimeFiller::parseLocalRoute, defaultExecutor(), urlsUpdated, COALESCE_MILLIS);
    }

    LocalRouteDistanceAndTimeFiller(RouteDistanceAndTimeCache cache, Function<String, DistanceAndTime> parser,
                                    ExecutorService executor, Consumer<Collection<String>> urlsUpdated,
                                    int coalesceMillis) {
        this.cache = cache;
        this.parser = parser;
        this.executor = executor;
        this.urlsUpdated = urlsUpdated;
        // coalesceMillis <= 0 disables the timer so tests can drive flush() directly
        this.flushTimer = coalesceMillis > 0 ? createFlushTimer(coalesceMillis) : null;
    }

    private static ExecutorService defaultExecutor() {
        return new ThreadPoolExecutor(1, 1, 60, SECONDS,
                new LinkedBlockingQueue<>(MAXIMUM_QUEUE_SIZE), runnable -> {
            Thread thread = new Thread(runnable, "LocalRouteDistanceAndTimeFiller");
            thread.setDaemon(true);
            return thread;
        });
    }

    private Timer createFlushTimer(int coalesceMillis) {
        Timer timer = new Timer(coalesceMillis, e -> flush());
        timer.setRepeats(false);
        return timer;
    }

    public void fill(String url) {
        if (url == null || cache.getDistanceAndTime(url) != null)
            return;
        if (!pendingUrls.add(url))
            return;

        try {
            executor.execute(() -> parseAndCache(url));
        } catch (RejectedExecutionException e) {
            // queue full: drop; the URL is enqueued again upon the next routes table update
            pendingUrls.remove(url);
        }
    }

    private void parseAndCache(String url) {
        DistanceAndTime result = null;
        try {
            result = parser.apply(url);
            if (result != null)
                cache.put(url, result);
        } catch (Throwable t) {
            log.warning(String.format("Cannot determine distance and time of %s: %s", url, t));
        } finally {
            pendingUrls.remove(url);
        }
        if (result != null)
            scheduleUpdate(url);
    }

    private void scheduleUpdate(String url) {
        synchronized (updatedUrls) {
            updatedUrls.add(url);
        }
        // start the (non-repeating) timer once per batch; javax.swing.Timer is thread-safe
        if (flushTimer != null && !flushTimer.isRunning())
            flushTimer.start();
    }

    /**
     * Drains the collected URLs and notifies the listener once for the whole batch.
     * Package-private and directly callable so tests need not wait for the Swing timer.
     */
    void flush() {
        Collection<String> batch;
        synchronized (updatedUrls) {
            if (updatedUrls.isEmpty())
                return;
            batch = new ArrayList<>(updatedUrls);
            updatedUrls.clear();
        }
        urlsUpdated.accept(batch);
    }

    private static DistanceAndTime parseLocalRoute(String url) {
        try {
            File file = toFile(new URL(url));
            if (file == null || !file.exists())
                return null;

            NavigationFormatParser parser = new NavigationFormatParser(new NavigationFormatRegistry());
            ParserResult result = parser.read(file);
            if (!result.isSuccessful())
                return null;

            BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = result.getTheRoute();
            if (route == null)
                return null;

            long time = route.getTime();
            return new DistanceAndTime(route.getDistance(), time > 0 ? time : null);
        } catch (Throwable t) {
            log.warning(String.format("Cannot parse local route %s: %s", url, t));
            return null;
        }
    }

    public void dispose() {
        if (flushTimer != null)
            flushTimer.stop();
        executor.shutdownNow();
    }
}
