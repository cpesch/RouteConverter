//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2014.07.23 um 04:23:54 PM CEST 
//


package slash.navigation.gpx.trackpoint2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.math.BigDecimal;


/**
 * 
 *     This type contains data fields that cannot
 *     be represented in track points in GPX 1.1 instances.
 *     
 * 
 * <p>Java-Klasse für TrackPointExtension_t complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="TrackPointExtension_t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="atemp" type="{http://www.garmin.com/xmlschemas/TrackPointExtension/v2}DegreesCelsius_t" minOccurs="0"/>
 *         &lt;element name="wtemp" type="{http://www.garmin.com/xmlschemas/TrackPointExtension/v2}DegreesCelsius_t" minOccurs="0"/>
 *         &lt;element name="depth" type="{http://www.garmin.com/xmlschemas/TrackPointExtension/v2}Meters_t" minOccurs="0"/>
 *         &lt;element name="hr" type="{http://www.garmin.com/xmlschemas/TrackPointExtension/v2}BeatsPerMinute_t" minOccurs="0"/>
 *         &lt;element name="cad" type="{http://www.garmin.com/xmlschemas/TrackPointExtension/v2}RevolutionsPerMinute_t" minOccurs="0"/>
 *         &lt;element name="speed" type="{http://www.garmin.com/xmlschemas/TrackPointExtension/v2}MetersPerSecond_t" minOccurs="0"/>
 *         &lt;element name="course" type="{http://www.garmin.com/xmlschemas/TrackPointExtension/v2}DegreesTrue_t" minOccurs="0"/>
 *         &lt;element name="bearing" type="{http://www.garmin.com/xmlschemas/TrackPointExtension/v2}DegreesTrue_t" minOccurs="0"/>
 *         &lt;element name="Extensions" type="{http://www.garmin.com/xmlschemas/TrackPointExtension/v2}Extensions_t" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TrackPointExtension_t", propOrder = {
    "atemp",
    "wtemp",
    "depth",
    "hr",
    "cad",
    "speed",
    "course",
    "bearing",
    "extensions"
})
public class TrackPointExtensionT {

    protected Double atemp;
    protected Double wtemp;
    protected Double depth;
    protected Short hr;
    protected Short cad;
    protected Double speed;
    protected BigDecimal course;
    protected BigDecimal bearing;
    @XmlElement(name = "Extensions")
    protected ExtensionsT extensions;

    /**
     * Ruft den Wert der atemp-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getAtemp() {
        return atemp;
    }

    /**
     * Legt den Wert der atemp-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setAtemp(Double value) {
        this.atemp = value;
    }

    /**
     * Ruft den Wert der wtemp-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getWtemp() {
        return wtemp;
    }

    /**
     * Legt den Wert der wtemp-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setWtemp(Double value) {
        this.wtemp = value;
    }

    /**
     * Ruft den Wert der depth-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getDepth() {
        return depth;
    }

    /**
     * Legt den Wert der depth-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setDepth(Double value) {
        this.depth = value;
    }

    /**
     * Ruft den Wert der hr-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Short }
     *     
     */
    public Short getHr() {
        return hr;
    }

    /**
     * Legt den Wert der hr-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Short }
     *     
     */
    public void setHr(Short value) {
        this.hr = value;
    }

    /**
     * Ruft den Wert der cad-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Short }
     *     
     */
    public Short getCad() {
        return cad;
    }

    /**
     * Legt den Wert der cad-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Short }
     *     
     */
    public void setCad(Short value) {
        this.cad = value;
    }

    /**
     * Ruft den Wert der speed-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getSpeed() {
        return speed;
    }

    /**
     * Legt den Wert der speed-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setSpeed(Double value) {
        this.speed = value;
    }

    /**
     * Ruft den Wert der course-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getCourse() {
        return course;
    }

    /**
     * Legt den Wert der course-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setCourse(BigDecimal value) {
        this.course = value;
    }

    /**
     * Ruft den Wert der bearing-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getBearing() {
        return bearing;
    }

    /**
     * Legt den Wert der bearing-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setBearing(BigDecimal value) {
        this.bearing = value;
    }

    /**
     * Ruft den Wert der extensions-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link ExtensionsT }
     *     
     */
    public ExtensionsT getExtensions() {
        return extensions;
    }

    /**
     * Legt den Wert der extensions-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link ExtensionsT }
     *     
     */
    public void setExtensions(ExtensionsT value) {
        this.extensions = value;
    }

}
