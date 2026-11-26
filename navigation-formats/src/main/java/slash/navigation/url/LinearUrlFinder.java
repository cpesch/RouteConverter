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

package slash.navigation.url;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.min;

/**
 * Finds the tail of a URL in a text in time linear to the length of the text.
 * <p>
 * It replaces a regular expression of the form
 * <code>.*http[s]?://.+MIDDLE.{gapMin,}MARKER([^\s]+).*</code> that has to be matched against the
 * complete text: the three greedy <code>.*</code>/<code>.+</code> make the regular expression
 * backtrack cubic in the length of a text with many <code>http://</code> and MIDDLE occurrences,
 * which took seconds for a 1 MB HTML page that is probed by all URL formats.
 * <p>
 * The regular expression chooses the last MARKER that is followed by a non white space character and
 * can be reached from some <code>http://</code> or <code>https://</code> followed by at least one character,
 * MIDDLE, and at least gapMin characters. The result is the run of non white space characters after it.
 * The <code>.</code> of the regular expression does not match line terminators, therefore texts that contain
 * some are handed to the regular expression itself.
 *
 * @author Christian Pesch
 */

class LinearUrlFinder {
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";

    private final Pattern pattern;
    private final String middle;
    private final int gapMinimum;
    private final String marker;

    /**
     * @param pattern    the regular expression with the tail of the URL in group 1, the reference semantics
     * @param middle     the literal that follows <code>http[s]?://.+</code>
     * @param gapMinimum the minimum number of characters between middle and marker
     * @param marker     the literal that precedes the tail of the URL
     */
    LinearUrlFinder(Pattern pattern, String middle, int gapMinimum, String marker) {
        this.pattern = pattern;
        this.middle = middle;
        this.gapMinimum = gapMinimum;
        this.marker = marker;
    }

    /**
     * @return the tail of the URL like group 1 of the pattern or null if the pattern does not match
     */
    String find(String text) {
        if (hasLineTerminator(text))
            return findWithPattern(text);

        int markerIndex = lastValidMarker(text);
        if (markerIndex == -1)
            return null;

        int schemeEnd = firstSchemeEnd(text);
        if (schemeEnd == -1)
            return null;

        // the latest middle that still leaves room for the gap before the marker
        int middleIndex = text.lastIndexOf(middle, markerIndex - gapMinimum - middle.length());
        // at least one character between scheme and middle
        if (middleIndex < schemeEnd + 1)
            return null;

        int start = markerIndex + marker.length();
        int end = start;
        while (end < text.length() && !isWhitespace(text.charAt(end)))
            end++;
        return text.substring(start, end);
    }

    private String findWithPattern(String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private int lastValidMarker(String text) {
        int index = text.lastIndexOf(marker);
        while (index != -1) {
            int next = index + marker.length();
            if (next < text.length() && !isWhitespace(text.charAt(next)))
                return index;
            index = index == 0 ? -1 : text.lastIndexOf(marker, index - 1);
        }
        return -1;
    }

    private static int firstSchemeEnd(String text) {
        int http = text.indexOf(HTTP);
        int https = text.indexOf(HTTPS);
        int httpEnd = http == -1 ? -1 : http + HTTP.length();
        int httpsEnd = https == -1 ? -1 : https + HTTPS.length();
        if (httpEnd == -1)
            return httpsEnd;
        return httpsEnd == -1 ? httpEnd : min(httpEnd, httpsEnd);
    }

    // the characters of \s
    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r';
    }

    // the characters . does not match
    private static boolean hasLineTerminator(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r' || c == '\u0085' || c == '\u2028' || c == '\u2029')
                return true;
        }
        return false;
    }
}
