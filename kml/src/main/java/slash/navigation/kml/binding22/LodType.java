//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-646 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.08.13 at 10:12:26 PM MESZ 
//


package slash.navigation.kml.binding22;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for LodType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LodType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.opengis.net/kml/2.2}AbstractObjectType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}minLodPixels" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}maxLodPixels" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}minFadeExtent" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}maxFadeExtent" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}LodSimpleExtensionGroup" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}LodObjectExtensionGroup" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LodType", propOrder = {
    "minLodPixels",
    "maxLodPixels",
    "minFadeExtent",
    "maxFadeExtent",
    "lodSimpleExtensionGroup",
    "lodObjectExtensionGroup"
})
public class LodType
    extends AbstractObjectType
{

    @XmlElement(defaultValue = "0.0")
    protected Double minLodPixels;
    @XmlElement(defaultValue = "-1.0")
    protected Double maxLodPixels;
    @XmlElement(defaultValue = "0.0")
    protected Double minFadeExtent;
    @XmlElement(defaultValue = "0.0")
    protected Double maxFadeExtent;
    @XmlElement(name = "LodSimpleExtensionGroup")
    @XmlSchemaType(name = "anySimpleType")
    protected List<Object> lodSimpleExtensionGroup;
    @XmlElement(name = "LodObjectExtensionGroup")
    protected List<AbstractObjectType> lodObjectExtensionGroup;

    /**
     * Gets the value of the minLodPixels property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getMinLodPixels() {
        return minLodPixels;
    }

    /**
     * Sets the value of the minLodPixels property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setMinLodPixels(Double value) {
        this.minLodPixels = value;
    }

    /**
     * Gets the value of the maxLodPixels property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getMaxLodPixels() {
        return maxLodPixels;
    }

    /**
     * Sets the value of the maxLodPixels property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setMaxLodPixels(Double value) {
        this.maxLodPixels = value;
    }

    /**
     * Gets the value of the minFadeExtent property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getMinFadeExtent() {
        return minFadeExtent;
    }

    /**
     * Sets the value of the minFadeExtent property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setMinFadeExtent(Double value) {
        this.minFadeExtent = value;
    }

    /**
     * Gets the value of the maxFadeExtent property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getMaxFadeExtent() {
        return maxFadeExtent;
    }

    /**
     * Sets the value of the maxFadeExtent property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setMaxFadeExtent(Double value) {
        this.maxFadeExtent = value;
    }

    /**
     * Gets the value of the lodSimpleExtensionGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the lodSimpleExtensionGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLodSimpleExtensionGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getLodSimpleExtensionGroup() {
        if (lodSimpleExtensionGroup == null) {
            lodSimpleExtensionGroup = new ArrayList<>();
        }
        return this.lodSimpleExtensionGroup;
    }

    /**
     * Gets the value of the lodObjectExtensionGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the lodObjectExtensionGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLodObjectExtensionGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AbstractObjectType }
     * 
     * 
     */
    public List<AbstractObjectType> getLodObjectExtensionGroup() {
        if (lodObjectExtensionGroup == null) {
            lodObjectExtensionGroup = new ArrayList<>();
        }
        return this.lodObjectExtensionGroup;
    }

}
