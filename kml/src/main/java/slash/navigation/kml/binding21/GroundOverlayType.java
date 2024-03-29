//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-646 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.01.16 at 09:21:13 PM MEZ 
//


package slash.navigation.kml.binding21;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for GroundOverlayType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GroundOverlayType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://earth.google.com/kml/2.1}OverlayType">
 *       &lt;sequence>
 *         &lt;element name="altitude" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="altitudeMode" type="{http://earth.google.com/kml/2.1}altitudeModeEnum" minOccurs="0"/>
 *         &lt;element ref="{http://earth.google.com/kml/2.1}LatLonBox" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GroundOverlayType", propOrder = {
    "altitude",
    "altitudeMode",
    "latLonBox"
})
public class GroundOverlayType
    extends OverlayType
{

    @XmlElement(defaultValue = "0")
    protected Double altitude;
    @XmlElement(defaultValue = "clampToGround")
    protected AltitudeModeEnum altitudeMode;
    @XmlElement(name = "LatLonBox")
    protected LatLonBoxType latLonBox;

    /**
     * Gets the value of the altitude property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getAltitude() {
        return altitude;
    }

    /**
     * Sets the value of the altitude property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setAltitude(Double value) {
        this.altitude = value;
    }

    /**
     * Gets the value of the altitudeMode property.
     * 
     * @return
     *     possible object is
     *     {@link AltitudeModeEnum }
     *     
     */
    public AltitudeModeEnum getAltitudeMode() {
        return altitudeMode;
    }

    /**
     * Sets the value of the altitudeMode property.
     * 
     * @param value
     *     allowed object is
     *     {@link AltitudeModeEnum }
     *     
     */
    public void setAltitudeMode(AltitudeModeEnum value) {
        this.altitudeMode = value;
    }

    /**
     * Gets the value of the latLonBox property.
     * 
     * @return
     *     possible object is
     *     {@link LatLonBoxType }
     *     
     */
    public LatLonBoxType getLatLonBox() {
        return latLonBox;
    }

    /**
     * Sets the value of the latLonBox property.
     * 
     * @param value
     *     allowed object is
     *     {@link LatLonBoxType }
     *     
     */
    public void setLatLonBox(LatLonBoxType value) {
        this.latLonBox = value;
    }

}
