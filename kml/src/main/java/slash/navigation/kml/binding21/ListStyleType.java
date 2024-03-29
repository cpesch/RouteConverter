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
import jakarta.xml.bind.annotation.adapters.HexBinaryAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for ListStyleType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ListStyleType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://earth.google.com/kml/2.1}ObjectType">
 *       &lt;sequence>
 *         &lt;element name="listItemType" type="{http://earth.google.com/kml/2.1}listItemTypeEnum" minOccurs="0"/>
 *         &lt;element name="bgColor" type="{http://earth.google.com/kml/2.1}color" minOccurs="0"/>
 *         &lt;element name="ItemIcon" type="{http://earth.google.com/kml/2.1}ItemIconType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ListStyleType", propOrder = {
    "listItemType",
    "bgColor",
    "itemIcon"
})
public class ListStyleType
    extends ObjectType
{

    @XmlElement(defaultValue = "check")
    protected ListItemTypeEnum listItemType;
    @XmlElement(type = String.class, defaultValue = "ffffffff")
    @XmlJavaTypeAdapter(HexBinaryAdapter.class)
    protected byte[] bgColor;
    @XmlElement(name = "ItemIcon")
    protected List<ItemIconType> itemIcon;

    /**
     * Gets the value of the listItemType property.
     * 
     * @return
     *     possible object is
     *     {@link ListItemTypeEnum }
     *     
     */
    public ListItemTypeEnum getListItemType() {
        return listItemType;
    }

    /**
     * Sets the value of the listItemType property.
     * 
     * @param value
     *     allowed object is
     *     {@link ListItemTypeEnum }
     *     
     */
    public void setListItemType(ListItemTypeEnum value) {
        this.listItemType = value;
    }

    /**
     * Gets the value of the bgColor property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public byte[] getBgColor() {
        return bgColor;
    }

    /**
     * Sets the value of the bgColor property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBgColor(byte[] value) {
        this.bgColor = value;
    }

    /**
     * Gets the value of the itemIcon property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the itemIcon property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getItemIcon().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ItemIconType }
     * 
     * 
     */
    public List<ItemIconType> getItemIcon() {
        if (itemIcon == null) {
            itemIcon = new ArrayList<>();
        }
        return this.itemIcon;
    }

}
