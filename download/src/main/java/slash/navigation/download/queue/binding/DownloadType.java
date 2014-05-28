//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2014.07.25 um 04:59:54 PM CEST 
//


package slash.navigation.download.queue.binding;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 a download refers to a downloadable, a file/map/theme, optionally fragments and
 *                 adds data to perform the download, resume, extraction, validation
 *             
 * 
 * <p>Java-Klasse für downloadType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="downloadType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="downloadable" type="{http://www.routeconverter.de/xmlschemas/Queue/1.0}downloadableType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="description" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="url" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="action" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="eTag" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="state" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="tempFile" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "downloadType", propOrder = {
    "downloadable"
})
public class DownloadType {

    @XmlElement(required = true)
    protected DownloadableType downloadable;
    @XmlAttribute(name = "description", required = true)
    protected String description;
    @XmlAttribute(name = "url", required = true)
    protected String url;
    @XmlAttribute(name = "action", required = true)
    protected String action;
    @XmlAttribute(name = "eTag", required = true)
    protected String eTag;
    @XmlAttribute(name = "state", required = true)
    protected String state;
    @XmlAttribute(name = "tempFile", required = true)
    protected String tempFile;

    /**
     * Ruft den Wert der downloadable-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link DownloadableType }
     *     
     */
    public DownloadableType getDownloadable() {
        return downloadable;
    }

    /**
     * Legt den Wert der downloadable-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link DownloadableType }
     *     
     */
    public void setDownloadable(DownloadableType value) {
        this.downloadable = value;
    }

    /**
     * Ruft den Wert der description-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Legt den Wert der description-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Ruft den Wert der url-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUrl() {
        return url;
    }

    /**
     * Legt den Wert der url-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUrl(String value) {
        this.url = value;
    }

    /**
     * Ruft den Wert der action-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAction() {
        return action;
    }

    /**
     * Legt den Wert der action-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAction(String value) {
        this.action = value;
    }

    /**
     * Ruft den Wert der eTag-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getETag() {
        return eTag;
    }

    /**
     * Legt den Wert der eTag-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setETag(String value) {
        this.eTag = value;
    }

    /**
     * Ruft den Wert der state-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getState() {
        return state;
    }

    /**
     * Legt den Wert der state-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setState(String value) {
        this.state = value;
    }

    /**
     * Ruft den Wert der tempFile-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTempFile() {
        return tempFile;
    }

    /**
     * Legt den Wert der tempFile-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTempFile(String value) {
        this.tempFile = value;
    }

}
