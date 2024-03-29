//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.16-hudson-jaxb-ri-2.1-pushtomaven-250--SNAPSHOT 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.10.28 at 04:01:32 PM MEZ 
//


package slash.navigation.gpx.trip1;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 *  Route via points are announced stops during a route. This type contains
 *         data fields intended to be used as child elements of the rtept element in the GPX 1.1 schema
 *       
 * 
 * <p>Java class for ViaPointExtension_t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ViaPointExtension_t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="DepartureTime" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         &lt;element name="StopDuration" type="{http://www.w3.org/2001/XMLSchema}duration" minOccurs="0"/>
 *         &lt;element name="ArrivalTime" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         &lt;element name="CalculationMode" type="{http://www.w3.org/2001/XMLSchema}token" minOccurs="0"/>
 *         &lt;element name="ElevationMode" type="{http://www.w3.org/2001/XMLSchema}token" minOccurs="0"/>
 *         &lt;element name="NamedRoad" type="{http://www.garmin.com/xmlschemas/TripExtensions/v1}NamedRoad_t" minOccurs="0"/>
 *         &lt;element name="Extensions" type="{http://www.garmin.com/xmlschemas/TripExtensions/v1}Extensions_t" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ViaPointExtension_t", propOrder = {
    "departureTime",
    "stopDuration",
    "arrivalTime",
    "calculationMode",
    "elevationMode",
    "namedRoad",
    "extensions"
})
public class ViaPointExtensionT {

    @XmlElement(name = "DepartureTime")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar departureTime;
    @XmlElement(name = "StopDuration")
    protected Duration stopDuration;
    @XmlElement(name = "ArrivalTime")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar arrivalTime;
    @XmlElement(name = "CalculationMode")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String calculationMode;
    @XmlElement(name = "ElevationMode")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String elevationMode;
    @XmlElement(name = "NamedRoad")
    protected NamedRoadT namedRoad;
    @XmlElement(name = "Extensions")
    protected ExtensionsT extensions;

    /**
     * Gets the value of the departureTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getDepartureTime() {
        return departureTime;
    }

    /**
     * Sets the value of the departureTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setDepartureTime(XMLGregorianCalendar value) {
        this.departureTime = value;
    }

    /**
     * Gets the value of the stopDuration property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getStopDuration() {
        return stopDuration;
    }

    /**
     * Sets the value of the stopDuration property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setStopDuration(Duration value) {
        this.stopDuration = value;
    }

    /**
     * Gets the value of the arrivalTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getArrivalTime() {
        return arrivalTime;
    }

    /**
     * Sets the value of the arrivalTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setArrivalTime(XMLGregorianCalendar value) {
        this.arrivalTime = value;
    }

    /**
     * Gets the value of the calculationMode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCalculationMode() {
        return calculationMode;
    }

    /**
     * Sets the value of the calculationMode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCalculationMode(String value) {
        this.calculationMode = value;
    }

    /**
     * Gets the value of the elevationMode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getElevationMode() {
        return elevationMode;
    }

    /**
     * Sets the value of the elevationMode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setElevationMode(String value) {
        this.elevationMode = value;
    }

    /**
     * Gets the value of the namedRoad property.
     * 
     * @return
     *     possible object is
     *     {@link NamedRoadT }
     *     
     */
    public NamedRoadT getNamedRoad() {
        return namedRoad;
    }

    /**
     * Sets the value of the namedRoad property.
     * 
     * @param value
     *     allowed object is
     *     {@link NamedRoadT }
     *     
     */
    public void setNamedRoad(NamedRoadT value) {
        this.namedRoad = value;
    }

    /**
     * Gets the value of the extensions property.
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
     * Sets the value of the extensions property.
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
