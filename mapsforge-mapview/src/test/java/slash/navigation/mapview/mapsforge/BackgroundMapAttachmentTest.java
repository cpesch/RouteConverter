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
package slash.navigation.mapview.mapsforge;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.mapview.mapsforge.BackgroundMapAttachment.shouldAttachBackground;

/**
 * Tests for {@link BackgroundMapAttachment}.
 *
 * @author Christian Pesch
 */
public class BackgroundMapAttachmentTest {
    @Test
    public void doesNotAttachWhenLayerNotReady() {
        assertFalse(shouldAttachBackground(false, true));
    }

    @Test
    public void doesNotAttachWhenLayerReadyButNoDisplayedMap() {
        assertFalse(shouldAttachBackground(true, false));
    }

    @Test
    public void attachesWhenLayerReadyAndAnyMapDisplayed() {
        // the world map is a global base layer: it belongs under every displayed
        // map type (Download/Mapsforge/MBTiles), so an offline edition whose only
        // selectable map is the online OpenStreetMap default still shows a map
        // instead of staying gray when those tiles cannot be reached
        assertTrue(shouldAttachBackground(true, true));
    }
}
