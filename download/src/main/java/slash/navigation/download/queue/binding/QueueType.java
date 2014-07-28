//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2014.07.25 um 04:59:54 PM CEST 
//


package slash.navigation.download.queue.binding;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * 
 *                 a queue contains queued downloads and a last sync time
 *             
 * 
 * <p>Java-Klasse für queueType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="queueType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="download" type="{http://www.routeconverter.de/xmlschemas/Queue/1.0}downloadType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="lastSync" use="required" type="{http://www.w3.org/2001/XMLSchema}dateTime" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "queueType", propOrder = {
    "download"
})
public class QueueType {

    protected List<DownloadType> download;
    @XmlAttribute(name = "lastSync", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar lastSync;

    /**
     * Gets the value of the download property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the download property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDownload().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DownloadType }
     * 
     * 
     */
    public List<DownloadType> getDownload() {
        if (download == null) {
            download = new ArrayList<DownloadType>();
        }
        return this.download;
    }

    /**
     * Ruft den Wert der lastSync-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getLastSync() {
        return lastSync;
    }

    /**
     * Legt den Wert der lastSync-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setLastSync(XMLGregorianCalendar value) {
        this.lastSync = value;
    }

}
