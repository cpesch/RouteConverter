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

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;

import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.impl.MapsforgeFileMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.mapview.mapsforge.MapSelector.DescriptionKeySelectionManager;
import static slash.navigation.mapview.mapsforge.MapSelector.clampZoom;
import static slash.navigation.mapview.mapsforge.MapSelector.isSeparator;
import static slash.navigation.mapview.mapsforge.renderer.LocalMapListCellRenderer.DOWNLOAD_MAP;
import static slash.navigation.mapview.mapsforge.renderer.LocalMapListCellRenderer.SEPARATOR_TO_DOWNLOAD_MAP;
import static slash.navigation.mapview.mapsforge.renderer.LocalThemeListCellRenderer.DOWNLOAD_THEME;
import static slash.navigation.mapview.mapsforge.renderer.LocalThemeListCellRenderer.SEPARATOR_TO_DOWNLOAD_THEME;

public class MapSelectorTest {
    @Test
    public void clampZoomWithinRange() {
        assertEquals(5, clampZoom(5, (byte) 0, (byte) 20));
    }

    @Test
    public void clampZoomBelowMin() {
        assertEquals(0, clampZoom(-1, (byte) 0, (byte) 20));
    }

    @Test
    public void clampZoomAboveMax() {
        assertEquals(20, clampZoom(25, (byte) 0, (byte) 20));
    }

    @Test
    public void clampZoomWithSingleValueRange() {
        assertEquals(3, clampZoom(3, (byte) 3, (byte) 3));
    }

    @Test
    public void isSeparatorIsTrueOnlyForTheSeparators() {
        assertTrue(isSeparator(SEPARATOR_TO_DOWNLOAD_MAP));
        assertTrue(isSeparator(SEPARATOR_TO_DOWNLOAD_THEME));
        assertFalse(isSeparator(DOWNLOAD_MAP));
        assertFalse(isSeparator(DOWNLOAD_THEME));
        assertFalse(isSeparator(new MapsforgeFileMap("Alps", "http://alps.url", null, null, null)));
    }

    private static ComboBoxModel<LocalMap> createModel() {
        LocalMap alps = new MapsforgeFileMap("Alps", "http://alps.url", null, null, null);
        LocalMap austria = new MapsforgeFileMap("Austria", "http://austria.url", null, null, null);
        LocalMap bavaria = new MapsforgeFileMap("Bavaria", "http://bavaria.url", null, null, null);
        DefaultComboBoxModel<LocalMap> model = new DefaultComboBoxModel<>(new LocalMap[]{alps, austria, bavaria, SEPARATOR_TO_DOWNLOAD_MAP, DOWNLOAD_MAP});
        model.setSelectedItem(null);
        return model;
    }

    @Test
    public void selectionForKeyWithNothingSelected() {
        ComboBoxModel<LocalMap> model = createModel();
        DescriptionKeySelectionManager manager = new DescriptionKeySelectionManager(SEPARATOR_TO_DOWNLOAD_MAP, DOWNLOAD_MAP);
        assertEquals(0, manager.selectionForKey('a', model));
    }

    @Test
    public void selectionForKeyWithAlpsSelected() {
        ComboBoxModel<LocalMap> model = createModel();
        model.setSelectedItem(model.getElementAt(0));
        DescriptionKeySelectionManager manager = new DescriptionKeySelectionManager(SEPARATOR_TO_DOWNLOAD_MAP, DOWNLOAD_MAP);
        assertEquals(1, manager.selectionForKey('a', model));
    }

    @Test
    public void selectionForKeyWithAustriaSelectedWrapsAround() {
        ComboBoxModel<LocalMap> model = createModel();
        model.setSelectedItem(model.getElementAt(1));
        DescriptionKeySelectionManager manager = new DescriptionKeySelectionManager(SEPARATOR_TO_DOWNLOAD_MAP, DOWNLOAD_MAP);
        assertEquals(0, manager.selectionForKey('a', model));
    }

    @Test
    public void selectionForKeyIsCaseInsensitive() {
        ComboBoxModel<LocalMap> model = createModel();
        DescriptionKeySelectionManager manager = new DescriptionKeySelectionManager(SEPARATOR_TO_DOWNLOAD_MAP, DOWNLOAD_MAP);
        assertEquals(0, manager.selectionForKey('A', model));
        model.setSelectedItem(model.getElementAt(0));
        assertEquals(1, manager.selectionForKey('A', model));
    }

    @Test
    public void selectionForKeyMatchesBavaria() {
        ComboBoxModel<LocalMap> model = createModel();
        DescriptionKeySelectionManager manager = new DescriptionKeySelectionManager(SEPARATOR_TO_DOWNLOAD_MAP, DOWNLOAD_MAP);
        assertEquals(2, manager.selectionForKey('b', model));
    }

    @Test
    public void selectionForKeyWithoutMatchReturnsMinusOne() {
        ComboBoxModel<LocalMap> model = createModel();
        DescriptionKeySelectionManager manager = new DescriptionKeySelectionManager(SEPARATOR_TO_DOWNLOAD_MAP, DOWNLOAD_MAP);
        // 'm' no longer matches the "MapsforgeFileMap" class-name prefix of the separator/download entries
        assertEquals(-1, manager.selectionForKey('m', model));
    }

    @Test
    public void selectionForKeyNeverSelectsSeparatorOrDownloadEntry() {
        ComboBoxModel<LocalMap> model = createModel();
        DescriptionKeySelectionManager manager = new DescriptionKeySelectionManager(SEPARATOR_TO_DOWNLOAD_MAP, DOWNLOAD_MAP);
        for (char key = 'a'; key <= 'z'; key++) {
            int index = manager.selectionForKey(key, model);
            assertFalse(index == 3 || index == 4);
        }
    }
}
