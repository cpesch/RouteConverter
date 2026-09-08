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

import slash.common.system.Version;

/**
 * Decides how insistently {@link UpdateChecker} should nudge towards a newer
 * RouteConverter version, based on how often the update has already been
 * offered and how far the running version has fallen behind.
 *
 * @author Christian Pesch
 */
public class UpdatePolicy {
    // from the 3rd offer of the same version on, show highlights instead of the one-line message
    static final int HIGHLIGHTS_FROM_OFFER_COUNT = 2;
    // 2 or more stable releases behind: hide the "skip this version" checkbox, keep the dialog dismissible
    static final int NO_SKIP_FROM_RELEASES_BEHIND = 2;
    // below this major version, RouteConverter is considered end of life (bundled Java 8, no map/routing updates)
    static final int END_OF_LIFE_BEFORE_MAJOR = 3;

    public enum Nudge {
        SHORT, HIGHLIGHTS, HIGHLIGHTS_NO_SKIP
    }

    private UpdatePolicy() {
    }

    public static Nudge decide(Version mine, Version latest, int offerCount) {
        if (offerCount < HIGHLIGHTS_FROM_OFFER_COUNT)
            return Nudge.SHORT;
        return releasesBehind(mine, latest) >= NO_SKIP_FROM_RELEASES_BEHIND ? Nudge.HIGHLIGHTS_NO_SKIP : Nudge.HIGHLIGHTS;
    }

    public static boolean isEndOfLife(Version mine) {
        return major(mine) < END_OF_LIFE_BEFORE_MAJOR;
    }

    static int releasesBehind(Version mine, Version latest) {
        return ordinal(latest) - ordinal(mine);
    }

    private static int ordinal(Version version) {
        return major(version) * 100 + minor(version);
    }

    private static int major(Version version) {
        return part(version, 0);
    }

    private static int minor(Version version) {
        return part(version, 1);
    }

    private static int part(Version version, int index) {
        String[] parts = version.getVersion().split("\\.");
        if (index >= parts.length)
            return 0;
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
