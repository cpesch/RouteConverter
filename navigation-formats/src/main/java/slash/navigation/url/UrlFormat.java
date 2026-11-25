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

import slash.navigation.base.BaseUrlFormat;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Route;
import slash.navigation.common.NavigationPosition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static slash.common.io.Transfer.trim;

/**
 * Reads routes from Internet Shortcuts (.url files) by following the URL of the shortcut.
 *
 * Only a file with an <code>[InternetShortcut]</code> section and a <code>URL=</code> line is a shortcut,
 * only <code>http</code> and <code>https</code> are followed. Other text containing URLs, e.g. mails or
 * READMEs, is declined without any network or file access.
 *
 * @author Christian Pesch
 */

public class UrlFormat extends BaseUrlFormat {
    // a shortcut has a handful of lines: do not scan arbitrary data any further
    static final int MAXIMUM_SHORTCUT_SIZE = 16 * 1024;
    private static final String SHORTCUT_SECTION = "[InternetShortcut]";
    private static final Pattern URL_LINE_PATTERN = Pattern.compile("URL\\s*=\\s*(.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOLLOWED_URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);

    public String getExtension() {
        return ".url";
    }

    public String getName() {
        return "URL Reference (" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        throw new UnsupportedOperationException();
    }

    public boolean isSupportsWriting() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        throw new UnsupportedOperationException();
    }

    public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        throw new UnsupportedOperationException();
    }

    public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) throws IOException {
        StringBuilder buffer = new StringBuilder();
        char[] chunk = new char[2048];
        int count;
        while (buffer.length() < MAXIMUM_SHORTCUT_SIZE && (count = reader.read(chunk)) != -1)
            buffer.append(chunk, 0, count);

        String url = findURL(buffer.toString());
        if (url == null)
            throw new IOException(format("Cannot find %s URL in %s", getName(), context.getFile()));
        processURL(url, encoding, context);
    }

    protected String findURL(String text) {
        boolean inShortcutSection = false;
        for (String line : text.split("\\R")) {
            line = trim(line.replace("\uFEFF", ""));
            if (line == null)
                continue;
            if (line.startsWith("[")) {
                inShortcutSection = line.equalsIgnoreCase(SHORTCUT_SECTION);
                continue;
            }
            if (!inShortcutSection)
                continue;
            Matcher urlMatcher = URL_LINE_PATTERN.matcher(line);
            if (urlMatcher.matches()) {
                String url = trim(urlMatcher.group(1));
                return url != null && FOLLOWED_URL_PATTERN.matcher(url).matches() ? url : null;
            }
        }
        return null;
    }

    protected void processURL(String url, String encoding, ParserContext<Wgs84Route> context) throws IOException {
        context.parse(url);
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        throw new UnsupportedOperationException();
    }
}
