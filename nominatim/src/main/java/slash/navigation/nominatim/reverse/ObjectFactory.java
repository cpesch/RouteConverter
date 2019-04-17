//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2019.04.17 um 08:52:04 PM CEST 
//


package slash.navigation.nominatim.reverse;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the slash.navigation.nominatim.reverse package. 
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

    private final static QName _Reversegeocode_QNAME = new QName("", "reversegeocode");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: slash.navigation.nominatim.reverse
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ReversegeocodeType }
     * 
     */
    public ReversegeocodeType createReversegeocodeType() {
        return new ReversegeocodeType();
    }

    /**
     * Create an instance of {@link AddresspartsType }
     * 
     */
    public AddresspartsType createAddresspartsType() {
        return new AddresspartsType();
    }

    /**
     * Create an instance of {@link ResultType }
     * 
     */
    public ResultType createResultType() {
        return new ResultType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReversegeocodeType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "reversegeocode")
    public JAXBElement<ReversegeocodeType> createReversegeocode(ReversegeocodeType value) {
        return new JAXBElement<ReversegeocodeType>(_Reversegeocode_QNAME, ReversegeocodeType.class, null, value);
    }

}
