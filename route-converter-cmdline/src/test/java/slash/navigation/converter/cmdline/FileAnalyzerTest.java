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

package slash.navigation.converter.cmdline;

import org.junit.Test;
import slash.navigation.base.CmdLineNavigationFormatRegistry;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verifies the {@code analyze} JSON contract (specs/00055) against small
 * fixtures. The two-track fixture asserts the exact JSON so a change in the
 * aggregation is caught; the broken fixture asserts a parse failure surfaces as
 * an {@link IOException} (mapped to a non-zero exit in the command).
 */
public class FileAnalyzerTest {
    private FileAnalyzer analyzer() {
        return new FileAnalyzer(new CmdLineNavigationFormatRegistry(), new PointToPointLengthComputer());
    }

    private File resource(String name) throws URISyntaxException {
        return new File(getClass().getResource(name).toURI());
    }

    @Test
    public void analyzeTwoTrackGpxProducesExactJson() throws IOException, URISyntaxException {
        File source = resource("analyze-two-tracks.gpx");
        String json = analyzer().analyze(source);

        String expected = "{" +
                "\"size\":" + source.length() + "," +
                "\"format\":\"GPS Exchange Format 1.1 (*.gpx)\"," +
                "\"positionLists\":2," +
                "\"positions\":5," +
                "\"bbox\":{\"north\":52.511,\"south\":52.5,\"east\":13.411,\"west\":13.4}," +
                "\"lengthM\":391," +
                "\"lengthKind\":\"track\"," +
                "\"durationS\":780," +
                "\"elevationGainM\":6," +
                "\"elevationLossM\":7," +
                "\"startTime\":\"2020-05-01T08:00:00Z\"," +
                "\"firstName\":\"Track A\"," +
                "\"extension\":\".gpx\"" +
                "}";
        assertEquals(expected, json);
    }

    @Test
    public void garbageFileWithoutPositionsThrows() throws URISyntaxException {
        // sniffed as a permissive text format but carries no positions
        File source = resource("analyze-broken.gpx");
        try {
            String json = analyzer().analyze(source);
            fail("expected IOException, got " + json);
        } catch (IOException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage(), e.getMessage().contains("No positions"));
        }
    }

    @Test
    public void routeFileWithBRouterButNoSegmentsStillEmitsBeelineJson() throws IOException, URISyntaxException {
        // wiring the BRouter computer at a non-existent segments directory must
        // never crash the analyze run: JSON is still produced, kind is beeline
        File missing = new File(System.getProperty("java.io.tmpdir"), "no-such-brouter-segments-dir");
        FileAnalyzer brouterAnalyzer = new FileAnalyzer(new CmdLineNavigationFormatRegistry(),
                new BRouterRouteLengthComputer(missing));

        File source = resource("analyze-route.gpx");
        String json = brouterAnalyzer.analyze(source);
        assertTrue(json, json.contains("\"lengthKind\":\"beeline\""));
        assertTrue(json, json.contains("\"positionLists\":1"));
    }

    @Test
    public void corruptZipFails() throws URISyntaxException {
        // a hostile/corrupt zip must fail the analysis, not produce metadata
        File source = resource("analyze-corrupt.zip");
        try {
            String json = analyzer().analyze(source);
            fail("expected failure, got " + json);
        } catch (Exception e) {
            // the command maps any analysis exception to a non-zero exit code
            assertNotNull(e);
        }
    }
}
