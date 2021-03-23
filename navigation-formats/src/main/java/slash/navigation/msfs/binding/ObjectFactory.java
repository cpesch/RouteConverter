//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2021.03.23 um 11:23:21 AM CET 
//


package slash.navigation.msfs.binding;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the slash.navigation.msfs.binding package. 
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

    private final static QName _ICAOIdent_QNAME = new QName("", "ICAOIdent");
    private final static QName _DepartureLLA_QNAME = new QName("", "DepartureLLA");
    private final static QName _DepartureName_QNAME = new QName("", "DepartureName");
    private final static QName _DestinationLLA_QNAME = new QName("", "DestinationLLA");
    private final static QName _FPType_QNAME = new QName("", "FPType");
    private final static QName _ArrivalFP_QNAME = new QName("", "ArrivalFP");
    private final static QName _Title_QNAME = new QName("", "Title");
    private final static QName _Descr_QNAME = new QName("", "Descr");
    private final static QName _DestinationID_QNAME = new QName("", "DestinationID");
    private final static QName _ATCWaypointType_QNAME = new QName("", "ATCWaypointType");
    private final static QName _RunwayNumberFP_QNAME = new QName("", "RunwayNumberFP");
    private final static QName _WorldPosition_QNAME = new QName("", "WorldPosition");
    private final static QName _RouteType_QNAME = new QName("", "RouteType");
    private final static QName _DepartureID_QNAME = new QName("", "DepartureID");
    private final static QName _DestinationName_QNAME = new QName("", "DestinationName");
    private final static QName _AppVersionMajor_QNAME = new QName("", "AppVersionMajor");
    private final static QName _ATCAirway_QNAME = new QName("", "ATCAirway");
    private final static QName _ICAORegion_QNAME = new QName("", "ICAORegion");
    private final static QName _CruisingAlt_QNAME = new QName("", "CruisingAlt");
    private final static QName _AppVersionBuild_QNAME = new QName("", "AppVersionBuild");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: slash.navigation.msfs.binding
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SimBaseDocument }
     * 
     */
    public SimBaseDocument createSimBaseDocument() {
        return new SimBaseDocument();
    }

    /**
     * Create an instance of {@link FlightPlanFlightPlan }
     * 
     */
    public FlightPlanFlightPlan createFlightPlanFlightPlan() {
        return new FlightPlanFlightPlan();
    }

    /**
     * Create an instance of {@link AppVersion }
     * 
     */
    public AppVersion createAppVersion() {
        return new AppVersion();
    }

    /**
     * Create an instance of {@link ATCWaypoint }
     * 
     */
    public ATCWaypoint createATCWaypoint() {
        return new ATCWaypoint();
    }

    /**
     * Create an instance of {@link ICAO }
     * 
     */
    public ICAO createICAO() {
        return new ICAO();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "ICAOIdent")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public JAXBElement<String> createICAOIdent(String value) {
        return new JAXBElement<String>(_ICAOIdent_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DepartureLLA")
    public JAXBElement<String> createDepartureLLA(String value) {
        return new JAXBElement<String>(_DepartureLLA_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DepartureName")
    public JAXBElement<String> createDepartureName(String value) {
        return new JAXBElement<String>(_DepartureName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DestinationLLA")
    public JAXBElement<String> createDestinationLLA(String value) {
        return new JAXBElement<String>(_DestinationLLA_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "FPType")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public JAXBElement<String> createFPType(String value) {
        return new JAXBElement<String>(_FPType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "ArrivalFP")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public JAXBElement<String> createArrivalFP(String value) {
        return new JAXBElement<String>(_ArrivalFP_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "Title")
    public JAXBElement<String> createTitle(String value) {
        return new JAXBElement<String>(_Title_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "Descr")
    public JAXBElement<String> createDescr(String value) {
        return new JAXBElement<String>(_Descr_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DestinationID")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public JAXBElement<String> createDestinationID(String value) {
        return new JAXBElement<String>(_DestinationID_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "ATCWaypointType")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public JAXBElement<String> createATCWaypointType(String value) {
        return new JAXBElement<String>(_ATCWaypointType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "RunwayNumberFP")
    public JAXBElement<BigInteger> createRunwayNumberFP(BigInteger value) {
        return new JAXBElement<BigInteger>(_RunwayNumberFP_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "WorldPosition")
    public JAXBElement<String> createWorldPosition(String value) {
        return new JAXBElement<String>(_WorldPosition_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "RouteType")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public JAXBElement<String> createRouteType(String value) {
        return new JAXBElement<String>(_RouteType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DepartureID")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public JAXBElement<String> createDepartureID(String value) {
        return new JAXBElement<String>(_DepartureID_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DestinationName")
    public JAXBElement<String> createDestinationName(String value) {
        return new JAXBElement<String>(_DestinationName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "AppVersionMajor")
    public JAXBElement<BigInteger> createAppVersionMajor(BigInteger value) {
        return new JAXBElement<BigInteger>(_AppVersionMajor_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "ATCAirway")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public JAXBElement<String> createATCAirway(String value) {
        return new JAXBElement<String>(_ATCAirway_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "ICAORegion")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public JAXBElement<String> createICAORegion(String value) {
        return new JAXBElement<String>(_ICAORegion_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigDecimal }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "CruisingAlt")
    public JAXBElement<BigDecimal> createCruisingAlt(BigDecimal value) {
        return new JAXBElement<BigDecimal>(_CruisingAlt_QNAME, BigDecimal.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "AppVersionBuild")
    public JAXBElement<BigInteger> createAppVersionBuild(BigInteger value) {
        return new JAXBElement<BigInteger>(_AppVersionBuild_QNAME, BigInteger.class, null, value);
    }

}
