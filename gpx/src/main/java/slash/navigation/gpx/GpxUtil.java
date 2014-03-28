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

import slash.navigation.gpx.binding10.Gpx;
import slash.navigation.gpx.binding11.GpxType;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import java.io.*;

import static slash.navigation.jaxb.JaxbUtils.*;

public class GpxUtil {
    private static final String GPX_10_NAMESPACE_URI = "http://www.topografix.com/GPX/1/0";
    private static final String GPX_11_NAMESPACE_URI = "http://www.topografix.com/GPX/1/1";
    private static final String GARMIN_EXTENSIONS_3_NAMESPACE_URI = "http://www.garmin.com/xmlschemas/GpxExtensions/v3";
    private static final String GARMIN_WAYPOINT_EXTENSIONS_1_NAMESPACE_URI = "http://www.garmin.com/xmlschemas/WaypointExtension/v1";
    private static final String GARMIN_TRACKPOINT_EXTENSIONS_1_NAMESPACE_URI = "http://www.garmin.com/xmlschemas/TrackPointExtension/v1";
    private static final String ROUTECATALOG_EXTENSIONS_1_NAMESPACE_URI = "http://www.routeconverter.de/xmlschemas/RouteCatalogExtensions/1.0";
    private static final String TREKBUDDY_EXTENSIONS_0984_NAMESPACE_URI = "http://trekbuddy.net/2009/01/gpx/nmea";
    private static final String XML_SCHEMA_INSTANCE_NAMESPACE_URI = "http://www.w3.org/2001/XMLSchema-instance";

    public static Unmarshaller newUnmarshaller10() {
        return newUnmarshaller(newContext(slash.navigation.gpx.binding10.ObjectFactory.class));
    }

    private static Marshaller newMarshaller10() {
        return newMarshaller(newContext(slash.navigation.gpx.binding10.ObjectFactory.class));
    }

    private static JAXBContext newContext11() {
        return newContext(slash.navigation.gpx.binding11.ObjectFactory.class,
                slash.navigation.gpx.garmin3.ObjectFactory.class,
                slash.navigation.gpx.routecatalog10.ObjectFactory.class);
    }

    private static Unmarshaller newUnmarshaller11() {
        return newUnmarshaller(newContext11());
    }

    private static Marshaller newMarshaller11() {
        return newMarshaller(newContext11(),
                XML_SCHEMA_INSTANCE_NAMESPACE_URI, "xsi",
                GARMIN_EXTENSIONS_3_NAMESPACE_URI, "gpxtrx",
                GARMIN_TRACKPOINT_EXTENSIONS_1_NAMESPACE_URI, "gpxtpx",
                GARMIN_WAYPOINT_EXTENSIONS_1_NAMESPACE_URI, "gpxx",
                ROUTECATALOG_EXTENSIONS_1_NAMESPACE_URI, "rcxx",
                TREKBUDDY_EXTENSIONS_0984_NAMESPACE_URI, "nmea"
        );
    }


    public static Gpx unmarshal10(Reader reader) throws JAXBException {
        Gpx result = null;
        try {
            result = (Gpx) newUnmarshaller10().unmarshal(reader);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage());
        }
        return result;
    }

    public static Gpx unmarshal10(InputStream in) throws JAXBException {
        Gpx result = null;
        try {
            result = (Gpx) newUnmarshaller10().unmarshal(in);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage());
        }
        return result;
    }

    public static void marshal10(Gpx gpx, OutputStream out) throws JAXBException {
        try {
            try {
                newMarshaller10().marshal(new JAXBElement<Gpx>(new QName(GPX_10_NAMESPACE_URI, "gpx"), Gpx.class, gpx), out);
            }
            finally {
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e.getMessage());
        }
    }


    public static GpxType unmarshal11(String string) throws JAXBException {
        return unmarshal11(new StringReader(string));
    }

    public static GpxType unmarshal11(Reader reader) throws JAXBException {
        GpxType result = null;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller11().unmarshal(reader);
            result = (GpxType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage());
        }
        return result;
    }

    public static GpxType unmarshal11(InputStream in) throws JAXBException {
        GpxType result = null;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller11().unmarshal(in);
            result = (GpxType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage());
        }

        return result;
    }

    public static void marshal11(GpxType gpxType, Writer writer) throws JAXBException {
        newMarshaller11().marshal(new slash.navigation.gpx.binding11.ObjectFactory().createGpx(gpxType), writer);
    }

    public static void marshal11(GpxType gpxType, OutputStream out) throws JAXBException {
        try {
            try {
                newMarshaller11().marshal(new slash.navigation.gpx.binding11.ObjectFactory().createGpx(gpxType), out);
            }
            finally {
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e.getMessage());
        }
    }
}
