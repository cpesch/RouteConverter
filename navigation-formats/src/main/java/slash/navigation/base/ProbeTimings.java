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
package slash.navigation.base;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;

import static java.lang.String.format;
import static java.util.Comparator.comparingLong;

/**
 * Measures how long probing takes per container entry and per format while a container such
 * as a ZIP archive is read, and logs the costliest ones, to find out where reading a large
 * archive like a Google Takeout export spends its time. A probe's time includes the full read
 * when the format accepts the entry. Measures nothing unless INFO is logged.
 *
 * @author Christian Pesch
 */

public class ProbeTimings {
    private static final Logger log = Logger.getLogger(ProbeTimings.class.getName());
    private static final ThreadLocal<ProbeTimings> current = new ThreadLocal<>();
    static final int TOP = 15;

    private final String containerName;
    private final long startNanos = System.nanoTime();
    private final Map<String, Long> entryNanos = new HashMap<>();
    private final Map<String, Long> formatNanos = new HashMap<>();
    private final Map<String, Integer> formatCounts = new HashMap<>();
    // the TOP slowest single probes: an entry is probed once per format, so a probe is an
    // entry | format pair; bounded, since a large archive has hundreds of thousands of them
    private final PriorityQueue<Map.Entry<String, Long>> slowestPairs = new PriorityQueue<>(comparingLong(Map.Entry::getValue));
    private final Deque<String> entryStack = new ArrayDeque<>();
    private String currentEntry;
    private int entryCount;

    private ProbeTimings(String containerName) {
        this.containerName = containerName;
    }

    /**
     * Starts measuring for the current thread unless an outer container already does.
     * @return true if this call started the measurement and must {@link #finish()} it
     */
    public static boolean start(String containerName) {
        if (current.get() != null || !log.isLoggable(INFO))
            return false;
        current.set(new ProbeTimings(containerName));
        return true;
    }

    /**
     * Pushes the entry onto a stack so a nested container's own entries (an archive within
     * this entry) can push and pop their own names without losing this entry's name once
     * control returns here for {@link #endEntry(long)}.
     */
    public static void startEntry(String entryName) {
        ProbeTimings timings = current.get();
        if (timings != null) {
            timings.entryStack.push(entryName);
            timings.currentEntry = entryName;
            timings.entryCount++;
        }
    }

    public static void endEntry(long nanos) {
        ProbeTimings timings = current.get();
        if (timings != null && !timings.entryStack.isEmpty()) {
            String entryName = timings.entryStack.pop();
            timings.entryNanos.merge(entryName, nanos, Long::sum);
            timings.currentEntry = timings.entryStack.peek();
        }
    }

    static void recordProbe(NavigationFormat<?> format, long nanos) {
        ProbeTimings timings = current.get();
        if (timings == null)
            return;
        String formatName = format.getClass().getSimpleName();
        timings.formatNanos.merge(formatName, nanos, Long::sum);
        timings.formatCounts.merge(formatName, 1, Integer::sum);
        if (timings.currentEntry != null) {
            PriorityQueue<Map.Entry<String, Long>> pairs = timings.slowestPairs;
            if (pairs.size() < TOP || nanos > pairs.peek().getValue()) {
                pairs.add(Map.entry(timings.currentEntry + " | " + formatName, nanos));
                if (pairs.size() > TOP)
                    pairs.poll();
            }
        }
    }

    public static void finish() {
        ProbeTimings timings = current.get();
        current.remove();
        if (timings != null)
            log.info(timings.summary());
    }

    static void discard() {
        current.remove();
    }

    static String currentSummary() {
        ProbeTimings timings = current.get();
        return timings != null ? timings.summary() : null;
    }

    String summary() {
        StringBuilder buffer = new StringBuilder(format("Probe timings for %s: %d entries probed in %d ms%n",
                containerName, entryCount, millis(System.nanoTime() - startNanos)));
        buffer.append(format("Slowest entries:%n"));
        append(buffer, entryNanos, null);
        buffer.append(format("Slowest formats (summed over all entries, with probe count):%n"));
        append(buffer, formatNanos, formatCounts);
        buffer.append(format("Slowest entry | format pairs:%n"));
        Map<String, Long> pairs = new HashMap<>();
        for (Map.Entry<String, Long> pair : slowestPairs)
            pairs.put(pair.getKey(), pair.getValue());
        append(buffer, pairs, null);
        return buffer.toString();
    }

    private static void append(StringBuilder buffer, Map<String, Long> nanos, Map<String, Integer> counts) {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(nanos.entrySet());
        entries.sort(comparingLong(Map.Entry<String, Long>::getValue).reversed());
        for (Map.Entry<String, Long> entry : entries.subList(0, Math.min(TOP, entries.size()))) {
            buffer.append(format("  %8d ms  %s", millis(entry.getValue()), entry.getKey()));
            if (counts != null)
                buffer.append(format(" (%d probes)", counts.get(entry.getKey())));
            buffer.append(format("%n"));
        }
    }

    private static long millis(long nanos) {
        return nanos / 1_000_000;
    }
}
