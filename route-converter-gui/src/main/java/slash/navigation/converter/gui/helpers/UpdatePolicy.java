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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides how insistently {@link UpdateChecker} should nudge towards a newer
 * RouteConverter version, based on how far the running version has fallen
 * behind.
 *
 * @author Christian Pesch
 */
public class UpdatePolicy {
    // 2 or more releases behind: hide the "skip this version" checkbox, keep the dialog dismissible
    static final int NO_SKIP_FROM_RELEASES_BEHIND = 2;
    // below this major version, RouteConverter is considered end of life (bundled Java 8, no map/routing updates)
    static final int END_OF_LIFE_BEFORE_MAJOR = 3;
    private static final Pattern LEADING_NUMBER = Pattern.compile("\\d+");

    public enum Nudge {
        HIGHLIGHTS, HIGHLIGHTS_NO_SKIP
    }

    private UpdatePolicy() {
    }

    /**
     * Every offer shows the release highlights; how far the running version has fallen behind
     * only decides whether the "skip this version" checkbox is still offered.
     */
    public static Nudge decide(Version mine, Version latest) {
        return releasesBehind(mine, latest) >= NO_SKIP_FROM_RELEASES_BEHIND ? Nudge.HIGHLIGHTS_NO_SKIP : Nudge.HIGHLIGHTS;
    }

    /**
     * Whether the running version belongs to a generation that no longer receives map and
     * routing updates. The gate is the RouteConverter major version alone - the Java version
     * is not inspected, because every 2.x release bundled Java 8.
     */
    public static boolean isEndOfLife(Version mine) {
        return major(mine) < END_OF_LIFE_BEFORE_MAJOR;
    }

    /**
     * How many releases apart two versions are, as the distance between their major.minor
     * numbers. A pre-release suffix is ignored, so a development build of 3.7 counts as 3.7
     * and not, as a plain parse would have it, as 3.0.
     */
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
        // "7-SNAPSHOT", "7rc1": count the leading number, a plain parse would yield 0
        Matcher matcher = LEADING_NUMBER.matcher(parts[index]);
        return matcher.lookingAt() ? Integer.parseInt(matcher.group()) : 0;
    }
}
