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
import slash.navigation.gpx.GpxFormat;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.binding11.WptType;

import static slash.common.io.Transfer.parseDouble;

/**
 * The base of all Training Center Database formats.
 *
 * @author Christian Pesch
 */

public abstract class TcxFormat extends GpxFormat {

    public String getExtension() {
        return ".tcx";
    }

    protected Short getHeartBeatRate(GpxPosition position) {
        if (position != null) {
            WptType wpt = position.getOrigin(WptType.class);
            if (wpt != null) {
                Double heartBeatRate = getHeartBeatRate(wpt);
                if (heartBeatRate != null)
                    return heartBeatRate.shortValue();
            }
        }
        return null;
    }

    protected Double getHeartBeatRate(WptType wptType) {
        Double heartBeatRate = null;
        if (wptType.getExtensions() != null) {
            for (Object any : wptType.getExtensions().getAny()) {
                if (any instanceof Element) {
                    Element extension = (Element) any;
                    if ("TrackPointExtension".equals(extension.getLocalName())) {
                        for (int i = 0; i < extension.getChildNodes().getLength(); i++) {
                            Node hr = extension.getChildNodes().item(i);
                            if ("hr".equals(hr.getLocalName()))
                                heartBeatRate = parseDouble(hr.getTextContent());
                        }
                    }
                }
            }
        }
        return heartBeatRate;
    }
}
