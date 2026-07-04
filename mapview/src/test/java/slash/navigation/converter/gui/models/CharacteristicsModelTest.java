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
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.values;

/**
 * Tests for {@link CharacteristicsModel}, a headless {@code ComboBoxModel} over a route's characteristics.
 *
 * @author Christian Pesch
 */
public class CharacteristicsModelTest {

    @SuppressWarnings("unchecked")
    private static BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route() {
        return mock(BaseRoute.class);
    }

    @Test
    public void sizeAndElementsMirrorTheEnum() {
        CharacteristicsModel sut = new CharacteristicsModel();

        assertEquals(values().length, sut.getSize());
        assertEquals(values()[0], sut.getElementAt(0));
    }

    @Test
    public void selectionIsNullWithoutARoute() {
        CharacteristicsModel sut = new CharacteristicsModel();

        assertNull(sut.getSelectedItem());
        assertNull(sut.getSelectedCharacteristics());
    }

    @Test
    public void selectionReflectsTheRoute() {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = route();
        when(route.getCharacteristics()).thenReturn(Track);
        CharacteristicsModel sut = new CharacteristicsModel();

        sut.setRoute(route);

        assertEquals(Track, sut.getSelectedItem());
        assertEquals(Track, sut.getSelectedCharacteristics());
    }

    @Test
    public void setSelectedItemUpdatesTheRouteWhenChanged() {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = route();
        when(route.getCharacteristics()).thenReturn(Route);
        CharacteristicsModel sut = new CharacteristicsModel();
        sut.setRoute(route);

        sut.setSelectedItem(Track);

        verify(route).setCharacteristics(Track);
    }

    @Test
    public void setSelectedItemIsANoOpWhenUnchanged() {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = route();
        when(route.getCharacteristics()).thenReturn(Track);
        CharacteristicsModel sut = new CharacteristicsModel();
        sut.setRoute(route);

        sut.setSelectedItem(Track);

        verify(route, never()).setCharacteristics(any());
    }

    @Test
    public void setSelectedItemUpdatesTheRouteFromNullSelection() {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = route();
        when(route.getCharacteristics()).thenReturn(null);
        CharacteristicsModel sut = new CharacteristicsModel();
        sut.setRoute(route);

        sut.setSelectedItem(Track);

        verify(route).setCharacteristics(Track);
    }
}
