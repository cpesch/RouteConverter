//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2014.07.25 um 05:04:52 PM CEST 
//


package slash.navigation.datasources.binding;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 a downloadable contains checksums, fragments and uri for a file relative to the base url
 *             
 * 
 * <p>Java-Klasse für downloadableType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="downloadableType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="checksum" type="{http://www.routeconverter.de/xmlschemas/Datasources/1.0}checksumType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="fragment" type="{http://www.routeconverter.de/xmlschemas/Datasources/1.0}fragmentType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="uri" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "downloadableType", propOrder = {
    "checksum",
    "fragment"
})
@XmlSeeAlso({
    FileType.class,
    ThemeType.class,
    MapType.class
})
public class DownloadableType {

    protected List<ChecksumType> checksum;
    protected List<FragmentType> fragment;
    @XmlAttribute(name = "uri", required = true)
    protected String uri;

    /**
     * Gets the value of the checksum property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the checksum property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getChecksum().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ChecksumType }
     * 
     * 
     */
    public List<ChecksumType> getChecksum() {
        if (checksum == null) {
            checksum = new ArrayList<ChecksumType>();
        }
        return this.checksum;
    }

    /**
     * Gets the value of the fragment property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the fragment property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFragment().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FragmentType }
     * 
     * 
     */
    public List<FragmentType> getFragment() {
        if (fragment == null) {
            fragment = new ArrayList<FragmentType>();
        }
        return this.fragment;
    }

    /**
     * Ruft den Wert der uri-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUri() {
        return uri;
    }

    /**
     * Legt den Wert der uri-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUri(String value) {
        this.uri = value;
    }

}
