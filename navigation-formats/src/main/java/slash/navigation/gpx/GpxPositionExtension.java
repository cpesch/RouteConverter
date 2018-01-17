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
import java.util.*;

import static java.util.Arrays.asList;
import static slash.common.io.Transfer.formatDouble;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.io.Transfer.parseDouble;
import static slash.navigation.common.NavigationConversion.*;
import static slash.navigation.common.NavigationConversion.formatHeading;
import static slash.navigation.gpx.GpxExtensionType.*;
import static slash.navigation.gpx.GpxFormat.asKmh;
import static slash.navigation.gpx.GpxFormat.asMs;

/**
 * Represents a temperature, heading or speed extension to a {@link GpxPosition}
 * in a GPS Exchange 1.1 Format (.gpx) file.
 *
 * @author Christian Pesch
 */

public class GpxPositionExtension {
    private WptType wptType;
    private static final Set<String> WELL_KNOWN_ELEMENT_NAMES = new HashSet<>(asList("course", "speed", "temperature"));

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
                    if (isWellKnownElementName(element)) {
                        extensionTypes.add(Text);
                    }
                }
            }
        }
        return extensionTypes;
    }

    private boolean isWellKnownElementName(Element element) {
        return WELL_KNOWN_ELEMENT_NAMES.contains(element.getLocalName().toLowerCase());
    }

    public Double getHeading() {
        Double result = null;

        ExtensionsType extensions = wptType.getExtensions();
        if (extensions != null) {
            for (Object any : extensions.getAny()) {
                if (any instanceof JAXBElement) {
                    Object anyValue = ((JAXBElement) any).getValue();
                    if (anyValue instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT) {
                        slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint = (slash.navigation.gpx.trackpoint2.TrackPointExtensionT) anyValue;
                        result = formatDouble(trackPoint.getCourse());
                    }

                } else if (any instanceof Element) {
                    Element element = (Element) any;
                    if ("course".equalsIgnoreCase(element.getLocalName()))
                        result = parseDouble(element.getTextContent());
                }

                if(result != null)
                    break;
            }
        }

        return result;
    }

    public void setHeading(Double heading) {
        if (wptType.getExtensions() == null)
            wptType.setExtensions(new ObjectFactory().createExtensionsType());
        List<Object> anys = wptType.getExtensions().getAny();

        boolean foundHeading = false;
        for (Object any : anys) {
            if (any instanceof JAXBElement) {
                Object anyValue = ((JAXBElement) any).getValue();
                if (anyValue instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT) {
                    slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint = (slash.navigation.gpx.trackpoint2.TrackPointExtensionT) anyValue;
                    trackPoint.setCourse(formatHeading(heading));
                    foundHeading = true;
                }

            } else if (any instanceof Element) {
                Element element = (Element) any;
                if ("course".equalsIgnoreCase(element.getLocalName())) {
                    element.setTextContent(formatHeadingAsString(heading));
                    foundHeading = true;
                }
            }
        }

        if (!foundHeading) {
            slash.navigation.gpx.trackpoint2.ObjectFactory trackpoint2Factory = new slash.navigation.gpx.trackpoint2.ObjectFactory();
            slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPointExtensionT = trackpoint2Factory.createTrackPointExtensionT();
            trackPointExtensionT.setCourse(formatHeading(heading));
            anys.add(trackpoint2Factory.createTrackPointExtension(trackPointExtensionT));
        }
    }

    public Double getSpeed() {
        Double result = null;

        ExtensionsType extensions = wptType.getExtensions();
        if (extensions != null) {
            for (Object any : extensions.getAny()) {
                if (any instanceof JAXBElement) {
                    Object anyValue = ((JAXBElement) any).getValue();
                    if (anyValue instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT) {
                        slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint = (slash.navigation.gpx.trackpoint2.TrackPointExtensionT) anyValue;
                        result = asKmh(trackPoint.getSpeed());
                    }

                } else if (any instanceof Element) {
                    Element element = (Element) any;
                    if ("speed".equalsIgnoreCase(element.getLocalName()))
                        result = asKmh(parseDouble(element.getTextContent()));
                }

                if(result != null)
                    break;
            }
        }

        return result;
    }

    public void setSpeed(Double speed) {
        if (wptType.getExtensions() == null)
            wptType.setExtensions(new ObjectFactory().createExtensionsType());
        List<Object> anys = wptType.getExtensions().getAny();

        boolean foundSpeed = false;
        for (Object any : anys) {
            if (any instanceof JAXBElement) {
                Object anyValue = ((JAXBElement) any).getValue();
                if (anyValue instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT) {
                    slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint = (slash.navigation.gpx.trackpoint2.TrackPointExtensionT) anyValue;
                    trackPoint.setSpeed(formatSpeedAsDouble(asMs(speed)));
                    foundSpeed = true;
                }

            } else if (any instanceof Element) {
                Element element = (Element) any;
                if ("speed".equalsIgnoreCase(element.getLocalName())) {
                    element.setTextContent(formatSpeedAsString(asMs(speed)));
                    foundSpeed = true;
                }
            }
        }

        // create new TrackPointExtension v2 element if there was no existing value found
        if (!foundSpeed) {
            slash.navigation.gpx.trackpoint2.ObjectFactory trackpoint2Factory = new slash.navigation.gpx.trackpoint2.ObjectFactory();
            slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPointExtensionT = trackpoint2Factory.createTrackPointExtensionT();
            trackPointExtensionT.setSpeed(formatSpeedAsDouble(asMs(speed)));
            anys.add(trackpoint2Factory.createTrackPointExtension(trackPointExtensionT));
        }
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

                if(result != null)
                    break;
            }
        }
        return result;
    }

    public void setTemperature(Double temperature) {
        if (wptType.getExtensions() == null)
            wptType.setExtensions(new ObjectFactory().createExtensionsType());
        List<Object> anys = wptType.getExtensions().getAny();

        boolean foundTemperature = false;
        for (Object any : anys) {
            if (any instanceof JAXBElement) {
                Object anyValue = ((JAXBElement) any).getValue();
                if (anyValue instanceof slash.navigation.gpx.garmin3.TrackPointExtensionT) {
                    slash.navigation.gpx.garmin3.TrackPointExtensionT trackPoint = (slash.navigation.gpx.garmin3.TrackPointExtensionT) anyValue;
                    trackPoint.setTemperature(formatTemperatureAsDouble(temperature));
                    foundTemperature = true;

                } else if (anyValue instanceof slash.navigation.gpx.trackpoint1.TrackPointExtensionT) {
                    slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackPoint = (slash.navigation.gpx.trackpoint1.TrackPointExtensionT) anyValue;
                    if(isEmpty(trackPoint.getAtemp()) && isEmpty(temperature))
                        trackPoint.setWtemp(null);
                    trackPoint.setAtemp(formatTemperatureAsDouble(temperature));

                    foundTemperature = true;

                } else if (anyValue instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT) {
                    slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint = (slash.navigation.gpx.trackpoint2.TrackPointExtensionT) anyValue;
                    if(isEmpty(trackPoint.getAtemp()) && isEmpty(temperature))
                        trackPoint.setWtemp(null);
                    trackPoint.setAtemp(formatTemperatureAsDouble(temperature));
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


    private <T> T getExtension(Class<T> extensionClass) {
        List<Object> anys = wptType.getExtensions().getAny();
        for (Object any : anys) {
            if (any instanceof JAXBElement) {
                Object anyValue = ((JAXBElement) any).getValue();
                if (extensionClass.isInstance(anyValue)) {
                    return extensionClass.cast(anyValue);
                }
            }
        }
        return null;
    }

    private void mergeExtension(slash.navigation.gpx.garmin3.TrackPointExtensionT garmin3, slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackpoint2) {
        if(!isEmpty(garmin3.getDepth()) && isEmpty(trackpoint2.getDepth()))
            trackpoint2.setDepth(garmin3.getDepth());
        if(!isEmpty(garmin3.getTemperature()) && isEmpty(trackpoint2.getAtemp()))
            trackpoint2.setAtemp(garmin3.getTemperature());
    }

    private void mergeExtension(slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackpoint1, slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackpoint2) {
        if(!isEmpty(trackpoint1.getAtemp()) && isEmpty(trackpoint2.getAtemp()))
            trackpoint2.setAtemp(trackpoint1.getAtemp());
        if(!isEmpty(trackpoint1.getCad()) && isEmpty(trackpoint2.getCad()))
            trackpoint2.setCad(trackpoint1.getCad());
        if(!isEmpty(trackpoint1.getDepth()) && isEmpty(trackpoint2.getDepth()))
            trackpoint2.setDepth(trackpoint1.getDepth());
        if(!isEmpty(trackpoint1.getHr()) && isEmpty(trackpoint2.getHr()))
            trackpoint2.setHr(trackpoint1.getHr());
        if(!isEmpty(trackpoint1.getWtemp()) && isEmpty(trackpoint2.getWtemp()))
            trackpoint2.setWtemp(trackpoint1.getWtemp());
    }

    public void mergeExtensions() {
        if (wptType.getExtensions() == null)
            return;
        if (getExtensionTypes().size() == 1)
            return;

        slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackpoint2 = getExtension(slash.navigation.gpx.trackpoint2.TrackPointExtensionT.class);
        if(trackpoint2 == null)
            return;

        slash.navigation.gpx.garmin3.TrackPointExtensionT garmin3 = getExtension(slash.navigation.gpx.garmin3.TrackPointExtensionT.class);
        if (garmin3 != null) {
            mergeExtension(garmin3, trackpoint2);
            removeExtension(garmin3);
        }

        slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackpoint1 = getExtension(slash.navigation.gpx.trackpoint1.TrackPointExtensionT.class);
        if(trackpoint1 != null) {
            mergeExtension(trackpoint1, trackpoint2);
            removeExtension(trackpoint1);
        }
    }

    private boolean isEmptyExtension(slash.navigation.gpx.garmin3.TrackPointExtensionT trackPoint) {
        return isEmpty(trackPoint.getDepth()) && isEmpty(trackPoint.getTemperature()) &&
                (trackPoint.getExtensions() == null || trackPoint.getExtensions().getAny().size() == 0);
    }

    private boolean isEmptyExtension(slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackPoint) {
        return isEmpty(trackPoint.getAtemp()) && isEmpty(trackPoint.getCad()) && isEmpty(trackPoint.getDepth()) &&
                isEmpty(trackPoint.getHr()) && isEmpty(trackPoint.getWtemp()) &&
                (trackPoint.getExtensions() == null || trackPoint.getExtensions().getAny().size() == 0);
    }

    private boolean isEmptyExtension(slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint) {
        return isEmpty(trackPoint.getAtemp()) && isEmpty(trackPoint.getBearing()) && isEmpty(trackPoint.getCad()) &&
                isEmpty(trackPoint.getCourse()) &&  isEmpty(trackPoint.getDepth()) && isEmpty(trackPoint.getHr()) &&
                isEmpty(trackPoint.getSpeed()) && isEmpty(trackPoint.getWtemp()) &&
                (trackPoint.getExtensions() == null || trackPoint.getExtensions().getAny().size() == 0);
    }

    private void removeExtension(Object extension) {
        List<Object> anys = wptType.getExtensions().getAny();
        for (Iterator<Object> iterator = anys.iterator(); iterator.hasNext(); ) {
            Object any = iterator.next();
            if (any instanceof JAXBElement) {
                Object anyValue = ((JAXBElement) any).getValue();
                if (anyValue.equals(extension))
                    iterator.remove();
            }
        }
    }

    public void removeEmptyExtensions() {
        if (wptType.getExtensions() == null)
            return;

        slash.navigation.gpx.garmin3.TrackPointExtensionT garmin3 = getExtension(slash.navigation.gpx.garmin3.TrackPointExtensionT.class);
        if(garmin3 != null && isEmptyExtension(garmin3))
            removeExtension(garmin3);

        slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackpoint1 = getExtension(slash.navigation.gpx.trackpoint1.TrackPointExtensionT.class);
        if(trackpoint1 != null && isEmptyExtension(trackpoint1))
            removeExtension(trackpoint1);

        slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackpoint2 = getExtension(slash.navigation.gpx.trackpoint2.TrackPointExtensionT.class);
        if(trackpoint2 != null && isEmptyExtension(trackpoint2))
            removeExtension(trackpoint2);

        List<Object> anys = wptType.getExtensions().getAny();
        for (Iterator<Object> iterator = anys.iterator(); iterator.hasNext(); ) {
            Object any = iterator.next();

            if (any instanceof Element) {
                Element element = (Element) any;
                if (isWellKnownElementName(element)) {
                    if (isEmpty(parseDouble(element.getTextContent())))
                        iterator.remove();
                }
            }
        }

        if (wptType.getExtensions() != null && wptType.getExtensions().getAny().size() == 0)
            wptType.setExtensions(null);
    }
}
