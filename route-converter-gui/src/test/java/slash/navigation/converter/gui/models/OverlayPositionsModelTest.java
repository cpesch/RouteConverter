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

import org.junit.Test;
import slash.navigation.base.BaseRoute;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.converter.gui.models.PositionColumns.DESCRIPTION_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.TIME_COLUMN_INDEX;

public class OverlayPositionsModelTest {
    private final PositionsModel delegate = mock(PositionsModel.class);
    private final BaseRoute route = mock(BaseRoute.class);
    private final OverlayPositionsModel sut = new OverlayPositionsModel(delegate);

    @Test
    public void isCellEditableIsFalseForTimeColumnOfARoute() {
        when(delegate.getRoute()).thenReturn(route);
        when(route.getCharacteristics()).thenReturn(Route);

        assertFalse(sut.isCellEditable(0, TIME_COLUMN_INDEX));
    }

    @Test
    public void isCellEditableIsDelegatedForTimeColumnOfATrack() {
        when(delegate.getRoute()).thenReturn(route);
        when(route.getCharacteristics()).thenReturn(Track);
        when(delegate.isCellEditable(0, TIME_COLUMN_INDEX)).thenReturn(true);

        assertTrue(sut.isCellEditable(0, TIME_COLUMN_INDEX));
    }

    @Test
    public void isCellEditableIsDelegatedForTimeColumnOfWaypoints() {
        when(delegate.getRoute()).thenReturn(route);
        when(route.getCharacteristics()).thenReturn(Waypoints);
        when(delegate.isCellEditable(0, TIME_COLUMN_INDEX)).thenReturn(true);

        assertTrue(sut.isCellEditable(0, TIME_COLUMN_INDEX));
    }

    @Test
    public void isCellEditableIsDelegatedForOtherColumnsOfARoute() {
        when(delegate.getRoute()).thenReturn(route);
        when(route.getCharacteristics()).thenReturn(Route);
        when(delegate.isCellEditable(0, DESCRIPTION_COLUMN_INDEX)).thenReturn(true);

        assertTrue(sut.isCellEditable(0, DESCRIPTION_COLUMN_INDEX));
    }
}

