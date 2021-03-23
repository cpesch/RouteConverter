//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2021.03.23 um 11:23:21 AM CET 
//


package slash.navigation.msfs.binding;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
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
 *         &lt;element ref="{}ATCWaypointType"/>
 *         &lt;element ref="{}WorldPosition"/>
 *         &lt;element ref="{}ArrivalFP" minOccurs="0"/>
 *         &lt;choice minOccurs="0">
 *           &lt;element ref="{}ATCAirway"/>
 *           &lt;element ref="{}RunwayNumberFP"/>
 *         &lt;/choice>
 *         &lt;element ref="{}ICAO"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}NCName" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "atcWaypointType",
    "worldPosition",
    "arrivalFP",
    "atcAirway",
    "runwayNumberFP",
    "icao"
})
@XmlRootElement(name = "ATCWaypoint")
public class ATCWaypoint {

    @XmlElement(name = "ATCWaypointType", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String atcWaypointType;
    @XmlElement(name = "WorldPosition", required = true)
    protected String worldPosition;
    @XmlElement(name = "ArrivalFP")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String arrivalFP;
    @XmlElement(name = "ATCAirway")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String atcAirway;
    @XmlElement(name = "RunwayNumberFP")
    protected BigInteger runwayNumberFP;
    @XmlElement(name = "ICAO", required = true)
    protected ICAO icao;
    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String id;

    /**
     * Ruft den Wert der atcWaypointType-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getATCWaypointType() {
        return atcWaypointType;
    }

    /**
     * Legt den Wert der atcWaypointType-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setATCWaypointType(String value) {
        this.atcWaypointType = value;
    }

    /**
     * Ruft den Wert der worldPosition-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getWorldPosition() {
        return worldPosition;
    }

    /**
     * Legt den Wert der worldPosition-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setWorldPosition(String value) {
        this.worldPosition = value;
    }

    /**
     * Ruft den Wert der arrivalFP-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getArrivalFP() {
        return arrivalFP;
    }

    /**
     * Legt den Wert der arrivalFP-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setArrivalFP(String value) {
        this.arrivalFP = value;
    }

    /**
     * Ruft den Wert der atcAirway-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getATCAirway() {
        return atcAirway;
    }

    /**
     * Legt den Wert der atcAirway-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setATCAirway(String value) {
        this.atcAirway = value;
    }

    /**
     * Ruft den Wert der runwayNumberFP-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getRunwayNumberFP() {
        return runwayNumberFP;
    }

    /**
     * Legt den Wert der runwayNumberFP-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setRunwayNumberFP(BigInteger value) {
        this.runwayNumberFP = value;
    }

    /**
     * Ruft den Wert der icao-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link ICAO }
     *     
     */
    public ICAO getICAO() {
        return icao;
    }

    /**
     * Legt den Wert der icao-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link ICAO }
     *     
     */
    public void setICAO(ICAO value) {
        this.icao = value;
    }

    /**
     * Ruft den Wert der id-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Legt den Wert der id-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

}
