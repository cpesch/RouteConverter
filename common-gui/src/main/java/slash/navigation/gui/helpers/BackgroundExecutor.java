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
package slash.navigation.gui.helpers;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A single, bounded, named thread pool for fire-and-forget background work that has no
 * result to hand back to the EDT.
 * <p>
 * Work that updates Swing state should use a {@link javax.swing.SwingWorker} instead, so the
 * EDT hand-off goes through the framework's {@code done()} rather than a hand-rolled
 * {@code invokeLater}.
 * <p>
 * Pool threads are named {@code rc-worker-<n>} and are daemon threads, so they never keep the
 * JVM alive. While a submitted task runs, its thread is temporarily renamed to
 * {@code rc-worker-<n>[<taskName>]} so a thread dump (jstack) or a crash report taken during
 * the task shows what the pool was doing, not just which of the bounded set of pool threads it
 * was on; the thread's name is restored once the task finishes.
 *
 * @author Christian Pesch
 */

public class BackgroundExecutor {
    private static final Logger log = Logger.getLogger(BackgroundExecutor.class.getName());
    private static final String THREAD_NAME_PREFIX = "rc-worker-";

    private static final BackgroundExecutor INSTANCE = new BackgroundExecutor(
            Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()),
                    new NamedThreadFactory()));

    private final ExecutorService executor;

    /**
     * Package-private: lets tests exercise an isolated pool instead of the shared, static one
     * that the rest of the application submits to.
     */
    BackgroundExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Submits fire-and-forget {@code task} to the shared pool. An exception escaping the task
     * is logged, not rethrown, and does not affect other tasks or the pool itself.
     *
     * @param taskName a short, stable name for the task, used only to enrich the thread's name
     *                 for the duration of the run (e.g. for crash telemetry and thread dumps)
     * @param task     the work to run
     */
    public static void submit(String taskName, Runnable task) {
        INSTANCE.submitTask(taskName, task);
    }

    /**
     * Stops accepting new work and interrupts running tasks. Called from the application's
     * shutdown path.
     */
    public static void shutdownNow() {
        INSTANCE.shutdownExecutorNow();
    }

    void submitTask(String taskName, Runnable task) {
        executor.submit(() -> runNamed(taskName, task));
    }

    void shutdownExecutorNow() {
        executor.shutdownNow();
    }

    private static void runNamed(String taskName, Runnable task) {
        Thread current = Thread.currentThread();
        String poolThreadName = current.getName();
        try {
            current.setName(poolThreadName + "[" + taskName + "]");
            task.run();
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Task " + taskName + " failed: " + t, t);
        } finally {
            current.setName(poolThreadName);
        }
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);

        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, THREAD_NAME_PREFIX + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
