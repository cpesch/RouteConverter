//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2014.07.25 um 05:04:52 PM CEST 
//


package slash.navigation.datasources.binding;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 a map is a downloadable with a bounding box
 *             
 * 
 * <p>Java-Klasse für mapType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="mapType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.routeconverter.de/xmlschemas/Datasources/1.0}downloadableType">
 *       &lt;sequence>
 *         &lt;element name="boundingBox" type="{http://www.routeconverter.de/xmlschemas/Datasources/1.0}boundingBoxType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mapType", propOrder = {
    "boundingBox"
})
public class MapType
    extends DownloadableType
{

    protected BoundingBoxType boundingBox;

    /**
     * Ruft den Wert der boundingBox-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link BoundingBoxType }
     *     
     */
    public BoundingBoxType getBoundingBox() {
        return boundingBox;
    }

    /**
     * Legt den Wert der boundingBox-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link BoundingBoxType }
     *     
     */
    public void setBoundingBox(BoundingBoxType value) {
        this.boundingBox = value;
    }

}
