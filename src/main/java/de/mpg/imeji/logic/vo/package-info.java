/**
 *
 */
/**
 * @author hnguyen
 *
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED,
    xmlns = {@XmlNs(prefix = "imeji", namespaceURI = "http://imeji.org/terms/"),
        @XmlNs(prefix = "imeji-metadata", namespaceURI = ImejiNamespaces.METADATA),
        @XmlNs(prefix = "dcterms", namespaceURI = "http://purl.org/dc/terms/"),
        @XmlNs(prefix = "dc", namespaceURI = "http://purl.org/dc/elements/1.1/"),
        @XmlNs(prefix = "eprofiles",
            namespaceURI = "http://purl.org/escidoc/metadata/profiles/0.1/"),
        @XmlNs(prefix = "eterms", namespaceURI = "http://purl.org/escidoc/metadata/terms/0.1/"),
        @XmlNs(prefix = "foaf", namespaceURI = "http://xmlns.com/foaf/0.1/"),
        @XmlNs(prefix = "rdfs", namespaceURI = "http://www.w3.org/2000/01/rdf-schema#"),
        @XmlNs(prefix = "rdf", namespaceURI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
        @XmlNs(prefix = "dcam", namespaceURI = "http://purl.org/dc/dcam/")})
package de.mpg.imeji.logic.vo;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;

import de.mpg.imeji.logic.ImejiNamespaces;

