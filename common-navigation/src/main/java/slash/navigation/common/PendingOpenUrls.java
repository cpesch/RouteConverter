/*
    This file is part of BaseRouteConverter.

    BaseRouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    BaseRouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with BaseRouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.common;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Buffers files/URLs delivered by the operating system (e.g. a macOS {@code odoc}
 * Apple Event on a cold launch) before the UI is ready to open them, then merges
 * them with the URLs passed as command line arguments once the UI becomes ready.
 *
 * @author Christian Pesch
 */

public class PendingOpenUrls {
    private final List<URL> buffered = new ArrayList<>();
    private boolean released = false;

    /**
     * @return {@code true} if the urls were buffered since the UI is not yet ready;
     * {@code false} if the caller should open them immediately since this instance
     * has already been {@link #release released}
     */
    public synchronized boolean addOrDefer(List<URL> urls) {
        if (released)
            return false;
        buffered.addAll(urls);
        return true;
    }

    /**
     * Marks this instance as ready and returns the merged list of URLs to open:
     * the given {@code initialArgs} first, then any buffered URLs, deduped by URL
     * with order preserved. After this call {@link #addOrDefer} always returns
     * {@code false}.
     */
    public synchronized List<URL> release(List<URL> initialArgs) {
        released = true;
        // Note: URL#equals()/hashCode() perform host resolution (a blocking DNS call) for
        // URLs with a non-empty host. Harmless here since only file:// URLs with an empty
        // host flow through this method; if this class is ever used for remote URLs,
        // dedupe by URI (or String) instead to avoid a network call on the EDT.
        LinkedHashSet<URL> merged = new LinkedHashSet<>(initialArgs);
        merged.addAll(buffered);
        buffered.clear();
        return new ArrayList<>(merged);
    }
}
