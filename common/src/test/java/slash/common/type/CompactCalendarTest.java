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

package slash.common.type;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static slash.common.TestCase.assertCalendarEquals;
import static slash.common.type.CompactCalendar.UTC;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.common.type.CompactCalendar.fromMillisAndTimeZone;

public class CompactCalendarTest {

    @Test
    public void testFromMillis() {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.setTimeInMillis(1000);
        assertEquals(1000, calendar.getTimeInMillis());
        assertEquals(UTC, calendar.getTimeZone());

        CompactCalendar compactCalendar = fromMillis(1000);

        assertEquals(compactCalendar.getCalendar().getTimeInMillis(), 1000);
        assertCalendarEquals(calendar, compactCalendar.getCalendar());
        assertEquals(UTC, calendar.getTimeZone());
    }

    @Test
    public void testBeforeAndAfter() {
        CompactCalendar early = fromMillis(1000);
        CompactCalendar late = fromMillis(2000);

        assertTrue(early.before(late));
        assertFalse(late.before(early));

        assertFalse(early.after(late));
        assertTrue(late.after(early));

        assertFalse(early.before(early));
        assertFalse(early.after(early));
    }

    @Test
    public void testAsUTCTimeInTimeZone() {
        CompactCalendar calendar = fromMillis(1000000);

        CompactCalendar inTimeZone = calendar.asUTCTimeInTimeZone(TimeZone.getTimeZone("GMT+2"));

        assertEquals(calendar.getCalendar().getTimeInMillis(), 1000000);
        assertEquals(inTimeZone.getCalendar().getTimeInMillis(), 1000000 - 2 * 60 * 60 * 1000);
        assertEquals("UTC", inTimeZone.getTimeZoneId());
    }

    @Test
    public void testSameDay() {
        CompactCalendar today = CompactCalendar.now();
        CompactCalendar todayMinusAMilli = fromMillis(today.getTimeInMillis() - 1);
        CompactCalendar yesterday = fromMillis(today.getTimeInMillis() - 24 * 60 * 60 * 1000);
        CompactCalendar tomorrow = fromMillis(today.getTimeInMillis() + 24 * 60 * 60 * 1000);

        assertTrue(today.sameDay(today));
        assertTrue(today.sameDay(todayMinusAMilli));
        assertTrue(yesterday.sameDay(yesterday));
        assertTrue(tomorrow.sameDay(tomorrow));

        assertFalse(today.sameDay(yesterday));
        assertFalse(today.sameDay(tomorrow));
    }

    @Test
    public void testBeforeAndAfterWithEqualTimeZoneIds() {
        CompactCalendar early = fromMillisAndTimeZone(1000, "Europe/Berlin");
        CompactCalendar late = fromMillisAndTimeZone(2000, "Europe/Berlin");

        assertTrue(early.before(late));
        assertFalse(late.before(early));

        assertFalse(early.after(late));
        assertTrue(late.after(early));
    }

    @Test
    public void testBeforeAndAfterWithDifferingTimeZoneIds() {
        CompactCalendar early = fromMillisAndTimeZone(1000, "Europe/Berlin");
        CompactCalendar late = fromMillisAndTimeZone(2000, "Asia/Tokyo");

        assertTrue(early.before(late));
        assertFalse(late.before(early));

        assertFalse(early.after(late));
        assertTrue(late.after(early));
    }

    @Test
    public void testSameDayWithEqualTimeZoneIds() {
        CompactCalendar today = fromMillisAndTimeZone(CompactCalendar.now().getTimeInMillis(), "Europe/Berlin");
        CompactCalendar todayMinusAMilli = fromMillisAndTimeZone(today.getTimeInMillis() - 1, "Europe/Berlin");
        CompactCalendar yesterday = fromMillisAndTimeZone(today.getTimeInMillis() - 24 * 60 * 60 * 1000, "Europe/Berlin");

        assertTrue(today.sameDay(todayMinusAMilli));
        assertFalse(today.sameDay(yesterday));
    }

    @Test
    public void testSameDayWithDifferingTimeZoneIds() {
        CompactCalendar today = fromMillisAndTimeZone(CompactCalendar.now().getTimeInMillis(), "Europe/Berlin");
        CompactCalendar todayOtherZone = fromMillisAndTimeZone(today.getTimeInMillis(), "Asia/Tokyo");
        CompactCalendar yesterdayOtherZone = fromMillisAndTimeZone(today.getTimeInMillis() - 24 * 60 * 60 * 1000, "Asia/Tokyo");

        assertTrue(today.sameDay(todayOtherZone));
        assertFalse(today.sameDay(yesterdayOtherZone));
    }

    // Characterises decision 4 of #191: a malformed time zone id must not throw, it must
    // fall back to UTC. This is a *new* guarantee introduced by the Instant/ZoneId refactor
    // (unmodified CompactCalendar stores the raw id and only maps it to GMT internally in
    // getCalendar()), so this test is expected to fail against the pre-refactor class.
    @Test
    public void testFromMillisAndTimeZoneFallsBackToUtcForGarbageZoneId() {
        CompactCalendar calendar = fromMillisAndTimeZone(1000, "Not/AZone");

        assertEquals("UTC", calendar.getTimeZoneId());
        assertEquals(UTC, calendar.getCalendar().getTimeZone());
    }

    // Proves the replacement for the deleted double-checked-locking timezone cache is safe:
    // 100 threads resolving 20 distinct zone ids concurrently must each see the right offset.
    @Test
    public void testFromMillisAndTimeZoneIsThreadSafeAcrossManyZoneIds() throws Exception {
        String[] zoneIds = {
                "UTC", "Europe/Berlin", "Europe/London", "America/New_York", "America/Los_Angeles",
                "Asia/Tokyo", "Asia/Shanghai", "Australia/Sydney", "Africa/Cairo", "America/Sao_Paulo",
                "Asia/Kolkata", "Pacific/Auckland", "Europe/Moscow", "America/Chicago", "Asia/Dubai",
                "Europe/Paris", "Asia/Singapore", "America/Denver", "Africa/Johannesburg", "Asia/Seoul"
        };
        long millis = 1_700_000_000_000L;
        int threadCount = 100;

        ExecutorService executor = Executors.newFixedThreadPool(20);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                String zoneId = zoneIds[i % zoneIds.length];
                futures.add(executor.submit(() -> {
                    start.await();
                    CompactCalendar calendar = fromMillisAndTimeZone(millis, zoneId);
                    int expectedOffset = TimeZone.getTimeZone(zoneId).getOffset(millis);
                    int actualOffset = calendar.getCalendar().getTimeZone().getOffset(millis);
                    return expectedOffset == actualOffset && zoneId.equals(calendar.getTimeZoneId());
                }));
            }

            start.countDown();

            for (Future<Boolean> future : futures) {
                assertTrue(future.get(10, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdown();
        }
    }
}
