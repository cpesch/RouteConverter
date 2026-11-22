package slash.navigation.maps.mapsforge.mbtiles;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MBTilesFileTest {
    @TempDir
    File temporaryDirectory;
    private MBTilesFile mbTilesFile;

    @AfterEach
    void tearDown() {
        if (mbTilesFile != null)
            mbTilesFile.close();
    }

    private File createMBTiles(String format, String tileResource) throws IOException, SQLException {
        File file = new File(temporaryDirectory, format + ".mbtiles");
        byte[] tile;
        try (InputStream inputStream = Objects.requireNonNull(getClass().getResourceAsStream(tileResource), tileResource)) {
            tile = inputStream.readAllBytes();
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE metadata (name TEXT, value TEXT)");
            statement.execute("CREATE TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB)");
            statement.execute("INSERT INTO metadata VALUES ('format', '" + format + "')");
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO tiles VALUES (1, 0, 0, ?)")) {
                insert.setBytes(1, tile);
                insert.execute();
            }
        }
        return file;
    }

    private BufferedImage readTile(String format, String tileResource) throws IOException, SQLException {
        mbTilesFile = new MBTilesFile(createMBTiles(format, tileResource));
        // XYZ row 1 at zoom 1 is TMS row 0
        try (InputStream inputStream = mbTilesFile.getTileAsBytes(0, 1, (byte) 1)) {
            assertNotNull(inputStream);
            return ImageIO.read(inputStream);
        }
    }

    private static void assertTile(BufferedImage image) {
        assertNotNull(image, "tile could not be decoded");
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        int rgb = image.getRGB(128, 128);
        assertTrue(Math.abs(((rgb >> 16) & 0xff) - 40) <= 8, "red");
        assertTrue(Math.abs(((rgb >> 8) & 0xff) - 120) <= 8, "green");
        assertTrue(Math.abs((rgb & 0xff) - 200) <= 8, "blue");
    }

    @Test
    void readsPngTiles() throws Exception {
        assertTile(readTile("png", "tile.png"));
    }

    @Test
    void readsLosslessWebPTiles() throws Exception {
        assertTile(readTile("webp", "tile-lossless.webp"));
    }

    @Test
    void readsLossyWebPTiles() throws Exception {
        assertTile(readTile("webp", "tile-lossy.webp"));
    }

    @Test
    void rejectsVectorTiles() throws Exception {
        File file = createMBTiles("pbf", "tile.png");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new MBTilesFile(file));
        assertTrue(exception.getMessage().contains("pbf"));
        // the rejected file must not stay open: on Windows an open SQLite connection locks it
        assertTrue(file.delete(), "rejected MBTiles file is still open");
    }
}
