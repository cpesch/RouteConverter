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

package slash.navigation.download.actions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.download.Checksum;
import slash.navigation.download.Download;
import slash.navigation.download.FileAndChecksum;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.download.State.Queued;

/**
 * Unit tests for {@link Validator}.
 *
 * @author Christian Pesch
 */
public class ValidatorTest {

    private File target;
    private File fragment1;
    private File fragment2;
    private File absent;

    @Before
    public void setUp() throws IOException {
        target = File.createTempFile("validator-target", ".bin");
        writeContent(target, "hello");

        fragment1 = File.createTempFile("validator-frag1", ".bin");
        writeContent(fragment1, "world");

        fragment2 = File.createTempFile("validator-frag2", ".bin");
        writeContent(fragment2, "!");

        absent = File.createTempFile("validator-absent", ".bin");
        assertTrue(absent.delete()); // make it absent
    }

    @After
    public void tearDown() {
        deleteIfExists(target);
        deleteIfExists(fragment1);
        deleteIfExists(fragment2);
        deleteIfExists(absent);
    }

    private static void writeContent(File f, String content) throws IOException {
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(content);
        }
    }

    private static void deleteIfExists(File f) {
        if (f != null && f.exists())
            //noinspection ResultOfMethodCallIgnored
            f.delete();
    }

    private static Download downloadWithFile(File file) {
        return new Download("desc", "http://example.com/f", Copy,
                new FileAndChecksum(file, null), null);
    }

    private static Download downloadWithFragments(File file, File... fragments) {
        java.util.List<FileAndChecksum> frags = new java.util.ArrayList<>();
        for (File f : fragments)
            frags.add(new FileAndChecksum(f, null));
        return new Download("desc", "http://example.com/f", Copy,
                new FileAndChecksum(file, null), frags);
    }

    // --- isExistsTargets ---

    @Test
    public void existsTargetsIsTrueWhenFileExists() {
        Validator v = new Validator(downloadWithFile(target));
        assertTrue(v.isExistsTargets());
    }

    @Test
    public void existsTargetsIsFalseWhenFileAbsent() {
        Validator v = new Validator(downloadWithFile(absent));
        assertFalse(v.isExistsTargets());
    }

    @Test
    public void existsTargetsIsTrueWhenAllFragmentsExist() {
        Validator v = new Validator(downloadWithFragments(target, fragment1, fragment2));
        assertTrue(v.isExistsTargets());
    }

    @Test
    public void existsTargetsIsFalseWhenOneFragmentAbsent() {
        Validator v = new Validator(downloadWithFragments(target, fragment1, absent));
        assertFalse(v.isExistsTargets());
    }

    // --- isChecksumsValid with null expected ---

    @Test
    public void checksumsValidWhenExpectedChecksumIsNull() throws IOException {
        Validator v = new Validator(downloadWithFile(target));
        assertTrue(v.isChecksumsValid());
    }

    // --- isChecksumsValid with matching and non-matching checksums ---

    @Test
    public void checksumsValidWhenContentLengthMatches() throws IOException {
        long len = target.length();
        Checksum expected = new Checksum(null, len, null);
        FileAndChecksum fac = new FileAndChecksum(target, expected);
        Download d = new Download("desc", "http://example.com/f", Copy, fac, null);

        Validator v = new Validator(d);
        assertTrue(v.isChecksumsValid());
    }

    @Test
    public void checksumsInvalidWhenContentLengthMismatches() throws IOException {
        Checksum expected = new Checksum(null, target.length() + 999L, null);
        FileAndChecksum fac = new FileAndChecksum(target, expected);
        Download d = new Download("desc", "http://example.com/f", Copy, fac, null);

        Validator v = new Validator(d);
        assertFalse(v.isChecksumsValid());
    }

    @Test
    public void checksumsValidWhenSHA1Matches() throws IOException {
        // compute real SHA1 first
        Checksum real = Checksum.createChecksum(target, true);
        assertNotNull(real);
        assertNotNull(real.getSHA1());

        Checksum expected = new Checksum(null, null, real.getSHA1());
        FileAndChecksum fac = new FileAndChecksum(target, expected);
        Download d = new Download("desc", "http://example.com/f", Copy, fac, null);

        Validator v = new Validator(d);
        assertTrue(v.isChecksumsValid());
    }

    @Test
    public void checksumsInvalidWhenSHA1Mismatches() throws IOException {
        Checksum expected = new Checksum(null, null, "wrong-sha1");
        FileAndChecksum fac = new FileAndChecksum(target, expected);
        Download d = new Download("desc", "http://example.com/f", Copy, fac, null);

        Validator v = new Validator(d);
        assertFalse(v.isChecksumsValid());
    }

    // --- expectedChecksumIsCurrentChecksum ---

    @Test
    public void expectedChecksumIsCurrentChecksumUpdatesExpected() throws IOException {
        FileAndChecksum fac = new FileAndChecksum(target, null);
        Download d = new Download("desc", "http://example.com/f", Copy, fac, null);

        Validator v = new Validator(d);
        v.expectedChecksumIsCurrentChecksum();

        Checksum updated = d.getFile().getExpectedChecksum();
        assertNotNull(updated);
        assertEquals(target.length(), updated.getContentLength().longValue());
        assertNotNull(updated.getSHA1());
    }

    @Test
    public void expectedChecksumIsCurrentChecksumReplacesPreviousExpected() throws IOException {
        Checksum oldExpected = new Checksum(null, 9999L, "old-sha");
        FileAndChecksum fac = new FileAndChecksum(target, oldExpected);
        Download d = new Download("desc", "http://example.com/f", Copy, fac, null);

        Validator v = new Validator(d);
        v.expectedChecksumIsCurrentChecksum();

        Checksum updated = d.getFile().getExpectedChecksum();
        // the actual checksum is based on the real file, not the old expected value
        assertEquals(target.length(), updated.getContentLength().longValue());
        assertNotEquals("old-sha", updated.getSHA1());
    }
}

