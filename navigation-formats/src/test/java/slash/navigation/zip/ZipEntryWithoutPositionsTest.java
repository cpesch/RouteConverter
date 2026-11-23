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
package slash.navigation.zip;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserResult;
import slash.navigation.kml.Kml22Format;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Entries of a ZIP archive that no format reads positions from, like the settings and README
 * files of a Google Takeout archive, do not take part in the format reported for the archive.
 *
 * @author Christian Pesch
 */
public class ZipEntryWithoutPositionsTest {
    private static final String KML22 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><name>D</name>\n" +
            "<Placemark><name>C</name><Point><coordinates>11.1,51.1,0</coordinates></Point></Placemark>\n" +
            "<Placemark><name>E</name><Point><coordinates>11.2,51.2,0</coordinates></Point></Placemark>\n" +
            "</Document></kml>\n";
    // synthetic shapes of Takeout files without positions
    private static final String SETTINGS_JSON = "{\"createdTime\": \"2024-01-01T00:00:00.000Z\", \"deviceSettings\": []}";
    private static final String README_TXT = "This archive contains your data.\nSee the other files.\n";

    private final NavigationFormatParser parser = new NavigationFormatParser(new NavigationFormatRegistry());
    private final List<File> files = new ArrayList<>();

    @AfterEach
    public void tearDown() {
        for (File file : files)
            assertTrue(file.delete());
    }

    private File zip(String... namesAndContents) throws IOException {
        File file = File.createTempFile("zipentrywithoutpositions", ".zip");
        files.add(file);
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            for (int i = 0; i < namesAndContents.length; i += 2) {
                zip.putNextEntry(new ZipEntry(namesAndContents[i]));
                zip.write(namesAndContents[i + 1].getBytes(UTF_8));
                zip.closeEntry();
            }
        }
        return file;
    }

    @Test
    public void entriesWithoutPositionsDoNotNameTheArchiveFormat() throws IOException {
        ParserResult result = parser.read(zip("Takeout/Zeitachse/Settings.json", SETTINGS_JSON,
                "Takeout/README.txt", README_TXT, "Takeout/places.kml", KML22));
        assertTrue(result.isSuccessful());
        assertEquals(Kml22Format.class, result.getFormat().getClass());
        assertEquals(1, result.getAllRoutes().size());
        assertEquals(2, result.getTheRoute().getPositionCount());
    }

    @Test
    public void archiveWithoutPositionsReadsWithoutError() throws IOException {
        ParserResult result = parser.read(zip("Takeout/Zeitachse/Settings.json", SETTINGS_JSON, "Takeout/README.txt", README_TXT));
        if (result.isSuccessful())
            assertEquals(0, result.getAllRoutes().stream().mapToInt(route -> route.getPositionCount()).sum());
    }
}
