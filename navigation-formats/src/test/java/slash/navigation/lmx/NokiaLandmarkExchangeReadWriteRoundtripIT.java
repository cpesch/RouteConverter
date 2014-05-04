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

package slash.navigation.lmx;

import org.junit.Test;
import slash.navigation.base.ParserResult;
import slash.navigation.base.ReadWriteTestCallback;
import slash.navigation.base.Wgs84Position;
import slash.navigation.lmx.binding.LandmarkType;
import slash.navigation.lmx.binding.Lmx;
import slash.navigation.lmx.binding.MediaLinkType;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.ReadWriteBase.readWriteRoundtrip;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class NokiaLandmarkExchangeReadWriteRoundtripIT {

    private void checkUnprocessed(Lmx lmx) {
        assertNotNull(lmx);
        assertNotNull(lmx.getLandmark());
        assertNotNull(lmx.getLandmarkCollection());
    }

    private void checkUnprocessed(LandmarkType type) {
        assertNotNull(type);
        assertEquals("Waypoint1 Name", type.getName());
        assertEquals("Description", type.getDescription());
        assertDoubleEquals(2.0f, type.getCoverageRadius());
        List<MediaLinkType> linkTypes = type.getMediaLink();
        assertNotNull(linkTypes);
        MediaLinkType mediaLinkType = linkTypes.get(0);
        assertEquals("URL", mediaLinkType.getUrl());
        assertEquals("URLName", mediaLinkType.getName());
        assertEquals("URLMime", mediaLinkType.getMime());
    }

    @Test
    public void testNokiaLandmarkExchangeRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.lmx", new ReadWriteTestCallback() {
            public void test(ParserResult source, ParserResult target) {
                NokiaLandmarkExchangeRoute sourceWaypoints = (NokiaLandmarkExchangeRoute) source.getAllRoutes().get(0);
                assertEquals(Waypoints, sourceWaypoints.getCharacteristics());
                assertNotNull(sourceWaypoints.getLmx());
                assertEquals(Lmx.class, sourceWaypoints.getLmx().getClass());
                checkUnprocessed(sourceWaypoints.getLmx());
                Wgs84Position sourceWaypoint = sourceWaypoints.getPosition(0);
                assertNotNull(sourceWaypoint.getOrigin());
                checkUnprocessed(sourceWaypoint.getOrigin(LandmarkType.class));

                NokiaLandmarkExchangeRoute targetWaypoints = (NokiaLandmarkExchangeRoute) source.getAllRoutes().get(0);
                assertEquals(Waypoints, targetWaypoints.getCharacteristics());
                assertNotNull(targetWaypoints.getLmx());
                assertEquals(Lmx.class, targetWaypoints.getLmx().getClass());
                checkUnprocessed(targetWaypoints.getLmx());
                Wgs84Position targetWaypoint = targetWaypoints.getPosition(0);
                assertNotNull(targetWaypoint.getOrigin());
                checkUnprocessed(targetWaypoint.getOrigin(LandmarkType.class));
            }
        });
    }
}