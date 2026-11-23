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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Pairs the photos of a ZIP archive with the JSON sidecars Google Takeout writes next to them
 * ({@code IMG_1.jpg} and {@code IMG_1.jpg.json}): Google strips the EXIF GPS data from most
 * photos and keeps the location in the sidecar's {@code geoDataExif} or {@code geoData}.
 * Photos that end up without coordinates are dropped from the archive's position lists.
 *
 * @author Christian Pesch
 */

class PhotoSidecars {
    private static final Logger log = Logger.getLogger(PhotoSidecars.class.getName());
    private static final String[] PHOTO_EXTENSIONS = {".jpg", ".jpeg", ".png", ".heic", ".tif", ".tiff"};
    private static final String SIDECAR_EXTENSION = ".json";
    private static final String SUPPLEMENTAL_METADATA = ".supplemental-metadata";
    private static final String NO_EXIF_DATA = "No EXIF data";

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
    }

    private final Map<String, Sidecar> sidecars = new HashMap<>();
    private final Map<String, PhotoPosition> photos = new HashMap<>();

    static boolean isPhoto(String entryName) {
        String lowerCase = entryName.toLowerCase(Locale.ROOT);
        for (String extension : PHOTO_EXTENSIONS)
            if (lowerCase.endsWith(extension))
                return true;
        return false;
    }

    /**
     * @return the name of the photo the given entry is the sidecar of, or null
     */
    static String photoNameOfSidecar(String entryName) {
        if (!entryName.toLowerCase(Locale.ROOT).endsWith(SIDECAR_EXTENSION))
            return null;
        String photoName = entryName.substring(0, entryName.length() - SIDECAR_EXTENSION.length());
        if (photoName.endsWith(SUPPLEMENTAL_METADATA))
            photoName = photoName.substring(0, photoName.length() - SUPPLEMENTAL_METADATA.length());
        return isPhoto(photoName) ? photoName : null;
    }

    void addSidecar(String photoName, byte[] json) {
        Sidecar sidecar;
        try {
            sidecar = parseSidecar(new ObjectMapper().readTree(json));
        } catch (IOException e) {
            log.fine("Cannot read photo sidecar of " + photoName + ": " + e);
            return;
        }
        sidecars.put(photoName, sidecar);
        PhotoPosition photo = photos.get(photoName);
        if (photo != null)
            complete(photo, sidecar);
    }

    void addPhotoRoutes(String photoName, List<BaseRoute<?, ?>> routes) {
        for (BaseRoute<?, ?> route : routes)
            for (Object position : route.getPositions())
                if (position instanceof PhotoPosition photo) {
                    photos.put(photoName, photo);
                    Sidecar sidecar = sidecars.get(photoName);
                    if (sidecar != null)
                        complete(photo, sidecar);
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
            if (!(position instanceof PhotoPosition photo) || photo.hasCoordinates() || !photos.containsValue(photo))
                return false;
        return true;
    }

    private static void complete(PhotoPosition photo, Sidecar sidecar) {
        if (photo.hasCoordinates() || sidecar.longitude == null || sidecar.latitude == null)
            return;
        photo.setLongitude(sidecar.longitude);
        photo.setLatitude(sidecar.latitude);
        // without EXIF GPS the photo's time is the start date of the archive, not when it was taken
        if (sidecar.time != null)
            photo.setTime(sidecar.time);
        if (sidecar.title != null && (photo.getDescription() == null || NO_EXIF_DATA.equals(photo.getDescription())))
            photo.setDescription(sidecar.title);
    }

    private static Sidecar parseSidecar(JsonNode root) {
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
