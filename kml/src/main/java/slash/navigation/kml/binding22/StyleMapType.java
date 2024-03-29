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
 * <p>Java class for StyleMapType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StyleMapType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.opengis.net/kml/2.2}AbstractStyleSelectorType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}Pair" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}StyleMapSimpleExtensionGroup" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}StyleMapObjectExtensionGroup" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StyleMapType", propOrder = {
    "pair",
    "styleMapSimpleExtensionGroup",
    "styleMapObjectExtensionGroup"
})
public class StyleMapType
    extends AbstractStyleSelectorType
{

    @XmlElement(name = "Pair")
    protected List<PairType> pair;
    @XmlElement(name = "StyleMapSimpleExtensionGroup")
    @XmlSchemaType(name = "anySimpleType")
    protected List<Object> styleMapSimpleExtensionGroup;
    @XmlElement(name = "StyleMapObjectExtensionGroup")
    protected List<AbstractObjectType> styleMapObjectExtensionGroup;

    /**
     * Gets the value of the pair property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the pair property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPair().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PairType }
     * 
     * 
     */
    public List<PairType> getPair() {
        if (pair == null) {
            pair = new ArrayList<>();
        }
        return this.pair;
    }

    /**
     * Gets the value of the styleMapSimpleExtensionGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the styleMapSimpleExtensionGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getStyleMapSimpleExtensionGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getStyleMapSimpleExtensionGroup() {
        if (styleMapSimpleExtensionGroup == null) {
            styleMapSimpleExtensionGroup = new ArrayList<>();
        }
        return this.styleMapSimpleExtensionGroup;
    }

    /**
     * Gets the value of the styleMapObjectExtensionGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the styleMapObjectExtensionGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getStyleMapObjectExtensionGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AbstractObjectType }
     * 
     * 
     */
    public List<AbstractObjectType> getStyleMapObjectExtensionGroup() {
        if (styleMapObjectExtensionGroup == null) {
            styleMapObjectExtensionGroup = new ArrayList<>();
        }
        return this.styleMapObjectExtensionGroup;
    }

}
