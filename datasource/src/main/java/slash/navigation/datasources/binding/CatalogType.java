//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.10.28 at 03:39:26 PM CET 
//


package slash.navigation.datasources.binding;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 *                 catalog contains edition and datasource definitions.
 *             
 * 
 * <p>Java class for catalogType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="catalogType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="edition" type="{http://api.routeconverter.com/v1/schemas/datasource-catalog}editionType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="datasource" type="{http://api.routeconverter.com/v1/schemas/datasource-catalog}datasourceType" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "catalogType", propOrder = {
    "edition",
    "datasource"
})
public class CatalogType {

    protected List<EditionType> edition;
    @XmlElement(required = true)
    protected List<DatasourceType> datasource;

    /**
     * Gets the value of the edition property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the edition property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEdition().add(newItem);
     * </pre>
     * 
     *
     * @return
     * Objects of the following type(s) are allowed in the list
     * {@link EditionType }
     */
    public List<EditionType> getEdition() {
        if (edition == null) {
            edition = new ArrayList<>();
        }
        return this.edition;
    }

    /**
     * Gets the value of the datasource property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the datasource property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDatasource().add(newItem);
     * </pre>
     * 
     *
     * @return
     * Objects of the following type(s) are allowed in the list
     * {@link DatasourceType }
     */
    public List<DatasourceType> getDatasource() {
        if (datasource == null) {
            datasource = new ArrayList<>();
        }
        return this.datasource;
    }

}
