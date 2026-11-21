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
package slash.navigation.maps.mapsforge;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.datasources.helpers.DataSourceService;
import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.tileserver.TileServerMapManager;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.navigation.maps.mapsforge.MapsforgeMapManager.extractMapProvider;

public class MapsforgeMapManagerDefaultThemeTest {
    private MapsforgeMapManager mapManager;

    @Before
    public void setUp() {
        List<DataSource> dataSources = List.of(
                createDataSource("openandromaps", "maps/openandromaps5", "openandromaps/Elevate.xml"),
                createDataSource("openandromaps-mbtiles", "maps/openandromaps-mbtiles", null),
                createDataSource("elevate-themes", "themes/openandromaps", "openandromaps/Elevate.xml"),
                createDataSource("mapsforge-maps", "maps/mapsforge5", "OpenStreetMap Default"),
                createDataSource("freizeitkarte", "maps/freizeitkarte", "")
        );
        DataSourceService dataSourceService = mock(DataSourceService.class);
        when(dataSourceService.getDataSources()).thenReturn(dataSources);
        DataSourceManager dataSourceManager = mock(DataSourceManager.class);
        when(dataSourceManager.getDataSourceService()).thenReturn(dataSourceService);

        TileServerMapManager tileServerMapManager = mock(TileServerMapManager.class);
        when(tileServerMapManager.getAvailableMapsModel()).thenReturn(new ItemTableModel<>(1));

        mapManager = new MapsforgeMapManager(dataSourceManager, tileServerMapManager);
    }

    @After
    public void tearDown() {
        if (mapManager != null)
            mapManager.dispose();
    }

    private static DataSource createDataSource(String id, String directory, String defaultTheme) {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getId()).thenReturn(id);
        when(dataSource.getDirectory()).thenReturn(directory);
        when(dataSource.getDefaultTheme()).thenReturn(defaultTheme);
        return dataSource;
    }

    @Test
    public void testExtractMapProvider() {
        assertEquals("openandromaps5", extractMapProvider("maps/openandromaps5"));
        assertEquals("openandromaps5", extractMapProvider("maps/openandromaps5/europe"));
        assertEquals("freizeitkarte", extractMapProvider("maps\\freizeitkarte"));
        assertNull(extractMapProvider("themes/openandromaps"));
        assertNull(extractMapProvider("srtm3"));
        assertNull(extractMapProvider(null));
    }

    @Test
    public void testDefaultThemeOfMapDataSource() {
        assertEquals("openandromaps/Elevate.xml", mapManager.getDefaultTheme("openandromaps5"));
        assertEquals("OpenStreetMap Default", mapManager.getDefaultTheme("mapsforge5"));
    }

    @Test
    public void testNoDefaultTheme() {
        assertNull(mapManager.getDefaultTheme("openandromaps-mbtiles"));
        assertNull(mapManager.getDefaultTheme("freizeitkarte"));
        assertNull(mapManager.getDefaultTheme("openandromaps"));
        assertNull(mapManager.getDefaultTheme("unknown"));
        assertNull(mapManager.getDefaultTheme(null));
    }
}
