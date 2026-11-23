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
package slash.navigation.maps.mapsforge.helpers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.mapsforge.impl.TileDownloadMap;
import slash.navigation.maps.mapsforge.models.TileMapTableModel;
import slash.navigation.maps.tileserver.TileServer;

import java.util.ArrayList;
import java.util.List;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TileServerToTileMapMediatorSortTest {
    private ItemTableModel<TileServer> source;
    private TileMapTableModel destination;

    private static TileServer ts(String id, String name) {
        return new TileServer(id, name, "pattern", of("host"), true, 0, 18, "copyright", "text");
    }

    private static List<String> descriptions(ItemTableModel<TileDownloadMap> model) {
        List<String> result = new ArrayList<>();
        for (TileDownloadMap map : model.getItems())
            result.add(map.description());
        return result;
    }

    private static List<String> urls(ItemTableModel<TileDownloadMap> model) {
        List<String> result = new ArrayList<>();
        for (TileDownloadMap map : model.getItems())
            result.add(map.getUrl());
        return result;
    }

    private void addZetaAlphaMid() {
        source.addOrUpdateItem(ts("z-id", "Zeta"));
        source.addOrUpdateItem(ts("a-id", "alpha"));
        source.addOrUpdateItem(ts("m-id", "Mid"));
    }

    @BeforeEach
    public void setUp() {
        source = new ItemTableModel<>(1);
        destination = new TileMapTableModel();
    }

    @Test
    public void testConvertUsesNameAsDescriptionAndIdAsUrl() {
        TileDownloadMap map = TileServerToTileMapMediator.convert(ts("z-id", "Zeta"));
        assertEquals("Zeta", map.description());
        assertEquals("z-id", map.getUrl());
    }

    @Test
    public void testInsertedMapsAreSortedByNameCaseInsensitive() {
        new TileServerToTileMapMediator(source, destination);
        addZetaAlphaMid();

        assertEquals(of("alpha", "Mid", "Zeta"), descriptions(destination));
        assertEquals(of("a-id", "m-id", "z-id"), urls(destination));
    }

    @Test
    public void testPreExistingDestinationItemDoesNotMisalignInsert() {
        destination.addOrUpdateItem(new TileDownloadMap("Berta", "berta-id", true, null, "c"));
        new TileServerToTileMapMediator(source, destination);
        source.addOrUpdateItem(ts("z-id", "Zeta"));
        source.addOrUpdateItem(ts("a-id", "alpha"));

        assertEquals(of("alpha", "Berta", "Zeta"), descriptions(destination));
        assertEquals(of("a-id", "berta-id", "z-id"), urls(destination));
    }

    @Test
    public void testUpdateReplacesTheMatchingRowOnly() {
        new TileServerToTileMapMediator(source, destination);
        addZetaAlphaMid();
        TileDownloadMap alpha = destination.getItem(0);
        TileDownloadMap mid = destination.getItem(1);
        TileDownloadMap zeta = destination.getItem(2);

        source.update(0, ts("z-id", "Zeta"));

        assertEquals(3, destination.getRowCount());
        assertEquals(of("alpha", "Mid", "Zeta"), descriptions(destination));
        assertSame(alpha, destination.getItem(0));
        assertSame(mid, destination.getItem(1));
        assertNotSame(zeta, destination.getItem(2));
        assertEquals("z-id", destination.getItem(2).getUrl());

        // a full-table clear event of the source reaches rows beyond its size and must not throw
        source.clear();
        assertEquals(3, destination.getRowCount());
    }

    @Test
    public void testRemoveDeletesTheMatchingRowNotTheRowAtTheSameIndex() {
        new TileServerToTileMapMediator(source, destination);
        addZetaAlphaMid();

        source.remove(0, 0);

        assertEquals(of("alpha", "Mid"), descriptions(destination));
    }

    @Test
    public void testPreFilledSourceIsConvertedOnCreation() {
        source.addOrUpdateItem(ts("z-id", "Zeta"));
        source.addOrUpdateItem(ts("a-id", "alpha"));

        new TileServerToTileMapMediator(source, destination);

        assertEquals(of("alpha", "Zeta"), descriptions(destination));
        assertEquals(of("a-id", "z-id"), urls(destination));
    }
}
