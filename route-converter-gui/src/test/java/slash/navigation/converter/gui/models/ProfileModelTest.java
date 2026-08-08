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

package slash.navigation.converter.gui.models;

import org.junit.Before;
import org.junit.Test;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.DistanceAndTimeAggregator;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.profileview.XAxisMode;
import slash.navigation.converter.gui.profileview.YAxisMode;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Regression test for the profile-view side effect of the aggregator-starvation bug class (#259):
 * a starved {@link DistanceAndTimeAggregator} must freeze the {@link ProfileModel}'s domain on
 * append, and a subsequent correct aggregator feed must fully recover it. Wires the real
 * production pipeline exactly as {@link slash.navigation.converter.gui.profileview.ProfileView#initialize}
 * does, since the seam is only testable headless through these Swing model classes.
 *
 * @author Christian Pesch
 */
public class ProfileModelTest {
    private PositionsModelImpl delegate;
    private CharacteristicsModel characteristicsModel;
    private DistanceAndTimeAggregator aggregator;
    private OverlayPositionsModel overlay;
    private PatchedXYSeries series;
    private ProfileModel profileModel;

    @Before
    public void setUp() {
        delegate = new PositionsModelImpl(new PositionsModelCallback() {
            public String getStringAt(NavigationPosition position, int columnIndex) {
                return "";
            }

            public void setValueAt(NavigationPosition position, int columnIndex, Object value) {
            }
        });
        characteristicsModel = new CharacteristicsModel();
        aggregator = new DistanceAndTimeAggregator();
        overlay = new OverlayPositionsModel(delegate, characteristicsModel, aggregator);
        series = new PatchedXYSeries("Profile");
        profileModel = new ProfileModel(overlay, series, UnitSystem.Metric, XAxisMode.Distance, YAxisMode.Elevation);
    }

    private GpxRoute createRoute(RouteCharacteristics characteristics, int positionCount) {
        List<GpxPosition> positions = new ArrayList<>();
        for (int i = 0; i < positionCount; i++) {
            positions.add(new GpxPosition(10.0 + i, 50.0 + i, 100.0 + i, null, null, "position" + i));
        }
        return new GpxRoute(new Gpx11Format(), characteristics, "route", null, positions);
    }

    @SuppressWarnings("unchecked")
    private void setRoute(GpxRoute route) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            delegate.setRoute(route);
            characteristicsModel.setRoute((BaseRoute) route);
        });
    }

    private void addPositions(int rowIndex, List<BaseNavigationPosition> positions) throws Exception {
        SwingUtilities.invokeAndWait(() -> delegate.add(rowIndex, positions));
    }

    private void feedAggregator(Map<Integer, DistanceAndTime> indexToDistanceAndTime) throws Exception {
        aggregator.updateDistancesAndTimes(indexToDistanceAndTime);
        // OverlayPositionsModel forwards aggregator changes via invokeLater; drain before asserting
        SwingUtilities.invokeAndWait(() -> {
        });
    }

    @Test
    public void starvedAggregatorFreezesDomainOnAppend() throws Exception {
        setRoute(createRoute(Track, 3));
        feedAggregator(Map.of(
                1, new DistanceAndTime(1000.0, 1000L),
                2, new DistanceAndTime(1000.0, 1000L)));

        double maxXBeforeAppend = series.getMaxX();
        assertEquals(2.0, maxXBeforeAppend, 0.001);

        List<BaseNavigationPosition> appended = List.of(
                new GpxPosition(13.0, 53.0, 103.0, null, null, "position3"),
                new GpxPosition(14.0, 54.0, 104.0, null, null, "position4"));
        addPositions(3, appended);

        // appended points carry the last known distance forward - the frozen-domain symptom
        assertEquals(5, series.getItemCount());
        assertEquals(maxXBeforeAppend, series.getMaxX(), 0.001);
    }

    @Test
    public void aggregatorFeedRecoversDomain() throws Exception {
        setRoute(createRoute(Track, 3));
        feedAggregator(Map.of(
                1, new DistanceAndTime(1000.0, 1000L),
                2, new DistanceAndTime(1000.0, 1000L)));

        List<BaseNavigationPosition> appended = List.of(
                new GpxPosition(13.0, 53.0, 103.0, null, null, "position3"),
                new GpxPosition(14.0, 54.0, 104.0, null, null, "position4"));
        addPositions(3, appended);

        feedAggregator(Map.of(
                3, new DistanceAndTime(1000.0, 1000L),
                4, new DistanceAndTime(1000.0, 1000L)));

        // the DISTANCE_COLUMN continuous-range event alone must fully repair the chart
        assertEquals(4.0, series.getMaxX(), 0.001);
        assertEquals(delegate.getRowCount(), series.getItemCount());
    }

    @Test
    public void waypointsCharacteristicsYieldsEmptySeries() throws Exception {
        setRoute(createRoute(Waypoints, 3));

        // by design: OverlayPositionsModel.getDistancesFromStart returns null for Waypoints
        assertEquals(0, series.getItemCount());
    }
}
