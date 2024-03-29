//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-646 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.08.13 at 10:12:26 PM MESZ 
//


package slash.navigation.kml.binding22;

import slash.navigation.kml.binding22gx.SimpleArrayDataType;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for SchemaDataType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SchemaDataType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.opengis.net/kml/2.2}AbstractObjectType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}SimpleData" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/kml/2.2}SchemaDataExtension" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="schemaUrl" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SchemaDataType", propOrder = {
    "simpleData",
    "schemaDataExtension"
})
public class SchemaDataType
    extends AbstractObjectType
{

    @XmlElement(name = "SimpleData")
    protected List<SimpleDataType> simpleData;
    @XmlElementRef(name = "SchemaDataExtension", namespace = "http://www.opengis.net/kml/2.2", type = JAXBElement.class)
    protected List<JAXBElement<?>> schemaDataExtension;
    @XmlAttribute
    @XmlSchemaType(name = "anyURI")
    protected String schemaUrl;

    /**
     * Gets the value of the simpleData property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the simpleData property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSimpleData().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SimpleDataType }
     * 
     * 
     */
    public List<SimpleDataType> getSimpleData() {
        if (simpleData == null) {
            simpleData = new ArrayList<>();
        }
        return this.simpleData;
    }

    /**
     * Gets the value of the schemaDataExtension property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the schemaDataExtension property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSchemaDataExtension().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link Object }{@code >}
     * {@link JAXBElement }{@code <}{@link SimpleArrayDataType }{@code >}
     * 
     * 
     */
    public List<JAXBElement<?>> getSchemaDataExtension() {
        if (schemaDataExtension == null) {
            schemaDataExtension = new ArrayList<>();
        }
        return this.schemaDataExtension;
    }

    /**
     * Gets the value of the schemaUrl property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSchemaUrl() {
        return schemaUrl;
    }

    /**
     * Sets the value of the schemaUrl property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSchemaUrl(String value) {
        this.schemaUrl = value;
    }

}
