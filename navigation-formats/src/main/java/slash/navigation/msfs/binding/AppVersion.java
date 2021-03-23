//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2021.03.23 um 11:23:21 AM CET 
//


package slash.navigation.msfs.binding;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für anonymous complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}AppVersionMajor"/>
 *         &lt;element ref="{}AppVersionBuild"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "appVersionMajor",
    "appVersionBuild"
})
@XmlRootElement(name = "AppVersion")
public class AppVersion {

    @XmlElement(name = "AppVersionMajor", required = true)
    protected BigInteger appVersionMajor;
    @XmlElement(name = "AppVersionBuild", required = true)
    protected BigInteger appVersionBuild;

    /**
     * Ruft den Wert der appVersionMajor-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getAppVersionMajor() {
        return appVersionMajor;
    }

    /**
     * Legt den Wert der appVersionMajor-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setAppVersionMajor(BigInteger value) {
        this.appVersionMajor = value;
    }

    /**
     * Ruft den Wert der appVersionBuild-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getAppVersionBuild() {
        return appVersionBuild;
    }

    /**
     * Legt den Wert der appVersionBuild-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setAppVersionBuild(BigInteger value) {
        this.appVersionBuild = value;
    }

}
