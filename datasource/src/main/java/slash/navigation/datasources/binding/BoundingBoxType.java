//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2014.07.25 um 05:04:52 PM CEST 
//


package slash.navigation.datasources.binding;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 a bounding box is a north east and a south west position
 *             
 * 
 * <p>Java-Klasse für boundingBoxType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="boundingBoxType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="northEast" type="{http://www.routeconverter.de/xmlschemas/Datasources/1.0}positionType"/>
 *         &lt;element name="southWest" type="{http://www.routeconverter.de/xmlschemas/Datasources/1.0}positionType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "boundingBoxType", propOrder = {
    "northEast",
    "southWest"
})
public class BoundingBoxType {

    @XmlElement(required = true)
    protected PositionType northEast;
    @XmlElement(required = true)
    protected PositionType southWest;

    /**
     * Ruft den Wert der northEast-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link PositionType }
     *     
     */
    public PositionType getNorthEast() {
        return northEast;
    }

    /**
     * Legt den Wert der northEast-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link PositionType }
     *     
     */
    public void setNorthEast(PositionType value) {
        this.northEast = value;
    }

    /**
     * Ruft den Wert der southWest-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link PositionType }
     *     
     */
    public PositionType getSouthWest() {
        return southWest;
    }

    /**
     * Legt den Wert der southWest-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link PositionType }
     *     
     */
    public void setSouthWest(PositionType value) {
        this.southWest = value;
    }

}
