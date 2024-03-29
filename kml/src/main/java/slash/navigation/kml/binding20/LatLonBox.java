//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.5-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.02.17 at 01:40:15 PM MEZ
//


package slash.navigation.kml.binding20;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.math.BigDecimal;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element ref="{http://earth.google.com/kml/2.0}north"/>
 *         &lt;element ref="{http://earth.google.com/kml/2.0}east"/>
 *         &lt;element ref="{http://earth.google.com/kml/2.0}south"/>
 *         &lt;element ref="{http://earth.google.com/kml/2.0}west"/>
 *         &lt;element ref="{http://earth.google.com/kml/2.0}rotation" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {

})
@XmlRootElement(name = "LatLonBox")
public class LatLonBox {

    protected double north;
    protected double east;
    protected double south;
    protected double west;
    protected BigDecimal rotation;
    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected String id;

    /**
     * Gets the value of the north property.
     * 
     */
    public double getNorth() {
        return north;
    }

    /**
     * Sets the value of the north property.
     * 
     */
    public void setNorth(double value) {
        this.north = value;
    }

    /**
     * Gets the value of the east property.
     * 
     */
    public double getEast() {
        return east;
    }

    /**
     * Sets the value of the east property.
     * 
     */
    public void setEast(double value) {
        this.east = value;
    }

    /**
     * Gets the value of the south property.
     * 
     */
    public double getSouth() {
        return south;
    }

    /**
     * Sets the value of the south property.
     * 
     */
    public void setSouth(double value) {
        this.south = value;
    }

    /**
     * Gets the value of the west property.
     * 
     */
    public double getWest() {
        return west;
    }

    /**
     * Sets the value of the west property.
     * 
     */
    public void setWest(double value) {
        this.west = value;
    }

    /**
     * Gets the value of the rotation property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getRotation() {
        return rotation;
    }

    /**
     * Sets the value of the rotation property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setRotation(BigDecimal value) {
        this.rotation = value;
    }

    /**
     * Gets the value of the id property.
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
     * Sets the value of the id property.
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
