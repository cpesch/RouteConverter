//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2014.07.23 um 04:23:54 PM CEST 
//


package slash.navigation.gpx.trackpoint2;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the slash.navigation.gpx.trackpoint2 package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _TrackPointExtension_QNAME = new QName("http://www.garmin.com/xmlschemas/TrackPointExtension/v2", "TrackPointExtension");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: slash.navigation.gpx.trackpoint2
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link TrackPointExtensionT }
     * 
     */
    public TrackPointExtensionT createTrackPointExtensionT() {
        return new TrackPointExtensionT();
    }

    /**
     * Create an instance of {@link ExtensionsT }
     * 
     */
    public ExtensionsT createExtensionsT() {
        return new ExtensionsT();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TrackPointExtensionT }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.garmin.com/xmlschemas/TrackPointExtension/v2", name = "TrackPointExtension")
    public JAXBElement<TrackPointExtensionT> createTrackPointExtension(TrackPointExtensionT value) {
        return new JAXBElement<>(_TrackPointExtension_QNAME, TrackPointExtensionT.class, null, value);
    }

}
