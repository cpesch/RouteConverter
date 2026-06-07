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

package slash.navigation.photo;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link TagState}.
 *
 * @author Christian Pesch
 */

public class TagStateTest {

    @Test
    public void testThreeValues() {
        assertEquals(3, TagState.values().length);
    }

    @Test
    public void testTaggedOrdinal() {
        assertEquals(0, TagState.Tagged.ordinal());
    }

    @Test
    public void testTaggableOrdinal() {
        assertEquals(1, TagState.Taggable.ordinal());
    }

    @Test
    public void testNotTaggableOrdinal() {
        assertEquals(2, TagState.NotTaggable.ordinal());
    }

    @Test
    public void testValueOfTagged() {
        assertEquals(TagState.Tagged, TagState.valueOf("Tagged"));
    }

    @Test
    public void testValueOfTaggable() {
        assertEquals(TagState.Taggable, TagState.valueOf("Taggable"));
    }

    @Test
    public void testValueOfNotTaggable() {
        assertEquals(TagState.NotTaggable, TagState.valueOf("NotTaggable"));
    }

    @Test
    public void testNameTagged() {
        assertEquals("Tagged", TagState.Tagged.name());
    }
}

