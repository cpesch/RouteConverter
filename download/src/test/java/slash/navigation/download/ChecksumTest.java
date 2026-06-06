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

package slash.navigation.download;

import org.junit.Test;
import slash.common.type.CompactCalendar;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Unit tests for {@link Checksum}.
 *
 * @author Christian Pesch
 */
public class ChecksumTest {

    private static final long DAY1 = 1_000_000_000_000L;  // some fixed epoch millis (day 1)
    private static final long DAY2 = DAY1 + 86_400_000L;  // 24 hours later (day 2)
    private static final long DAY1_LATER = DAY1 + 3_600_000L; // 1 hour later on day 1

    // --- sameDay ---

    @Test
    public void sameDayReturnsTrueForNullOther() {
        Checksum c = new Checksum(fromMillis(DAY1), 100L, "sha1");
        assertTrue(c.sameDay(null));
    }

    @Test
    public void sameDayReturnsTrueWhenSameDay() {
        Checksum c1 = new Checksum(fromMillis(DAY1), 100L, "sha1");
        Checksum c2 = new Checksum(fromMillis(DAY1_LATER), 200L, "sha2");
        assertTrue(c1.sameDay(c2));
    }

    @Test
    public void sameDayReturnsFalseWhenDifferentDay() {
        Checksum c1 = new Checksum(fromMillis(DAY1), 100L, "sha1");
        Checksum c2 = new Checksum(fromMillis(DAY2), 200L, "sha2");
        assertFalse(c1.sameDay(c2));
    }

    @Test
    public void sameDayReturnsFalseWhenThisLastModifiedIsNull() {
        Checksum c1 = new Checksum(null, 100L, "sha1");
        Checksum c2 = new Checksum(fromMillis(DAY1), 200L, "sha2");
        assertFalse(c1.sameDay(c2));
    }

    @Test
    public void sameDayReturnsFalseWhenOtherLastModifiedIsNull() {
        Checksum c1 = new Checksum(fromMillis(DAY1), 100L, "sha1");
        Checksum c2 = new Checksum(null, 200L, "sha2");
        assertFalse(c1.sameDay(c2));
    }

    // --- laterThan ---

    @Test
    public void laterThanReturnsTrueForNullOther() {
        Checksum c = new Checksum(fromMillis(DAY1), 100L, "sha1");
        assertTrue(c.laterThan(null));
    }

    @Test
    public void laterThanReturnsTrueWhenThisIsLater() {
        Checksum earlier = new Checksum(fromMillis(DAY1), 100L, "sha1");
        Checksum later = new Checksum(fromMillis(DAY2), 200L, "sha2");
        assertTrue(later.laterThan(earlier));
    }

    @Test
    public void laterThanReturnsFalseWhenThisIsEarlier() {
        Checksum earlier = new Checksum(fromMillis(DAY1), 100L, "sha1");
        Checksum later = new Checksum(fromMillis(DAY2), 200L, "sha2");
        assertFalse(earlier.laterThan(later));
    }

    @Test
    public void laterThanReturnsFalseWhenThisLastModifiedIsNull() {
        Checksum c1 = new Checksum(null, 100L, "sha1");
        Checksum c2 = new Checksum(fromMillis(DAY1), 200L, "sha2");
        assertFalse(c1.laterThan(c2));
    }

    @Test
    public void laterThanReturnsFalseWhenOtherLastModifiedIsNull() {
        Checksum c1 = new Checksum(fromMillis(DAY2), 100L, "sha1");
        Checksum c2 = new Checksum(null, 200L, "sha2");
        assertFalse(c1.laterThan(c2));
    }

    // --- getLatestChecksum ---

    @Test
    public void getLatestChecksumReturnsNullForEmptyList() {
        assertNull(Checksum.getLatestChecksum(Collections.emptyList()));
    }

    @Test
    public void getLatestChecksumReturnsSingleItem() {
        Checksum c = new Checksum(fromMillis(DAY1), 100L, "sha1");
        assertSame(c, Checksum.getLatestChecksum(Collections.singletonList(c)));
    }

    @Test
    public void getLatestChecksumReturnsLatest() {
        Checksum c1 = new Checksum(fromMillis(DAY1), 100L, "sha1");
        Checksum c2 = new Checksum(fromMillis(DAY2), 200L, "sha2");
        Checksum c3 = new Checksum(fromMillis(DAY1_LATER), 300L, "sha3");
        assertSame(c2, Checksum.getLatestChecksum(Arrays.asList(c1, c2, c3)));
    }

    // --- equals and hashCode ---

    @Test
    public void equalsReturnsTrueForSameInstance() {
        Checksum c = new Checksum(fromMillis(DAY1), 100L, "sha1");
        assertEquals(c, c);
    }

    @Test
    public void equalsReturnsTrueForEqualChecksums() {
        CompactCalendar ts = fromMillis(DAY1);
        Checksum c1 = new Checksum(ts, 100L, "sha1");
        Checksum c2 = new Checksum(ts, 100L, "sha1");
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void equalsReturnsFalseForDifferentSHA1() {
        CompactCalendar ts = fromMillis(DAY1);
        Checksum c1 = new Checksum(ts, 100L, "sha1");
        Checksum c2 = new Checksum(ts, 100L, "different");
        assertNotEquals(c1, c2);
    }

    @Test
    public void equalsReturnsFalseForDifferentContentLength() {
        CompactCalendar ts = fromMillis(DAY1);
        Checksum c1 = new Checksum(ts, 100L, "sha1");
        Checksum c2 = new Checksum(ts, 999L, "sha1");
        assertNotEquals(c1, c2);
    }

    @Test
    public void equalsReturnsFalseForNull() {
        Checksum c = new Checksum(fromMillis(DAY1), 100L, "sha1");
        assertNotEquals(c, null);
    }

    // --- getters ---

    @Test
    public void gettersReturnConstructorValues() {
        CompactCalendar ts = fromMillis(DAY1);
        Checksum c = new Checksum(ts, 42L, "abc");
        assertEquals(ts, c.getLastModified());
        assertEquals(Long.valueOf(42L), c.getContentLength());
        assertEquals("abc", c.getSHA1());
    }

    @Test
    public void toStringContainsKeyInfo() {
        Checksum c = new Checksum(fromMillis(DAY1), 100L, "sha1");
        String s = c.toString();
        assertTrue(s.contains("Checksum"));
        assertTrue(s.contains("sha1"));
        assertTrue(s.contains("100"));
    }
}

