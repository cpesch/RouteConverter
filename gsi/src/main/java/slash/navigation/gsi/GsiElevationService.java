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
package slash.navigation.gsi;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.MapDescriptor;
import slash.navigation.elevation.ElevationService;
import slash.navigation.rest.Get;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Transfer.isEmpty;

/**
 * Encapsulates access to the digital elevation model tiles of the Japanese
 * Geospatial Information Authority (GSI, 国土地理院), available at
 * <a href="https://maps.gsi.go.jp/development/elevation.html">maps.gsi.go.jp</a>.
 *
 * Elevation tiles are served as 256x256 PNG slippy map tiles, one dataset per
 * resolution. For a given position the best available dataset is tried first,
 * falling back to coarser datasets, then to {@code null} if the position is
 * outside Japan or no dataset covers it. Tiles are downloaded on demand and
 * cached on disk so repeated lookups near the same position do not hit the
 * GSI servers again.
 *
 * @author Christian Pesch
 */

public class GsiElevationService implements ElevationService {
    private static final Logger log = Logger.getLogger(GsiElevationService.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(GsiElevationService.class);
    private static final String DIRECTORY_PREFERENCE = "directory";
    private static final String NAME = "GSI Japan";
    private static final String BASE_URL = "https://cyberjapandata.gsi.go.jp/xyz/";
    private static final int TILE_SIZE = 256;

    // Rough bounding box of Japan including its outlying islands, used to avoid
    // sending requests to the GSI servers for positions that cannot possibly be covered.
    private static final double JAPAN_WEST = 122.0;
    private static final double JAPAN_EAST = 154.0;
    private static final double JAPAN_SOUTH = 20.0;
    private static final double JAPAN_NORTH = 46.0;

    // Best dataset first; the GSI servers answer with 404 for tiles a dataset does not cover.
    static final Dataset[] DATASETS = {
            new Dataset("dem1a", 17),
            new Dataset("dem5a", 15),
            new Dataset("dem5b", 15),
            new Dataset("dem5c", 15),
            new Dataset("dem10b", 14)
    };

    public String getName() {
        return NAME;
    }

    public boolean isDownload() {
        return true;
    }

    public boolean isOverQueryLimit() {
        return false;
    }

    public String getPath() {
        return preferences.get(DIRECTORY_PREFERENCE, "");
    }

    public void setPath(String path) {
        preferences.put(DIRECTORY_PREFERENCE, path);
    }

    public File getDirectory() {
        String path = getPath();
        if (isEmpty(path) || !new File(path).exists())
            return getApplicationDirectory("gsi");
        return ensureDirectory(path);
    }

    private static boolean isWithinJapan(double longitude, double latitude) {
        return longitude >= JAPAN_WEST && longitude <= JAPAN_EAST &&
                latitude >= JAPAN_SOUTH && latitude <= JAPAN_NORTH;
    }

    public Double getElevationFor(double longitude, double latitude) throws IOException {
        if (!isWithinJapan(longitude, latitude))
            return null;

        IOException lastException = null;
        for (Dataset dataset : DATASETS) {
            try {
                Double elevation = getElevationFor(dataset, longitude, latitude);
                if (elevation != null)
                    return elevation;
            } catch (IOException e) {
                lastException = e;
                log.fine(format("Cannot get elevation from dataset %s for %f/%f: %s", dataset.id, longitude, latitude, e));
            }
        }

        if (lastException != null)
            throw lastException;
        return null;
    }

    private Double getElevationFor(Dataset dataset, double longitude, double latitude) throws IOException {
        int tileX = longitudeToTileX(longitude, dataset.zoom);
        int tileY = latitudeToTileY(latitude, dataset.zoom);

        BufferedImage tile = loadTile(dataset, tileX, tileY);
        if (tile == null)
            return null;

        int pixelX = longitudeToPixelX(longitude, dataset.zoom, tileX);
        int pixelY = latitudeToPixelY(latitude, dataset.zoom, tileY);
        int rgb = tile.getRGB(pixelX, pixelY);
        return decodeElevation(rgb);
    }

    // Standard Web Mercator slippy map tile math, see https://maps.gsi.go.jp/development/tileCoordCheck.html
    static int longitudeToTileX(double longitude, int zoom) {
        return (int) Math.floor((longitude + 180.0) / 360.0 * (1L << zoom));
    }

    static int latitudeToTileY(double latitude, int zoom) {
        double latitudeAsRadians = Math.toRadians(latitude);
        return (int) Math.floor((1.0 - Math.log(Math.tan(latitudeAsRadians) + 1.0 / Math.cos(latitudeAsRadians)) / Math.PI) /
                2.0 * (1L << zoom));
    }

    static int longitudeToPixelX(double longitude, int zoom, int tileX) {
        double exactTileX = (longitude + 180.0) / 360.0 * (1L << zoom);
        int pixelX = (int) Math.floor((exactTileX - tileX) * TILE_SIZE);
        return Math.min(Math.max(pixelX, 0), TILE_SIZE - 1);
    }

    static int latitudeToPixelY(double latitude, int zoom, int tileY) {
        double latitudeAsRadians = Math.toRadians(latitude);
        double exactTileY = (1.0 - Math.log(Math.tan(latitudeAsRadians) + 1.0 / Math.cos(latitudeAsRadians)) / Math.PI) /
                2.0 * (1L << zoom);
        int pixelY = (int) Math.floor((exactTileY - tileY) * TILE_SIZE);
        return Math.min(Math.max(pixelY, 0), TILE_SIZE - 1);
    }

    // Decodes the elevation encoded by GSI in the RGB channels of a DEM tile pixel,
    // see https://maps.gsi.go.jp/development/demtile.html. The value (128,0,0), i.e.
    // u == 0x800000, marks "no data" (ocean, cloud cover gaps, outside the survey area).
    static Double decodeElevation(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        int u = (red << 16) + (green << 8) + blue;

        if (u == 0x800000)
            return null;

        double resolution = 0.01;
        return u < 0x800000 ? u * resolution : (u - 0x1000000) * resolution;
    }

    private BufferedImage loadTile(Dataset dataset, int tileX, int tileY) throws IOException {
        File file = createTileFile(dataset, tileX, tileY);
        if (!file.exists())
            downloadTile(dataset, tileX, tileY, file);

        // a missing file after a download attempt means the GSI server has no tile here
        // (outside the dataset's coverage); treat that as "no data" instead of an error
        if (!file.exists())
            return null;

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null)
                throw new IOException("Cannot decode GSI tile " + file);
            return image;
        } catch (IOException e) {
            // do not keep a corrupted cache entry around
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            throw e;
        }
    }

    private void downloadTile(Dataset dataset, int tileX, int tileY, File file) throws IOException {
        String url = BASE_URL + dataset.id + "/" + dataset.zoom + "/" + tileX + "/" + tileY + ".png";
        Get get = new Get(url);
        byte[] bytes = get.execute(response -> {
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toByteArray(entity) : null;
        });

        if (!get.isSuccessful() || bytes == null)
            return;

        ensureDirectory(file.getParentFile());
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        }
    }

    File createTileFile(Dataset dataset, int tileX, int tileY) {
        return new File(getDirectory(), dataset.id + File.separator + dataset.zoom + File.separator +
                tileX + File.separator + tileY + ".png");
    }

    public void downloadElevationDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes, boolean waitForDownload) {
        // tiles are downloaded and cached lazily by #getElevationFor; eagerly warm the
        // cache for the given positions so a later lookup can work offline
        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            try {
                getElevationFor(longitudeAndLatitude.longitude(), longitudeAndLatitude.latitude());
            } catch (IOException e) {
                log.fine(format("Cannot warm GSI elevation cache for %s: %s", longitudeAndLatitude, e));
            }
        }
    }

    public long calculateRemainingDownloadSize(List<MapDescriptor> mapDescriptors) {
        // GSI tiles are not organized in a catalog of downloadable areas; the size of the
        // tiles covering an arbitrary map area is unknown without probing the server for
        // every tile, which would cause excessive requests. Report nothing to pre-download.
        return 0L;
    }

    public void downloadElevationData(List<MapDescriptor> mapDescriptors) {
        // intentionally a no-op, see #calculateRemainingDownloadSize
    }

    public Map<BoundingBox, Boolean> getCoverageTiles(BoundingBox area) {
        return Collections.emptyMap();
    }

    static class Dataset {
        final String id;
        final int zoom;

        Dataset(String id, int zoom) {
            this.id = id;
            this.zoom = zoom;
        }
    }
}
