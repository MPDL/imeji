// package de.mpg.imeji.logic.search.model;
//
// import java.util.HashMap;
// import java.util.Map;
//
// import org.apache.log4j.Logger;
//
// import com.hp.hpl.jena.vocabulary.RDF;
//
// import de.mpg.imeji.exceptions.BadRequestException;
// import de.mpg.imeji.logic.ImejiNamespaces;
//
/// **
// * Initialize the imeji {@link SearchIndex}
// *
// * @author saquet (initial creation)
// * @author $Author$ (last modification)
// * @version $Revision$ $LastChangedDate$
// */
// public class SearchIndexes {
// private static final Logger LOGGER = Logger.getLogger(SearchIndexes.class);
// public static final Map<String, SearchIndex> indexes = SearchIndexes.init();
//
// /**
// * Get {@link SearchIndex} from its {@link String} name
// *
// * @param indexName
// * @return
// * @throws BadRequestException
// */
// public static SearchIndex getIndex(String indexName) {
// try {
// SearchIndex index = indexes.get(indexName);
// if (index == null) {
// index = new SearchIndex(SearchFields.valueOf(indexName));
// }
// return index;
// } catch (final Exception e) {
// LOGGER.error("Unknown index: " + indexName, e);
// }
// return null;
// }
//
// /**
// * Get {@link SearchIndex} from its {@link SearchFields}
// *
// * @param indexname
// * @return
// * @throws BadRequestException
// */
// public static SearchIndex getIndex(SearchFields indexname) {
// return getIndex(indexname.name());
// }
//
// /**
// * Initialize all {@link SearchIndex} in imeji
// *
// * @return
// */
// public static Map<String, SearchIndex> init() {
// final Map<String, SearchIndex> indexes = new HashMap<String, SearchIndex>();
// indexes.putAll(initBasisIndexes());
// indexes.putAll(initMetadataIndexes());
// return indexes;
// }
//
// /**
// * Initialize all {@link SearchIndex} which are not related to {@link Metadata}
// *
// * @return
// */
// private static Map<String, SearchIndex> initBasisIndexes() {
// Map<String, SearchIndex> indexes = new HashMap<String, SearchIndex>();
// indexes =
// put(indexes, new SearchIndex(SearchFields.member.name(), "http://imeji.org/terms/item"));
// indexes =
// put(indexes, new SearchIndex(SearchFields.hasgrant.name(), "http://imeji.org/terms/user"));
// /**
// * Fulltext search index
// */
// indexes =
// put(indexes, new SearchIndex(SearchFields.all.name(), "http://imeji.org/terms/fulltext"));
// /**
// * Properties indexes
// */
// indexes =
// put(indexes, new SearchIndex(SearchFields.creator_id.name(), ImejiNamespaces.CREATOR));
// indexes =
// put(indexes, new SearchIndex(SearchFields.editor.name(), ImejiNamespaces.MODIFIED_BY));
// indexes =
// put(indexes, new SearchIndex(SearchFields.created.name(), ImejiNamespaces.DATE_CREATED));
// indexes = put(indexes,
// new SearchIndex(SearchFields.modified.name(), ImejiNamespaces.LAST_MODIFICATION_DATE));
// indexes = put(indexes, new SearchIndex(SearchFields.status.name(), ImejiNamespaces.STATUS));
// indexes = put(indexes,
// new SearchIndex(SearchFields.checksum.name(), "http://imeji.org/terms/checksum"));
// /**
// * Grant indexes
// */
// indexes = put(indexes, new SearchIndex(SearchFields.grant.name(),
// "http://xmlns.com/foaf/0.1/grants", indexes.get(SearchFields.creator_id.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.grant_type.name(),
// "http://imeji.org/terms/grantType", indexes.get(SearchFields.grant.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.grant_for.name(),
// "http://imeji.org/terms/grantFor", indexes.get(SearchFields.grant.name())));
// /**
// * Item Indexes
// */
// indexes = put(indexes,
// new SearchIndex(SearchFields.filename.name(), "http://imeji.org/terms/filename"));
// indexes = put(indexes,
// new SearchIndex(SearchFields.filetype.name(), "http://imeji.org/terms/filetype"));
// indexes = put(indexes,
// new SearchIndex(SearchFields.visibility.name(), "http://imeji.org/terms/visibility"));
// /**
// * Collection indexes
// */
// indexes =
// put(indexes, new SearchIndex(SearchFields.col.name(), "http://imeji.org/terms/collection"));
// indexes = put(indexes,
// new SearchIndex(SearchFields.title.name(), "http://purl.org/dc/elements/1.1/title"));
// indexes = put(indexes, new SearchIndex(SearchFields.description.name(),
// "http://purl.org/dc/elements/1.1/description"));
// indexes = put(indexes,
// new SearchIndex(SearchFields.cont_person.name(),
// "http://purl.org/escidoc/metadata/terms/0.1/creator",
// indexes.get(SearchFields.cont_md.name())));
// indexes = put(indexes,
// new SearchIndex(SearchFields.author_familyname.name(),
// "http://purl.org/escidoc/metadata/terms/0.1/family-name",
// indexes.get(SearchFields.cont_person.name())));
// indexes = put(indexes,
// new SearchIndex(SearchFields.author_givenname.name(),
// "http://purl.org/escidoc/metadata/terms/0.1/given-name",
// indexes.get(SearchFields.cont_person.name())));
// indexes = put(indexes,
// new SearchIndex(SearchFields.author_name.name(),
// "http://purl.org/escidoc/metadata/terms/0.1/complete-name",
// indexes.get(SearchFields.cont_person.name())));
// indexes = put(indexes,
// new SearchIndex(SearchFields.cont_person_org.name(),
// "http://purl.org/escidoc/metadata/profiles/0.1/organizationalunit",
// indexes.get(SearchFields.cont_person.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.author_org_name.name(),
// "http://purl.org/dc/elements/1.1/title", indexes.get(SearchFields.cont_person_org.name())));
// /**
// * Metadata profile indexes
// */
// indexes =
// put(indexes, new SearchIndex(SearchFields.prof.name(), "http://imeji.org/terms/mdprofile"));
// /**
// * Image Metadata indexes
// */
// indexes = put(indexes, new SearchIndex(SearchFields.md.name(), ImejiNamespaces.METADATA));
// indexes = put(indexes,
// new SearchIndex(SearchFields.statement.name(), "http://imeji.org/terms/statement"));
// indexes = put(indexes, new SearchIndex(SearchFields.metadatatype.name(),
// RDF.type.getNameSpace(), indexes.get(SearchFields.md.name())));
// return indexes;
// }
//
// /**
// * Initialized all {@link SearchIndex} related to {@link Metadata}
// *
// * @return
// */
// private static Map<String, SearchIndex> initMetadataIndexes() {
// Map<String, SearchIndex> indexes = new HashMap<String, SearchIndex>();
// indexes = put(indexes, new SearchIndex(SearchFields.text.name(), "http://imeji.org/terms/text",
// indexes.get(SearchFields.md.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.number.name(),
// "http://imeji.org/terms/number", indexes.get(SearchFields.md.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.date.name(), "http://imeji.org/terms/date",
// indexes.get(SearchFields.md.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.time.name(), "http://imeji.org/terms/time",
// indexes.get(SearchFields.md.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.location.name(),
// "http://purl.org/dc/terms/title", indexes.get(SearchFields.md.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.license.name(),
// "http://imeji.org/terms/license", indexes.get(SearchFields.md.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.url.name(), "http://imeji.org/terms/uri",
// indexes.get(SearchFields.md.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.label.name(),
// "http://www.w3.org/2000/01/rdf-schema#label", indexes.get(SearchFields.md.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.citation.name(),
// "http://imeji.org/terms/citation", indexes.get(SearchFields.md.name())));
//
// indexes = put(indexes, new SearchIndex(SearchFields.cone.name(),
// "http://imeji.org/terms/coneId", indexes.get(SearchFields.md.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.person.name(),
// "http://xmlns.com/foaf/0.1/person", indexes.get(SearchFields.md.name())));
// indexes = put(indexes,
// new SearchIndex(SearchFields.person_completename.name(),
// "http://purl.org/escidoc/metadata/terms/0.1/complete-name",
// indexes.get(SearchFields.person.name())));
// indexes = put(indexes,
// new SearchIndex(SearchFields.person_family.name(),
// "http://purl.org/escidoc/metadata/terms/0.1/family-name",
// indexes.get(SearchFields.person.name())));
// indexes = put(indexes,
// new SearchIndex(SearchFields.person_given.name(),
// "http://purl.org/escidoc/metadata/terms/0.1/given-name",
// indexes.get(SearchFields.person.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.person_id.name(),
// "http://purl.org/dc/elements/1.1/identifier", indexes.get(SearchFields.person.name())));
// indexes = put(indexes,
// new SearchIndex(SearchFields.person_role.name(),
// "http://purl.org/escidoc/metadata/terms/0.1/role",
// indexes.get(SearchFields.person.name())));
// indexes = put(indexes,
// new SearchIndex(SearchFields.person_org.name(),
// "http://purl.org/escidoc/metadata/profiles/0.1/organizationalunit",
// indexes.get(SearchFields.person.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.person_org_name.name(),
// "http://purl.org/dc/terms/title", indexes.get(SearchFields.person_org.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.person_org_id.name(),
// "http://purl.org/dc/terms/identifier", indexes.get(SearchFields.person_org.name())));
// indexes = put(indexes, new SearchIndex(SearchFields.person_org_description.name(),
// "http://purl.org/dc/terms/description", indexes.get(SearchFields.person_org.name())));
// indexes = put(indexes,
// new SearchIndex(SearchFields.person_org_city.name(),
// "http://purl.org/escidoc/metadata/terms/0.1/city",
// indexes.get(SearchFields.person_org.name())));
// indexes = put(indexes,
// new SearchIndex(SearchFields.person_org_country.name(),
// "http://purl.org/escidoc/metadata/terms/0.1/country",
// indexes.get(SearchFields.person_org.name())));
// return indexes;
// }
//
// private static Map<String, SearchIndex> put(Map<String, SearchIndex> map, SearchIndex index) {
// map.put(index.getName(), index);
// return map;
// }
// }
