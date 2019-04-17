//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2019.04.17 um 08:52:04 PM CEST 
//


package slash.navigation.nominatim.reverse;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für reversegeocodeType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="reversegeocodeType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="result" type="{}resultType"/>
 *         &lt;element name="addressparts" type="{}addresspartsType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="timestamp" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="attribution" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="querystring" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "reversegeocodeType", propOrder = {
    "result",
    "addressparts"
})
public class ReversegeocodeType {

    @XmlElement(required = true)
    protected ResultType result;
    @XmlElement(required = true)
    protected AddresspartsType addressparts;
    @XmlAttribute(name = "timestamp")
    protected String timestamp;
    @XmlAttribute(name = "attribution")
    protected String attribution;
    @XmlAttribute(name = "querystring")
    protected String querystring;

    /**
     * Ruft den Wert der result-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link ResultType }
     *     
     */
    public ResultType getResult() {
        return result;
    }

    /**
     * Legt den Wert der result-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link ResultType }
     *     
     */
    public void setResult(ResultType value) {
        this.result = value;
    }

    /**
     * Ruft den Wert der addressparts-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link AddresspartsType }
     *     
     */
    public AddresspartsType getAddressparts() {
        return addressparts;
    }

    /**
     * Legt den Wert der addressparts-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link AddresspartsType }
     *     
     */
    public void setAddressparts(AddresspartsType value) {
        this.addressparts = value;
    }

    /**
     * Ruft den Wert der timestamp-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Legt den Wert der timestamp-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTimestamp(String value) {
        this.timestamp = value;
    }

    /**
     * Ruft den Wert der attribution-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAttribution() {
        return attribution;
    }

    /**
     * Legt den Wert der attribution-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAttribution(String value) {
        this.attribution = value;
    }

    /**
     * Ruft den Wert der querystring-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getQuerystring() {
        return querystring;
    }

    /**
     * Legt den Wert der querystring-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setQuerystring(String value) {
        this.querystring = value;
    }

}
