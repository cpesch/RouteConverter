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
package slash.navigation.download.tools.helpers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides anchor filtering makes absolute paths and URLs relative, removes dot slashes at
 * the start and query strings at the end, filters for a set of extensions.
 *
 * @author Christian Pesch
 */

public class AnchorFilter {
    private static final Logger log = Logger.getLogger(AnchorFilter.class.getName());

    public List<String> filterAnchors(String url, List<String> anchors, Set<String> extensions,
                                      Set<String> includes, Set<String> excludes) {
        List<String> result = new ArrayList<>();
        for (String anchor : anchors) {
            try {
                anchor = filterDotSlash(anchor);
                anchor = filterQueryString(anchor);
                anchor = makeAbsolutePathRelative(url, anchor);
                anchor = makeAbsoluteURLRelative(url, anchor);

                if (anchor.equals("index.html") || anchor.startsWith(".."))
                    anchor = "";

                if (anchor.length() > 0 && filterExtension(anchor, extensions) && filterIncludes(anchor, includes) && !filterExcludes(anchor, excludes))
                    result.add(anchor);
            } catch (URISyntaxException e) {
                log.warning("No valid uri: " + e);
            }
        }
        return result;
    }

    private boolean filterExtension(String anchor, Set<String> extensions) {
        if (extensions == null)
            return true;
        for (String extension : extensions)
            if (anchor.endsWith(extension))
                return true;
        return false;
    }

    private boolean filterIncludes(String anchor, Set<String> includes) {
        if (includes == null)
            return true;
        for (String include : includes) {
            Pattern pattern = Pattern.compile(include);
            Matcher matcher = pattern.matcher(anchor);
            if (matcher.matches())
                return true;
        }
        return false;
    }

    private boolean filterExcludes(String anchor, Set<String> excludes) {
        if (excludes == null)
            return false;
        for (String exclude : excludes) {
            Pattern pattern = Pattern.compile(exclude);
            Matcher matcher = pattern.matcher(anchor);
            if (matcher.matches())
                return true;
        }
        return false;
    }

    private String makeAbsoluteURLRelative(String baseUrl, String url) throws URISyntaxException {
        if (url.startsWith(baseUrl))
            url = url.substring(baseUrl.length());
        URI uri = new URI(url);
        if (uri.isAbsolute())
            uri = uri.relativize(new URI(baseUrl));
        if (uri.isAbsolute()) /* since it's absolute but not baseUrl */
            uri = new URI("");
        return uri.toString();
    }

    private String makeAbsolutePathRelative(String baseUrl, String url) throws URISyntaxException {
        if (url.startsWith("/")) {
            URI uri = new URI(url);
            uri = uri.resolve(new URI(baseUrl));
            url = uri.toString();
        }
        return url;
    }

    private String filterQueryString(String anchor) {
        int index = anchor.indexOf('?');
        if (index != -1)
            anchor = anchor.substring(0, index);
        return anchor;
    }

    private String filterDotSlash(String anchor) {
        if (anchor.startsWith("./"))
            anchor = anchor.substring(2);
        return anchor;
    }
}
