/**
 * Contains the <a href="https://jaxb.dev.java.net/nonav/2.2.4/docs/api/">JAXB</a> binding for the Gpx11Format.
 */
@javax.xml.bind.annotation.XmlSchema(namespace = GPX_11_NAMESPACE_URI, elementFormDefault = QUALIFIED, xmlns = {
		@XmlNs(prefix = "", namespaceURI = GPX_11_NAMESPACE_URI),
		@XmlNs(prefix = "xsi", namespaceURI = XML_SCHEMA_INSTANCE_NAMESPACE_URI),
        @XmlNs(prefix = "gpxtpx", namespaceURI = GARMIN_TRACKPOINT_EXTENSIONS_2_NAMESPACE_URI),
        @XmlNs(prefix = "trp", namespaceURI = GARMIN_TRIP_EXTENSIONS_1_NAMESPACE_URI),
		@XmlNs(prefix = "gpxx", namespaceURI = GARMIN_EXTENSIONS_3_NAMESPACE_URI),
		@XmlNs(prefix = "nmea", namespaceURI = TREKBUDDY_EXTENSIONS_0984_NAMESPACE_URI) })
package slash.navigation.gpx.binding11;

import javax.xml.bind.annotation.XmlNs;

import static javax.xml.bind.annotation.XmlNsForm.QUALIFIED;
import static slash.navigation.gpx.GpxUtil.*;

