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

package slash.navigation.converter.gui.models;

import slash.common.io.Files;

import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.Math.min;

/**
 * Collects the last opened URLs.
 *
 * @author Christian Pesch
 */

public class RecentUrlsModel {
    private static final Logger log = Logger.getLogger(RecentUrlsModel.class.getName());
    private static final String RECENT_URLS_PREFERENCE = "recentUrls";
    private static final String RECENT_URL_PREFERENCE = "recentUrl";
    private static final String MAXIMUM_RECENT_URL_COUNT_PREFERENCE = "maximumRecentUrlCount";
    private static final char FIRST_CHAR = 'a';

    private final Preferences preferences;
    private EventListenerList listenerList = new EventListenerList();

    public RecentUrlsModel() {
        this(Preferences.userNodeForPackage(RecentUrlsModel.class));
    }

    public RecentUrlsModel(Preferences preferences) {
        this.preferences = preferences;
    }

    private int getMaximumCount() {
        return preferences.getInt(MAXIMUM_RECENT_URL_COUNT_PREFERENCE, 10);
    }

    private char getNextCharacter(String recentUrls) {
        char found = 0;
        for(char c : recentUrls.toCharArray()) {
            if (c > found)
                found = c;
        }
        found++;
        if (found < FIRST_CHAR)
            found = FIRST_CHAR;
        if (found >= FIRST_CHAR + getMaximumCount())
            found = recentUrls.charAt(0);
        return found;
    }

    private Character findCharForUrl(String recentUrls, String url) {
        for(char c : recentUrls.toCharArray()) {
            String found = preferences.get(RECENT_URL_PREFERENCE + c, null);
            if (found != null && found.equals(url)) {
                return c;
            }
        }
        return null;
    }

    public void addUrl(URL url) {
        String recentUrls = preferences.get(RECENT_URLS_PREFERENCE, "");
        Character character = findCharForUrl(recentUrls, url.toExternalForm());
        if (character != null) {
            recentUrls = recentUrls.replaceAll(character.toString(), "");
        } else {
            character = getNextCharacter(recentUrls);
            preferences.put(RECENT_URL_PREFERENCE + character, url.toExternalForm());
        }
        recentUrls = recentUrls + character;
        if (recentUrls.length() > getMaximumCount())
            recentUrls = recentUrls.substring(recentUrls.length() - getMaximumCount());
        preferences.put(RECENT_URLS_PREFERENCE, recentUrls);
        fireChanged();
    }

    public List<URL> getUrls() {
        List<URL> result = new ArrayList<>();
        String recentUrls = preferences.get(RECENT_URLS_PREFERENCE, "");
        for(char c : recentUrls.toCharArray()) {
            String urlString = preferences.get(RECENT_URL_PREFERENCE + c, null);
            if (urlString != null) {
                try {
                    URL url = new URL(urlString);
                    File file = Files.toFile(url);
                    if (file == null || file.exists())
                        result.add(0, url);
                } catch (MalformedURLException e) {
                    log.warning("Recent URL '" + urlString + "' is malformed: " + e);
                }
            }
        }
        return result.subList(0, min(result.size(), getMaximumCount()));
    }

    public void removeAllUrls() {
        for (char c = FIRST_CHAR; c < FIRST_CHAR + getMaximumCount(); c++)
            preferences.remove(RECENT_URL_PREFERENCE + c);
        preferences.remove(RECENT_URLS_PREFERENCE);
    }

    protected void fireChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(null);
            }
        }
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }
}
