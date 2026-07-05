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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

/**
 * Covers the {@code --brouter-segments} argument handling of
 * {@link RouteConverterCmdLine#createLengthComputer}: a dangling option and a
 * non-existent directory both degrade to the point-to-point (beeline) computer
 * so the analyze run still emits JSON, while an existing directory selects the
 * BRouter-backed computer (specs/00055).
 */
public class RouteConverterCmdLineTest {

    @Test
    public void noBRouterOptionUsesPointToPoint() {
        RouteLengthComputer computer = RouteConverterCmdLine.createLengthComputer(
                new String[]{"analyze", "route.gpx"});
        assertTrue(computer.getClass().getName(), computer instanceof PointToPointLengthComputer);
    }

    @Test
    public void danglingBRouterSegmentsOptionUsesPointToPoint() {
        // --brouter-segments given without a directory argument: warn and beeline
        RouteLengthComputer computer = RouteConverterCmdLine.createLengthComputer(
                new String[]{"analyze", "route.gpx", "--brouter-segments"});
        assertTrue(computer.getClass().getName(), computer instanceof PointToPointLengthComputer);
    }

    @Test
    public void nonExistentBRouterSegmentsDirectoryUsesPointToPoint() {
        File missing = new File(System.getProperty("java.io.tmpdir"), "no-such-brouter-segments-dir-" + System.nanoTime());
        RouteLengthComputer computer = RouteConverterCmdLine.createLengthComputer(
                new String[]{"analyze", "route.gpx", "--brouter-segments", missing.getAbsolutePath()});
        assertTrue(computer.getClass().getName(), computer instanceof PointToPointLengthComputer);
    }

    @Test
    public void existingBRouterSegmentsDirectorySelectsBRouterComputer() throws IOException {
        File segments = Files.createTempDirectory("brouter-segments-test").toFile();
        segments.deleteOnExit();
        RouteLengthComputer computer = RouteConverterCmdLine.createLengthComputer(
                new String[]{"analyze", "route.gpx", "--brouter-segments", segments.getAbsolutePath()});
        assertTrue(computer.getClass().getName(), computer instanceof BRouterRouteLengthComputer);
    }
}
