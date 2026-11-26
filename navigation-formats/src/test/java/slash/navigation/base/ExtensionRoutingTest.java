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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.nmn.NavigatingPoiWarnerFormat;
import slash.navigation.simple.GlopusFormat;
import slash.navigation.simple.GpsTunerFormat;
import slash.navigation.simple.KienzleGpsFormat;
import slash.navigation.simple.KompassFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Files with an extension that clearly names their content are only probed by the formats of that
 * extension, all other files are probed by all formats.
 *
 * @author Christian Pesch
 */
public class ExtensionRoutingTest {
    // synthetic data: a health export without position columns and a track of two NMEA sentences
    private static final String CSV_WITHOUT_POSITIONS = "Date,Steps,Heart Rate\n2024-01-01,100,60\n2024-01-02,200,61\n";
    private static final String CSV_TRACK = "Longitude,Latitude,Elevation,Speed,Time,Description\n" +
            "8.4,50.2,650.0,10.0,14.12.17 19:38:00,Berlin\n";
    private static final String GEOJSON = "{\"type\": \"FeatureCollection\", \"features\": [{\"type\": \"Feature\", " +
            "\"properties\": {\"name\": \"A\"}, \"geometry\": {\"type\": \"Point\", \"coordinates\": [11.1, 51.1]}}]}";
    private static final String NMEA = "$GPRMC,081836,A,3751.65,S,14507.36,E,000.0,360.0,130998,011.3,E*62\n" +
            "$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47\n";

    /**
     * Records which formats the parser asked to read a source.
     */
    private static class RecordingRegistry extends NavigationFormatRegistry {
        private final Map<NavigationFormat<?>, Class<?>> spies = new IdentityHashMap<>();

        @SuppressWarnings("unchecked")
        public List<NavigationFormat<BaseRoute<?, ?>>> getReadFormats() {
            List<NavigationFormat<BaseRoute<?, ?>>> result = new ArrayList<>();
            for (NavigationFormat<BaseRoute<?, ?>> format : super.getReadFormats()) {
                NavigationFormat<BaseRoute<?, ?>> spy = Mockito.spy(format);
                spies.put(spy, format.getClass());
                result.add(spy);
            }
            return result;
        }

        Set<String> probedFormats() {
            Set<String> result = new TreeSet<>();
            for (Map.Entry<NavigationFormat<?>, Class<?>> entry : spies.entrySet()) {
                boolean probed = Mockito.mockingDetails(entry.getKey()).getInvocations().stream()
                        .anyMatch(invocation -> invocation.getMethod().getName().equals("read"));
                if (probed)
                    result.add(entry.getValue().getSimpleName());
            }
            return result;
        }
    }

    private final List<File> files = new ArrayList<>();

    @AfterEach
    public void tearDown() {
        for (File file : files)
            assertTrue(file.delete());
    }

    private File file(String suffix, String content) throws IOException {
        File file = File.createTempFile("extensionrouting", suffix);
        files.add(file);
        Files.write(file.toPath(), content.getBytes(UTF_8));
        return file;
    }

    private File zip(String... namesAndContents) throws IOException {
        File file = File.createTempFile("extensionrouting", ".zip");
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

    private static boolean onlyCsvFormats(Set<String> probed) {
        return probed.stream().allMatch(name -> Set.of("ColumbusFusionFormat", "ColumbusGpsType1Format", "ColumbusGpsType2Format",
                "CsvCommaFormat", "CsvSemicolonFormat", "Flightradar24Format", "GarbleColumbusGpsType1Format",
                "GarbleHaicomLoggerFormat", "GoogleSavedListFormat", "HaicomLoggerFormat", "Iblue747Format",
                "QstarzQ1000Format", "Route66Format", "NavigatingPoiWarnerFormat", "OpelNaviFormat", "GarminPoiDbFormat", "KompassFormat",
                "GlopusFormat", "GpsTunerFormat", "KienzleGpsFormat").contains(name));
    }

    // a format may accept a file without positions as an empty route: no positions is what counts
    private static void assertNoPositions(ParserResult result) {
        assertTrue(!result.isSuccessful() || result.getAllRoutes().stream().allMatch(route -> route.getPositionCount() == 0));
    }

    @Test
    public void csvWithoutPositionsIsOnlyProbedByCsvFormats() throws IOException {
        RecordingRegistry registry = new RecordingRegistry();
        ParserResult result = new NavigationFormatParser(registry).read(file(".csv", CSV_WITHOUT_POSITIONS));

        assertNoPositions(result);
        Set<String> probed = registry.probedFormats();
        assertFalse(probed.isEmpty());
        assertTrue(onlyCsvFormats(probed), probed.toString());
    }

    @Test
    public void csvExtensionIsMatchedCaseInsensitively() throws IOException {
        RecordingRegistry registry = new RecordingRegistry();
        ParserResult result = new NavigationFormatParser(registry).read(file(".CSV", CSV_WITHOUT_POSITIONS));

        assertNoPositions(result);
        assertTrue(onlyCsvFormats(registry.probedFormats()), registry.probedFormats().toString());
    }

    @Test
    public void sameContentWithOtherExtensionIsProbedByOtherFormatsToo() throws IOException {
        RecordingRegistry registry = new RecordingRegistry();
        ParserResult result = new NavigationFormatParser(registry).read(file(".txt", CSV_WITHOUT_POSITIONS));

        assertNoPositions(result);
        assertFalse(onlyCsvFormats(registry.probedFormats()), registry.probedFormats().toString());
        assertTrue(registry.probedFormats().size() > 50, registry.probedFormats().toString());
    }

    @Test
    public void gpxWithoutPositionsIsOnlyProbedByGpxFormats() throws IOException {
        RecordingRegistry registry = new RecordingRegistry();
        ParserResult result = new NavigationFormatParser(registry).read(file(".gpx", "not a gpx file\n"));

        assertNoPositions(result);
        Set<String> probed = registry.probedFormats();
        assertFalse(probed.isEmpty());
        assertTrue(Set.of("Gpx10Format", "Gpx11Format", "GarbleGpx10Format", "GarbleGpx11Format").containsAll(probed), probed.toString());
    }

    @Test
    public void jsonWithoutPositionsIsOnlyProbedByJsonFormats() throws IOException {
        RecordingRegistry registry = new RecordingRegistry();
        ParserResult result = new NavigationFormatParser(registry).read(file(".json", "{\"steps\": [1, 2, 3]}"));

        assertNoPositions(result);
        Set<String> probed = registry.probedFormats();
        assertFalse(probed.isEmpty());
        assertTrue(Set.of("GeoJsonFormat", "GoogleCommuteRoutesFormat", "GoogleTimelineFormat", "NavigonCruiserFormat")
                .containsAll(probed), probed.toString());
    }

    @Test
    public void csvTrackAndGeoJsonAreStillRead() throws IOException {
        NavigationFormatParser parser = new NavigationFormatParser(new NavigationFormatRegistry());

        ParserResult csv = parser.read(file(".csv", CSV_TRACK));
        assertTrue(csv.isSuccessful());
        assertEquals(1, csv.getTheRoute().getPositionCount());

        ParserResult json = parser.read(file(".json", GEOJSON));
        assertTrue(json.isSuccessful());
        assertEquals(1, json.getTheRoute().getPositionCount());
    }

    @Test
    public void poiListsSavedAsCsvAreStillRead() throws IOException {
        ParserResult result = new NavigationFormatParser(new NavigationFormatRegistry())
                .read(file(".csv", "8.6180900,50.2175100,\"Place A\"\n8.7000000,50.3000000,\"Place B\"\n"));

        assertTrue(result.isSuccessful());
        assertEquals(NavigatingPoiWarnerFormat.class, result.getFormat().getClass());
        assertEquals(2, result.getTheRoute().getPositionCount());
    }

    // synthetic lists of coordinates in the shapes of the formats added to the .csv group
    private static final String KOMPASS = "51.0450383,7.0508300,124.5\n51.0460383,7.0518300,125.5\n";
    private static final String GLOPUS = "51.04503,7.05083\n51.04603,7.05183\n";
    private static final String GLOPUS_COMMENTS = "51.0450383,7.0508300,Place A\n51.0460383,7.0518300,Place B\n";
    private static final String GPS_TUNER = "GPS Tracklog - Generated by GPS Tuner 5.0\n" +
            "Latitude(Degree);Longitude(Degree);Altitude(m);Speed(kmph);Date(Unix TimeStamp);Segment;Heading(Degree)\n" +
            "50.3965966666667;7.53247333333333;74.4000015258789;77.56176;1172932595;1;279\n" +
            "50.3975966666667;7.53347333333333;75.4000015258789;78.56176;1172932599;1;280\n";
    private static final String KIENZLE = "Position;X;Y;Empfänger;Land;PLZ;Ort;Strasse;Hausnummer;Planankunft;Zusatzinfos\n" +
            "113;7.0475000000;50.7500000000;PHE I;;53119;Bonn;Oppelner Str.;126;16:49;\n" +
            "103;7.0997220000;50.7494440000;;;53117;Bonn;Am Jesuitenhof;;16:16;\n";

    private void assertCsvIsReadAsOnMaster(String content, Class<?> expectedFormat) throws IOException {
        File file = file(".csv", content);
        NavigationFormatRegistry registry = new NavigationFormatRegistry();
        NavigationFormatParser parser = new NavigationFormatParser(registry);

        ParserResult master = parser.read(file, registry.getReadFormatsPreferredByExtension(".csv"));
        ParserResult result = parser.read(file);

        assertTrue(master.isSuccessful());
        assertTrue(result.isSuccessful());
        assertEquals(expectedFormat, master.getFormat().getClass());
        assertEquals(expectedFormat, result.getFormat().getClass());
        assertEquals(2, result.getTheRoute().getPositionCount());
        assertEquals(master.getTheRoute().getPositionCount(), result.getTheRoute().getPositionCount());
    }

    @Test
    public void kompassListsSavedAsCsvAreStillReadByKompass() throws IOException {
        assertCsvIsReadAsOnMaster(KOMPASS, KompassFormat.class);
    }

    @Test
    public void glopusListsSavedAsCsvAreStillReadByGlopus() throws IOException {
        assertCsvIsReadAsOnMaster(GLOPUS, GlopusFormat.class);
        assertCsvIsReadAsOnMaster(GLOPUS_COMMENTS, GlopusFormat.class);
    }

    @Test
    public void gpsTunerTracksSavedAsCsvAreStillReadByGpsTuner() throws IOException {
        assertCsvIsReadAsOnMaster(GPS_TUNER, GpsTunerFormat.class);
    }

    @Test
    public void kienzleListsSavedAsCsvAreStillReadByKienzle() throws IOException {
        assertCsvIsReadAsOnMaster(KIENZLE, KienzleGpsFormat.class);
    }

    @Test
    public void nmeaInTxtFileIsStillRead() throws IOException {
        ParserResult result = new NavigationFormatParser(new NavigationFormatRegistry()).read(file(".txt", NMEA));

        assertTrue(result.isSuccessful());
        assertEquals(NmeaFormat.class, result.getFormat().getClass());
    }

    @Test
    public void entriesOfAZipAreRoutedByTheirExtension() throws IOException {
        RecordingRegistry registry = new RecordingRegistry();
        ParserResult result = new NavigationFormatParser(registry).read(zip("health/steps.csv", CSV_WITHOUT_POSITIONS));

        assertFalse(result.isSuccessful());
        Set<String> probed = registry.probedFormats();
        probed.remove("ZipFormat");
        assertTrue(onlyCsvFormats(probed), probed.toString());
    }

    @Test
    public void zipWithCsvTrackGeoJsonAndNmeaTextReadsAllThree() throws IOException {
        ParserResult result = new NavigationFormatParser(new NavigationFormatRegistry())
                .read(zip("track.csv", CSV_TRACK, "places.json", GEOJSON, "log.txt", NMEA));

        assertTrue(result.isSuccessful());
        assertEquals(3, result.getAllRoutes().size());
    }
}
