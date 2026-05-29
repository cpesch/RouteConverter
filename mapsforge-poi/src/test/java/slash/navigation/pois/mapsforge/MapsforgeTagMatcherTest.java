package slash.navigation.pois.mapsforge;

import org.junit.Test;
import org.mapsforge.core.model.Tag;

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

        assertEquals("Tankstelle (fuel)", MapsforgeTagMatcher.buildDescription(tags, List.of("fuel"), match, "Unnamed POI"));
    }

    @Test
    public void buildsDescriptionsFromMatchedNameAndCategory() {
        List<Tag> tags = List.of(new Tag("name", "Prague"), new Tag("name:cs", "Praha"), new Tag("place", "city"));
        MapsforgeTagMatcher.Match match = MapsforgeTagMatcher.findMatch(tags, emptyList(), "praha", true);

        assertEquals("Praha (city)", MapsforgeTagMatcher.buildDescription(tags, emptyList(), match, "Unnamed feature"));
    }

    @Test
    public void usesFallbackForUnnamedFeatures() {
        assertFalse(MapsforgeTagMatcher.hasUsefulDescription(List.of(new Tag("addr:street", "Main Street")), List.of()));
        assertEquals("Unnamed feature", MapsforgeTagMatcher.buildDescription(List.of(), List.of(), null, "Unnamed feature"));
    }
}

