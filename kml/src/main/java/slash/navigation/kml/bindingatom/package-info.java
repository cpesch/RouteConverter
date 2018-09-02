/**
 * Contains the <a href="https://jaxb.dev.java.net/nonav/2.2.4/docs/api/">JAXB</a> binding for Atom.
 */
@javax.xml.bind.annotation.XmlSchema(namespace = ATOM_2005_NAMESPACE_URI, elementFormDefault = QUALIFIED, xmlns = {
        @XmlNs(prefix = "atom", namespaceURI = ATOM_2005_NAMESPACE_URI) })
package slash.navigation.kml.bindingatom;

import javax.xml.bind.annotation.XmlNs;

import static javax.xml.bind.annotation.XmlNsForm.QUALIFIED;
import static slash.navigation.kml.KmlUtil.ATOM_2005_NAMESPACE_URI;