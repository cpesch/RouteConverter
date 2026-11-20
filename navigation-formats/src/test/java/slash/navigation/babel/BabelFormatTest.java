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

package slash.navigation.babel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static slash.navigation.babel.BabelFormat.checkIfBabelExists;

/**
 * Pins which paths {@link BabelFormat} accepts as the GPSBabel executable,
 * without needing GPSBabel installed.
 */
public class BabelFormatTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void emptyPathIsNotTheExecutable() {
        // the default of the babelPath preference; new File("") is the
        // current directory and exists since Java 24
        assertNull(checkIfBabelExists(""));
    }

    @Test
    public void blankAndNullPathsAreNotTheExecutable() {
        assertNull(checkIfBabelExists("  "));
        assertNull(checkIfBabelExists(null));
    }

    @Test
    public void directoryIsNotTheExecutable() throws IOException {
        assertNull(checkIfBabelExists(folder.newFolder("GPSBabel").getPath()));
        assertNull(checkIfBabelExists("."));
    }

    @Test
    public void missingFileIsNotTheExecutable() {
        assertNull(checkIfBabelExists(new File(folder.getRoot(), "gpsbabel").getPath()));
    }

    @Test
    public void regularFileIsTheExecutable() throws IOException {
        File gpsbabel = folder.newFile("gpsbabel");
        assertEquals(gpsbabel, checkIfBabelExists(gpsbabel.getPath()));
    }
}
