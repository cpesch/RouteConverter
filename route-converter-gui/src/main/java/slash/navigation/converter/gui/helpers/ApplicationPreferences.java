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
package slash.navigation.converter.gui.helpers;

import slash.navigation.common.NumberPattern;
import slash.navigation.common.NumberingStrategy;
import slash.navigation.rest.Credentials;

import java.io.File;
import java.util.prefs.Preferences;

import static java.nio.charset.StandardCharsets.UTF_8;
import static slash.common.io.Files.findExistingPath;
import static slash.navigation.common.NumberPattern.Number_Space_Then_Description;
import static slash.navigation.common.NumberingStrategy.Absolute_Position_Within_Position_List;
import static slash.navigation.converter.gui.helpers.TagStrategy.Create_Backup_In_Subdirectory;

/**
 * Owns the flat, directly {@link Preferences}-backed getters and setters that
 * used to live on {@code BaseRouteConverter}: selection thresholds, the
 * last-used paths for photo/audio/upload, login credentials and a handful of
 * enum-valued preferences (issue #214). Holds only the preference key
 * constants and the {@code Preferences} node passed in by the caller -- no
 * application state, no locking; every method here does exactly what the
 * matching {@code BaseRouteConverter} accessor did before the move.
 * <p>
 * The four constants that stay {@code public} ({@link #SHOW_ALL_POSITIONS_AFTER_LOADING_PREFERENCE},
 * {@link #RECENTER_AFTER_ZOOMING_PREFERENCE}, {@link #TIME_ZONE_PREFERENCE},
 * {@link #PHOTO_TIMEZONE_PREFERENCE}) are needed by {@code BaseRouteConverter}
 * itself to construct its {@code BooleanModel}/{@code TimeZoneModel} fields,
 * which read and write the preference directly rather than through this class.
 */
public class ApplicationPreferences {
    public static final String SHOW_ALL_POSITIONS_AFTER_LOADING_PREFERENCE = "showAllPositionsAfterLoading";
    public static final String RECENTER_AFTER_ZOOMING_PREFERENCE = "recenterAfterZooming";
    public static final String TIME_ZONE_PREFERENCE = "timeZone";
    public static final String PHOTO_TIMEZONE_PREFERENCE = "photoTimeZone";

    private static final String MAP_VIEW_PREFERENCE = "mapView";
    private static final String NUMBER_PATTERN_PREFERENCE = "numberPattern";
    private static final String NUMBERING_STRATEGY_PREFERENCE = "numberingStrategy";
    private static final String SELECT_BY_DISTANCE_PREFERENCE = "selectByDistance";
    private static final String SELECT_BY_ORDER_PREFERENCE = "selectByOrder";
    private static final String SELECT_BY_SIGNIFICANCE_PREFERENCE = "selectBySignificance";
    private static final String DELETE_POSITIONS_SHIFT_TIMES_PREFERENCE = "deletePositionsShiftTimes";
    private static final String SELECT_BY_SPEED_PREFERENCE = "selectBySpeed";
    private static final String INSERT_STRAIGHT_LINE_INTERVAL_PREFERENCE = "insertStraightLineInterval";
    private static final String FIND_PLACE_PREFERENCE = "findPlace";
    private static final String TAG_STRATEGY_PREFERENCE = "tagStrategy";
    private static final String USERNAME_PREFERENCE = "userName";
    private static final String PASSWORD_PREFERENCE = "userAuthentication";
    private static final String CATEGORY_PREFERENCE = "category";
    private static final String ADD_PHOTO_PREFERENCE = "addPhoto";
    private static final String ADD_AUDIO_PREFERENCE = "addAudio";
    private static final String UPLOAD_ROUTE_PREFERENCE = "uploadRoute";

    private final Preferences preferences;

    public ApplicationPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public double getSelectByDistancePreference() {
        return preferences.getDouble(SELECT_BY_DISTANCE_PREFERENCE, 1000);
    }

    public void setSelectByDistancePreference(double selectByDistancePreference) {
        preferences.putDouble(SELECT_BY_DISTANCE_PREFERENCE, selectByDistancePreference);
    }

    public int getSelectByOrderPreference() {
        return preferences.getInt(SELECT_BY_ORDER_PREFERENCE, 5);
    }

    public void setSelectByOrderPreference(int selectByOrderPreference) {
        preferences.putInt(SELECT_BY_ORDER_PREFERENCE, selectByOrderPreference);
    }

    public int getInsertStraightLineIntervalPreference() {
        return preferences.getInt(INSERT_STRAIGHT_LINE_INTERVAL_PREFERENCE, 100);
    }

    public void setInsertStraightLineIntervalPreference(int insertStraightLineIntervalPreference) {
        preferences.putInt(INSERT_STRAIGHT_LINE_INTERVAL_PREFERENCE, insertStraightLineIntervalPreference);
    }

    public double getSelectBySignificancePreference() {
        return preferences.getDouble(SELECT_BY_SIGNIFICANCE_PREFERENCE, 20);
    }

    public void setSelectBySignificancePreference(double selectBySignificancePreference) {
        preferences.putDouble(SELECT_BY_SIGNIFICANCE_PREFERENCE, selectBySignificancePreference);
    }

    public boolean getDeletePositionsShiftTimesPreference() {
        return preferences.getBoolean(DELETE_POSITIONS_SHIFT_TIMES_PREFERENCE, false);
    }

    public void setDeletePositionsShiftTimesPreference(boolean deletePositionsShiftTimesPreference) {
        preferences.putBoolean(DELETE_POSITIONS_SHIFT_TIMES_PREFERENCE, deletePositionsShiftTimesPreference);
    }

    public double getSelectBySpeedPreference() {
        return preferences.getDouble(SELECT_BY_SPEED_PREFERENCE, 3.0);
    }

    public void setSelectBySpeedPreference(double selectBySpeedPreference) {
        preferences.putDouble(SELECT_BY_SPEED_PREFERENCE, selectBySpeedPreference);
    }

    public String getFindPlacePreference() {
        return preferences.get(FIND_PLACE_PREFERENCE, "");
    }

    public void setFindPlacePreference(String searchPositionPreference) {
        preferences.put(FIND_PLACE_PREFERENCE, searchPositionPreference);
    }

    /**
     * Important: returns a {@link Credentials} that reads the current preference
     * values lazily, since the instance is handed to the RemoteCatalog and may
     * outlive a login/logout.
     */
    public Credentials getCredentials() {
        return new Credentials() {
            public String userName() {
                return preferences.get(USERNAME_PREFERENCE, null);
            }

            public char[] password() {
                byte[] byteArray = preferences.getByteArray(PASSWORD_PREFERENCE, null);
                return byteArray != null ? new String(byteArray, UTF_8).toCharArray() : null;
            }
        };
    }

    public String getUserNamePreference() {
        return preferences.get(USERNAME_PREFERENCE, null);
    }

    public void setCredentials(String userName, String password) {
        preferences.put(USERNAME_PREFERENCE, userName);
        preferences.putByteArray(PASSWORD_PREFERENCE, password.getBytes(UTF_8));
    }

    public void removeCredentials() {
        preferences.remove(USERNAME_PREFERENCE);
        preferences.remove(PASSWORD_PREFERENCE);
    }

    public File getUploadRoutePreference() {
        File path = new File(preferences.get(UPLOAD_ROUTE_PREFERENCE, ""));
        return findExistingPath(path);
    }

    public void setUploadRoutePreference(File path) {
        preferences.put(UPLOAD_ROUTE_PREFERENCE, path.getPath());
    }

    public File getAddPhotoPreference() { // for TimeAlbum
        File path = new File(preferences.get(ADD_PHOTO_PREFERENCE, ""));
        return findExistingPath(path);
    }

    public void setAddPhotoPreference(File path) { // for TimeAlbum
        preferences.put(ADD_PHOTO_PREFERENCE, path.getPath());
    }

    public File getAddAudioPreference() { // for TimeAlbum
        File path = new File(preferences.get(ADD_AUDIO_PREFERENCE, ""));
        return findExistingPath(path);
    }

    public void setAddAudioPreference(File path) { // for TimeAlbum
        preferences.put(ADD_AUDIO_PREFERENCE, path.getPath());
    }

    public TagStrategy getTagStrategyPreference() { // for TimeAlbum
        try {
            return TagStrategy.valueOf(preferences.get(TAG_STRATEGY_PREFERENCE, Create_Backup_In_Subdirectory.toString()));
        } catch (IllegalArgumentException e) {
            return Create_Backup_In_Subdirectory;
        }
    }

    public void setTagStrategyPreference(TagStrategy tagStrategy) { // for TimeAlbum
        preferences.put(TAG_STRATEGY_PREFERENCE, tagStrategy.toString());
    }

    public String getCategoryPreference() {
        return preferences.get(CATEGORY_PREFERENCE, "");
    }

    public void setCategoryPreference(String category) {
        preferences.put(CATEGORY_PREFERENCE, category);
    }

    public NumberPattern getNumberPatternPreference() {
        try {
            return NumberPattern.valueOf(preferences.get(NUMBER_PATTERN_PREFERENCE, Number_Space_Then_Description.toString()));
        } catch (IllegalArgumentException e) {
            return Number_Space_Then_Description;
        }
    }

    public void setNumberPatternPreference(NumberPattern numberPattern) {
        preferences.put(NUMBER_PATTERN_PREFERENCE, numberPattern.toString());
    }

    public NumberingStrategy getNumberingStrategyPreference() {
        try {
            return NumberingStrategy.valueOf(
                    preferences.get(NUMBERING_STRATEGY_PREFERENCE, Absolute_Position_Within_Position_List.toString()));
        } catch (IllegalArgumentException e) {
            return Absolute_Position_Within_Position_List;
        }
    }

    public void setNumberingStrategyPreference(NumberingStrategy numberingStrategy) {
        preferences.put(NUMBERING_STRATEGY_PREFERENCE, numberingStrategy.toString());
    }

    public String getMapViewPreference(String defaultMapViewName) {
        return preferences.get(MAP_VIEW_PREFERENCE, defaultMapViewName);
    }

    public void setMapViewPreference(String mapViewName) {
        preferences.put(MAP_VIEW_PREFERENCE, mapViewName);
    }
}
