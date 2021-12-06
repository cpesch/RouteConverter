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

import slash.navigation.base.MultipleRoutesFormat;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.XmlNavigationFormat;
import slash.navigation.common.NavigationPosition;

import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.parseDouble;

/**
 * The base of all GPS Exchange formats.
 *
 * @author Christian Pesch
 */

public abstract class GpxFormat extends XmlNavigationFormat<GpxRoute> implements MultipleRoutesFormat<GpxRoute> {
    private static final Preferences preferences = Preferences.userNodeForPackage(GpxFormat.class);
    static final Pattern TRIPMASTER_REASON_PATTERN = Pattern.compile("(Punkt|Richtung \\d+|Abstand \\d+|Dur. \\d+:\\d+:\\d+|Course \\d+|Dist. \\d+) (-|:) (.+)");
    private static final Pattern TRIPMASTER_SPEED_PATTERN = Pattern.compile("[^-\\d\\.]*([-\\d\\.]+)\\s*(K|k)m/h.*");
    private static final Pattern QSTARTZ_SPEED_PATTERN = Pattern.compile(".*Speed[^-\\d\\.]*([-\\d\\.]+)(K|k)m/h.*Course[^\\d\\.]*([\\d]+).*");
    private static final Pattern SPORTSTRACKER_SPEED_PATTERN = Pattern.compile(".*Speed\\s*([-\\d\\.]+)\\s*(K|k)m/h.*");

    public String getExtension() {
        return ".gpx";
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public boolean isWritingRouteCharacteristics() {
        return true; 
    }

    @SuppressWarnings({"unchecked"})
    public <P extends NavigationPosition> GpxRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new GpxRoute(this, characteristics, name, null, (List<GpxPosition>) positions);
    }

    protected String asWayPointDescription(String name, String description) {
        return asDescription(name, description);
    }

    static Double parseSpeed(String description) {
        if (description != null) {
            Matcher tripMasterMatcher = TRIPMASTER_SPEED_PATTERN.matcher(description);
            if (tripMasterMatcher.matches())
                return parseDouble(tripMasterMatcher.group(1));
            Matcher qstartzMatcher = QSTARTZ_SPEED_PATTERN.matcher(description);
            if (qstartzMatcher.matches())
                return parseDouble(qstartzMatcher.group(1));
            Matcher sportsTrackerMatcher = SPORTSTRACKER_SPEED_PATTERN.matcher(description);
            if (sportsTrackerMatcher.matches())
                return parseDouble(sportsTrackerMatcher.group(1));
        }
        return null;
    }

    static Double parseHeading(String description) {
        if (description != null) {
            Matcher qstartzPattern = QSTARTZ_SPEED_PATTERN.matcher(description);
            if (qstartzPattern.matches())
                return parseDouble(qstartzPattern.group(3));
        }
        return null;
    }

    protected boolean isWriteAccuracy() {
        return preferences.getBoolean("writeAccuracy", true);
    }

    protected boolean isWriteElevation() {
        return preferences.getBoolean("writeElevation", true);
    }

    protected boolean isWriteHeading() {
        return preferences.getBoolean("writeHeading", true);
    }

    protected boolean isWriteTemperature() {
        return preferences.getBoolean("writeTemperature", true);
    }

    protected boolean isWriteHeartBeat() {
        return preferences.getBoolean("writeHeartBeat", true);
    }

    protected boolean isWriteName() {
        return preferences.getBoolean("writeName", true);
    }

    protected boolean isWriteSpeed() {
        return preferences.getBoolean("writeSpeed", true);
    }

    protected boolean isWriteTime() {
        return preferences.getBoolean("writeTime", true);
    }

    protected boolean isWriteMetaData() {
        return preferences.getBoolean("writeMetaData", true);
    }

    protected boolean isWriteTrip() {
        return preferences.getBoolean("writeTrip", false);
    }

    protected boolean isWriteExtensions() {
        return preferences.getBoolean("writeExtensions", true);
    }
}
