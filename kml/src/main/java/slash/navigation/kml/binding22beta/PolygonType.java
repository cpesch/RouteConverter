//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.5-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2008.10.12 at 02:39:09 PM CEST 
//


package slash.navigation.kml.binding22beta;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for PolygonType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PolygonType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://earth.google.com/kml/2.2}AbstractGeometryType">
 *       &lt;sequence>
 *         &lt;element ref="{http://earth.google.com/kml/2.2}extrude" minOccurs="0"/>
 *         &lt;element ref="{http://earth.google.com/kml/2.2}tessellate" minOccurs="0"/>
 *         &lt;element ref="{http://earth.google.com/kml/2.2}altitudeModeGroup" minOccurs="0"/>
 *         &lt;element ref="{http://earth.google.com/kml/2.2}outerBoundaryIs" minOccurs="0"/>
 *         &lt;element ref="{http://earth.google.com/kml/2.2}innerBoundaryIs" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PolygonType", propOrder = {
    "extrude",
    "tessellate",
    "altitudeModeGroup",
    "outerBoundaryIs",
    "innerBoundaryIs"
})
public class PolygonType
    extends AbstractGeometryType
{

    @XmlElement(defaultValue = "0")
    protected Boolean extrude;
    @XmlElement(defaultValue = "0")
    protected Boolean tessellate;
    @XmlElementRef(name = "altitudeModeGroup", namespace = "http://earth.google.com/kml/2.2", type = JAXBElement.class)
    protected JAXBElement<?> altitudeModeGroup;
    protected BoundaryType outerBoundaryIs;
    protected List<BoundaryType> innerBoundaryIs;

    /**
     * Gets the value of the extrude property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isExtrude() {
        return extrude;
    }

    /**
     * Sets the value of the extrude property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setExtrude(Boolean value) {
        this.extrude = value;
    }

    /**
     * Gets the value of the tessellate property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isTessellate() {
        return tessellate;
    }

    /**
     * Sets the value of the tessellate property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setTessellate(Boolean value) {
        this.tessellate = value;
    }

    /**
     * Gets the value of the altitudeModeGroup property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link AltitudeModeEnumType }{@code >}
     *     {@link JAXBElement }{@code <}{@link Object }{@code >}
     *     
     */
    public JAXBElement<?> getAltitudeModeGroup() {
        return altitudeModeGroup;
    }

    /**
     * Sets the value of the altitudeModeGroup property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link AltitudeModeEnumType }{@code >}
     *     {@link JAXBElement }{@code <}{@link Object }{@code >}
     *     
     */
    public void setAltitudeModeGroup(JAXBElement<?> value) {
        this.altitudeModeGroup = value;
    }

    /**
     * Gets the value of the outerBoundaryIs property.
     * 
     * @return
     *     possible object is
     *     {@link BoundaryType }
     *     
     */
    public BoundaryType getOuterBoundaryIs() {
        return outerBoundaryIs;
    }

    /**
     * Sets the value of the outerBoundaryIs property.
     * 
     * @param value
     *     allowed object is
     *     {@link BoundaryType }
     *     
     */
    public void setOuterBoundaryIs(BoundaryType value) {
        this.outerBoundaryIs = value;
    }

    /**
     * Gets the value of the innerBoundaryIs property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the innerBoundaryIs property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInnerBoundaryIs().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link BoundaryType }
     * 
     * 
     */
    public List<BoundaryType> getInnerBoundaryIs() {
        if (innerBoundaryIs == null) {
            innerBoundaryIs = new ArrayList<>();
        }
        return this.innerBoundaryIs;
    }

}
