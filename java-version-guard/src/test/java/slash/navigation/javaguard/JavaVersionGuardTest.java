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
package slash.navigation.javaguard;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.jar.Attributes;

import static org.junit.jupiter.api.Assertions.*;
import static slash.navigation.javaguard.JavaVersionGuard.*;

public class JavaVersionGuardTest {

    public static class Target {
        static String[] received;

        public static void main(String[] args) {
            received = args;
        }
    }

    public static class Failing {
        public static void main(String[] args) {
            throw new IllegalArgumentException("from main");
        }
    }

    @Test
    public void parsesMajorVersions() {
        assertEquals(8, parseMajor("1.8"));
        assertEquals(8, parseMajor("1.8.0_292"));
        assertEquals(17, parseMajor("17"));
        assertEquals(21, parseMajor("21.0.8"));
        assertEquals(25, parseMajor("25-ea"));
        assertEquals(25, parseMajor(" 25 "));
        assertEquals(0, parseMajor(null));
        assertEquals(0, parseMajor(""));
        assertEquals(0, parseMajor("?"));
    }

    @Test
    public void blocksOnlyAKnownTooOldJava() {
        assertTrue(isSupported(25, 25));
        assertTrue(isSupported(26, 25));
        assertFalse(isSupported(21, 25));
        assertFalse(isSupported(8, 21));
        assertTrue(isSupported(0, 25));
        assertTrue(isSupported(21, 0));
    }

    @Test
    public void readsTheManifestThatCarriesTheGuardEntries() throws Exception {
        Attributes attributes = readGuardAttributes();
        assertEquals(Target.class.getName(), attributes.getValue(GUARDED_MAIN_CLASS));
        assertEquals("8", attributes.getValue(REQUIRED_JAVA_VERSION));
        assertEquals("GuardTest", attributes.getValue(APPLICATION_NAME));
    }

    @Test
    public void delegatesTheArgumentsToTheGuardedMainClass() throws Throwable {
        Target.received = null;
        delegate(Target.class.getName(), new String[]{"a.gpx", "b.kml"});
        assertArrayEquals(new String[]{"a.gpx", "b.kml"}, Target.received);
    }

    @Test
    public void rethrowsWhatTheGuardedMainClassThrows() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> delegate(Failing.class.getName(), new String[0]));
        assertEquals("from main", e.getMessage());
    }

    @Test
    public void bundlesHaveAllKeys() {
        for (Locale locale : new Locale[]{Locale.ROOT, Locale.GERMAN}) {
            ResourceBundle bundle = ResourceBundle.getBundle("slash/navigation/javaguard/JavaVersionGuard", locale);
            for (String key : new String[]{"java-too-old", "download-java", "close"})
                assertFalse(bundle.getString(key).isEmpty(), key + " in " + locale);
        }
    }
}
