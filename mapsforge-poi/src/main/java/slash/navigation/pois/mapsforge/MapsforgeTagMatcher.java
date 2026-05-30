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
package slash.navigation.pois.mapsforge;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import slash.navigation.geocoding.CategorizedNavigationPosition;
import slash.navigation.geocoding.SimpleCategorizedNavigationPosition;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.lang.Math.max;
import static slash.navigation.pois.mapsforge.MapsforgeGeocodingHelper.normalize;

/**
 * Matches Mapsforge tags and builds display descriptions for geocoding results.
 *
 * @author Christian Pesch
 */
final class MapsforgeTagMatcher {
    private static final Set<String> CATEGORY_TAGS = Set.of(
            "aeroway", "amenity", "building", "craft", "emergency", "historic", "highway", "landuse",
            "leisure", "man_made", "natural", "office", "place", "railway", "shop", "sport",
            "tourism", "waterway"
    );

    private MapsforgeTagMatcher() {
    }

    static MapsforgeTagMatcher.Match findMatch(List<Tag> tags, Collection<String> categories, String query, boolean exactOnly) {
        if (query == null || query.isEmpty())
            return null;

        MapsforgeTagMatcher.Match best = null;
        if (tags != null) {
            for (Tag tag : tags) {
                if (!isRelevantSearchTag(tag))
                    continue;
                String normalizedValue = normalize(tag.value);
                if (doesNotMatch(normalizedValue, query, exactOnly))
                    continue;
                int score = score(tag, query, normalizedValue);
                if (best == null || score < best.score())
                    best = new MapsforgeTagMatcher.Match(tag, tag.value, score);
            }
        }
        if (categories != null) {
            for (String category : categories) {
                String normalizedValue = normalize(category);
                if (doesNotMatch(normalizedValue, query, exactOnly))
                    continue;
                int score = normalizedValue.equals(query) ? 2 : 12 + max(0, normalizedValue.length() - query.length());
                if (best == null || score < best.score())
                    best = new MapsforgeTagMatcher.Match(null, category, score);
            }
        }
        return best;
    }

    static boolean hasUsefulDescription(List<Tag> tags, Collection<String> categories) {
        return firstNameVariant(tags) != null || firstCategoryValue(tags, categories) != null;
    }

    static CategorizedNavigationPosition buildDescriptionAndCategory(LatLong position, List<Tag> tags, Collection<String> categories,
                                                                    MapsforgeTagMatcher.Match match, String unnamedFallback) {
        String matchedName = match != null && match.tag() != null && isNameTag(match.tag()) ? match.tag().value : null;
        String primaryName = firstNameValue(tags);
        if (primaryName == null)
            primaryName = firstNameVariant(tags);

        String label = matchedName != null ? matchedName : primaryName;
        if (label == null && match != null)
            label = match.value();
        if (label == null)
            label = firstCategoryValue(tags, categories);
        if (label == null)
            label = unnamedFallback;

        String category = firstCategoryValue(tags, categories);
        if (category != null && category.equalsIgnoreCase(label))
            category = null;
        Double longitude = position != null ? position.longitude : null;
        Double latitude = position != null ? position.latitude : null;
        return new SimpleCategorizedNavigationPosition(longitude, latitude, null, label, category);
    }

    static String buildDescription(List<Tag> tags, Collection<String> categories, MapsforgeTagMatcher.Match match, String unnamedFallback) {
        CategorizedNavigationPosition position = buildDescriptionAndCategory(null, tags, categories, match, unnamedFallback);
        return buildDescription(position.getDescription(), position.getCategory());
    }

    static String buildDescription(String description, String category) {
        if (category != null)
            return description + " (" + category + ")";
        return description;
    }

    private static boolean doesNotMatch(String value, String query, boolean exactOnly) {
        if (value == null || value.isEmpty())
            return true;
        return exactOnly ? !value.equals(query) : !value.contains(query);
    }

    private static boolean isRelevantSearchTag(Tag tag) {
        if (tag == null || tag.key == null || tag.value == null)
            return false;
        if (tag.key.startsWith("addr:"))
            return false;
        return isNameTag(tag) || "normalized_name".equals(tag.key) || CATEGORY_TAGS.contains(tag.key);
    }

    private static boolean isNameTag(Tag tag) {
        return "name".equals(tag.key) || (tag.key != null && tag.key.startsWith("name:"));
    }

    private static int score(Tag tag, String query, String normalizedValue) {
        int score = normalizedValue.equals(query) ? 0 : 10;
        if (isNameTag(tag))
            score -= 5;
        else if ("normalized_name".equals(tag.key))
            score -= 3;
        return score + max(0, normalizedValue.length() - query.length());
    }

    private static String firstNameValue(List<Tag> tags) {
        if (tags == null)
            return null;
        for (Tag tag : tags) {
            if ("name".equals(tag.key) && tag.value != null && !tag.value.isBlank())
                return tag.value;
        }
        return null;
    }

    private static String firstNameVariant(List<Tag> tags) {
        if (tags == null)
            return null;
        for (Tag tag : tags) {
            if (isNameTag(tag) && tag.value != null && !tag.value.isBlank())
                return tag.value;
        }
        return null;
    }

    private static String firstCategoryValue(List<Tag> tags, Collection<String> categories) {
        if (tags != null) {
            for (Tag tag : tags) {
                if (CATEGORY_TAGS.contains(tag.key) && tag.value != null && !tag.value.isBlank())
                    return tag.value;
            }
        }
        if (categories != null) {
            for (String category : categories) {
                if (category != null && !category.isBlank())
                    return category;
            }
        }
        return null;
    }

    record Match(Tag tag, String value, int score) {
    }
}

