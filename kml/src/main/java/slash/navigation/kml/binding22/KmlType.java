//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-646 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.08.13 at 10:12:26 PM MESZ 
//


package slash.navigation.kml.binding22;

import slash.navigation.kml.binding22gx.TourType;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for KmlType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="KmlType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}NetworkLinkControl" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}AbstractFeatureGroup" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}KmlSimpleExtensionGroup" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}KmlObjectExtensionGroup" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="hint" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "KmlType", propOrder = {
    "networkLinkControl",
    "abstractFeatureGroup",
    "kmlSimpleExtensionGroup",
    "kmlObjectExtensionGroup"
})
public class KmlType {

    @XmlElement(name = "NetworkLinkControl")
    protected NetworkLinkControlType networkLinkControl;
    @XmlElementRef(name = "AbstractFeatureGroup", namespace = "http://www.opengis.net/kml/2.2", type = JAXBElement.class)
    protected JAXBElement<? extends AbstractFeatureType> abstractFeatureGroup;
    @XmlElement(name = "KmlSimpleExtensionGroup")
    @XmlSchemaType(name = "anySimpleType")
    protected List<Object> kmlSimpleExtensionGroup;
    @XmlElement(name = "KmlObjectExtensionGroup")
    protected List<AbstractObjectType> kmlObjectExtensionGroup;
    @XmlAttribute
    protected String hint;

    /**
     * Gets the value of the networkLinkControl property.
     * 
     * @return
     *     possible object is
     *     {@link NetworkLinkControlType }
     *     
     */
    public NetworkLinkControlType getNetworkLinkControl() {
        return networkLinkControl;
    }

    /**
     * Sets the value of the networkLinkControl property.
     * 
     * @param value
     *     allowed object is
     *     {@link NetworkLinkControlType }
     *     
     */
    public void setNetworkLinkControl(NetworkLinkControlType value) {
        this.networkLinkControl = value;
    }

    /**
     * Gets the value of the abstractFeatureGroup property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link AbstractFeatureType }{@code >}
     *     {@link JAXBElement }{@code <}{@link AbstractContainerType }{@code >}
     *     {@link JAXBElement }{@code <}{@link NetworkLinkType }{@code >}
     *     {@link JAXBElement }{@code <}{@link TourType }{@code >}
     *     {@link JAXBElement }{@code <}{@link FolderType }{@code >}
     *     {@link JAXBElement }{@code <}{@link PhotoOverlayType }{@code >}
     *     {@link JAXBElement }{@code <}{@link AbstractOverlayType }{@code >}
     *     {@link JAXBElement }{@code <}{@link ScreenOverlayType }{@code >}
     *     {@link JAXBElement }{@code <}{@link PlacemarkType }{@code >}
     *     {@link JAXBElement }{@code <}{@link GroundOverlayType }{@code >}
     *     {@link JAXBElement }{@code <}{@link DocumentType }{@code >}
     *     
     */
    public JAXBElement<? extends AbstractFeatureType> getAbstractFeatureGroup() {
        return abstractFeatureGroup;
    }

    /**
     * Sets the value of the abstractFeatureGroup property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link AbstractFeatureType }{@code >}
     *     {@link JAXBElement }{@code <}{@link AbstractContainerType }{@code >}
     *     {@link JAXBElement }{@code <}{@link NetworkLinkType }{@code >}
     *     {@link JAXBElement }{@code <}{@link TourType }{@code >}
     *     {@link JAXBElement }{@code <}{@link FolderType }{@code >}
     *     {@link JAXBElement }{@code <}{@link PhotoOverlayType }{@code >}
     *     {@link JAXBElement }{@code <}{@link AbstractOverlayType }{@code >}
     *     {@link JAXBElement }{@code <}{@link ScreenOverlayType }{@code >}
     *     {@link JAXBElement }{@code <}{@link PlacemarkType }{@code >}
     *     {@link JAXBElement }{@code <}{@link GroundOverlayType }{@code >}
     *     {@link JAXBElement }{@code <}{@link DocumentType }{@code >}
     *     
     */
    public void setAbstractFeatureGroup(JAXBElement<? extends AbstractFeatureType> value) {
        this.abstractFeatureGroup = value;
    }

    /**
     * Gets the value of the kmlSimpleExtensionGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the kmlSimpleExtensionGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getKmlSimpleExtensionGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getKmlSimpleExtensionGroup() {
        if (kmlSimpleExtensionGroup == null) {
            kmlSimpleExtensionGroup = new ArrayList<>();
        }
        return this.kmlSimpleExtensionGroup;
    }

    /**
     * Gets the value of the kmlObjectExtensionGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the kmlObjectExtensionGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getKmlObjectExtensionGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AbstractObjectType }
     * 
     * 
     */
    public List<AbstractObjectType> getKmlObjectExtensionGroup() {
        if (kmlObjectExtensionGroup == null) {
            kmlObjectExtensionGroup = new ArrayList<>();
        }
        return this.kmlObjectExtensionGroup;
    }

    /**
     * Gets the value of the hint property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHint() {
        return hint;
    }

    /**
     * Sets the value of the hint property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHint(String value) {
        this.hint = value;
    }

}
