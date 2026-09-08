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

import slash.navigation.rest.Get;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

/**
 * Provides the first bullet points of a RouteConverter release, to show as
 * highlights in the update nudge dialog.
 * <p>
 * The primary source is the live release page on the RouteConverter website
 * (routeconverter.com/de, published independently of any particular client
 * build, so it can describe a version newer than the running one). If that
 * page cannot be reached or parsed, this falls back to the RELEASE_NOTES.md
 * bundled into this jar at build time - which only ever covers versions up
 * to the running client's own build, so it can't describe a future release,
 * but degrades gracefully when offline.
 *
 * @author Christian Pesch
 */
public class ReleaseHighlights {
    private static final Logger log = Logger.getLogger(ReleaseHighlights.class.getName());
    static final int MAX_BULLETS = 5;
    private static final String RESOURCE_NAME = "/RELEASE_NOTES.md";
    // the release page template wraps its content in a single <article class="prose"> with one flat <ul>
    private static final Pattern ARTICLE_PATTERN = Pattern.compile("<article[^>]*class=\"prose\"[^>]*>(.*?)</article>", Pattern.DOTALL);
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("<li[^>]*>(.*?)</li>", Pattern.DOTALL);

    private ReleaseHighlights() {
    }

    public static List<String> first(String version, Locale locale) {
        return first(version, locale, ReleaseHighlights::fetchPage);
    }

    // package-private seam for tests: inject a fake page fetcher instead of hitting the network
    static List<String> first(String version, Locale locale, Function<String, String> pageFetcher) {
        List<String> fromWeb = fromWebPage(version, locale, pageFetcher);
        if (!fromWeb.isEmpty())
            return fromWeb;
        return fromBundledReleaseNotes(version);
    }

    static String releasePageUrl(String version, Locale locale) {
        String host = "de".equalsIgnoreCase(locale.getLanguage()) ? "https://www.routeconverter.de/" : "https://www.routeconverter.com/";
        return host + "releases/" + version.replace('.', '-') + "/";
    }

    private static List<String> fromWebPage(String version, Locale locale, Function<String, String> pageFetcher) {
        try {
            String html = pageFetcher.apply(releasePageUrl(version, locale));
            return html != null ? extractFromHtml(html, MAX_BULLETS) : emptyList();
        } catch (Exception e) {
            log.fine("Cannot fetch release highlights for " + version + ": " + e.getMessage());
            return emptyList();
        }
    }

    private static String fetchPage(String url) {
        try {
            return new Get(url).executeAsString();
        } catch (IOException e) {
            return null;
        }
    }

    static List<String> extractFromHtml(String html, int max) {
        List<String> result = new ArrayList<>();
        Matcher articleMatcher = ARTICLE_PATTERN.matcher(html);
        if (!articleMatcher.find())
            return result;
        Matcher itemMatcher = LIST_ITEM_PATTERN.matcher(articleMatcher.group(1));
        while (itemMatcher.find() && result.size() < max) {
            String item = unescapeHtml(stripTags(itemMatcher.group(1))).trim();
            if (!item.isEmpty())
                result.add(item);
        }
        return result;
    }

    private static String stripTags(String html) {
        return html.replaceAll("<[^>]+>", "");
    }

    private static String unescapeHtml(String text) {
        return text.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'");
    }

    private static List<String> fromBundledReleaseNotes(String version) {
        try (InputStream in = ReleaseHighlights.class.getResourceAsStream(RESOURCE_NAME)) {
            if (in == null)
                return emptyList();
            return extract(readLines(in), version, MAX_BULLETS);
        } catch (IOException e) {
            return emptyList();
        }
    }

    private static List<String> readLines(InputStream in) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null)
                lines.add(line);
        }
        return lines;
    }

    static List<String> extract(List<String> lines, String version, int max) {
        List<String> result = new ArrayList<>();
        String sectionMarker = "## " + version;
        boolean inSection = false;
        for (String line : lines) {
            if (line.startsWith("## ")) {
                if (inSection)
                    break;
                inSection = line.equals(sectionMarker) || line.startsWith(sectionMarker + " ");
                continue;
            }
            if (!inSection)
                continue;
            if (line.startsWith("- ")) {
                result.add(line.substring(2).trim());
                if (result.size() >= max)
                    break;
            }
        }
        return result;
    }
}
