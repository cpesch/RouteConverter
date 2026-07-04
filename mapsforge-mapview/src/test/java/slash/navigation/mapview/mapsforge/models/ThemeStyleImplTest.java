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
package slash.navigation.mapview.mapsforge.models;

import org.junit.Test;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;
import slash.navigation.maps.mapsforge.ThemeStyleCategory;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ThemeStyleImpl}.
 *
 * @author Christian Pesch
 */
public class ThemeStyleImplTest {
    private static final String DEFAULT_LANGUAGE = Locale.getDefault().getLanguage();
    private static final String MENU_LANGUAGE = "zz";  // deliberately not the system default

    private final XmlRenderThemeStyleMenu menu = mock(XmlRenderThemeStyleMenu.class);
    private final XmlRenderThemeStyleLayer layer = mock(XmlRenderThemeStyleLayer.class);
    private final ThemeStyleImpl sut = new ThemeStyleImpl(menu, layer);

    @Test
    public void descriptionPrefersTheDefaultLanguageTitle() {
        when(layer.getTitle(DEFAULT_LANGUAGE)).thenReturn("Localized");

        assertEquals("Localized", sut.description());
    }

    @Test
    public void descriptionFallsBackToTheMenuDefaultLanguageTitle() {
        when(menu.getDefaultLanguage()).thenReturn(MENU_LANGUAGE);
        when(layer.getTitle(DEFAULT_LANGUAGE)).thenReturn(null);
        when(layer.getTitle(MENU_LANGUAGE)).thenReturn("Fallback");

        assertEquals("Fallback", sut.description());
    }

    @Test
    public void descriptionFallsBackToTheLayerIdWhenNoTitle() {
        when(menu.getDefaultLanguage()).thenReturn(MENU_LANGUAGE);
        when(layer.getTitle(DEFAULT_LANGUAGE)).thenReturn(null);
        when(layer.getTitle(MENU_LANGUAGE)).thenReturn(null);
        when(layer.getId()).thenReturn("theme-id");

        assertEquals("theme-id", sut.description());
    }

    @Test
    public void urlIsTheLayerId() {
        when(layer.getId()).thenReturn("theme-id");

        assertEquals("theme-id", sut.getUrl());
    }

    @Test
    public void categoriesAreMappedFromTheLayer() {
        when(layer.getCategories()).thenReturn(Set.of("roads", "buildings"));

        Set<String> urls = sut.getCategories().stream()
                .map(ThemeStyleCategory::getUrl)
                .collect(Collectors.toSet());

        assertEquals(Set.of("roads", "buildings"), urls);
    }

    @Test
    public void noCategoriesYieldsEmptySet() {
        when(layer.getCategories()).thenReturn(Set.of());

        assertTrue(sut.getCategories().isEmpty());
    }

    @Test
    public void equalityIsByUrl() {
        when(layer.getId()).thenReturn("same");

        XmlRenderThemeStyleLayer otherLayer = mock(XmlRenderThemeStyleLayer.class);
        when(otherLayer.getId()).thenReturn("same");
        ThemeStyleImpl sameUrl = new ThemeStyleImpl(mock(XmlRenderThemeStyleMenu.class), otherLayer);

        assertEquals(sut, sameUrl);
        assertEquals(sut.hashCode(), sameUrl.hashCode());
    }

    @Test
    public void differentUrlIsNotEqual() {
        when(layer.getId()).thenReturn("one");

        XmlRenderThemeStyleLayer otherLayer = mock(XmlRenderThemeStyleLayer.class);
        when(otherLayer.getId()).thenReturn("two");
        ThemeStyleImpl differentUrl = new ThemeStyleImpl(mock(XmlRenderThemeStyleMenu.class), otherLayer);

        assertNotEquals(sut, differentUrl);
        assertNotEquals(sut, null);
        assertNotEquals(sut, "not a theme style");
    }
}
