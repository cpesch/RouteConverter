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

package slash.navigation.routes.local;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link LocalRoute}.
 *
 * @author Christian Pesch
 */
public class LocalRouteTest {

    private File existingFile;
    private File absentFile;

    @Before
    public void setUp() throws IOException {
        existingFile = File.createTempFile("local-route-test", ".gpx");
        existingFile.deleteOnExit();

        absentFile = File.createTempFile("local-route-absent", ".gpx");
        assertTrue(absentFile.delete()); // make it absent
    }

    @After
    public void tearDown() {
        if (existingFile.exists())
            //noinspection ResultOfMethodCallIgnored
            existingFile.delete();
    }

    // --- getName ---

    @Test
    public void getNameReturnsFileName() {
        LocalRoute route = new LocalRoute(existingFile);
        assertEquals(existingFile.getName(), route.getName());
    }

    // --- getHref ---

    @Test
    public void getHrefReturnsFileUrl() throws IOException {
        LocalRoute route = new LocalRoute(existingFile);
        String href = route.getHref();
        assertNotNull(href);
        assertTrue(href.startsWith("file:"));
        assertTrue(href.contains(existingFile.getName()));
    }

    // --- getDescription ---

    @Test
    public void getDescriptionReturnsNameWhenFileExists() {
        LocalRoute route = new LocalRoute(existingFile);
        assertEquals(existingFile.getName(), route.getDescription());
    }

    @Test
    public void getDescriptionReturnsBrokenLinkWhenFileAbsent() {
        LocalRoute route = new LocalRoute(absentFile);
        String desc = route.getDescription();
        assertTrue(desc.startsWith("broken link:"));
        assertTrue(desc.contains(absentFile.getName()));
    }

    // --- getUrl ---

    @Test
    public void getUrlMatchesGetHref() {
        LocalRoute route = new LocalRoute(existingFile);
        assertEquals(route.getHref(), route.getUrl());
    }

    // --- getCreator ---

    @Test
    public void getCreatorReturnsOwnerForExistingFile() throws IOException {
        LocalRoute route = new LocalRoute(existingFile);
        String creator = route.getCreator();
        assertNotNull(creator);
        assertFalse(creator.isEmpty());
    }

    @Test
    public void getCreatorReturnsUserNameForAbsentFile() throws IOException {
        LocalRoute route = new LocalRoute(absentFile);
        assertEquals(System.getProperty("user.name"), route.getCreator());
    }

    // --- equals and hashCode ---

    @Test
    public void equalsReturnsTrueForSameFile() {
        LocalRoute r1 = new LocalRoute(existingFile);
        LocalRoute r2 = new LocalRoute(existingFile);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    public void equalsReturnsFalseForDifferentFile() {
        LocalRoute r1 = new LocalRoute(existingFile);
        LocalRoute r2 = new LocalRoute(absentFile);
        assertNotEquals(r1, r2);
    }

    @Test
    public void equalsReturnsFalseForNull() {
        LocalRoute r = new LocalRoute(existingFile);
        assertNotEquals(r, null);
    }

    // --- toString ---

    @Test
    public void toStringContainsFileName() {
        LocalRoute route = new LocalRoute(existingFile);
        String s = route.toString();
        assertTrue(s.contains("LocalRoute"));
        assertTrue(s.contains(existingFile.getName()));
    }
}

