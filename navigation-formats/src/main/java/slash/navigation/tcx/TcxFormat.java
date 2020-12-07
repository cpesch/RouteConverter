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

package slash.navigation.tcx;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import slash.navigation.base.MultipleRoutesFormat;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.XmlNavigationFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.gpx.binding11.WptType;
import slash.navigation.tcx.binding2.HeartRateInBeatsPerMinuteT;

import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;

/**
 * The base of all Training Center Database formats.
 *
 * @author Christian Pesch
 */

public abstract class TcxFormat extends XmlNavigationFormat<TcxRoute> implements MultipleRoutesFormat<TcxRoute> {
    private static final Preferences preferences = Preferences.userNodeForPackage(TcxFormat.class);

    public String getExtension() {
        return ".tcx";
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public boolean isWritingRouteCharacteristics() {
        return true;
    }

    @SuppressWarnings({"unchecked"})
    public <P extends NavigationPosition> TcxRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new TcxRoute(this, characteristics, name, (List<Wgs84Position>) positions);
    }

    protected Short getHeartBeat(Wgs84Position position) {
        if (position != null) {
            WptType wpt = position.getOrigin(WptType.class);
            if (wpt != null) {
                Double heartBeat = getHeartBeat(wpt);
                if (heartBeat != null)
                    return heartBeat.shortValue();
            }
            slash.navigation.tcx.binding1.TrackpointT trackpointT1 = position.getOrigin(slash.navigation.tcx.binding1.TrackpointT.class);
            if (trackpointT1 != null) {
                return trackpointT1.getHeartRateBpm();
            }
            slash.navigation.tcx.binding2.TrackpointT trackpointT2 = position.getOrigin(slash.navigation.tcx.binding2.TrackpointT.class);
            if (trackpointT2 != null) {
                HeartRateInBeatsPerMinuteT heartRateBpm = trackpointT2.getHeartRateBpm();
                if (heartRateBpm != null)
                    return heartRateBpm.getValue();
            }
        }
        return null;
    }

    private Double getHeartBeat(WptType wptType) {
        Double heartBeat = null;
        if (wptType.getExtensions() != null) {
            for (Object any : wptType.getExtensions().getAny()) {
                if (any instanceof Element) {
                    Element extension = (Element) any;
                    if ("TrackPointExtension".equals(extension.getLocalName())) {
                        for (int i = 0; i < extension.getChildNodes().getLength(); i++) {
                            Node hr = extension.getChildNodes().item(i);
                            if ("hr".equals(hr.getLocalName()))
                                heartBeat = parseDouble(hr.getTextContent());
                        }
                    }
                }
            }
        }
        return heartBeat;
    }

    public int getMaximumRouteNameLength() {
        return preferences.getInt("maximumRouteNameLength", 15);
    }

    protected String createUniqueRouteName(String routeName, Set<String> routeNames) {
        String result = asRouteName(routeName);
        int index = 2;
        while (routeNames.contains(result)) {
            String suffix = " (" + index + ")";
            result = asRouteName(trim(routeName, getMaximumRouteNameLength() - suffix.length()) + suffix);
            index++;
        }
        return result;
    }
}
