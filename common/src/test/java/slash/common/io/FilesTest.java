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

package slash.common.io;

import slash.common.TestCase;

import java.io.File;
import java.io.IOException;

public class FilesTest extends TestCase {
    private File file;

    protected void setUp() throws Exception {
        super.setUp();
        file = File.createTempFile("convert", ".tmp");
        File renamed = new File(file.getParentFile(), "convert.tmp");
        assertTrue(file.renameTo(renamed));
        file = renamed;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        assertTrue(file.delete());
    }

    public void testNumberToString() throws IOException {
        assertEquals("5", Files.numberToString(5, 9));
        assertEquals("05", Files.numberToString(5, 10));
        assertEquals("005", Files.numberToString(5, 100));
        assertEquals("0005", Files.numberToString(5, 1000));
    }

    public void testCalculateConvertFileName() throws IOException {
        int FILE_NAME_LENGTH = 18;

        assertEquals(new File(file.getParentFile(), "convert.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 0, 0, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert1.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 1, 3, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert9.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 9, 9, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert5.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 5, 5, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert05.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 5, 50, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert005.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 5, 500, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert10.bcr").getAbsolutePath(),
                Files.calculateConvertFileName(file, 10, 10, ".bcr", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert99.bcr").getAbsolutePath(),
                Files.calculateConvertFileName(file, 99, 99, ".bcr", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert50.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 50, 50, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert050.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 50, 500, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert100.kml").getAbsolutePath(),
                Files.calculateConvertFileName(file, 100, 100, ".kml", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert999.long").getAbsolutePath(),
                Files.calculateConvertFileName(file, 999, 999, ".long", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert500.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 500, 500, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert500.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 500, 999, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert5000.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 5000, 5000, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert5000.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 5000, 9999, ".itn", FILE_NAME_LENGTH));
    }

    public void testCalculateConvertFileNameLimitedLength() throws IOException {
        int FILE_NAME_LENGTH = 4;

        assertEquals(new File(file.getParentFile(), "conv.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 0, 0, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "con9.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 9, 9, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "con5.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 5, 5, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "co05.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 5, 50, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "c005.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 5, 500, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "5000.itn").getAbsolutePath(),
                Files.calculateConvertFileName(file, 5000, 5000, ".itn", FILE_NAME_LENGTH));
    }

    public void testCalculateConvertFileNameThrowsException() throws IOException {
        try {
            Files.calculateConvertFileName(file, 10000, 10000, "gpx", 64);
            assertTrue("IllegalArgumentException expected", false);
        }
        catch (IllegalArgumentException e) {
        }

        try {
            Files.calculateConvertFileName(file, 5000, 10000, "gpx", 64);
            assertTrue("IllegalArgumentException expected", false);
        }
        catch (IllegalArgumentException e) {
        }

        try {
            Files.calculateConvertFileName(file, 1001, 999, "gpx", 64);
            assertTrue("IllegalArgumentException expected", false);
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testCreateGoPalFileName() {
        assertEquals("EIFELSTERN AACHEN", Files.createGoPalFileName("Eifelstern-Aachen"));
        assertEquals("EIFELSTERN.XML", Files.createGoPalFileName("Eifelstern.xml"));
    }

    public void testShortenPath() {
        assertEquals("http://maps.google.de/maps?f=d&hl=de&geocode=142500959607...",
                     Files.shortenPath("http://maps.google.de/maps?f=d&hl=de&geocode=14250095960720490931,54.083160,13.475246%3B13832872253745319564,54.096925,13.383573%3B4731465831403354564,54.114440,13.528310&saddr=54.096925,+13.383573&daddr=54.08316,13.475246+to:54.114440,+13.528310&mra=ps&mrcr=0,1&sll=54.105307,13.490181&sspn=0.132448,0.318604&ie=UTF8&z=12"));
    }

    public void testLastPathFragment() {
        assertEquals("file.gpx", Files.lastPathFragment("file.gpx"));
        assertEquals("file.gpx", Files.lastPathFragment("../file.gpx"));
        assertEquals("file.gpx", Files.lastPathFragment("c:\\bla\\bla\\file.gpx"));
        assertEquals("file.gpx", Files.lastPathFragment("c:/bla/bla/file.gpx"));
        assertEquals("file.gpx", Files.lastPathFragment("file:///c:/bla/bla/file.gpx"));
        assertEquals("file.gpx", Files.lastPathFragment("http://www.blabla.com/bla/bla/file.gpx"));
    }
}
