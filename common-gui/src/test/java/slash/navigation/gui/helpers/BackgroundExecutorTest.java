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

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Exercises {@link BackgroundExecutor} against an isolated pool (via the package-private
 * constructor) so a test never touches the shared, static pool the rest of the application
 * submits to.
 */

public class BackgroundExecutorTest {

    private BackgroundExecutor newExecutor() {
        return new BackgroundExecutor(Executors.newFixedThreadPool(2));
    }

    @Test
    public void submittedTaskRuns() throws InterruptedException {
        BackgroundExecutor executor = newExecutor();
        CountDownLatch ran = new CountDownLatch(1);
        executor.submitTask("test-task", ran::countDown);
        assertTrue("task did not run within timeout", ran.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void threadCarriesExpectedNamePrefixWhileRunning() throws InterruptedException {
        BackgroundExecutor executor = new BackgroundExecutor(
                Executors.newFixedThreadPool(1, runnable -> {
                    Thread thread = new Thread(runnable, "rc-worker-0");
                    thread.setDaemon(true);
                    return thread;
                }));
        CountDownLatch ran = new CountDownLatch(1);
        AtomicReference<String> nameDuringRun = new AtomicReference<>();
        executor.submitTask("named-task", () -> {
            nameDuringRun.set(Thread.currentThread().getName());
            ran.countDown();
        });
        assertTrue(ran.await(5, TimeUnit.SECONDS));
        assertEquals("rc-worker-0[named-task]", nameDuringRun.get());
    }

    @Test
    public void threadNameIsRestoredAfterTaskCompletes() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(1, runnable -> {
            Thread thread = new Thread(runnable, "rc-worker-0");
            thread.setDaemon(true);
            return thread;
        });
        BackgroundExecutor executor = new BackgroundExecutor(pool);
        CountDownLatch firstDone = new CountDownLatch(1);
        executor.submitTask("first-task", firstDone::countDown);
        assertTrue(firstDone.await(5, TimeUnit.SECONDS));

        CountDownLatch secondRan = new CountDownLatch(1);
        AtomicReference<String> nameBeforeRename = new AtomicReference<>();
        executor.submitTask("second-task", () -> {
            // the pool thread's name, as renamed by the FIRST task's finally block, must have
            // been restored to the bare pool name before this task's own renaming applies
            nameBeforeRename.set(Thread.currentThread().getName());
            secondRan.countDown();
        });
        assertTrue(secondRan.await(5, TimeUnit.SECONDS));
        assertEquals("rc-worker-0[second-task]", nameBeforeRename.get());
    }

    @Test
    public void exceptionInTaskIsLoggedNotSwallowingThePool() throws InterruptedException {
        BackgroundExecutor executor = newExecutor();
        executor.submitTask("failing-task", () -> {
            throw new RuntimeException("boom");
        });

        CountDownLatch nextRan = new CountDownLatch(1);
        executor.submitTask("next-task", nextRan::countDown);
        assertTrue("pool must keep accepting work after a task throws", nextRan.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void shutdownNowStopsAcceptingWorkWithoutThrowing() {
        BackgroundExecutor executor = newExecutor();
        executor.shutdownExecutorNow();

        AtomicBoolean ranAfterShutdown = new AtomicBoolean(false);
        // a rejected submission after shutdownNow() must be caught and logged, not thrown: a
        // listener still firing during BaseRouteConverter.shutdown()'s teardown tail must not
        // see an uncaught RuntimeException from a routine, expected-to-be-ignored submission
        executor.submitTask("late-task", () -> ranAfterShutdown.set(true));
        assertTrue("submission was rejected, so the task must not have run", !ranAfterShutdown.get());
    }

    @Test
    public void namedThreadFactoryNamesThreadsWithIncrementingCounterAndDaemonFlag() throws InterruptedException {
        ThreadFactory factory = BackgroundExecutor.newNamedThreadFactory();

        AtomicReference<String> firstName = new AtomicReference<>();
        AtomicReference<String> secondName = new AtomicReference<>();
        AtomicBoolean firstDaemon = new AtomicBoolean();
        AtomicBoolean secondDaemon = new AtomicBoolean();

        Thread first = factory.newThread(() -> {
        });
        firstName.set(first.getName());
        firstDaemon.set(first.isDaemon());

        Thread second = factory.newThread(() -> {
        });
        secondName.set(second.getName());
        secondDaemon.set(second.isDaemon());

        assertTrue("thread name must carry the rc-worker- prefix", firstName.get().startsWith("rc-worker-"));
        assertNotEquals("successive threads must get distinct, incrementing names", firstName.get(), secondName.get());
        assertTrue("pool threads must be daemon threads so they never keep the JVM alive", firstDaemon.get());
        assertTrue("pool threads must be daemon threads so they never keep the JVM alive", secondDaemon.get());
    }
}
