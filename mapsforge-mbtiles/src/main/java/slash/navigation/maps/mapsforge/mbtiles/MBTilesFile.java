package slash.navigation.maps.mapsforge.mbtiles;

import org.mapsforge.core.model.BoundingBox;

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

    private String getFormat() {
        return getMetadata().get("format");
    }

    private Integer getMetadata(String name) {
        String value = getMetadata().get(name);
        return value != null ? Integer.parseInt(value) : null;
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

    private Integer getZoomLevel(String name, String function) {
        Integer maxZoom = getMetadata(name);
        if (maxZoom == null) {
            String result = getSingleValue("SELECT " + function + "(zoom_level) AS value FROM tiles");
            if (result != null) {
                getMetadata().put(name, result);
            }
        }
        return getMetadata(name);
    }

    public Integer getZoomLevelMax() {
        return getZoomLevel("maxzoom", "MAX");
    }

    public int getZoomLevelMin() {
        return getZoomLevel("minzoom", "MIN");
    }

    private String getSingleValue(String query) {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);

            try (ResultSet resultSet = statement.executeQuery(query)) {
                if(resultSet.next()) {
                    return resultSet.getString("value");
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Converts a zoom level to a scale factor.
     *
     * @param zoomLevel the zoom level to convert.
     * @return the corresponding scale factor.
     */
   private double zoomLevelToScale(byte zoomLevel) {
        return 1 << zoomLevel;
    }

    /**
     * Converts a tile Y number at a certain zoom level to TMS notation.
     *
     * @param tileY     the tile Y number that should be converted.
     * @param zoomLevel the zoom level at which the number should be converted.
     * @return the TMS value of the tile Y number.
     */
    private long tileYToTMS(long tileY, byte zoomLevel) {
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
