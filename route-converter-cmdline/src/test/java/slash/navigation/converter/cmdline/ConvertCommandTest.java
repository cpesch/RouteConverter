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

    Copyright (C) 2026 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.converter.cmdline;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.converter.cmdline.ConvertCommand.*;

/**
 * The {@code convert} command over the first-release pair matrix of the online converter, with
 * the format lists the server passes, plus the refusals it relies on.
 */
public class ConvertCommandTest {
    private static final String GPX_IN = "Gpx11Format,Gpx10Format";
    private static final String KML_IN = "Kml22Format,Kml22BetaFormat,Kml21Format,Kml20Format";
    private static final String KMZ_IN = "Kmz22Format,Kmz22BetaFormat,Kmz21Format,Kmz20Format";
    private static final String TCX_IN = "Tcx2Format,Tcx1Format";

    private File dir;

    @Before
    public void setUp() throws IOException {
        dir = Files.createTempDirectory("convert-test").toFile();
    }

    @After
    public void tearDown() {
        File[] files = dir.listFiles();
        if (files != null)
            for (File file : files)
                //noinspection ResultOfMethodCallIgnored
                file.delete();
        //noinspection ResultOfMethodCallIgnored
        dir.delete();
    }

    private File resource(String name) throws URISyntaxException {
        return new File(getClass().getResource(name).toURI());
    }

    private String lastOutput;

    private int convert(File source, String from, String to, File target) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<String> args = new ArrayList<>(List.of("convert"));
        if (from != null)
            args.addAll(List.of("--from", from));
        args.addAll(List.of("--to", to, source.getPath(), target.getPath()));
        int exit = new ConvertCommand().run(args.toArray(new String[0]), new PrintStream(out, true, StandardCharsets.UTF_8));
        lastOutput = out.toString(StandardCharsets.UTF_8);
        return exit;
    }

    private File target(String name) {
        return new File(dir, name);
    }

    private static String text(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    @Test
    public void rtzToGpx() throws Exception {
        File out = target("out.gpx");
        assertEquals(OK, convert(resource("convert-from.rtz"), "RtzFormat", "Gpx11Format", out));
        assertTrue(text(out).contains("<gpx"));
        assertTrue(text(out).contains("Elbe 1"));
        assertTrue(lastOutput, lastOutput.contains("\"routesRead\":1"));
    }

    @Test
    public void kmlToGpx() throws Exception {
        File out = target("out.gpx");
        assertEquals(OK, convert(resource("convert-from.kml"), KML_IN, "Gpx11Format", out));
        assertTrue(text(out).contains("<gpx"));
    }

    @Test
    public void kmzToGpx() throws Exception {
        // a KMZ is the KML in a zip
        File kmz = target("in.kmz");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(kmz.toPath()))) {
            zip.putNextEntry(new ZipEntry("doc.kml"));
            zip.write(Files.readAllBytes(resource("convert-from.kml").toPath()));
            zip.closeEntry();
        }
        File out = target("out.gpx");
        assertEquals(OK, convert(kmz, KMZ_IN, "Gpx11Format", out));
        assertTrue(text(out).contains("<gpx"));
    }

    @Test
    public void gpxToKmlWritesAZipFreeKml() throws Exception {
        File out = target("out.kml");
        assertEquals(OK, convert(resource("analyze-two-tracks.gpx"), GPX_IN, "Kml22Format", out));
        assertTrue(text(out).contains("<kml"));
    }

    @Test
    public void gpxToFitAndBack() throws Exception {
        File fit = target("out.fit");
        assertEquals(OK, convert(resource("analyze-two-tracks.gpx"), GPX_IN, "FitFormat", fit));
        assertTrue(fit.length() > 14);
        // two tracks in, one in the single-route FIT: reported, not hidden
        assertTrue(lastOutput, lastOutput.contains("\"routesRead\":2,\"routesWritten\":1"));

        File gpx = target("back.gpx");
        assertEquals(OK, convert(fit, "FitFormat", "Gpx11Format", gpx));
        assertTrue(text(gpx).contains("<trkpt"));
    }

    @Test
    public void realFitToGpx() throws Exception {
        File out = target("out.gpx");
        assertEquals(OK, convert(resource("convert-from.fit"), "FitFormat", "Gpx11Format", out));
        assertTrue(text(out).contains("<gpx"));
    }

    @Test
    public void gpxToTcxAndBack() throws Exception {
        File tcx = target("out.tcx");
        assertEquals(OK, convert(resource("analyze-two-tracks.gpx"), GPX_IN, "Tcx2Format", tcx));
        assertTrue(text(tcx).contains("TrainingCenterDatabase"));

        File gpx = target("back.gpx");
        assertEquals(OK, convert(tcx, TCX_IN, "Gpx11Format", gpx));
        assertTrue(text(gpx).contains("<gpx"));
    }

    @Test
    public void tcxToGpx() throws Exception {
        File out = target("out.gpx");
        assertEquals(OK, convert(resource("convert-course.tcx"), TCX_IN, "Gpx11Format", out));
        assertTrue(text(out).contains("52.505"));
    }

    @Test
    public void googleTimelineToGpx() throws Exception {
        File out = target("out.gpx");
        assertEquals(OK, convert(resource("convert-timeline.json"), "GoogleTimelineFormat", "Gpx11Format", out));
        assertTrue(text(out).contains("52.521"));
    }

    @Test
    public void withoutFromTheFormatIsDetected() throws Exception {
        File out = target("out.gpx");
        assertEquals(OK, convert(resource("convert-from.rtz"), null, "Gpx11Format", out));
        assertTrue(text(out).contains("Elbe 1"));
    }

    @Test
    public void sponsorGatedTargetsAreRefused() throws Exception {
        for (String sponsor : List.of("RtzFormat", "GarminFlightPlanFormat", "MSFSFlightPlanFormat", "GoRiderGpsFormat")) {
            File out = target("sponsor-" + sponsor);
            assertEquals(sponsor, TARGET_FORMAT_NOT_ALLOWED, convert(resource("analyze-two-tracks.gpx"), GPX_IN, sponsor, out));
            assertFalse(sponsor + " wrote a file", out.exists());
        }
    }

    @Test
    public void sourceMustMatchTheDeclaredFormat() throws Exception {
        // a GPX presented as RTZ is not read by the GPX reader behind the RTZ label
        File out = target("out.gpx");
        assertEquals(SOURCE_NOT_READABLE, convert(resource("analyze-two-tracks.gpx"), "RtzFormat", "Gpx11Format", out));
        assertFalse(out.exists());
    }

    @Test
    public void garbageIsNotReadable() throws Exception {
        File junk = target("junk.gpx");
        Files.write(junk.toPath(), new byte[]{1, 2, 3, 4, 5});
        assertEquals(SOURCE_NOT_READABLE, convert(junk, GPX_IN, "Kml22Format", target("out.kml")));
    }

    @Test
    public void unknownFormatsAndMissingFilesAreRefused() throws Exception {
        File gpx = resource("analyze-two-tracks.gpx");
        assertEquals(UNKNOWN_TARGET_FORMAT, convert(gpx, GPX_IN, "NoSuchFormat", target("a")));
        assertEquals(UNKNOWN_SOURCE_FORMAT, convert(gpx, "NoSuchFormat", "Gpx11Format", target("b")));
        assertEquals(SOURCE_MISSING, convert(target("missing.gpx"), GPX_IN, "Kml22Format", target("c")));
        assertEquals(USAGE, new ConvertCommand().run(new String[]{"convert", "--to", "Gpx11Format"}, System.out));
    }

    @Test
    public void existingTargetIsNeverOverwritten() throws Exception {
        File out = target("exists.gpx");
        Files.writeString(out.toPath(), "keep");
        assertEquals(TARGET_EXISTS, convert(resource("convert-from.rtz"), "RtzFormat", "Gpx11Format", out));
        assertEquals("keep", text(out));
    }
}
