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
package slash.navigation.download.tools.helpers;

import org.junit.Test;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.binding.*;
import slash.navigation.datasources.impl.DataSourceImpl;

import java.nio.file.Paths;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;

public class WgetCommandBuilderTest {

    private final WgetCommandBuilder builder = new WgetCommandBuilder();

    @Test
    public void buildsWithIncludesAndLevel() {
        DataSource ds = dataSource("brouter-profiles", "http://brouter.de/brouter/profiles2/",
                source(null, 1, List.of("*.brf", "*.dat"), emptyList()), emptyList());
        java.nio.file.Path mirrorRoot = Paths.get("/m");

        List<String> cmd = builder.buildCommand(ds, ds.getSource(), mirrorRoot);

        assertEquals(List.of("wget", "-m", "-np", "-e", "robots=off", "--wait", "1", "-P", mirrorRoot.toString(),
                "-l", "1", "--accept", "*.brf", "--accept", "*.dat",
                "http://brouter.de/brouter/profiles2/"), cmd);
    }

    @Test
    public void prefersSourceUrlOverBaseUrl() {
        DataSource ds = dataSource("andromaps-themes", "https://example.com/themes/",
                source("https://example.com/index/", null, List.of("*.zip"), emptyList()), emptyList());

        List<String> cmd = builder.buildCommand(ds, ds.getSource(), Paths.get("/m"));

        assertTrue(cmd.get(cmd.size() - 1).equals("https://example.com/index/"));
    }

    @Test
    public void derivesAcceptsFromUriExtensionsWhenIncludesEmpty() {
        DataSource ds = dataSource("ds", "https://example.com/",
                source(null, null, emptyList(), emptyList()),
                List.of("eu/Germany.zip", "eu/France.zip", "README.txt"));

        java.util.Set<String> accepts = builder.resolveAccepts(ds, ds.getSource());

        assertEquals(java.util.Set.of("*.zip", "*.txt"), accepts);
    }

    @Test
    public void formatCommandQuotesGlobArgs() {
        List<String> cmd = List.of("wget", "--accept", "*.zip", "https://example.com/");
        assertEquals("wget --accept '*.zip' https://example.com/", builder.formatCommand(cmd));
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildCommandThrowsWhenSourceIsNull() {
        DataSource ds = dataSource("id1", "http://example.com/", null, emptyList());
        builder.buildCommand(ds, null, Paths.get("/m"));
    }

    @Test
    public void buildCommandOmitsLevelWhenNotSet() {
        DataSource ds = dataSource("id1", "http://example.com/",
                source(null, null, List.of("*.zip"), emptyList()), emptyList());
        List<String> cmd = builder.buildCommand(ds, ds.getSource(), Paths.get("/m"));
        assertFalse(cmd.contains("-l"));
    }

    @Test
    public void buildCommandFallsBackToBaseUrlWhenSourceUrlIsBlank() {
        DataSource ds = dataSource("id1", "http://base.example.com/",
                source("   ", null, List.of("*.zip"), emptyList()), emptyList());
        List<String> cmd = builder.buildCommand(ds, ds.getSource(), Paths.get("/m"));
        assertEquals("http://base.example.com/", cmd.get(cmd.size() - 1));
    }

    @Test
    public void derivesAcceptsFromMapExtensions() {
        ObjectFactory factory = new ObjectFactory();
        DatasourceType type = factory.createDatasourceType();
        type.setId("ds");
        type.setBaseUrl("https://example.com/");
        MapType mapType = factory.createMapType();
        mapType.setUri("world/europe.map");
        type.getMap().add(mapType);
        DataSource ds = new DataSourceImpl(type);
        SourceType src = source(null, null, emptyList(), emptyList());
        type.setSource(src);

        java.util.Set<String> accepts = builder.resolveAccepts(ds, ds.getSource());
        assertTrue(accepts.contains("*.map"));
    }

    private DataSource dataSource(String id, String baseUrl, SourceType source, List<String> fileUris) {
        ObjectFactory factory = new ObjectFactory();
        DatasourceType type = factory.createDatasourceType();
        type.setId(id);
        type.setBaseUrl(baseUrl);
        if (source != null)
            type.setSource(source);
        for (String uri : fileUris) {
            FileType fileType = factory.createFileType();
            fileType.setUri(uri);
            type.getFile().add(fileType);
        }
        return new DataSourceImpl(type);
    }

    private SourceType source(String url, Integer level, List<String> includes, List<String> excludes) {
        SourceType source = new ObjectFactory().createSourceType();
        source.setUrl(url);
        source.setLevel(level);
        source.getInclude().addAll(includes);
        source.getExclude().addAll(excludes);
        return source;
    }
}
