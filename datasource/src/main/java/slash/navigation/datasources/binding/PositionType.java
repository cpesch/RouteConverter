//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2014.07.25 um 05:04:52 PM CEST 
//


package slash.navigation.datasources.binding;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 a position is a point on the globe defined by longitude and latitude
 *             
 * 
 * <p>Java-Klasse für positionType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="positionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="longitude" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="latitude" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "positionType")
public class PositionType {

    @XmlAttribute(name = "longitude", required = true)
    protected double longitude;
    @XmlAttribute(name = "latitude", required = true)
    protected double latitude;

    /**
     * Ruft den Wert der longitude-Eigenschaft ab.
     * 
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Legt den Wert der longitude-Eigenschaft fest.
     * 
     */
    public void setLongitude(double value) {
        this.longitude = value;
    }

    /**
     * Ruft den Wert der latitude-Eigenschaft ab.
     * 
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Legt den Wert der latitude-Eigenschaft fest.
     * 
     */
    public void setLatitude(double value) {
        this.latitude = value;
    }

}
