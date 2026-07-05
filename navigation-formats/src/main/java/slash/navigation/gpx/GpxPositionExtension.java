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

import jakarta.xml.bind.JAXBElement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static slash.common.io.Transfer.formatDouble;
import static slash.common.io.Transfer.*;
import static slash.navigation.common.NavigationConversion.*;
import static slash.navigation.common.UnitConversion.kmhToMs;
import static slash.navigation.common.UnitConversion.msToKmh;
import static slash.navigation.gpx.GpxExtensionType.*;

/**
 * Represents a temperature, heading or speed extension to a {@link GpxPosition}
 * in a GPS Exchange 1.1 Format (.gpx) file.
 *
 * @author Christian Pesch
 */

public class GpxPositionExtension {
    private static final Set<String> WELL_KNOWN_ELEMENT_NAMES = new HashSet<>(asList("course", "heading", "speed", "temperature"));

    private final WptType wptType;

    GpxPositionExtension(WptType wptType) {
        this.wptType = wptType;
    }

    Set<GpxExtensionType> getExtensionTypes() {
        Set<GpxExtensionType> extensionTypes = new HashSet<>();
        ExtensionsType extensions = wptType.getExtensions();
        if (extensions != null) {
            for (Object any : extensions.getAny())
                if (any instanceof JAXBElement jaxbElement) {
                    Object anyValue = jaxbElement.getValue();
                    if (anyValue instanceof slash.navigation.gpx.garmin3.TrackPointExtensionT) {
                        extensionTypes.add(Garmin3);

                    } else if (anyValue instanceof slash.navigation.gpx.trackpoint1.TrackPointExtensionT) {
                        extensionTypes.add(TrackPoint1);

                    } else if (anyValue instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT) {
                        extensionTypes.add(TrackPoint2);
                    }

                } else if (any instanceof Element element) {
                    if (isWellKnownElementName(element)) {
                        extensionTypes.add(Text);
                    }
                }
        }
        return extensionTypes;
    }

    private boolean isWellKnownElementName(Element element) {
        return WELL_KNOWN_ELEMENT_NAMES.contains(element.getLocalName().toLowerCase());
    }

    // Walks the wpt <extensions>, returning the first non-null value produced either
    // from a typed extension (fromExtension) or from a plain DOM element whose local
    // name matches (parseElement). Centralises the JAXBElement-vs-Element walk that the
    // get*() accessors used to each re-implement.
    private <R> R readExtension(Function<Object, R> fromExtension,
                                Predicate<String> matchesElementName,
                                Function<String, R> parseElement) {
        ExtensionsType extensions = wptType.getExtensions();
        if (extensions == null)
            return null;
        for (Object any : extensions.getAny()) {
            R result = null;
            if (any instanceof JAXBElement<?> jaxbElement) {
                result = fromExtension.apply(jaxbElement.getValue());
                // simple-typed extensions (e.g. TrekBuddy's nmea:course/nmea:speed, bound to
                // JAXBElement<BigDecimal>) carry the value in the element itself, not in a typed
                // trackpoint object - parse them by name like a plain DOM element
                if (result == null && jaxbElement.getValue() != null
                        && matchesElementName.test(jaxbElement.getName().getLocalPart()))
                    result = parseElement.apply(jaxbElement.getValue().toString());
            } else if (any instanceof Element element && matchesElementName.test(element.getLocalName()))
                result = parseElement.apply(element.getTextContent());
            if (result != null)
                return result;
        }
        return null;
    }

    public Double getHeading() {
        return readExtension(
                value -> value instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint
                        ? formatDouble(trackPoint.getCourse()) : null,
                name -> "course".equalsIgnoreCase(name) || "heading".equalsIgnoreCase(name),
                text -> parseDouble(text));
    }

    // Updates the value in every matching existing extension (typed or DOM); if none
    // matched, appends a fresh TrackPointExtension v2 carrying it. Centralises the
    // find-or-create write that the set*() accessors used to each re-implement.
    private void writeExtension(Predicate<Object> updateExtension,
                                Predicate<String> matchesElementName,
                                Consumer<Element> updateElement,
                                Consumer<slash.navigation.gpx.trackpoint2.TrackPointExtensionT> populateNewTrackpoint2) {
        if (wptType.getExtensions() == null)
            wptType.setExtensions(new ObjectFactory().createExtensionsType());

        boolean found = false;
        for (Object any : wptType.getExtensions().getAny()) {
            if (any instanceof JAXBElement<?> jaxbElement) {
                if (updateExtension.test(jaxbElement.getValue()))
                    found = true;
            } else if (any instanceof Element element && matchesElementName.test(element.getLocalName())) {
                updateElement.accept(element);
                found = true;
            }
        }

        if (!found) {
            slash.navigation.gpx.trackpoint2.ObjectFactory factory = new slash.navigation.gpx.trackpoint2.ObjectFactory();
            slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint = factory.createTrackPointExtensionT();
            populateNewTrackpoint2.accept(trackPoint);
            wptType.getExtensions().getAny().add(factory.createTrackPointExtension(trackPoint));
        }
    }

    public void setHeading(Double heading) {
        writeExtension(
                value -> {
                    if (value instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint) {
                        trackPoint.setCourse(formatHeading(heading));
                        return true;
                    }
                    return false;
                },
                name -> "course".equalsIgnoreCase(name) || "heading".equalsIgnoreCase(name),
                element -> element.setTextContent(formatHeadingAsString(heading)),
                trackPoint -> trackPoint.setCourse(formatHeading(heading)));
    }

    public Double getSpeed() {
        return readExtension(
                value -> value instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint
                        ? msToKmh(trackPoint.getSpeed()) : null,
                name -> "speed".equalsIgnoreCase(name),
                text -> msToKmh(parseDouble(text)));
    }

    public void setSpeed(Double speed) {
        writeExtension(
                value -> {
                    if (value instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint) {
                        trackPoint.setSpeed(formatSpeedAsDouble(kmhToMs(speed)));
                        return true;
                    }
                    return false;
                },
                name -> "speed".equalsIgnoreCase(name),
                element -> element.setTextContent(formatSpeedAsString(kmhToMs(speed))),
                trackPoint -> trackPoint.setSpeed(formatSpeedAsDouble(kmhToMs(speed))));
    }

    public Double getTemperature() {
        return readExtension(
                value -> {
                    if (value instanceof slash.navigation.gpx.garmin3.TrackPointExtensionT trackPoint)
                        return trackPoint.getTemperature();
                    if (value instanceof slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackPoint) {
                        Double temperature = trackPoint.getAtemp();
                        return temperature != null ? temperature : trackPoint.getWtemp();
                    }
                    if (value instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint) {
                        Double temperature = trackPoint.getAtemp();
                        return temperature != null ? temperature : trackPoint.getWtemp();
                    }
                    return null;
                },
                name -> "temperature".equalsIgnoreCase(name),
                text -> parseDouble(text));
    }

    public void setTemperature(Double temperature) {
        writeExtension(
                value -> {
                    if (value instanceof slash.navigation.gpx.garmin3.TrackPointExtensionT trackPoint) {
                        trackPoint.setTemperature(formatTemperatureAsDouble(temperature));
                        return true;
                    }
                    if (value instanceof slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackPoint) {
                        if (isEmpty(trackPoint.getAtemp()) && isEmpty(temperature))
                            trackPoint.setWtemp(null);
                        trackPoint.setAtemp(formatTemperatureAsDouble(temperature));
                        return true;
                    }
                    if (value instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint) {
                        if (isEmpty(trackPoint.getAtemp()) && isEmpty(temperature))
                            trackPoint.setWtemp(null);
                        trackPoint.setAtemp(formatTemperatureAsDouble(temperature));
                        return true;
                    }
                    return false;
                },
                name -> "temperature".equalsIgnoreCase(name),
                element -> element.setTextContent(formatTemperatureAsString(temperature)),
                trackPoint -> trackPoint.setAtemp(formatTemperatureAsDouble(temperature)));
    }

    public Short getHeartBeat() {
        return readExtension(
                value -> {
                    if (value instanceof slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackPoint)
                        return trackPoint.getHr();
                    if (value instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint)
                        return trackPoint.getHr();
                    return null;
                },
                name -> "hr".equalsIgnoreCase(name),
                text -> parseShort(text));
    }

    public void setHeartBeat(Short heartBeat) {
        writeExtension(
                value -> {
                    if (value instanceof slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackPoint) {
                        trackPoint.setHr(heartBeat);
                        return true;
                    }
                    if (value instanceof slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint) {
                        trackPoint.setHr(heartBeat);
                        return true;
                    }
                    return false;
                },
                name -> "hr".equalsIgnoreCase(name),
                element -> element.setTextContent(formatShortAsString(heartBeat)),
                trackPoint -> trackPoint.setHr(heartBeat));
    }


    private <T> T getExtension(Class<T> extensionClass) {
        List<Object> anys = wptType.getExtensions().getAny();
        for (Object any : anys) {
            if (any instanceof JAXBElement jaxbElement) {
                Object anyValue = jaxbElement.getValue();
                if (extensionClass.isInstance(anyValue)) {
                    return extensionClass.cast(anyValue);
                }
            }
        }
        return null;
    }

    // Copies a source field into the target only when the source has a value and the
    // target does not. Centralises the per-field merge decision so each field is one
    // declarative line and the (single) decision branch is exercised once.
    private static void copyIfTargetEmpty(boolean sourceEmpty, boolean targetEmpty, Runnable copy) {
        if (!sourceEmpty && targetEmpty)
            copy.run();
    }

    private void mergeExtension(slash.navigation.gpx.garmin3.TrackPointExtensionT garmin3, slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackpoint2) {
        copyIfTargetEmpty(isEmpty(garmin3.getDepth()),       isEmpty(trackpoint2.getDepth()), () -> trackpoint2.setDepth(garmin3.getDepth()));
        copyIfTargetEmpty(isEmpty(garmin3.getTemperature()), isEmpty(trackpoint2.getAtemp()), () -> trackpoint2.setAtemp(garmin3.getTemperature()));
    }

    private void mergeExtension(slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackpoint1, slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackpoint2) {
        copyIfTargetEmpty(isEmpty(trackpoint1.getAtemp()), isEmpty(trackpoint2.getAtemp()), () -> trackpoint2.setAtemp(trackpoint1.getAtemp()));
        copyIfTargetEmpty(isEmpty(trackpoint1.getCad()),   isEmpty(trackpoint2.getCad()),   () -> trackpoint2.setCad(trackpoint1.getCad()));
        copyIfTargetEmpty(isEmpty(trackpoint1.getDepth()), isEmpty(trackpoint2.getDepth()), () -> trackpoint2.setDepth(trackpoint1.getDepth()));
        copyIfTargetEmpty(isEmpty(trackpoint1.getHr()),    isEmpty(trackpoint2.getHr()),    () -> trackpoint2.setHr(trackpoint1.getHr()));
        copyIfTargetEmpty(isEmpty(trackpoint1.getWtemp()), isEmpty(trackpoint2.getWtemp()), () -> trackpoint2.setWtemp(trackpoint1.getWtemp()));
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

    // True only when every supplied per-field emptiness flag is true. Replaces long
    // &&-chains so the "all fields empty" decision is a single, once-covered loop.
    private static boolean allEmpty(boolean... fieldEmpty) {
        for (boolean empty : fieldEmpty)
            if (!empty)
                return false;
        return true;
    }

    private boolean isEmptyExtension(slash.navigation.gpx.garmin3.TrackPointExtensionT trackPoint) {
        return allEmpty(isEmpty(trackPoint.getDepth()), isEmpty(trackPoint.getTemperature())) &&
                (trackPoint.getExtensions() == null || trackPoint.getExtensions().getAny().isEmpty());
    }

    private boolean isEmptyExtension(slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackPoint) {
        return allEmpty(isEmpty(trackPoint.getAtemp()), isEmpty(trackPoint.getCad()), isEmpty(trackPoint.getDepth()),
                isEmpty(trackPoint.getHr()), isEmpty(trackPoint.getWtemp())) &&
                (trackPoint.getExtensions() == null || trackPoint.getExtensions().getAny().isEmpty());
    }

    private boolean isEmptyExtension(slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPoint) {
        return allEmpty(isEmpty(trackPoint.getAtemp()), isEmpty(trackPoint.getBearing()), isEmpty(trackPoint.getCad()),
                isEmpty(trackPoint.getCourse()), isEmpty(trackPoint.getDepth()), isEmpty(trackPoint.getHr()),
                isEmpty(trackPoint.getSpeed()), isEmpty(trackPoint.getWtemp())) &&
                (trackPoint.getExtensions() == null || trackPoint.getExtensions().getAny().isEmpty());
    }

    private void removeExtension(Object extension) {
        List<Object> anys = wptType.getExtensions().getAny();
        for (Iterator<Object> iterator = anys.iterator(); iterator.hasNext(); ) {
            Object any = iterator.next();
            if (any instanceof JAXBElement jaxbElement) {
                Object anyValue = jaxbElement.getValue();
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

            if (any instanceof Element element) {
                if (isWellKnownElementName(element)) {
                    if (isEmpty(parseDouble(element.getTextContent())))
                        iterator.remove();
                }
            }
        }

        if (wptType.getExtensions() != null && wptType.getExtensions().getAny().isEmpty())
            wptType.setExtensions(null);
    }
}
