package slash.navigation.pois.mapsforge;

import org.junit.Test;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import slash.navigation.geocoding.CategorizedNavigationPosition;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static slash.navigation.pois.mapsforge.MapsforgeGeocodingHelper.normalize;

public class MapsforgeTagMatcherTest {
    @Test
    public void normalizesQueryText() {
        assertEquals("", normalize(null));
        assertEquals("", normalize("   "));
        assertEquals("new york", normalize(" New_York "));
    }

    @Test
    public void matchesNameVariantsNormalizedNamesAndCategoriesButIgnoresAddresses() {
        assertNotNull(MapsforgeTagMatcher.findMatch(List.of(new Tag("name", "Prague")), emptyList(), "prague", true));
        assertNotNull(MapsforgeTagMatcher.findMatch(List.of(new Tag("name:cs", "Praha")), emptyList(), "praha", true));
        assertNotNull(MapsforgeTagMatcher.findMatch(List.of(new Tag("normalized_name", "praha")), emptyList(), "praha", true));
        assertNotNull(MapsforgeTagMatcher.findMatch(List.of(new Tag("amenity", "fuel")), emptyList(), "fuel", true));
        assertNull(MapsforgeTagMatcher.findMatch(List.of(new Tag("addr:street", "Main Street")), emptyList(), "main street", true));
    }

    @Test
    public void prefersNamesOverCategoriesWhenBuildingDescription() {
        List<Tag> tags = List.of(new Tag("name", "Tankstelle"));
        MapsforgeTagMatcher.Match match = MapsforgeTagMatcher.findMatch(tags, List.of("fuel"), "fuel", true);
        LatLong point = new LatLong(52.5, 13.4);
        CategorizedNavigationPosition position =
                MapsforgeTagMatcher.buildDescriptionAndCategory(point, tags, List.of("fuel"), match, "Unnamed POI");

        assertEquals(13.4, position.getLongitude(), 0.0);
        assertEquals(52.5, position.getLatitude(), 0.0);
        assertEquals("Tankstelle", position.getDescription());
        assertEquals("fuel", position.getCategory());
        assertEquals("Tankstelle (fuel)", MapsforgeTagMatcher.buildDescription(tags, List.of("fuel"), match, "Unnamed POI"));
    }

    @Test
    public void buildsDescriptionsFromMatchedNameAndCategory() {
        List<Tag> tags = List.of(new Tag("name", "Prague"), new Tag("name:cs", "Praha"), new Tag("place", "city"));
        MapsforgeTagMatcher.Match match = MapsforgeTagMatcher.findMatch(tags, emptyList(), "praha", true);
        LatLong point = new LatLong(50.087, 14.421);
        CategorizedNavigationPosition position =
                MapsforgeTagMatcher.buildDescriptionAndCategory(point, tags, emptyList(), match, "Unnamed feature");

        assertEquals(14.421, position.getLongitude(), 0.0);
        assertEquals(50.087, position.getLatitude(), 0.0);
        assertEquals("Praha", position.getDescription());
        assertEquals("city", position.getCategory());
        assertEquals("Praha (city)", MapsforgeTagMatcher.buildDescription(tags, emptyList(), match, "Unnamed feature"));
    }

    @Test
    public void omitsDuplicateCategoryWhenItMatchesTheDescription() {
        List<Tag> tags = List.of(new Tag("amenity", "fuel"));
        MapsforgeTagMatcher.Match match = MapsforgeTagMatcher.findMatch(tags, emptyList(), "fuel", true);
        LatLong point = new LatLong(48.137, 11.575);
        CategorizedNavigationPosition position =
                MapsforgeTagMatcher.buildDescriptionAndCategory(point, tags, emptyList(), match, "Unnamed feature");

        assertEquals(11.575, position.getLongitude(), 0.0);
        assertEquals(48.137, position.getLatitude(), 0.0);
        assertEquals("fuel", position.getDescription());
        assertNull(position.getCategory());
        assertEquals("fuel", MapsforgeTagMatcher.buildDescription(tags, emptyList(), match, "Unnamed feature"));
    }

    @Test
    public void usesFallbackForUnnamedFeatures() {
        assertFalse(MapsforgeTagMatcher.hasUsefulDescription(List.of(new Tag("addr:street", "Main Street")), List.of()));
        assertEquals("Unnamed feature", MapsforgeTagMatcher.buildDescription(List.of(), List.of(), null, "Unnamed feature"));
    }
}

