package de.mpg.imeji.rest.to.predefinedMetadataTO.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.apache.jena.vocabulary.RDF;

/**
 * The {@link RDF}.type of an object.
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MetadataTOType {
	public String value();
}
