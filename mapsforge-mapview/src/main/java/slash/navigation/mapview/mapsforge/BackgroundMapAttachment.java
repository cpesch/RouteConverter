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

/**
 * Decides whether the world-map background layer should be attached as the
 * base layer, independent of the order in which the async background map
 * download (Path B) and the EDT map/theme layer-stack build (Path A) run.
 *
 * @author Christian Pesch
 */

public class BackgroundMapAttachment {
    private BackgroundMapAttachment() {
    }

    /**
     * @param layerReady            whether the background layer has been built successfully
     * @param hasDisplayedMap       whether a map is currently displayed
     * @param displayedMapIsMapsforge whether the displayed map is of type Mapsforge
     * @return true iff the world-map background layer should be attached as the base layer now
     */
    public static boolean shouldAttachBackground(boolean layerReady, boolean hasDisplayedMap, boolean displayedMapIsMapsforge) {
        if (!layerReady)
            return false;
        if (!hasDisplayedMap)
            return false;
        return displayedMapIsMapsforge;
    }
}
