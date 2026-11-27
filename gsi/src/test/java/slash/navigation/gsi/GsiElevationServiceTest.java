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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import slash.navigation.common.LongitudeAndLatitude;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static slash.navigation.gsi.GsiElevationService.*;

/**
 * Tests {@link GsiElevationService}: elevation decoding, coordinate conversion,
 * dataset fallback and tile caching.
 *
 * @author Christian Pesch
 */

public class GsiElevationServiceTest {
    private final GsiElevationService service = new GsiElevationService();

    @AfterEach
    public void tearDown() {
        service.setPath("");
    }

    // ---- simple accessors ----

    @Test
    public void testGetName() {
        assertEquals("GSI Japan", service.getName());
    }

    @Test
    public void testIsDownload() {
        assertTrue(service.isDownload());
    }

    @Test
    public void testIsOverQueryLimit() {
        assertFalse(service.isOverQueryLimit());
    }

    @Test
    public void testPathDefaultsToApplicationDirectory(@TempDir File tempDir) {
        service.setPath(tempDir.getAbsolutePath());
        assertEquals(tempDir.getAbsolutePath(), service.getPath());
        assertEquals(tempDir.getAbsolutePath(), service.getDirectory().getAbsolutePath());
    }

    @Test
    public void testGetDirectoryFallsBackWhenPathDoesNotExist() {
        service.setPath(new File("does/not/exist/at/all").getAbsolutePath());
        assertNotNull(service.getDirectory());
        assertTrue(service.getDirectory().isDirectory());
    }

    // ---- elevation decoding, see https://maps.gsi.go.jp/development/demtile.html ----

    @Test
    public void testDecodeElevationOfZero() {
        assertEquals(0.0, decodeElevation(0x000000), 0.0001);
    }

    @Test
    public void testDecodeElevationPositiveValue() {
        // u = 123456 -> 1234.56m
        assertEquals(1234.56, decodeElevation(123456), 0.0001);
    }

    @Test
    public void testDecodeElevationNegativeValue() {
        // u = 0x1000000 - 100 -> -1.00m
        int u = 0x1000000 - 100;
        assertEquals(-1.0, decodeElevation(u), 0.0001);
    }

    @Test
    public void testDecodeElevationNoDataIsNull() {
        assertNull(decodeElevation(0x800000));
    }

    // ---- coordinate conversion ----

    @Test
    public void testLongitudeToTileXAtZoomZeroIsSingleTile() {
        assertEquals(0, longitudeToTileX(0.0, 0));
    }

    @Test
    public void testLongitudeToTileXOfPrimeMeridian() {
        // the prime meridian is exactly in the middle of the world map
        assertEquals(1 << 13, longitudeToTileX(0.0, 14));
    }

    @Test
    public void testLatitudeToTileYOfEquator() {
        // the equator is exactly in the middle of the Web Mercator projection
        assertEquals(1 << 13, latitudeToTileY(0.0, 14));
    }

    @Test
    public void testLongitudeToTileXIncreasesByOnePerTileWidth() {
        int zoom = 14;
        int tileX = longitudeToTileX(139.0, zoom);
        double tileWidth = 360.0 / (1 << zoom);
        assertEquals(tileX + 1, longitudeToTileX(139.0 + tileWidth, zoom));
    }

    @Test
    public void testPixelWithinTileIsInRange() {
        int zoom = 14;
        double longitude = 139.7;
        double latitude = 35.7;
        int tileX = longitudeToTileX(longitude, zoom);
        int tileY = latitudeToTileY(latitude, zoom);

        int pixelX = longitudeToPixelX(longitude, zoom, tileX);
        int pixelY = latitudeToPixelY(latitude, zoom, tileY);

        assertTrue(pixelX >= 0 && pixelX < 256);
        assertTrue(pixelY >= 0 && pixelY < 256);
    }

    // ---- positions outside Japan return null without any dataset lookup ----

    @Test
    public void testGetElevationForOutsideJapanReturnsNull() throws IOException {
        // Berlin
        assertNull(service.getElevationFor(13.4, 52.5));
    }

    @Test
    public void testGetElevationForOutsideJapanDoesNotCreateAnyCacheFile(@TempDir File tempDir) throws IOException {
        service.setPath(tempDir.getAbsolutePath());
        service.getElevationFor(13.4, 52.5);
        assertEquals(0, tempDir.list().length);
    }

    // ---- dataset fallback and caching, using a tile pre-populated in the cache directory ----

    @Test
    public void testGetElevationForReadsCachedTileOfBestDataset(@TempDir File tempDir) throws IOException {
        service.setPath(tempDir.getAbsolutePath());

        double longitude = 139.7;
        double latitude = 35.7;
        Dataset best = DATASETS[0];

        writeTile(best, longitude, latitude, encodeElevation(500.25));

        Double elevation = service.getElevationFor(longitude, latitude);
        assertEquals(500.25, elevation, 0.0001);
    }

    @Test
    public void testGetElevationForFallsBackToNextDatasetWhenBestHasNoData(@TempDir File tempDir) throws IOException {
        service.setPath(tempDir.getAbsolutePath());

        double longitude = 139.7;
        double latitude = 35.7;
        Dataset best = DATASETS[0];
        Dataset fallback = DATASETS[1];

        writeTile(best, longitude, latitude, 0x800000); // "no data" pixel
        writeTile(fallback, longitude, latitude, encodeElevation(42.0));

        Double elevation = service.getElevationFor(longitude, latitude);
        assertEquals(42.0, elevation, 0.0001);
    }

    @Test
    public void testDownloadElevationDataForEmptyListDoesNothing() {
        service.downloadElevationDataFor(emptyList(), false);
    }

    @Test
    public void testDownloadElevationDataForWithPositionsOutsideJapanDoesNotThrow() {
        service.downloadElevationDataFor(List.of(new LongitudeAndLatitude(13.4, 52.5)), false);
    }

    @Test
    public void testCalculateRemainingDownloadSizeIsZero() {
        assertEquals(0L, service.calculateRemainingDownloadSize(emptyList()));
    }

    @Test
    public void testDownloadElevationDataIsNoOp() {
        service.downloadElevationData(emptyList());
    }

    @Test
    public void testGetCoverageTilesIsEmpty() {
        assertEquals(Collections.emptyMap(), service.getCoverageTiles(null));
    }

    // ---- test helpers ----

    private static int encodeElevation(double elevation) {
        long u = Math.round(elevation / 0.01);
        return (int) (u < 0 ? u + 0x1000000 : u);
    }

    private void writeTile(Dataset dataset, double longitude, double latitude, int rgb) throws IOException {
        int tileX = longitudeToTileX(longitude, dataset.zoom);
        int tileY = latitudeToTileY(latitude, dataset.zoom);
        int pixelX = longitudeToPixelX(longitude, dataset.zoom, tileX);
        int pixelY = latitudeToPixelY(latitude, dataset.zoom, tileY);

        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        image.setRGB(pixelX, pixelY, rgb);

        File file = service.createTileFile(dataset, tileX, tileY);
        file.getParentFile().mkdirs();
        ImageIO.write(image, "png", file);
    }
}
