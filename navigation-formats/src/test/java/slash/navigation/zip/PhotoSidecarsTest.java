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

    Copyright (C) 2026 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.zip;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserResult;
import slash.navigation.common.NavigationPosition;
import slash.navigation.photo.PhotoFormat;
import slash.navigation.photo.PhotoPosition;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.photo.TagState.Tagged;

/**
 * Tests pairing photos with their Google Takeout JSON sidecars in a ZIP archive, with synthetic images.
 *
 * @author Christian Pesch
 */
public class PhotoSidecarsTest {
    private static final double DELTA = 0.0000001;
    private static final String KML22 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><name>D</name>\n" +
            "<Placemark><name>C</name><Point><coordinates>11.1,51.1,0</coordinates></Point></Placemark>\n" +
            "</Document></kml>\n";

    private final NavigationFormatParser parser = new NavigationFormatParser(new NavigationFormatRegistry());
    private final List<File> files = new ArrayList<>();

    @AfterEach
    public void tearDown() {
        for (File file : files)
            assertTrue(file.delete());
    }

    private File temporaryFile(String suffix) throws IOException {
        File file = File.createTempFile("photosidecarstest", suffix);
        files.add(file);
        return file;
    }

    private static byte[] jpegWithoutExif() throws IOException {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "jpg", bytes));
        return bytes.toByteArray();
    }

    private byte[] jpegWithGps(double longitude, double latitude) throws IOException {
        File source = temporaryFile(".jpg");
        java.nio.file.Files.write(source.toPath(), jpegWithoutExif());
        PhotoPosition position = new PhotoPosition(longitude, latitude, null, null, fromMillis(1704791651000L),
                "With GPS", source, Tagged, null, "Make", "Model", 16, 16, new RationalNumber(28, 10),
                new RationalNumber(1, 100), 0, new RationalNumber(50, 1));
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        new PhotoFormat().write(position, source, target);
        return target.toByteArray();
    }

    private static byte[] sidecar(String title, double latitude, double longitude) {
        return ("{\"title\": \"" + title + "\", \"description\": \"\", " +
                "\"creationTime\": {\"timestamp\": \"1704791651\", \"formatted\": \"09.01.2024, 09:14:11 UTC\"}, " +
                "\"imageViews\": \"0\", " +
                "\"geoDataExif\": {\"latitude\": " + latitude + ", \"longitude\": " + longitude + ", \"altitude\": 0.0}}").getBytes(UTF_8);
    }

    private File zip(Object... namesAndContents) throws IOException {
        File file = temporaryFile(".zip");
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            for (int i = 0; i < namesAndContents.length; i += 2) {
                zip.putNextEntry(new ZipEntry((String) namesAndContents[i]));
                Object content = namesAndContents[i + 1];
                zip.write(content instanceof String string ? string.getBytes(UTF_8) : (byte[]) content);
                zip.closeEntry();
            }
        }
        return file;
    }

    private static List<NavigationPosition> photoPositions(ParserResult result) {
        List<NavigationPosition> positions = new ArrayList<>();
        for (BaseRoute<?, ?> route : result.getAllRoutes())
            for (Object position : route.getPositions())
                if (position instanceof PhotoPosition photo)
                    positions.add(photo);
        return positions;
    }

    @Test
    public void sidecarAfterThePhotoPlacesIt() throws IOException {
        ParserResult result = parser.read(zip("Maps/Fotos und Videos/IMG_1.jpg", jpegWithoutExif(),
                "Maps/Fotos und Videos/IMG_1.jpg.json", sidecar("IMG_1.jpg", 50.2, 8.4)));
        assertTrue(result.isSuccessful());
        List<NavigationPosition> photos = photoPositions(result);
        assertEquals(1, photos.size());
        NavigationPosition photo = photos.get(0);
        assertEquals(8.4, photo.getLongitude(), DELTA);
        assertEquals(50.2, photo.getLatitude(), DELTA);
        assertEquals(fromMillis(1704791651000L), photo.getTime());
        assertEquals("IMG_1.jpg", photo.getDescription());
    }

    @Test
    public void sidecarBeforeThePhotoPlacesIt() throws IOException {
        ParserResult result = parser.read(zip("Maps/Fotos und Videos/IMG_2.jpg.json", sidecar("IMG_2.jpg", 48.1, 11.6),
                "Maps/Fotos und Videos/IMG_2.jpg", jpegWithoutExif()));
        assertTrue(result.isSuccessful());
        List<NavigationPosition> photos = photoPositions(result);
        assertEquals(1, photos.size());
        assertEquals(11.6, photos.get(0).getLongitude(), DELTA);
        assertEquals(48.1, photos.get(0).getLatitude(), DELTA);
    }

    @Test
    public void photoWithoutLocationIsDropped() throws IOException {
        ParserResult result = parser.read(zip("Maps/Fotos und Videos/IMG_3.jpg", jpegWithoutExif(),
                "Maps/places.kml", KML22));
        assertTrue(result.isSuccessful());
        assertEquals(0, photoPositions(result).size());
        assertEquals(1, result.getAllRoutes().size());
    }

    @Test
    public void exifLocationWinsOverTheSidecar() throws IOException {
        ParserResult result = parser.read(zip("Maps/Fotos und Videos/IMG_4.jpg", jpegWithGps(13.4, 52.5),
                "Maps/Fotos und Videos/IMG_4.jpg.json", sidecar("IMG_4.jpg", 50.2, 8.4)));
        assertTrue(result.isSuccessful());
        List<NavigationPosition> photos = photoPositions(result);
        assertEquals(1, photos.size());
        assertEquals(13.4, photos.get(0).getLongitude(), 0.0001);
        assertEquals(52.5, photos.get(0).getLatitude(), 0.0001);
    }

    private byte[] jpegWithExifButNoGps() throws IOException {
        // like the photos Google leaves in Takeout: an EXIF block without GPS, description, make or model
        TiffOutputSet outputSet = new TiffOutputSet();
        try {
            outputSet.getOrCreateExifDirectory().add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, "2024:01:09 09:14:11");
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            new ExifRewriter().updateExifMetadataLossless(jpegWithoutExif(), target, outputSet);
            return target.toByteArray();
        } catch (ImageReadException | ImageWriteException e) {
            throw new IOException(e);
        }
    }

    private static String sidecarJson(String title, String geoKey, double latitude, double longitude) {
        return "{\"title\": \"" + title + "\", \"photoTakenTime\": {\"timestamp\": \"1704791651\"}, " +
                "\"" + geoKey + "\": {\"latitude\": " + latitude + ", \"longitude\": " + longitude + ", \"altitude\": 0.0}}";
    }

    private NavigationPosition onlyPhoto(ParserResult result) {
        assertTrue(result.isSuccessful());
        List<NavigationPosition> photos = photoPositions(result);
        assertEquals(1, photos.size());
        return photos.get(0);
    }

    @Test
    public void sidecarTitleReplacesTheGpsFallbackDescription() throws IOException {
        NavigationPosition photo = onlyPhoto(parser.read(zip("Fotos/IMG_5.jpg", jpegWithExifButNoGps(),
                "Fotos/IMG_5.jpg.json", sidecar("IMG_5.jpg", 50.2, 8.4))));
        assertEquals(8.4, photo.getLongitude(), DELTA);
        assertEquals("IMG_5.jpg", photo.getDescription());
    }

    @Test
    public void pairsTruncatedSupplementalMetadata() throws IOException {
        NavigationPosition photo = onlyPhoto(parser.read(zip("Fotos/IMG_6.jpg", jpegWithoutExif(),
                "Fotos/IMG_6.jpg.supplemental-metad.json", sidecar("IMG_6.jpg", 50.2, 8.4))));
        assertEquals(50.2, photo.getLatitude(), DELTA);
    }

    @Test
    public void pairsDuplicates() throws IOException {
        ParserResult result = parser.read(zip("Fotos/IMG_7.jpg", jpegWithoutExif(),
                "Fotos/IMG_7.jpg.json", sidecar("IMG_7.jpg", 50.2, 8.4),
                "Fotos/IMG_7(1).jpg", jpegWithoutExif(),
                "Fotos/IMG_7.jpg(1).json", sidecar("IMG_7(1).jpg", 48.1, 11.6)));
        assertTrue(result.isSuccessful());
        List<Double> latitudes = new ArrayList<>();
        for (NavigationPosition photo : photoPositions(result))
            latitudes.add(photo.getLatitude());
        latitudes.sort(null);
        assertEquals(List.of(48.1, 50.2), latitudes);
    }

    @Test
    public void pairsNamesTruncatedTo47Characters() throws IOException {
        String photoName = "PXL_20240109_091411123.MP.PORTRAIT.ORIGINAL-COVER.jpg";
        assertTrue(photoName.length() > 47);
        NavigationPosition photo = onlyPhoto(parser.read(zip("Fotos/" + photoName, jpegWithoutExif(),
                "Fotos/" + photoName.substring(0, 47) + ".json", sidecar(photoName, 50.2, 8.4))));
        assertEquals(50.2, photo.getLatitude(), DELTA);
    }

    @Test
    public void editedPhotoUsesTheOriginalsSidecar() throws IOException {
        NavigationPosition photo = onlyPhoto(parser.read(zip("Fotos/IMG_8-edited.jpg", jpegWithoutExif(),
                "Fotos/IMG_8.jpg.json", sidecar("IMG_8.jpg", 50.2, 8.4))));
        assertEquals(50.2, photo.getLatitude(), DELTA);
    }

    @Test
    public void fallsBackToGeoData() throws IOException {
        NavigationPosition photo = onlyPhoto(parser.read(zip("Fotos/IMG_9.jpg", jpegWithoutExif(),
                "Fotos/IMG_9.jpg.json", sidecarJson("IMG_9.jpg", "geoData", 47.3, 9.1))));
        assertEquals(47.3, photo.getLatitude(), DELTA);
    }

    @Test
    public void laterSidecarWithoutLocationKeepsTheLocatedOne() throws IOException {
        NavigationPosition photo = onlyPhoto(parser.read(zip(
                "Fotos/IMG_10.jpg.json", sidecar("IMG_10.jpg", 50.2, 8.4),
                "Fotos/IMG_10.jpg.supplemental-metadata.json", sidecar("IMG_10.jpg", 0.0, 0.0),
                "Fotos/IMG_10.jpg", jpegWithoutExif())));
        assertEquals(50.2, photo.getLatitude(), DELTA);
    }

    @Test
    public void sidecarAtZeroZeroDoesNotPlaceThePhoto() throws IOException {
        ParserResult result = parser.read(zip("Fotos/IMG_11.jpg", jpegWithoutExif(),
                "Fotos/IMG_11.jpg.json", sidecar("IMG_11.jpg", 0.0, 0.0), "places.kml", KML22));
        assertTrue(result.isSuccessful());
        assertEquals(0, photoPositions(result).size());
    }

    @Test
    public void malformedJsonAndSidecarsOfOtherFilesAreHarmless() throws IOException {
        ParserResult result = parser.read(zip("Fotos/IMG_12.jpg.json", "{\"title\": \"IMG_12.jpg\", \"geoDataExif\": {",
                "Fotos/clip.mp4.json", sidecarJson("clip.mp4", "geoDataExif", 50.2, 8.4), "places.kml", KML22));
        assertTrue(result.isSuccessful());
        assertEquals(1, result.getAllRoutes().size());
        assertEquals(0, photoPositions(result).size());
    }

    @Test
    public void sidecarKeys() {
        assertEquals("a/IMG_1.jpg", PhotoSidecars.sidecarKey("a/IMG_1.jpg.json"));
        assertEquals("a/IMG_1.jpg", PhotoSidecars.sidecarKey("a/IMG_1.jpg.supplemental-metadata.json"));
        assertEquals("a/IMG_1.jpg", PhotoSidecars.sidecarKey("a/IMG_1.jpg.supplemental-me.json"));
        String supplementalMetadata = "supplemental-metadata";
        for (int length = 0; length <= supplementalMetadata.length(); length++)
            assertEquals("a/IMG_1.jpg", PhotoSidecars.sidecarKey("a/IMG_1.jpg." + supplementalMetadata.substring(0, length) + ".json"),
                    "suffix of length " + length);
        assertEquals("a/IMG_1.jpg.x", PhotoSidecars.sidecarKey("a/IMG_1.jpg.x.json"));
        assertEquals("a/IMG_1.jpg(1)", PhotoSidecars.sidecarKey("a/IMG_1.jpg(1).json"));
        assertEquals("a/IMG_1.jpg(1)", PhotoSidecars.sidecarKey("a/IMG_1.jpg.supplemental-metadata(1).json"));
        assertTrue(PhotoSidecars.candidateKeys("a/IMG_1(1).jpg").contains("a/IMG_1.jpg(1)"));
        assertTrue(PhotoSidecars.candidateKeys("a/IMG_1-edited.jpg").contains("a/IMG_1.jpg"));
        assertNull(PhotoSidecars.parseSidecar("a/Settings.json", "{\"createdTime\": \"x\"}".getBytes(UTF_8)));
        assertNull(PhotoSidecars.parseSidecar("a/IMG_1.jpg", sidecar("IMG_1.jpg", 1, 1)));
    }
}
