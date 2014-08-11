//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2014.07.25 um 05:04:52 PM CEST 
//


package slash.navigation.datasources.binding;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the slash.navigation.datasources.binding package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Datasources_QNAME = new QName("http://www.routeconverter.de/xmlschemas/Datasources/1.0", "datasources");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: slash.navigation.datasources.binding
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link DatasourcesType }
     * 
     */
    public DatasourcesType createDatasourcesType() {
        return new DatasourcesType();
    }

    /**
     * Create an instance of {@link DatasourceType }
     * 
     */
    public DatasourceType createDatasourceType() {
        return new DatasourceType();
    }

    /**
     * Create an instance of {@link FileType }
     * 
     */
    public FileType createFileType() {
        return new FileType();
    }

    /**
     * Create an instance of {@link FragmentType }
     * 
     */
    public FragmentType createFragmentType() {
        return new FragmentType();
    }

    /**
     * Create an instance of {@link PositionType }
     * 
     */
    public PositionType createPositionType() {
        return new PositionType();
    }

    /**
     * Create an instance of {@link ChecksumType }
     * 
     */
    public ChecksumType createChecksumType() {
        return new ChecksumType();
    }

    /**
     * Create an instance of {@link DownloadableType }
     * 
     */
    public DownloadableType createDownloadableType() {
        return new DownloadableType();
    }

    /**
     * Create an instance of {@link BoundingBoxType }
     * 
     */
    public BoundingBoxType createBoundingBoxType() {
        return new BoundingBoxType();
    }

    /**
     * Create an instance of {@link ThemeType }
     * 
     */
    public ThemeType createThemeType() {
        return new ThemeType();
    }

    /**
     * Create an instance of {@link MapType }
     * 
     */
    public MapType createMapType() {
        return new MapType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DatasourcesType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.routeconverter.de/xmlschemas/Datasources/1.0", name = "datasources")
    public JAXBElement<DatasourcesType> createDatasources(DatasourcesType value) {
        return new JAXBElement<DatasourcesType>(_Datasources_QNAME, DatasourcesType.class, null, value);
    }

}
