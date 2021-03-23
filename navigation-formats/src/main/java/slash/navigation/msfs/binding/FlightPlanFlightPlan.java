//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2021.03.23 um 11:23:21 AM CET 
//


package slash.navigation.msfs.binding;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java-Klasse für anonymous complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}Title"/>
 *         &lt;element ref="{}FPType"/>
 *         &lt;element ref="{}RouteType"/>
 *         &lt;element ref="{}CruisingAlt"/>
 *         &lt;element ref="{}DepartureID"/>
 *         &lt;element ref="{}DepartureLLA"/>
 *         &lt;element ref="{}DestinationID"/>
 *         &lt;element ref="{}DestinationLLA"/>
 *         &lt;element ref="{}Descr"/>
 *         &lt;element ref="{}DepartureName"/>
 *         &lt;element ref="{}DestinationName"/>
 *         &lt;element ref="{}AppVersion"/>
 *         &lt;element ref="{}ATCWaypoint" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "title",
    "fpType",
    "routeType",
    "cruisingAlt",
    "departureID",
    "departureLLA",
    "destinationID",
    "destinationLLA",
    "descr",
    "departureName",
    "destinationName",
    "appVersion",
    "atcWaypoint"
})
@XmlRootElement(name = "FlightPlan.FlightPlan")
public class FlightPlanFlightPlan {

    @XmlElement(name = "Title", required = true)
    protected String title;
    @XmlElement(name = "FPType", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String fpType;
    @XmlElement(name = "RouteType", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String routeType;
    @XmlElement(name = "CruisingAlt", required = true)
    protected BigDecimal cruisingAlt;
    @XmlElement(name = "DepartureID", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String departureID;
    @XmlElement(name = "DepartureLLA", required = true)
    protected String departureLLA;
    @XmlElement(name = "DestinationID", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String destinationID;
    @XmlElement(name = "DestinationLLA", required = true)
    protected String destinationLLA;
    @XmlElement(name = "Descr", required = true)
    protected String descr;
    @XmlElement(name = "DepartureName", required = true)
    protected String departureName;
    @XmlElement(name = "DestinationName", required = true)
    protected String destinationName;
    @XmlElement(name = "AppVersion", required = true)
    protected AppVersion appVersion;
    @XmlElement(name = "ATCWaypoint", required = true)
    protected List<ATCWaypoint> atcWaypoint;

    /**
     * Ruft den Wert der title-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTitle() {
        return title;
    }

    /**
     * Legt den Wert der title-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTitle(String value) {
        this.title = value;
    }

    /**
     * Ruft den Wert der fpType-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFPType() {
        return fpType;
    }

    /**
     * Legt den Wert der fpType-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFPType(String value) {
        this.fpType = value;
    }

    /**
     * Ruft den Wert der routeType-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRouteType() {
        return routeType;
    }

    /**
     * Legt den Wert der routeType-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRouteType(String value) {
        this.routeType = value;
    }

    /**
     * Ruft den Wert der cruisingAlt-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getCruisingAlt() {
        return cruisingAlt;
    }

    /**
     * Legt den Wert der cruisingAlt-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setCruisingAlt(BigDecimal value) {
        this.cruisingAlt = value;
    }

    /**
     * Ruft den Wert der departureID-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDepartureID() {
        return departureID;
    }

    /**
     * Legt den Wert der departureID-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDepartureID(String value) {
        this.departureID = value;
    }

    /**
     * Ruft den Wert der departureLLA-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDepartureLLA() {
        return departureLLA;
    }

    /**
     * Legt den Wert der departureLLA-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDepartureLLA(String value) {
        this.departureLLA = value;
    }

    /**
     * Ruft den Wert der destinationID-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDestinationID() {
        return destinationID;
    }

    /**
     * Legt den Wert der destinationID-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDestinationID(String value) {
        this.destinationID = value;
    }

    /**
     * Ruft den Wert der destinationLLA-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDestinationLLA() {
        return destinationLLA;
    }

    /**
     * Legt den Wert der destinationLLA-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDestinationLLA(String value) {
        this.destinationLLA = value;
    }

    /**
     * Ruft den Wert der descr-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescr() {
        return descr;
    }

    /**
     * Legt den Wert der descr-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescr(String value) {
        this.descr = value;
    }

    /**
     * Ruft den Wert der departureName-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDepartureName() {
        return departureName;
    }

    /**
     * Legt den Wert der departureName-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDepartureName(String value) {
        this.departureName = value;
    }

    /**
     * Ruft den Wert der destinationName-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDestinationName() {
        return destinationName;
    }

    /**
     * Legt den Wert der destinationName-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDestinationName(String value) {
        this.destinationName = value;
    }

    /**
     * Ruft den Wert der appVersion-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link AppVersion }
     *     
     */
    public AppVersion getAppVersion() {
        return appVersion;
    }

    /**
     * Legt den Wert der appVersion-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link AppVersion }
     *     
     */
    public void setAppVersion(AppVersion value) {
        this.appVersion = value;
    }

    /**
     * Gets the value of the atcWaypoint property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the atcWaypoint property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getATCWaypoint().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ATCWaypoint }
     * 
     * 
     */
    public List<ATCWaypoint> getATCWaypoint() {
        if (atcWaypoint == null) {
            atcWaypoint = new ArrayList<ATCWaypoint>();
        }
        return this.atcWaypoint;
    }

}
