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
package slash.navigation.maps.tileserver.helpers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import slash.navigation.maps.tileserver.bindingmap.MapServerType;
import slash.navigation.maps.tileserver.bindingoverlay.OverlayServerType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TileServerServiceTest {
    @TempDir
    public File tmp;

    private static String mapServerEntry(String id, String host) {
        return "    <mapServer id=\"" + id + "\" name=\"" + id + "\" minZoom=\"1\" maxZoom=\"18\" active=\"true\">\n" +
                "        <host>" + host + "</host>\n" +
                "        <urlPattern>/tiles/{z}/{x}/{y}.png</urlPattern>\n" +
                "        <copyright>copyright</copyright>\n" +
                "    </mapServer>\n";
    }

    private static String mapCatalogXml(String... entries) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<catalog xmlns=\"http://api.routeconverter.com/v1/schemas/mapserver-catalog\">\n");
        for (String entry : entries)
            builder.append(entry);
        builder.append("</catalog>\n");
        return builder.toString();
    }

    private static String overlayServerEntry(String id, String host) {
        return "    <overlayServer id=\"" + id + "\" name=\"" + id + "\" minZoom=\"1\" maxZoom=\"18\" active=\"true\">\n" +
                "        <host>" + host + "</host>\n" +
                "        <urlPattern>/overlay/{z}/{x}/{y}.png</urlPattern>\n" +
                "        <copyright>copyright</copyright>\n" +
                "    </overlayServer>\n";
    }

    private static String overlayCatalogXml(String... entries) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<catalog xmlns=\"http://api.routeconverter.com/v1/schemas/overlayserver-catalog\">\n");
        for (String entry : entries)
            builder.append(entry);
        builder.append("</catalog>\n");
        return builder.toString();
    }

    private void writeFile(String name, String content) throws IOException {
        File file = newFile(tmp, name);
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    }

    private static List<String> ids(List<MapServerType> maps) {
        return maps.stream().map(MapServerType::getId).toList();
    }

    @Test
    public void testUserFileAddsNewIdsButCatalogIdWins() throws IOException {
        writeFile("mapservers.xml", mapCatalogXml(
                mapServerEntry("A", "catalog.example"),
                mapServerEntry("B", "catalog.example")));
        writeFile("user.xml", mapCatalogXml(
                mapServerEntry("A", "user.example"),
                mapServerEntry("C", "user.example")));

        TileServerService service = new TileServerService(tmp);
        service.initialize();

        assertEquals(List.of("A", "B", "C"), ids(service.getMaps()));
        MapServerType a = service.getMaps().stream().filter(m -> m.getId().equals("A")).findFirst().orElseThrow();
        assertEquals("catalog.example", a.getHost().get(0));
    }

    @Test
    public void testFirstUserFileInNaturalOrderWinsWithoutCatalog() throws IOException {
        writeFile("a.xml", mapCatalogXml(mapServerEntry("X", "a.example")));
        writeFile("b.xml", mapCatalogXml(mapServerEntry("X", "b.example")));

        TileServerService service = new TileServerService(tmp);
        service.initialize();

        assertEquals(List.of("X"), ids(service.getMaps()));
        assertEquals("a.example", service.getMaps().get(0).getHost().get(0));
    }

    @Test
    public void testIdsDifferingOnlyInCaseAreKeptSeparately() throws IOException {
        writeFile("mapservers.xml", mapCatalogXml(mapServerEntry("4UMaps", "catalog.example")));
        writeFile("user.xml", mapCatalogXml(mapServerEntry("4umaps", "user.example")));

        TileServerService service = new TileServerService(tmp);
        service.initialize();

        assertEquals(List.of("4UMaps", "4umaps"), ids(service.getMaps()));
    }

    @Test
    public void testOverlayCatalogWinsOverUserOverlayFile() throws IOException {
        writeFile("overlayservers.xml", overlayCatalogXml(overlayServerEntry("O", "catalog.example")));
        writeFile("user-overlays.xml", overlayCatalogXml(overlayServerEntry("O", "user.example")));

        TileServerService service = new TileServerService(tmp);
        service.initialize();

        List<OverlayServerType> overlays = service.getOverlays();
        assertEquals(1, overlays.size());
        assertEquals("O", overlays.get(0).getId());
        assertEquals("catalog.example", overlays.get(0).getHost().get(0));
    }

    @Test
    public void testMapCatalogWinsOverUserFileSortingBeforeIt() throws IOException {
        // "Kopie von mapservers.xml" sorts before "mapservers.xml" in natural String order,
        // so only the catalog-first ordering keeps the catalog definition
        writeFile("mapservers.xml", mapCatalogXml(mapServerEntry("A", "catalog.example")));
        writeFile("Kopie von mapservers.xml", mapCatalogXml(
                mapServerEntry("A", "user.example"),
                mapServerEntry("C", "user.example")));

        TileServerService service = new TileServerService(tmp);
        service.initialize();

        assertEquals(List.of("A", "C"), ids(service.getMaps()));
        assertEquals("catalog.example", service.getMaps().get(0).getHost().get(0));
    }

    @Test
    public void testOverlayCatalogWinsOverUserFileSortingBeforeIt() throws IOException {
        // "aaa.xml" sorts before "overlayservers.xml", so only the catalog-first ordering keeps the catalog definition
        writeFile("overlayservers.xml", overlayCatalogXml(overlayServerEntry("O", "catalog.example")));
        writeFile("aaa.xml", overlayCatalogXml(overlayServerEntry("O", "user.example")));

        TileServerService service = new TileServerService(tmp);
        service.initialize();

        List<OverlayServerType> overlays = service.getOverlays();
        assertEquals(1, overlays.size());
        assertEquals("catalog.example", overlays.get(0).getHost().get(0));
    }

    @Test
    public void testInitializeTwiceDoesNotGrowLists() throws IOException {
        writeFile("mapservers.xml", mapCatalogXml(mapServerEntry("A", "catalog.example")));
        writeFile("overlayservers.xml", overlayCatalogXml(overlayServerEntry("O", "catalog.example")));

        TileServerService service = new TileServerService(tmp);
        service.initialize();
        service.initialize();

        assertEquals(1, service.getMaps().size());
        assertEquals(1, service.getOverlays().size());
    }

    private static File newFile(File parent, String child) throws IOException {
        File result = new File(parent, child);
        result.createNewFile();
        return result;
    }
}
