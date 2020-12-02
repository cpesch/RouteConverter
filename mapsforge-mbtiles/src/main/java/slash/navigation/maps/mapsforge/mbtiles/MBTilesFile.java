package slash.navigation.maps.mapsforge.mbtiles;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.util.MercatorProjection;

import java.io.File;
import java.io.InputStream;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Double.parseDouble;
import static java.lang.String.format;

public class MBTilesFile {
    private final Connection connection;
    private Map<String, String> metadata = null;

    private static final String SELECT_METADATA = "SELECT name, value FROM metadata";
    private static final String SELECT_TILES =
            "SELECT tile_data " +
                    "FROM tiles " +
                    "WHERE zoom_level=%s AND tile_column=%s AND tile_row=%s " +
                    "ORDER BY zoom_level DESC " +
                    "LIMIT 1";
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("png", "jpg", "jpeg");

    public MBTilesFile(File file) {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        String format = getFormat();
        if (format == null)
            throw new IllegalArgumentException("'metadata.format' field was not found. Is this an MBTiles database?");
        if (!SUPPORTED_FORMATS.contains(format))
            throw new IllegalArgumentException(format("Unsupported 'metadata.format: %s'. Supported format(s) are: %s", format, SUPPORTED_FORMATS));
    }

    public void close() {
        try {
            metadata = null;
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public BoundingBox getBoundingBox() {
        String bounds = getMetadata().get("bounds");
        if (bounds == null)
            return null;
        String[] split = bounds.split(",");
        if (split.length != 4) {
            return null;
        }
        double minimumLongitude = parseDouble(split[0]);
        double minimumLatitude = parseDouble(split[1]);
        double maximumLongitude = parseDouble(split[2]);
        double maximumLatitude = parseDouble(split[3]);
        return new BoundingBox(minimumLatitude, minimumLongitude, maximumLatitude, maximumLongitude);
    }

    public int getMaxZoom() {
        String maxZoom = getMetadata().get("maxzoom");
        return maxZoom != null ? Integer.parseInt(maxZoom) : 21 /*TODO really?*/;
    }

    public int getMinZoom() {
        String minZoom = getMetadata().get("minzoom");
        return minZoom != null ? Integer.parseInt(minZoom) : 0 /*TODO really?*/;
    }

    private String getFormat() {
        return getMetadata().get("format");
    }

    private Map<String, String> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();

            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(30);

                try (ResultSet resultSet = statement.executeQuery(SELECT_METADATA)) {
                    while (resultSet.next()) {
                        String name = resultSet.getString("name");
                        String value = resultSet.getString("value");
                        metadata.put(name, value);
                    }
                }
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return metadata;
    }

    public static double zoomLevelToScale(byte zoomLevel) { // TODO
        return 1 << zoomLevel;
    }

    public static long tileYToTMS(long tileY, byte zoomLevel) { // TODO
        return (long) (zoomLevelToScale(zoomLevel) - tileY - 1);
    }

    public InputStream getTileAsBytes(int tileX, int tileY, byte zoomLevel) {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);

            long tmsTileY = tileYToTMS(tileY, zoomLevel);
            try (ResultSet resultSet = statement.executeQuery(format(SELECT_TILES, String.valueOf(zoomLevel), tileX, tmsTileY))) {
                if(resultSet.next()) {
                    return resultSet.getBinaryStream("tile_data");
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
