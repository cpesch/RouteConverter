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
package slash.navigation.datasources.helpers;

import org.junit.Test;
import slash.navigation.datasources.binding.CatalogType;
import slash.navigation.datasources.binding.DatasourceType;
import slash.navigation.datasources.binding.SourceType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.navigation.datasources.helpers.DataSourcesUtil.toXml;
import static slash.navigation.datasources.helpers.DataSourcesUtil.unmarshal;

/**
 * Tests JAXB round-tripping for datasource catalog bindings.
 *
 * @author Christian Pesch
 */
public class DataSourcesUtilTest {
    @Test
    public void testRoundTripSourceConfiguration() throws Exception {
        CatalogType catalogType;
        try (InputStream in = getClass().getResourceAsStream("/slash/navigation/datasources/testdatasources.xml")) {
            catalogType = unmarshal(in);
        }

        String xml = toXml(catalogType);
        assertTrue(xml.contains("http://api.routeconverter.com/v1/schemas/datasource-catalog"));

        CatalogType roundTrip;
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            roundTrip = unmarshal(in);
        }

        checkSource(getDataSource(roundTrip, "id1"), "http://local1/index.html", Integer.valueOf(2), asList("*.data", "*.zip"), asList("*/skip.data"));
        checkSource(getDataSource(roundTrip, "id2"), null, null, asList(), asList());
        assertNull(getDataSource(roundTrip, "id3").getSource());
    }

    private DatasourceType getDataSource(CatalogType catalogType, String id) {
        for (DatasourceType datasourceType : catalogType.getDatasource()) {
            if (id.equals(datasourceType.getId()))
                return datasourceType;
        }
        throw new AssertionError("Datasource not found: " + id);
    }

    private void checkSource(DatasourceType datasourceType, String url, Integer level,
                             java.util.List<String> includes, java.util.List<String> excludes) {
        assertNotNull(datasourceType);

        SourceType sourceType = datasourceType.getSource();
        assertNotNull(sourceType);
        assertEquals(url, sourceType.getUrl());
        assertEquals(level, sourceType.getLevel());
        assertEquals(includes, sourceType.getInclude());
        assertEquals(excludes, sourceType.getExclude());
    }
}

