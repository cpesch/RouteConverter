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

package slash.navigation.gpx;

import slash.navigation.BaseNavigationPosition;
import slash.navigation.MultipleRoutesFormat;
import slash.navigation.RouteCharacteristics;
import slash.navigation.XmlNavigationFormat;
import slash.navigation.util.Conversion;

import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * The base of all GPS Exchange formats.
 *
 * @author Christian Pesch
 */

public abstract class GpxFormat extends XmlNavigationFormat<GpxRoute> implements MultipleRoutesFormat<GpxRoute> {
    private static final Preferences preferences = Preferences.userNodeForPackage(GpxFormat.class);
    static final Pattern TRIPMASTER_REASON_PATTERN = Pattern.compile("(Punkt|Richtung \\d+|Abstand \\d+|Dur. \\d+:\\d+:\\d+|Course \\d+|Dist. \\d+) (-|:) (.+)");
    static final Pattern TRIPMASTER_DESCRIPTION_PATTERN = Pattern.compile("(.+); (.+)");
    static final Pattern TRIPMASTER_SPEED_PATTERN = Pattern.compile("\\s*([-\\d\\.]+)\\s*(K|k)m/h\\s*");

    public String getExtension() {
        return ".gpx";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public <P extends BaseNavigationPosition> GpxRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new GpxRoute(this, characteristics, name, null, (List<GpxPosition>) positions);
    }

    protected String asDescription(List<String> strings) {
        if (strings == null)
            return null;
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < strings.size(); i++) {
            buffer.append(strings.get(i));
            if (i != strings.size() - 1)
                buffer.append(",\n");
        }
        return buffer.toString();
    }

    protected String asWayPointComment(String name, String description) {
        return asComment(name, description);
    }

    protected Double extractSpeed(String comment) {
        Matcher matcher = TRIPMASTER_SPEED_PATTERN.matcher(comment);
        if (matcher.matches())
            return Conversion.parseDouble(matcher.group(1));
        return null;
    }

    protected boolean isWriteName() {
        return preferences.getBoolean("writeName", true);
    }

    protected boolean isWriteElevation() {
        return preferences.getBoolean("writeElevation", true);
    }

    protected boolean isWriteSpeed() {
        return preferences.getBoolean("writeSpeed", true);
    }

    protected boolean isWriteTime() {
        return preferences.getBoolean("writeTime", true);
    }
}
