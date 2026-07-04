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
package slash.navigation.converter.gui.helpers;

import org.junit.Test;
import slash.navigation.base.NavigationFormat;

import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link NavigationFormatFileFilter}.
 *
 * @author Christian Pesch
 */
public class NavigationFormatFileFilterTest {

    private NavigationFormat gpxFormat() {
        NavigationFormat format = mock(NavigationFormat.class);
        when(format.getExtension()).thenReturn(".gpx");
        when(format.getName()).thenReturn("GPS Exchange Format");
        return format;
    }

    @Test
    public void acceptsMatchingExtension() {
        NavigationFormatFileFilter sut = new NavigationFormatFileFilter(gpxFormat());

        assertTrue(sut.accept(new File("track.gpx")));
    }

    @Test
    public void rejectsOtherExtension() {
        NavigationFormatFileFilter sut = new NavigationFormatFileFilter(gpxFormat());

        assertFalse(sut.accept(new File("track.kml")));
    }

    @Test
    public void acceptsDirectoriesRegardlessOfExtension() {
        NavigationFormatFileFilter sut = new NavigationFormatFileFilter(gpxFormat());

        File directory = mock(File.class);
        when(directory.isDirectory()).thenReturn(true);

        assertTrue(sut.accept(directory));
    }

    @Test
    public void exposesDescriptionAndFormat() {
        NavigationFormat format = gpxFormat();
        NavigationFormatFileFilter sut = new NavigationFormatFileFilter(format);

        assertEquals("GPS Exchange Format", sut.getDescription());
        assertSame(format, sut.getFormat());
    }
}
