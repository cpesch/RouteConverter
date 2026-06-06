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

package slash.navigation.converter.gui.predicates;

import org.junit.Test;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.WaypointType;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.photo.PhotoPosition;
import slash.navigation.photo.TagState;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.base.WaypointType.Photo;
import static slash.navigation.base.WaypointType.PointOfInterestC;
import static slash.navigation.base.WaypointType.PointOfInterestD;
import static slash.navigation.base.WaypointType.Voice;
import static slash.navigation.base.WaypointType.Waypoint;
import static slash.navigation.photo.TagState.Tagged;
import static slash.navigation.photo.TagState.Taggable;
import static slash.navigation.photo.TagState.NotTaggable;

/**
 * Unit tests for {@link TautologyPredicate}, {@link PointOfInterestPositionPredicate},
 * and {@link TagStatePhotoPredicate}.
 *
 * @author Christian Pesch
 */
public class PredicatesTest {

    // ---- TautologyPredicate ----

    @Test
    public void tautologyAlwaysTrue() {
        TautologyPredicate predicate = new TautologyPredicate("all");
        assertTrue(predicate.shouldInclude(new SimpleNavigationPosition(1.0, 2.0)));
        assertTrue(predicate.shouldInclude(new Wgs84Position(1.0, 2.0, null, null, null, null)));
    }

    @Test
    public void tautologyNameReturnsConstructorArg() {
        assertEquals("myName", new TautologyPredicate("myName").name());
    }

    // ---- PointOfInterestPositionPredicate ----

    private static final PointOfInterestPositionPredicate POI = new PointOfInterestPositionPredicate();

    private static Wgs84Position wgsWithType(WaypointType type) {
        Wgs84Position pos = new Wgs84Position(1.0, 2.0, null, null, null, null);
        pos.setWaypointType(type);
        return pos;
    }

    @Test
    public void poiIncludesPhotoWaypoint() {
        assertTrue(POI.shouldInclude(wgsWithType(Photo)));
    }

    @Test
    public void poiIncludesPointOfInterestC() {
        assertTrue(POI.shouldInclude(wgsWithType(PointOfInterestC)));
    }

    @Test
    public void poiIncludesPointOfInterestD() {
        assertTrue(POI.shouldInclude(wgsWithType(PointOfInterestD)));
    }

    @Test
    public void poiIncludesVoice() {
        assertTrue(POI.shouldInclude(wgsWithType(Voice)));
    }

    @Test
    public void poiExcludesRegularWaypoint() {
        assertFalse(POI.shouldInclude(wgsWithType(Waypoint)));
    }

    @Test
    public void poiExcludesNonWgs84Position() {
        assertFalse(POI.shouldInclude(new SimpleNavigationPosition(1.0, 2.0)));
    }

    @Test
    public void poiNameIsPointOfInterest() {
        assertEquals("PointOfInterest", POI.name());
    }

    // ---- TagStatePhotoPredicate ----

    private static PhotoPosition photoWithState(TagState state) {
        return new PhotoPosition(state, null, "test.jpg", new File("test.jpg"));
    }

    @Test
    public void tagStateMatchesTagged() {
        TagStatePhotoPredicate pred = new TagStatePhotoPredicate(Tagged);
        assertTrue(pred.shouldInclude(photoWithState(Tagged)));
        assertFalse(pred.shouldInclude(photoWithState(Taggable)));
        assertFalse(pred.shouldInclude(photoWithState(NotTaggable)));
    }

    @Test
    public void tagStateMatchesTaggable() {
        TagStatePhotoPredicate pred = new TagStatePhotoPredicate(Taggable);
        assertFalse(pred.shouldInclude(photoWithState(Tagged)));
        assertTrue(pred.shouldInclude(photoWithState(Taggable)));
        assertFalse(pred.shouldInclude(photoWithState(NotTaggable)));
    }

    @Test
    public void tagStateMatchesNotTaggable() {
        TagStatePhotoPredicate pred = new TagStatePhotoPredicate(NotTaggable);
        assertFalse(pred.shouldInclude(photoWithState(Tagged)));
        assertFalse(pred.shouldInclude(photoWithState(Taggable)));
        assertTrue(pred.shouldInclude(photoWithState(NotTaggable)));
    }

    @Test
    public void tagStateExcludesNonPhotoPosition() {
        TagStatePhotoPredicate pred = new TagStatePhotoPredicate(Tagged);
        assertFalse(pred.shouldInclude(new SimpleNavigationPosition(1.0, 2.0)));
        assertFalse(pred.shouldInclude(new Wgs84Position(1.0, 2.0, null, null, null, null)));
    }

    @Test
    public void tagStateNameMatchesTagStateName() {
        assertEquals("Tagged", new TagStatePhotoPredicate(Tagged).name());
        assertEquals("Taggable", new TagStatePhotoPredicate(Taggable).name());
        assertEquals("NotTaggable", new TagStatePhotoPredicate(NotTaggable).name());
    }
}

