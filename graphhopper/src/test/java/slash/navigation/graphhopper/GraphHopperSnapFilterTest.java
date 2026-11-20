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
package slash.navigation.graphhopper;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.Snap;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.routing.TravelMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GraphHopperSnapFilterTest {
    // a car-only road and, about 11 m north of it, a separated bike-only cycleway
    private static final double ROAD_LATITUDE = 46.6057;
    private static final double CYCLEWAY_LATITUDE = 46.6058;
    // a click 2 m north of the road, 9 m south of the cycleway
    private static final double CLICK_LATITUDE = 46.60572;
    private static final double CLICK_LONGITUDE = 13.8755;

    private EncodingManager encodingManager;
    private LocationIndexTree locationIndex;

    @Before
    public void setUp() {
        BooleanEncodedValue carAccess = VehicleAccess.create("car");
        BooleanEncodedValue bikeAccess = VehicleAccess.create("bike");
        encodingManager = EncodingManager.start().add(carAccess).add(bikeAccess).build();

        BaseGraph graph = new BaseGraph.Builder(encodingManager.getBytesForFlags()).create();
        NodeAccess nodes = graph.getNodeAccess();
        nodes.setNode(0, ROAD_LATITUDE, 13.875, 0);
        nodes.setNode(1, ROAD_LATITUDE, 13.876, 0);
        nodes.setNode(2, CYCLEWAY_LATITUDE, 13.875, 0);
        nodes.setNode(3, CYCLEWAY_LATITUDE, 13.876, 0);
        graph.edge(0, 1).setDistance(76.5).set(carAccess, true, true).set(bikeAccess, false, false);
        graph.edge(2, 3).setDistance(76.5).set(carAccess, false, false).set(bikeAccess, true, true);

        locationIndex = new LocationIndexTree(graph, graph.getDirectory());
        locationIndex.prepareIndex();
    }

    private double snapLatitude(EdgeFilter filter) {
        Snap snap = locationIndex.findClosest(CLICK_LATITUDE, CLICK_LONGITUDE, filter);
        assertTrue(snap.isValid());
        return snap.getSnappedPoint().lat;
    }

    @Test
    public void testAllEdgesSnapsToNearestEdge() {
        assertEquals(ROAD_LATITUDE, snapLatitude(EdgeFilter.ALL_EDGES), 0.000001);
    }

    @Test
    public void testBikeSnapsToCyclewayNotToNearerCarOnlyRoad() {
        EdgeFilter filter = GraphHopper.createSnapFilter(encodingManager, new TravelMode("bike"));
        assertEquals(CYCLEWAY_LATITUDE, snapLatitude(filter), 0.000001);
    }

    @Test
    public void testCarSnapsToRoad() {
        EdgeFilter filter = GraphHopper.createSnapFilter(encodingManager, new TravelMode("car"));
        assertEquals(ROAD_LATITUDE, snapLatitude(filter), 0.000001);
    }

    @Test
    public void testUnknownTravelModeSnapsToAllEdges() {
        assertSame(EdgeFilter.ALL_EDGES, GraphHopper.createSnapFilter(encodingManager, new TravelMode("foot")));
    }
}
