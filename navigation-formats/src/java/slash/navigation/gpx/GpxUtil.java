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
import slash.navigation.jaxb.JaxbUtils;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import java.io.*;

public class GpxUtil {
    public static final JAXBContext CONTEXT_10 = JaxbUtils.newContext(slash.navigation.gpx.binding10.ObjectFactory.class);
    public static final JAXBContext CONTEXT_11 = JaxbUtils.newContext(slash.navigation.gpx.binding11.ObjectFactory.class,
            slash.navigation.gpx.garmin3.ObjectFactory.class,
            slash.navigation.gpx.routecatalog10.ObjectFactory.class);

    public static final String GPX_10_NAMESPACE_URI = "http://www.topografix.com/GPX/1/0";
    public static final String GPX_11_NAMESPACE_URI = "http://www.topografix.com/GPX/1/1";
    public static final String GARMIN_EXTENSIONS_3_NAMESPACE_URI = "http://www.garmin.com/xmlschemas/GpxExtensions/v3";
    public static final String ROUTECATALOG_EXTENSIONS_1_NAMESPACE_URI = "http://www.routeconverter.de/xmlschemas/RouteCatalogExtensions/1.0";

    public static Unmarshaller newUnmarshaller10() {
        return JaxbUtils.newUnmarshaller(CONTEXT_10);
    }

    public static Marshaller newMarshaller10() {
        return JaxbUtils.newMarshaller(CONTEXT_10);
    }

    public static Unmarshaller newUnmarshaller11() {
        return JaxbUtils.newUnmarshaller(CONTEXT_11);
    }

    public static Marshaller newMarshaller11() {
        return JaxbUtils.newMarshaller(CONTEXT_11,
                GARMIN_EXTENSIONS_3_NAMESPACE_URI, "gpxx",
                ROUTECATALOG_EXTENSIONS_1_NAMESPACE_URI, "rcxx"
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

    public static Gpx unmarshal10(File file) throws JAXBException {
        try {
            FileInputStream in = new FileInputStream(file);
            try {
                return unmarshal10(in);
            }
            finally {
                in.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while unmarshalling from " + file + ": " + e.getMessage());
        }
    }

    public static void marshal10(Gpx gpx, Writer writer) throws JAXBException {
        newMarshaller10().marshal(new JAXBElement<Gpx>(new QName(GPX_10_NAMESPACE_URI, "gpx"), Gpx.class, gpx), writer);
    }

    public static void marshal10(Gpx gpx, File file) throws JAXBException {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                newMarshaller10().marshal(new JAXBElement<Gpx>(new QName(GPX_10_NAMESPACE_URI, "gpx"), Gpx.class, gpx), fos);
            }
            finally {
                fos.flush();
                fos.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling to " + file + ": " + e.getMessage());
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

    public static GpxType unmarshal11(File file) throws JAXBException {
        try {
            FileInputStream in = new FileInputStream(file);
            try {
                return unmarshal11(in);
            }
            finally {
                in.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while unmarshalling from " + file + ": " + e.getMessage());
        }
    }

    public static void marshal11(GpxType gpxType, Writer writer) throws JAXBException {
        newMarshaller11().marshal(new JAXBElement<GpxType>(new QName(GPX_11_NAMESPACE_URI, "gpx"), GpxType.class, gpxType), writer);
    }

    public static void marshal11(GpxType gpxType, File file) throws JAXBException {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                newMarshaller11().marshal(new JAXBElement<GpxType>(new QName(GPX_11_NAMESPACE_URI, "gpx"), GpxType.class, gpxType), fos);
            }
            finally {
                fos.flush();
                fos.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling to " + file + ": " + e.getMessage());
        }
    }
}
