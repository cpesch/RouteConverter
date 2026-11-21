package slash.navigation.maps.tileserver;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static javax.swing.SwingUtilities.invokeAndWait;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.navigation.maps.tileserver.TileServerMapManager.extractCopyrightHref;

public class TileServerMapManagerTest {
    private static final String OVERLAY_NS = "http://api.routeconverter.com/v1/schemas/overlayserver-catalog";
    private static final String MAP_NS = "http://api.routeconverter.com/v1/schemas/mapserver-catalog";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static String entry(String element, String id, String activeAttribute) {
        return "    <" + element + " id=\"" + id + "\" name=\"" + id + "\" minZoom=\"1\" maxZoom=\"18\"" + activeAttribute + ">\n" +
                "        <host>" + id + ".example</host>\n" +
                "        <urlPattern>/tiles/{z}/{x}/{y}.png</urlPattern>\n" +
                "        <copyright>copyright</copyright>\n" +
                "    </" + element + ">\n";
    }

    private void writeCatalog(String name, String namespace, String... entries) throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<catalog xmlns=\"" + namespace + "\">\n" +
                String.join("", entries) + "</catalog>\n";
        File file = tmp.newFile(name);
        Files.writeString(file.toPath(), xml, StandardCharsets.UTF_8);
    }

    private TileServerMapManager scan() throws InvocationTargetException, InterruptedException {
        TileServerMapManager manager = new TileServerMapManager(tmp.getRoot());
        manager.scanTileServers();
        // scanTileServers() fills the models on the event dispatch thread; wait for it
        invokeAndWait(() -> {
        });
        return manager;
    }

    private static List<String> ids(TileServerMapManager manager) {
        return manager.getAvailableOverlaysModel().getItems().stream().map(TileServer::id).toList();
    }

    @Test
    public void testInactiveOverlayIsNotAvailable() throws Exception {
        writeCatalog("overlayservers.xml", OVERLAY_NS,
                entry("overlayServer", "alive", " active=\"true\""),
                entry("overlayServer", "dead", " active=\"false\""));

        TileServerMapManager manager = scan();
        try {
            assertEquals(List.of("alive"), ids(manager));
            assertTrue(manager.getAvailableOverlaysModel().getItem(0).active());
        } finally {
            manager.dispose();
        }
    }

    @Test
    public void testOverlayWithoutActiveAttributeIsAvailable() throws Exception {
        writeCatalog("overlayservers.xml", OVERLAY_NS, entry("overlayServer", "unflagged", ""));

        TileServerMapManager manager = scan();
        try {
            assertEquals(List.of("unflagged"), ids(manager));
        } finally {
            manager.dispose();
        }
    }

    @Test
    public void testInactiveMapIsStillAvailable() throws Exception {
        writeCatalog("mapservers.xml", MAP_NS,
                entry("mapServer", "alive", " active=\"true\""),
                entry("mapServer", "dead", " active=\"false\""));

        TileServerMapManager manager = scan();
        try {
            // the tile download table shows inactive maps with its Active column
            assertEquals(2, manager.getAvailableMapsModel().getRowCount());
            assertFalse(manager.getAvailableMapsModel().getItems().stream()
                    .filter(m -> m.id().equals("dead")).findFirst().orElseThrow().active());
        } finally {
            manager.dispose();
        }
    }

    @Test
    public void testExtractHref() {
        assertEquals("http://www.wanderreitkarte.de/", extractCopyrightHref("Map data &copy; <a href=\"http://www.wanderreitkarte.de/\" target=\"_blank\">Wanderreitkarte</a>"));
        assertEquals("http://www.wanderreitkarte.de/", extractCopyrightHref("href=\"http://www.wanderreitkarte.de/\""));
        assertNull(extractCopyrightHref("http://www.wanderreitkarte.de/"));
    }
}
