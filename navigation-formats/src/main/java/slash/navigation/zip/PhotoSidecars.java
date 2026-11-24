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
package slash.navigation.zip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.ParserContext;
import slash.navigation.photo.PhotoPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Pairs the photos of a ZIP archive with the JSON sidecars Google Takeout writes next to them
 * ({@code IMG_1.jpg} and {@code IMG_1.jpg.json}): Google strips the EXIF GPS data from most
 * photos and keeps the location in the sidecar's {@code geoDataExif} or {@code geoData}.
 * Photos that end up without coordinates are dropped from the archive's position lists.
 * <p>
 * Sidecars are recognized by their content and paired by name, including the variants Google
 * writes: {@code IMG_1.jpg.supplemental-metadata.json} (also truncated, e.g.
 * {@code .supplemental-metad.json}), duplicates {@code IMG_1.jpg(1).json} for {@code IMG_1(1).jpg},
 * names truncated to 46 or 47 characters, and {@code IMG_1-edited.jpg} using the sidecar of
 * {@code IMG_1.jpg}.
 *
 * @author Christian Pesch
 */

class PhotoSidecars {
    private static final Logger log = Logger.getLogger(PhotoSidecars.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String[] PHOTO_EXTENSIONS = {".jpg", ".jpeg", ".png", ".heic", ".tif", ".tiff", ".webp", ".gif"};
    private static final String SIDECAR_EXTENSION = ".json";
    static final int MAXIMUM_SIDECAR_SIZE = 64 * 1024;
    private static final int[] TRUNCATED_NAME_LENGTHS = {46, 47};
    private static final Pattern DUPLICATE_PATTERN = Pattern.compile("^(.*)(\\(\\d+\\))$");
    // Google cuts sidecar names to 51 characters, which leaves any prefix of this suffix, down to "."
    private static final String SUPPLEMENTAL_METADATA = "supplemental-metadata";
    private static final Pattern PHOTO_DUPLICATE_PATTERN = Pattern.compile("^(.*)(\\(\\d+\\))(\\.[^.]+)$");
    private static final Pattern EDITED_PATTERN = Pattern.compile("(?i)^(.*)-(edited|bearbeitet)(\\.[^.]+)$");

    private static class Sidecar {
        final Double longitude, latitude;
        final CompactCalendar time;
        final String title;

        Sidecar(Double longitude, Double latitude, CompactCalendar time, String title) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.time = time;
            this.title = title;
        }

        boolean hasCoordinates() {
            return longitude != null && latitude != null;
        }
    }

    private final Map<String, Sidecar> sidecars = new HashMap<>();
    // photos without EXIF coordinates by the keys of the sidecars that may still place them
    private final Map<String, List<PhotoPosition>> waitingPhotos = new HashMap<>();
    private final Set<PhotoPosition> unplacedPhotos = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<PhotoPosition> archivePhotos = Collections.newSetFromMap(new IdentityHashMap<>());

    static boolean isPhoto(String entryName) {
        String lowerCase = entryName.toLowerCase(Locale.ROOT);
        for (String extension : PHOTO_EXTENSIONS)
            if (lowerCase.endsWith(extension))
                return true;
        return false;
    }

    /**
     * @return the parsed sidecar if the entry is a small JSON file with the keys of a Google
     * Takeout photo sidecar, otherwise null
     */
    static JsonNode parseSidecar(String entryName, byte[] bytes) {
        if (!entryName.toLowerCase(Locale.ROOT).endsWith(SIDECAR_EXTENSION) || bytes.length > MAXIMUM_SIDECAR_SIZE)
            return null;
        try {
            JsonNode root = MAPPER.readTree(bytes);
            if (root != null && root.isObject() && (root.has("geoDataExif") || root.has("geoData") || root.has("photoTakenTime")))
                return root;
        } catch (IOException e) {
            log.fine("Not a photo sidecar " + entryName + ": " + e);
        }
        return null;
    }

    /**
     * @return the key a sidecar is stored under: the directory and the photo name it names,
     * with a duplicate counter moved behind it, e.g. {@code a/IMG_1.jpg(1)}
     */
    static String sidecarKey(String entryName) {
        String name = entryName.substring(0, entryName.length() - SIDECAR_EXTENSION.length());
        String duplicate = "";
        Matcher matcher = DUPLICATE_PATTERN.matcher(name);
        if (matcher.matches()) {
            name = matcher.group(1);
            duplicate = matcher.group(2);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            String suffix = name.substring(dot + 1).toLowerCase(Locale.ROOT);
            String withoutSupplemental = name.substring(0, dot);
            if (SUPPLEMENTAL_METADATA.startsWith(suffix) && isPhoto(withoutSupplemental))
                name = withoutSupplemental;
        }
        return name + duplicate;
    }

    /**
     * @return the keys under which the sidecar of the given photo may be stored, most specific first
     */
    static List<String> candidateKeys(String photoName) {
        List<String> keys = new ArrayList<>();
        addKeys(photoName, keys);
        Matcher edited = EDITED_PATTERN.matcher(photoName);
        if (edited.matches())
            addKeys(edited.group(1) + edited.group(3), keys);
        return keys;
    }

    private static void addKeys(String photoName, List<String> keys) {
        keys.add(photoName);
        Matcher duplicate = PHOTO_DUPLICATE_PATTERN.matcher(photoName);
        if (duplicate.matches())
            keys.add(duplicate.group(1) + duplicate.group(3) + duplicate.group(2));
        int slash = Math.max(photoName.lastIndexOf('/'), photoName.lastIndexOf('\\')) + 1;
        String directory = photoName.substring(0, slash), fileName = photoName.substring(slash);
        for (int length : TRUNCATED_NAME_LENGTHS)
            if (fileName.length() > length)
                keys.add(directory + fileName.substring(0, length));
    }

    void addSidecar(String entryName, JsonNode root) {
        String key = sidecarKey(entryName);
        Sidecar sidecar = toSidecar(root);
        // a second sidecar for the same photo, e.g. .json and .supplemental-metadata.json,
        // must not replace a located one by one without location
        Sidecar existing = sidecars.get(key);
        if (existing != null && existing.hasCoordinates() && !sidecar.hasCoordinates())
            return;
        sidecars.put(key, sidecar);

        List<PhotoPosition> waiting = waitingPhotos.get(key);
        if (waiting != null)
            for (PhotoPosition photo : new ArrayList<>(waiting))
                complete(photo, sidecar);
    }

    void addPhotoRoutes(String photoName, List<BaseRoute<?, ?>> routes) {
        for (BaseRoute<?, ?> route : routes)
            for (Object position : route.getPositions())
                if (position instanceof PhotoPosition photo) {
                    archivePhotos.add(photo);
                    if (photo.hasCoordinates())
                        continue;
                    unplacedPhotos.add(photo);
                    for (String key : candidateKeys(photoName)) {
                        Sidecar sidecar = sidecars.get(key);
                        if (sidecar != null && sidecar.hasCoordinates()) {
                            complete(photo, sidecar);
                            break;
                        }
                        waitingPhotos.computeIfAbsent(key, k -> new ArrayList<>()).add(photo);
                    }
                }
    }

    void removePhotosWithoutCoordinates(ParserContext<BaseRoute<?, ?>> context) {
        List<BaseRoute<?, ?>> routes = context.getRoutes();
        List<BaseRoute<?, ?>> kept = new ArrayList<>();
        for (BaseRoute<?, ?> route : routes)
            if (!isPhotoRouteWithoutCoordinates(route))
                kept.add(route);
        if (kept.size() == routes.size())
            return;
        context.removeRoutes();
        context.appendRoutes(kept);
    }

    private boolean isPhotoRouteWithoutCoordinates(BaseRoute<?, ?> route) {
        List<?> positions = route.getPositions();
        if (positions.isEmpty())
            return false;
        for (Object position : positions)
            if (!(position instanceof PhotoPosition photo) || photo.hasCoordinates() || !archivePhotos.contains(photo))
                return false;
        return true;
    }

    // for a photo without EXIF coordinates, the sidecar's location, time and title count
    private void complete(PhotoPosition photo, Sidecar sidecar) {
        if (!sidecar.hasCoordinates() || !unplacedPhotos.contains(photo))
            return;
        photo.setLongitude(sidecar.longitude);
        photo.setLatitude(sidecar.latitude);
        if (sidecar.time != null)
            photo.setTime(sidecar.time);
        if (sidecar.title != null)
            photo.setDescription(sidecar.title);
        unplacedPhotos.remove(photo);
    }

    private static Sidecar toSidecar(JsonNode root) {
        double[] coordinates = coordinates(root.path("geoDataExif"));
        if (coordinates == null)
            coordinates = coordinates(root.path("geoData"));
        CompactCalendar time = time(root.path("photoTakenTime"));
        if (time == null)
            time = time(root.path("creationTime"));
        return new Sidecar(coordinates != null ? coordinates[0] : null, coordinates != null ? coordinates[1] : null,
                time, trim(root.path("title").asText(null)));
    }

    private static double[] coordinates(JsonNode geoData) {
        JsonNode latitudeNode = geoData.path("latitude"), longitudeNode = geoData.path("longitude");
        if (!latitudeNode.isNumber() || !longitudeNode.isNumber())
            return null;
        double latitude = latitudeNode.asDouble(), longitude = longitudeNode.asDouble();
        // Google writes 0.0/0.0 for "no location"
        if (latitude == 0.0 && longitude == 0.0)
            return null;
        if (Math.abs(latitude) > 90.0 || Math.abs(longitude) > 180.0)
            return null;
        return new double[]{longitude, latitude};
    }

    private static CompactCalendar time(JsonNode timeNode) {
        String timestamp = trim(timeNode.path("timestamp").asText(null));
        if (timestamp == null)
            return null;
        try {
            return fromMillis(Long.parseLong(timestamp) * 1000);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
