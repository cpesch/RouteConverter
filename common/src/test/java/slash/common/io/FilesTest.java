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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.common.io.Files.*;

public class FilesTest {
    private File file;

    @Before
    public void setUp() throws Exception {
        file = File.createTempFile("convert", ".tmp");
        File renamed = new File(file.getParentFile(), "convert.tmp");
        assertTrue(file.renameTo(renamed));
        file = renamed;
    }

    @After
    public void tearDown() throws Exception {
        assertTrue(file.delete());
    }

    @Test
    public void testNumberToString() throws IOException {
        assertEquals("5", numberToString(5, 9));
        assertEquals("05", numberToString(5, 10));
        assertEquals("005", numberToString(5, 100));
        assertEquals("0005", numberToString(5, 1000));
    }

    @Test
    public void testCalculateConvertFileName() throws IOException {
        int FILE_NAME_LENGTH = 18;

        assertEquals(new File(file.getParentFile(), "convert.itn").getAbsolutePath(),
                calculateConvertFileName(file, 0, 0, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert1.itn").getAbsolutePath(),
                calculateConvertFileName(file, 1, 3, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert9.itn").getAbsolutePath(),
                calculateConvertFileName(file, 9, 9, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert5.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5, 5, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert05.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5, 50, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert005.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5, 500, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert10.bcr").getAbsolutePath(),
                calculateConvertFileName(file, 10, 10, ".bcr", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert99.bcr").getAbsolutePath(),
                calculateConvertFileName(file, 99, 99, ".bcr", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert50.itn").getAbsolutePath(),
                calculateConvertFileName(file, 50, 50, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert050.itn").getAbsolutePath(),
                calculateConvertFileName(file, 50, 500, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert100.kml").getAbsolutePath(),
                calculateConvertFileName(file, 100, 100, ".kml", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert999.long").getAbsolutePath(),
                calculateConvertFileName(file, 999, 999, ".long", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert500.itn").getAbsolutePath(),
                calculateConvertFileName(file, 500, 500, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert500.itn").getAbsolutePath(),
                calculateConvertFileName(file, 500, 999, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "convert5000.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5000, 5000, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "convert5000.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5000, 9999, ".itn", FILE_NAME_LENGTH));
    }

    @Test
    public void testCalculateConvertFileNameLimitedLength() throws IOException {
        int FILE_NAME_LENGTH = 4;

        assertEquals(new File(file.getParentFile(), "conv.itn").getAbsolutePath(),
                calculateConvertFileName(file, 0, 0, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "con9.itn").getAbsolutePath(),
                calculateConvertFileName(file, 9, 9, ".itn", FILE_NAME_LENGTH));

        assertEquals(new File(file.getParentFile(), "con5.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5, 5, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "co05.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5, 50, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "c005.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5, 500, ".itn", FILE_NAME_LENGTH));
        assertEquals(new File(file.getParentFile(), "5000.itn").getAbsolutePath(),
                calculateConvertFileName(file, 5000, 5000, ".itn", FILE_NAME_LENGTH));
    }

    @Test
    public void testCalculateConvertFileNameThrowsException() throws IOException {
        try {
            calculateConvertFileName(file, 10000, 10000, "gpx", 64);
            assertTrue("IllegalArgumentException expected", false);
        }
        catch (IllegalArgumentException e) {
        }

        try {
            calculateConvertFileName(file, 5000, 10000, "gpx", 64);
            assertTrue("IllegalArgumentException expected", false);
        }
        catch (IllegalArgumentException e) {
        }

        try {
            calculateConvertFileName(file, 1001, 999, "gpx", 64);
            assertTrue("IllegalArgumentException expected", false);
        }
        catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testCreateGoPalFileName() {
        assertEquals("EIFELSTERN AACHEN", createGoPalFileName("Eifelstern-Aachen"));
        assertEquals("EIFELSTERN.XML", createGoPalFileName("Eifelstern.xml"));
    }

    @Test
    public void testShortenPath() {
        assertEquals("http://maps.google.de/maps?f=d&hl=de&geocode=142500959607...",
                     shortenPath("http://maps.google.de/maps?f=d&hl=de&geocode=14250095960720490931,54.083160,13.475246%3B13832872253745319564,54.096925,13.383573%3B4731465831403354564,54.114440,13.528310&saddr=54.096925,+13.383573&daddr=54.08316,13.475246+to:54.114440,+13.528310&mra=ps&mrcr=0,1&sll=54.105307,13.490181&sspn=0.132448,0.318604&ie=UTF8&z=12", 60));
    }

    @Test
    public void testLastPathFragment() {
        assertEquals("file.gpx", lastPathFragment("file.gpx"));
        assertEquals("file.gpx", lastPathFragment("../file.gpx"));
        assertEquals("file.gpx", lastPathFragment("c:\\bla\\bla\\file.gpx"));
        assertEquals("file.gpx", lastPathFragment("c:/bla/bla/file.gpx"));
        assertEquals("file.gpx", lastPathFragment("file:///c:/bla/bla/file.gpx"));
        assertEquals("file.gpx", lastPathFragment("http://www.blabla.com/bla/bla/file.gpx"));
        assertEquals("maps?f=d&hl=de&geocode=14250095960720490931,54.0831601,13...", lastPathFragment("http://maps.google.de/maps?f=d&hl=de&geocode=14250095960720490931,54.0831601,13.475246%3B13832872253745319564,54.096925,13.383573%3B4731465831403354564,54.114440,13.528310&saddr=54.096925,+13.383573&daddr=54.08316,13.475246+to:54.114440,+13.528310&mra=ps&mrcr=0,1&sll=54.105307,13.490181&sspn=0.132448,0.318604&ie=UTF8&z=12"));
    }
}
