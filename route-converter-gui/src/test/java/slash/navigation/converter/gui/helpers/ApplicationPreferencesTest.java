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

import org.junit.Test;
import slash.common.prefs.InMemoryPreferences;
import slash.navigation.common.NumberPattern;
import slash.navigation.common.NumberingStrategy;
import slash.navigation.rest.Credentials;

import java.io.File;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static slash.navigation.common.NumberPattern.Number_Space_Then_Description;
import static slash.navigation.common.NumberingStrategy.Absolute_Position_Within_Position_List;
import static slash.navigation.converter.gui.helpers.TagStrategy.Create_Backup_In_Subdirectory;
import static slash.navigation.converter.gui.helpers.TagStrategy.Create_Tagged_Photo_In_Subdirectory;

public class ApplicationPreferencesTest {
    private final Preferences preferences = new InMemoryPreferences();
    private final ApplicationPreferences applicationPreferences = new ApplicationPreferences(preferences);

    @Test
    public void selectByDistanceDefaultsAndRoundTrips() {
        assertEquals(1000.0, applicationPreferences.getSelectByDistancePreference(), 0.0);
        applicationPreferences.setSelectByDistancePreference(42.0);
        assertEquals(42.0, applicationPreferences.getSelectByDistancePreference(), 0.0);
    }

    @Test
    public void selectByOrderDefaultsAndRoundTrips() {
        assertEquals(5, applicationPreferences.getSelectByOrderPreference());
        applicationPreferences.setSelectByOrderPreference(7);
        assertEquals(7, applicationPreferences.getSelectByOrderPreference());
    }

    @Test
    public void insertStraightLineIntervalDefaultsAndRoundTrips() {
        assertEquals(100, applicationPreferences.getInsertStraightLineIntervalPreference());
        applicationPreferences.setInsertStraightLineIntervalPreference(250);
        assertEquals(250, applicationPreferences.getInsertStraightLineIntervalPreference());
    }

    @Test
    public void selectBySignificanceDefaultsAndRoundTrips() {
        assertEquals(20.0, applicationPreferences.getSelectBySignificancePreference(), 0.0);
        applicationPreferences.setSelectBySignificancePreference(33.0);
        assertEquals(33.0, applicationPreferences.getSelectBySignificancePreference(), 0.0);
    }

    @Test
    public void deletePositionsShiftTimesDefaultsAndRoundTrips() {
        assertEquals(false, applicationPreferences.getDeletePositionsShiftTimesPreference());
        applicationPreferences.setDeletePositionsShiftTimesPreference(true);
        assertEquals(true, applicationPreferences.getDeletePositionsShiftTimesPreference());
    }

    @Test
    public void selectBySpeedDefaultsAndRoundTrips() {
        assertEquals(3.0, applicationPreferences.getSelectBySpeedPreference(), 0.0);
        applicationPreferences.setSelectBySpeedPreference(5.5);
        assertEquals(5.5, applicationPreferences.getSelectBySpeedPreference(), 0.0);
    }

    @Test
    public void findPlaceDefaultsAndRoundTrips() {
        assertEquals("", applicationPreferences.getFindPlacePreference());
        applicationPreferences.setFindPlacePreference("Berlin");
        assertEquals("Berlin", applicationPreferences.getFindPlacePreference());
    }

    @Test
    public void credentialsRoundTripAndReadCurrentValuesLazily() {
        assertNull(applicationPreferences.getUserNamePreference());
        Credentials credentials = applicationPreferences.getCredentials();
        assertNull(credentials.userName());
        assertNull(credentials.password());

        applicationPreferences.setCredentials("user", "secret");
        assertEquals("user", applicationPreferences.getUserNamePreference());
        // the same Credentials instance reflects the value set afterwards
        assertEquals("user", credentials.userName());
        assertArrayEquals("secret".toCharArray(), credentials.password());

        applicationPreferences.removeCredentials();
        assertNull(applicationPreferences.getUserNamePreference());
        assertNull(credentials.password());
    }

    @Test
    public void uploadRouteRoundTrips() {
        File path = new File("some-upload-path");
        applicationPreferences.setUploadRoutePreference(path);
        assertEquals(path.getPath(), preferences.get("uploadRoute", null));
    }

    @Test
    public void addPhotoAndAddAudioRoundTrip() {
        File photoPath = new File("some-photo-path");
        applicationPreferences.setAddPhotoPreference(photoPath);
        assertEquals(photoPath.getPath(), preferences.get("addPhoto", null));

        File audioPath = new File("some-audio-path");
        applicationPreferences.setAddAudioPreference(audioPath);
        assertEquals(audioPath.getPath(), preferences.get("addAudio", null));
    }

    @Test
    public void tagStrategyDefaultsAndRoundTripsAndFallsBackOnGarbage() {
        assertEquals(Create_Backup_In_Subdirectory, applicationPreferences.getTagStrategyPreference());
        applicationPreferences.setTagStrategyPreference(Create_Tagged_Photo_In_Subdirectory);
        assertEquals(Create_Tagged_Photo_In_Subdirectory, applicationPreferences.getTagStrategyPreference());

        preferences.put("tagStrategy", "not-a-tag-strategy");
        assertEquals(Create_Backup_In_Subdirectory, applicationPreferences.getTagStrategyPreference());
    }

    @Test
    public void categoryDefaultsAndRoundTrips() {
        assertEquals("", applicationPreferences.getCategoryPreference());
        applicationPreferences.setCategoryPreference("hiking");
        assertEquals("hiking", applicationPreferences.getCategoryPreference());
    }

    @Test
    public void numberPatternDefaultsAndRoundTripsAndFallsBackOnGarbage() {
        assertEquals(Number_Space_Then_Description, applicationPreferences.getNumberPatternPreference());
        NumberPattern other = NumberPattern.values()[NumberPattern.values().length - 1];
        applicationPreferences.setNumberPatternPreference(other);
        assertEquals(other, applicationPreferences.getNumberPatternPreference());

        preferences.put("numberPattern", "not-a-number-pattern");
        assertEquals(Number_Space_Then_Description, applicationPreferences.getNumberPatternPreference());
    }

    @Test
    public void numberingStrategyDefaultsAndRoundTripsAndFallsBackOnGarbage() {
        assertEquals(Absolute_Position_Within_Position_List, applicationPreferences.getNumberingStrategyPreference());
        NumberingStrategy other = NumberingStrategy.values()[NumberingStrategy.values().length - 1];
        applicationPreferences.setNumberingStrategyPreference(other);
        assertEquals(other, applicationPreferences.getNumberingStrategyPreference());

        preferences.put("numberingStrategy", "not-a-numbering-strategy");
        assertEquals(Absolute_Position_Within_Position_List, applicationPreferences.getNumberingStrategyPreference());
    }

    @Test
    public void mapViewPreferenceDefaultsAndRoundTrips() {
        assertEquals("default-map-view", applicationPreferences.getMapViewPreference("default-map-view"));
        applicationPreferences.setMapViewPreference("Fx8");
        assertEquals("Fx8", applicationPreferences.getMapViewPreference("default-map-view"));
    }
}
