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

import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static java.lang.Math.min;

/**
 * Collects the last saved formats.
 *
 * @author Christian Pesch
 */

public class RecentFormatsModel {
    private static final Preferences preferences = Preferences.userNodeForPackage(RecentFormatsModel.class);
    private static final String RECENT_FORMATS_PREFERENCE = "recentFormats";
    private static final String RECENT_FORMAT_PREFERENCE = "recentFormat";
    private static final String MAXIMUM_RECENT_FORMAT_COUNT_PREFERENCE = "maximumRecentFormatCount";
    private static final char FIRST_CHAR = 'a';

    private NavigationFormatRegistry navigationFormatRegistry;

    public RecentFormatsModel(NavigationFormatRegistry navigationFormatRegistry) {
        this.navigationFormatRegistry = navigationFormatRegistry;
    }

    private int getMaximumCount() {
        return preferences.getInt(MAXIMUM_RECENT_FORMAT_COUNT_PREFERENCE, 5);
    }

    private char getNextCharacter(String recentFormats) {
        char found = 0;
        for (char c : recentFormats.toCharArray()) {
            if (c > found)
                found = c;
        }
        found++;
        if (found < FIRST_CHAR)
            found = FIRST_CHAR;
        if (found >= FIRST_CHAR + getMaximumCount())
            found = recentFormats.charAt(0);
        return found;
    }

    private Character findCharForFormat(String recentFormats, String format) {
        for (char c : recentFormats.toCharArray()) {
            String found = preferences.get(RECENT_FORMAT_PREFERENCE + c, null);
            if (found != null && found.equals(format)) {
                return c;
            }
        }
        return null;
    }

    public void addFormat(NavigationFormat format) {
        String recentFormats = preferences.get(RECENT_FORMATS_PREFERENCE, "");
        Character character = findCharForFormat(recentFormats, format.getClass().getName());
        if (character != null) {
            recentFormats = recentFormats.replaceAll(character.toString(), "");
        } else {
            character = getNextCharacter(recentFormats);
            preferences.put(RECENT_FORMAT_PREFERENCE + character, format.getClass().getName());
        }
        recentFormats = recentFormats + character;
        if (recentFormats.length() > getMaximumCount())
            recentFormats = recentFormats.substring(recentFormats.length() - getMaximumCount());
        preferences.put(RECENT_FORMATS_PREFERENCE, recentFormats);
    }

    public List<NavigationFormat> getFormats() {
        List<NavigationFormat> result = new ArrayList<>();
        String recentFormats = preferences.get(RECENT_FORMATS_PREFERENCE, "");
        for (char c : recentFormats.toCharArray()) {
            String formatString = preferences.get(RECENT_FORMAT_PREFERENCE + c, null);
            if (formatString != null) {
                List<NavigationFormat> writeFormats = navigationFormatRegistry.getWriteFormats();
                for (NavigationFormat format : writeFormats) {
                    if (format.getClass().getName().equals(formatString)) {
                        result.add(0, format);
                        break;
                    }
                }
            }
        }
        return result.subList(0, min(result.size(), getMaximumCount()));
    }

    public void removeAllFormats() {
        for (char c = FIRST_CHAR; c < FIRST_CHAR + getMaximumCount(); c++)
            preferences.remove(RECENT_FORMAT_PREFERENCE + c);
        preferences.remove(RECENT_FORMATS_PREFERENCE);
    }
}
