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

import org.w3c.dom.Element;
import slash.navigation.gpx.binding11.ExtensionsType;
import slash.navigation.gpx.binding11.ObjectFactory;
import slash.navigation.gpx.binding11.WptType;
import slash.navigation.gpx.garmin3.TrackPointExtensionT;

import javax.xml.bind.JAXBElement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static slash.common.io.Transfer.parseDouble;
import static slash.navigation.common.NavigationConversion.formatTemperatureAsDouble;
import static slash.navigation.common.NavigationConversion.formatTemperatureAsString;
import static slash.navigation.gpx.GpxExtensionType.*;

/**
 * Represents a temperature, heading or speed extension to a {@link GpxPosition}
 * in a GPS Exchange 1.1 Format (.gpx) file.
 *
 * @author Christian Pesch
 */

public class GpxPositionExtension {
    private WptType wptType;

    GpxPositionExtension(WptType wptType) {
        this.wptType = wptType;
    }

    Set<GpxExtensionType> getExtensionTypes() {
        Set<GpxExtensionType> extensionTypes = new HashSet<>();
        ExtensionsType extensions = wptType.getExtensions();
        if (extensions != null) {
            for (Object any : extensions.getAny()) {
                if (any instanceof JAXBElement) {
                    Object anyValue = ((JAXBElement) any).getValue();
                    if (anyValue instanceof slash.navigation.gpx.garmin3.TrackPointExtensionT) {
                        extensionTypes.add(Garmin3);

                    } else if (anyValue instanceof slash.navigation.gpx.trackpoint1.TrackPointExtensionT) {
                        extensionTypes.add(TrackPoint1);

                    } else if (anyValue instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT) {
                        extensionTypes.add(TrackPoint2);
                    }

                } else if (any instanceof Element) {
                    Element element = (Element) any;
                    if ("temperature".equalsIgnoreCase(element.getLocalName())) {
                        extensionTypes.add(Text);
                    }
                }
            }
        }
        return extensionTypes;
    }

    public Double getTemperature() {
        Double result = null;

        ExtensionsType extensions = wptType.getExtensions();
        if (extensions != null) {
            for (Object any : extensions.getAny()) {
                if (any instanceof JAXBElement) {
                    Object anyValue = ((JAXBElement) any).getValue();
                    if (anyValue instanceof slash.navigation.gpx.garmin3.TrackPointExtensionT) {
                        slash.navigation.gpx.garmin3.TrackPointExtensionT trackPoint = (slash.navigation.gpx.garmin3.TrackPointExtensionT) anyValue;
                        result = trackPoint.getTemperature();

                    } else if (anyValue instanceof slash.navigation.gpx.trackpoint1.TrackPointExtensionT) {
                        slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackPoint = (slash.navigation.gpx.trackpoint1.TrackPointExtensionT) anyValue;
                        result = trackPoint.getAtemp();
                        if (result == null)
                            result = trackPoint.getWtemp();

                    } else if (anyValue instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT) {
                        slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint = (slash.navigation.gpx.trackpoint2.TrackPointExtensionT) anyValue;
                        result = trackPoint.getAtemp();
                        if (result == null)
                            result = trackPoint.getWtemp();
                    }

                } else if (any instanceof Element) {
                    Element element = (Element) any;
                    if ("temperature".equalsIgnoreCase(element.getLocalName()))
                        result = parseDouble(element.getTextContent());
                }
            }
        }
        return result;
    }

    public void setTemperature(Double temperature) {
        if (wptType.getExtensions() == null) {
            // do not introduce extension element if there is no data
            if(temperature == null)
                return;

            wptType.setExtensions(new ObjectFactory().createExtensionsType());
        }
        List<Object> anys = wptType.getExtensions().getAny();

        // replace existing values
        boolean foundTemperature = false;
        for (Object any : anys) {
            if (any instanceof JAXBElement) {
                Object anyValue = ((JAXBElement) any).getValue();
                if (anyValue instanceof TrackPointExtensionT) {
                    TrackPointExtensionT trackPoint = (TrackPointExtensionT) anyValue;
                    trackPoint.setTemperature(formatTemperatureAsDouble(temperature));
                    foundTemperature = true;

                } else if (anyValue instanceof slash.navigation.gpx.trackpoint1.TrackPointExtensionT) {
                    slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackPoint = (slash.navigation.gpx.trackpoint1.TrackPointExtensionT) anyValue;
                    trackPoint.setAtemp(formatTemperatureAsDouble(temperature));
                    // do not overwrite Wtemp
                    foundTemperature = true;

                } else if (anyValue instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT) {
                    slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint = (slash.navigation.gpx.trackpoint2.TrackPointExtensionT) anyValue;
                    trackPoint.setAtemp(formatTemperatureAsDouble(temperature));
                    // do not overwrite Wtemp
                    foundTemperature = true;
                }

            } else if (any instanceof Element) {
                Element element = (Element) any;
                if ("temperature".equalsIgnoreCase(element.getLocalName())) {
                    element.setTextContent(formatTemperatureAsString(temperature));
                    foundTemperature = true;
                }
            }
        }

        // create new TrackPointExtension v2 element if there was no existing value found
        if (!foundTemperature) {
            slash.navigation.gpx.trackpoint2.ObjectFactory trackpoint2Factory = new slash.navigation.gpx.trackpoint2.ObjectFactory();
            slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPointExtensionT = trackpoint2Factory.createTrackPointExtensionT();
            trackPointExtensionT.setAtemp(formatTemperatureAsDouble(temperature));
            anys.add(trackpoint2Factory.createTrackPointExtension(trackPointExtensionT));
        }
    }
}
