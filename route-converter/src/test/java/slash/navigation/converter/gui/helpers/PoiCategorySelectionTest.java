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
package slash.navigation.converter.gui.helpers;

import org.junit.Test;
import slash.navigation.pois.mapsforge.PoiCategory;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;

public class PoiCategorySelectionTest {
    private static final PoiCategory BANKS = leaf("Banks");
    private static final PoiCategory ATMS = leaf("ATMs");
    private static final PoiCategory FINANCIAL = new PoiCategory("Financial", "Financial", List.of(ATMS, BANKS));
    private static final PoiCategory TOILETS = leaf("Toilets");
    private static final PoiCategory AMENITIES = new PoiCategory("Amenities", "Amenities", List.of(FINANCIAL, TOILETS));

    private static PoiCategory leaf(String key) {
        return new PoiCategory(key, key, emptyList());
    }

    @Test
    public void selectingCategoryKeepsOnlyItsKeyAndSelectsItsSubtree() {
        PoiCategorySelection selection = new PoiCategorySelection(Set.of("Banks", "Toilets"));

        selection.toggle(List.of(AMENITIES));

        assertEquals(Set.of("Amenities"), selection.getKeys());
        assertTrue(selection.isSelected(List.of(AMENITIES, FINANCIAL, BANKS)));
        assertFalse(selection.isPartiallySelected(List.of(AMENITIES)));
    }

    @Test
    public void deselectingBelowSelectedAncestorKeepsTheRestOfItsSubtree() {
        PoiCategorySelection selection = new PoiCategorySelection(Set.of("Amenities"));

        selection.toggle(List.of(AMENITIES, FINANCIAL, BANKS));

        assertEquals(Set.of("ATMs", "Toilets"), selection.getKeys());
        assertFalse(selection.isSelected(List.of(AMENITIES, FINANCIAL, BANKS)));
        assertTrue(selection.isSelected(List.of(AMENITIES, FINANCIAL, ATMS)));
        assertTrue(selection.isPartiallySelected(List.of(AMENITIES)));
        assertTrue(selection.isPartiallySelected(List.of(AMENITIES, FINANCIAL)));
    }

    @Test
    public void deselectingSelectedCategoryRemovesItsSubtree() {
        PoiCategorySelection selection = new PoiCategorySelection(Set.of("Financial", "Toilets"));

        selection.toggle(List.of(AMENITIES, FINANCIAL));

        assertEquals(Set.of("Toilets"), selection.getKeys());
    }

    @Test
    public void deselectingBelowSeveralSelectedAncestorsRemovesThemAll() {
        PoiCategorySelection selection = new PoiCategorySelection(Set.of("Amenities", "Financial", "Banks"));

        selection.toggle(List.of(AMENITIES, FINANCIAL, BANKS));

        assertEquals(Set.of("ATMs", "Toilets"), selection.getKeys());
    }

    @Test
    public void reportsNothingSelectedForEmptySelection() {
        PoiCategorySelection selection = new PoiCategorySelection(Set.of());

        assertFalse(selection.isSelected(List.of(AMENITIES)));
        assertFalse(selection.isPartiallySelected(List.of(AMENITIES)));
        assertFalse(selection.isPartiallySelected(List.of(AMENITIES, TOILETS)));
    }
}
