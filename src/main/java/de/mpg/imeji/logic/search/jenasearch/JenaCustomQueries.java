package de.mpg.imeji.logic.search.jenasearch;

import java.net.URI;
import java.util.Calendar;

import de.mpg.imeji.logic.ImejiNamespaces;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.ObjectHelper.ObjectType;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.util.DateHelper;

/**
 * SPARQL queries for imeji
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class JenaCustomQueries {

  private static final String X_PATH_FUNCTIONS_DECLARATION =
      " PREFIX fn: <http://www.w3.org/2005/xpath-functions#> ";
  private static final String XSD_DECLARATION = " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

  private JenaCustomQueries() {
    // private constructor
  }

  /**
   * Select all {@link Username}
   *
   * @return
   */
  public static final String selectOrganizationByName(String name) {
    return X_PATH_FUNCTIONS_DECLARATION
        + "  SELECT DISTINCT ?s WHERE {?person <http://purl.org/escidoc/metadata/profiles/0.1/organizationalunit> ?s . ?s <http://purl.org/dc/terms/title> ?name . filter(regex(?name, '"
        + name + "','i'))}";
  }

  /**
   * Select all {@link Username}
   *
   * @return
   */
  public static final String selectPersonByName(String name) {
    return X_PATH_FUNCTIONS_DECLARATION
        + "  SELECT DISTINCT ?s WHERE {?s <http://purl.org/escidoc/metadata/terms/0.1/complete-name>  ?name . filter(regex(?name, '"
        + name + "','i'))}";
  }

  /**
   * Select a User by its Email
   *
   * @param email
   * @return
   */
  public static final String selectUserByEmail(String email) {
    return X_PATH_FUNCTIONS_DECLARATION
        + "  SELECT DISTINCT ?s WHERE { ?s a  <http://imeji.org/terms/user>. "
        + " ?s <http://xmlns.com/foaf/0.1/email> \"" + email
        + "\"^^<http://www.w3.org/2001/XMLSchema#string> }";
  }

  /**
   * Select all subscription by its objectId
   * 
   * @param objectId
   * @return
   */
  public static final String selectSubscriptionByObjectId(String objectId) {
    return X_PATH_FUNCTIONS_DECLARATION
        + "  SELECT DISTINCT ?s WHERE {?s <http://imeji.org/terms/objectId> \"" + objectId
        + "\"^^<http://www.w3.org/2001/XMLSchema#string> }";
  }

  /**
   * Select all subscriptions for the user with this id
   * 
   * @param email
   * @return
   */
  public static final String selectSubscriptionByUserId(String userId) {
    return X_PATH_FUNCTIONS_DECLARATION
        + "  SELECT DISTINCT ?s WHERE {?s <http://imeji.org/terms/userId> \"" + userId
        + "\"^^<http://www.w3.org/2001/XMLSchema#string> }";
  }

  public static final String selectSubscriptionAll() {
    return X_PATH_FUNCTIONS_DECLARATION
        + "  SELECT DISTINCT ?s WHERE {?s a <http://imeji.org/terms/subscription>}";
  }


  /**
   * Select a User by its Email
   *
   * @param email
   * @return
   */
  public static final String selectUserByRegistrationToken(String registrationToken) {
    return X_PATH_FUNCTIONS_DECLARATION + "  SELECT DISTINCT ?s WHERE { "
        + " ?s <http://imeji.org/terms/registrationToken> \"" + registrationToken
        + "\"^^<http://www.w3.org/2001/XMLSchema#string> . "
        + " ?s a <http://imeji.org/terms/user> }";
  }

  /**
   * Find all the user which have SysAdmin rights for imeji
   *
   * @return
   */
  public static final String selectUserSysAdmin() {
    return X_PATH_FUNCTIONS_DECLARATION
        + " SELECT DISTINCT ?s WHERE {?s <http://imeji.org/terms/grant>  \"ADMIN,"
        + Imeji.PROPERTIES.getBaseURI() + "\"^^<http://www.w3.org/2001/XMLSchema#string>}";
  }

  /**
   * Select all {@link UserGroup}
   *
   * @return
   */
  public static final String selectUserGroupAll(String name) {
    return X_PATH_FUNCTIONS_DECLARATION
        + "  SELECT DISTINCT ?s WHERE {?s a <http://imeji.org/terms/userGroup> . ?s <http://xmlns.com/foaf/0.1/name> ?name . filter(regex(?name, '"
        + name + "','i'))}";
  }

  public static final String selectUserGroupByName(String name) {
    return X_PATH_FUNCTIONS_DECLARATION
        + "  SELECT DISTINCT ?s WHERE {?s a <http://imeji.org/terms/userGroup> . ?s <http://xmlns.com/foaf/0.1/name> \""
        + name + "\"^^<http://www.w3.org/2001/XMLSchema#string>}";
  }

  /**
   * Select all {@link UserGroup}
   *
   * @return
   */
  public static final String selectUserGroupAll() {
    return X_PATH_FUNCTIONS_DECLARATION
        + "  SELECT DISTINCT ?s WHERE {?s a <http://imeji.org/terms/userGroup>}";
  }

  /**
   * Select {@link UserGroup} of User
   *
   * @return
   */
  public static final String selectUserGroupOfUser(User user) {
    return X_PATH_FUNCTIONS_DECLARATION
        + "  SELECT DISTINCT ?s WHERE {?s a <http://imeji.org/terms/userGroup> . ?s <http://xmlns.com/foaf/0.1/member> <"
        + user.getId().toString() + ">}";
  }


  /**
   * Select Users to be notified by file download Note: Current <code>user</code> is excluded from
   * the result set
   *
   * @return
   * @param user
   * @param c
   */
  public static final String selectUsersToBeNotifiedByFileDownload(User user, CollectionImeji c) {
    return X_PATH_FUNCTIONS_DECLARATION + "  " + "SELECT DISTINCT ?s WHERE {" + "filter(?c='"
        + ObjectHelper.getId(c.getId()) + "'"
        + (user != null ? " && ?s!=<" + user.getId().toString() + "> " : "")
        + ") . ?s <http://imeji.org/terms/observedCollections> ?c }";
  }

  /**
   * Find the collection of an item and return its uri
   *
   * @param fileUrl
   * @return
   */
  public static final String selectCollectionIdOfItem(String itemUri) {
    return " SELECT DISTINCT ?s WHERE {<" + itemUri
        + "> <http://imeji.org/terms/collection> ?s} LIMIT 1 ";
  }

  /**
   * @param fileUrl
   * @return
   */
  public static final String selectItemIdOfFileUrl(String fileUrl) {
    final String path = URI.create(fileUrl).getPath();
    return X_PATH_FUNCTIONS_DECLARATION + "  SELECT DISTINCT ?s WHERE {"
        + "?s <http://imeji.org/terms/webImageUrl> ?url1 . ?s <http://imeji.org/terms/thumbnailImageUrl> ?url2 . ?s <http://imeji.org/terms/fullImageUrl> ?url3 . FILTER(REGEX(str(?url1), '"
        + path + "', 'i') || REGEX(str(?url2), '" + path + "', 'i') || REGEX(str(?url3), '" + path
        + "', 'i'))} LIMIT 1 ";
  }

  /**
   * Selecte the item id for the file with the passed fileStorage
   *
   * @param storageId
   * @return
   */
  public static final String selectItemOfFile(String fileUrl) {
    return X_PATH_FUNCTIONS_DECLARATION + XSD_DECLARATION
        + "SELECT DISTINCT  (str(?id) AS ?s) WHERE {?content ?p \"" + fileUrl
        + "\"^^<http://www.w3.org/2001/XMLSchema#string> . ?content <http://imeji.org/terms/itemId> ?id} limit 1";
  }


  /**
   * Select all {@link Grant} which are not valid anymore. For instance, when a {@link User}, or an
   * object ( {@link CollectionImeji}, {@link Album}) is deleted, some related {@link Grant} might
   * stay in the database, even if they are not needed anymore.
   *
   * @return
   */
  public static final String selectGrantWithoutUser() {
    return X_PATH_FUNCTIONS_DECLARATION
        + "  SELECT DISTINCT ?s WHERE { ?s <http://imeji.org/terms/grantType> ?type"
        + " . not exists{ ?user <http://imeji.org/terms/grant> ?s}}";
  }

  /**
   * Remove the grants withtout users
   *
   * @return
   */
  public static final String removeGrantWithoutUser() {
    return "WITH <http://imeji.org/user> "
        + "DELETE {?user <http://imeji.org/terms/grant> ?s . ?s ?p ?o}  "
        + "USING <http://imeji.org/user> " + "WHERE { ?s <http://imeji.org/terms/grantType> ?type "
        + ". not exists{ ?user <http://imeji.org/terms/grant> ?s} . ?s ?p ?o}";
  }

  /**
   * Select {@link Grant} which don't have any triple
   *
   * @return
   */
  public static final String selectGrantEmtpy() {
    return "SELECT DISTINCT ?s WHERE {?user <http://imeji.org/terms/grant> ?s . not exists{?s ?p ?o}}";
  }

  /**
   * Remove the emtpy Grants
   *
   * @return
   */
  public static final String removeGrantEmtpy() {
    return "WITH <http://imeji.org/user> DELETE {?user <http://imeji.org/terms/grant> ?s} USING <http://imeji.org/user> WHERE {?user <http://imeji.org/terms/grant> ?s . not exists{?s ?p ?o}}";
  }

  /**
   * Select Grant which don't have a grantfor
   *
   * @return
   */
  public static final String selectGrantWithoutObjects() {
    return X_PATH_FUNCTIONS_DECLARATION
        + "  SELECT DISTINCT ?s WHERE {?s <http://imeji.org/terms/grantFor> ?for"
        + " . not exists{?for ?p ?o} .filter (?for!= <http://imeji.org/> &&  ?for != <"
        + Imeji.PROPERTIES.getBaseURI() + ">)}";
  }

  /**
   * Remove Grant which don't have a grantfor
   *
   * @return
   */
  public static final String removeGrantWithoutObject() {
    return "WITH <http://imeji.org/user> " + "DELETE {?s ?prop ?sub}"
        + "USING <http://imeji.org/user> " + "USING <http://imeji.org/item> "
        + "USING <http://imeji.org/collection> " + "USING <http://imeji.org/album> "
        + "USING <http://imeji.org/metadataProfile> "
        + " WHERE {?s <http://imeji.org/terms/grantFor> ?for"
        + " . not exists{?for ?p ?o} .filter (?for!= <http://imeji.org/> &&  ?for != <"
        + Imeji.PROPERTIES.getBaseURI() + ">) . ?s ?prop ?sub}";
  }

  /**
   * Delete all grants granting for the given uri
   *
   * @param uri
   * @return
   */
  public static final String updateRemoveGrantsFor(String uri) {
    return "WITH <http://imeji.org/user> DELETE {?user <http://imeji.org/terms/grant> ?s . ?s ?p ?o } "
        + "USING <http://imeji.org/user> "
        + " WHERE {?user <http://imeji.org/terms/grant> ?s . ?s <http://imeji.org/terms/grantFor> <"
        + uri + "> . ?s ?p ?o}";
  }

  /**
   * Update the <http://imeji.org/terms/collection> with the new uri
   * 
   * @param oldUri
   * @param newUri
   * @return
   */
  public static final String updateCollectionParent(String id, String oldUri, String newUri) {
    String q = "WITH <http://imeji.org/collection> ";
    if (oldUri != null) {
      q += "DELETE {<" + id + "> <http://imeji.org/terms/collection>  <" + oldUri + ">} ";
    }
    q += "INSERT {<" + id + "> <http://imeji.org/terms/collection>  <" + newUri + ">} "
        + "USING <http://imeji.org/collection> WHERE {<" + id
        + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://imeji.org/terms/collection>}";
    return q;
  }

  /**
   * Update Query to release a collection or an item
   * 
   * @param id
   * @return
   */
  public static final String updateReleaseObject(String id, Calendar date) {
    String model =
        ObjectHelper.getObjectType(URI.create(id)) == ObjectType.ITEM ? "item" : "collection";
    String q = "WITH <http://imeji.org/" + model + "> ";
    q += " DELETE{<" + id
        + "> <http://imeji.org/terms/status> <http://imeji.org/terms/status#PENDING>} ";
    q += "INSERT{<" + id
        + "> <http://imeji.org/terms/status> <http://imeji.org/terms/status#RELEASED> . <" + id
        + "> <http://purl.org/dc/terms/issued> \"" + DateHelper.printJenaDate(date)
        + "\"^^<http://www.w3.org/2001/XMLSchema#dateTime>} ";
    q += "USING <http://imeji.org/" + model + "> WHERE {<" + id
        + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://imeji.org/terms/" + model
        + ">}";
    return q;
  }


  /**
   * Update Query to withdraw a collection or an item
   * 
   * @param id
   * @return
   */
  public static final String updateWitdrawObject(String id, Calendar date, String comment) {
    String model =
        ObjectHelper.getObjectType(URI.create(id)) == ObjectType.ITEM ? "item" : "collection";
    String q = "WITH <http://imeji.org/" + model + "> ";
    q += " DELETE{<" + id
        + "> <http://imeji.org/terms/status> <http://imeji.org/terms/status#RELEASED>} ";
    q += "INSERT{<" + id
        + "> <http://imeji.org/terms/status> <http://imeji.org/terms/status#WITHDRAWN> . <" + id
        + "> <http://purl.org/dc/terms/issued> \"" + DateHelper.printJenaDate(date)
        + "\"^^<http://www.w3.org/2001/XMLSchema#dateTime> . <" + id
        + "> <http://imeji.org/terms/discardComment> \"" + comment
        + "\"^^<http://www.w3.org/2001/XMLSchema#string> } ";
    q += "USING <http://imeji.org/" + model + "> WHERE {<" + id
        + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://imeji.org/terms/" + model
        + ">}";
    return q;
  }


  /**
   * Add a license to an item
   * 
   * @param itemId
   * @param l
   * @return
   */
  public static final String updateAddLicensetoItem(String itemId, License l) {
    final String licenseId = itemId + "/license@pos0";
    return "WITH <http://imeji.org/item> " + "INSERT {<" + itemId
        + "> <http://imeji.org/terms/license> <" + licenseId + "> . <" + licenseId
        + ">  <http://imeji.org/terms/name> \"" + l.getName()
        + "\"^^<http://www.w3.org/2001/XMLSchema#string> "
        + (StringHelper.isNullOrEmptyTrim(l.getLabel()) ? ""
            : " . <" + licenseId + ">  <http://imeji.org/terms/label> \"" + l.getLabel()
                + "\"^^<http://www.w3.org/2001/XMLSchema#string> ")
        + (StringHelper.isNullOrEmptyTrim(l.getUrl()) ? ""
            : " . <" + licenseId + ">  <http://imeji.org/terms/url> \"" + l.getUrl()
                + "\"^^<http://www.w3.org/2001/XMLSchema#string> ")
        + " . <" + licenseId + ">  <http://imeji.org/terms/start>" + System.currentTimeMillis()
        + " . <" + licenseId + ">  <http://imeji.org/terms/start> -1} "
        + "USING <http://imeji.org/item> " + "WHERE{ <" + itemId
        + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://imeji.org/terms/item>}";
  }

  public static final String selectAllSubcollections() {
    return "SELECT distinct ?s ?o WHERE{?s <http://imeji.org/terms/collection> ?o} ";
  }

  /**
   * Update the <http://imeji.org/terms/collection> with the new uri
   * 
   * @param oldUri
   * @param newUri
   * @return
   */
  public static final String updateCollection(String id, String oldUri, String newUri) {
    return "WITH <http://imeji.org/item> " + "DELETE {<" + id
        + "> <http://imeji.org/terms/collection>  <" + oldUri + ">} " + "INSERT {<" + id
        + "> <http://imeji.org/terms/collection>  <" + newUri + ">} "
        + "USING <http://imeji.org/item> " + " WHERE {<" + id
        + "> <http://imeji.org/terms/collection>  <" + oldUri + ">}";
  }

  /**
   * Select all {@link CollectionImeji} available imeji
   *
   * @return
   */
  public static final String selectCollectionAll() {
    return "SELECT ?s WHERE { ?s a <http://imeji.org/terms/collection>}";
  }

  /**
   * Select all {@link Album} available imeji
   *
   * @return
   */
  public static final String selectAlbumAll() {
    return "SELECT ?s WHERE { ?s a <http://imeji.org/terms/album>}";
  }

  /**
   * select status
   *
   * @return
   */
  public static final String selectStatus(String id) {
    return "SELECT ?s WHERE { <" + id + "> <" + ImejiNamespaces.STATUS + "> ?s}";
  }

  /**
   * SElect the Version number of the object
   *
   * @param id
   * @return
   */
  public static final String selectVersion(String id) {
    return "SELECT (str(?version) AS ?s) WHERE { <" + id + "> <" + ImejiNamespaces.VERSION
        + "> ?version}";
  }

  /**
   * Select all {@link Item} available imeji
   *
   * @return
   */
  public static final String selectItemAll() {
    return "SELECT ?s WHERE { ?s a <http://imeji.org/terms/item>}";
  }

  /**
   * Select all {@link Statement} available imeji
   *
   * @return
   */
  public static final String selectStatementAll() {
    return "SELECT ?s WHERE { ?s a <http://imeji.org/terms/statement>}";
  }


  /**
   * Select all {@link Facet} available imeji
   *
   * @return
   */
  public static final String selectFacetAll() {
    return "SELECT ?s WHERE { ?s a <http://imeji.org/terms/facet>}";
  }

  /**
   * Select all statements which are not used by any metadata
   * 
   * @return
   */
  public static final String selectStatementNotUsed() {
    return "SELECT DISTINCT ?s WHERE {?s <http://imeji.org/terms/index>  ?index . ?md <http://imeji.org/terms/statement> ?index }";
  }

  public static final String getFullUrlByItem(String itemId) {
    return "SELECT ?s " + "WHERE{<" + itemId + "> <http://imeji.org/terms/fullImageUrl> ?s}";
  }

  public static final String getWebUrlByItem(String itemId) {
    return "SELECT ?s " + "WHERE{<" + itemId + "> <http://imeji.org/terms/webImageUrl> ?s}";
  }

  public static final String getThumbnailUrlByItem(String itemId) {
    return "SELECT ?s " + "WHERE{<" + itemId + "> <http://imeji.org/terms/thumbnailImageUrl> ?s}";
  }

  /**
   * Select all {@link Item} available imeji
   *
   * @return
   */
  public static final String selectContentAll() {
    return "SELECT ?s WHERE { ?s a <http://imeji.org/terms/content>}";
  }

  /**
   * Select all {@link Item} available imeji
   *
   * @return
   */
  public static final String selectUserAll() {
    return "SELECT ?s WHERE { ?s a <http://imeji.org/terms/user>}";
  }

  /**
   * Update the filesize of a {@link Item}
   *
   * @param itemId
   * @param fileSize
   * @return
   */
  public static final String insertFileSize(String itemId, String fileSize) {
    return "WITH <http://imeji.org/item> " + "INSERT {<" + itemId
        + "> <http://imeji.org/terms/fileSize> " + fileSize + "}" + "USING <http://imeji.org/item> "
        + "WHERE{<" + itemId + "> ?p ?o}";
  }

  /**
   * Update the filesize of a {@link Item}
   *
   * @param itemId
   * @param fileSize
   * @return
   */
  public static final String insertFileSizeAndDimension(String itemId, String fileSize,
      String width, String height) {
    return "WITH <http://imeji.org/item> " + "INSERT {<" + itemId
        + "> <http://imeji.org/terms/fileSize> " + fileSize + " . <" + itemId
        + "> <http://www.w3.org/2003/12/exif/ns#width> " + width + " . <" + itemId
        + "> <http://www.w3.org/2003/12/exif/ns#height> " + height + "}"
        + "USING <http://imeji.org/item> " + "WHERE{<" + itemId + "> ?p ?o}";
  }


  public static final String getInactiveUsers() {
    return "select ?s where { ?s a <http://imeji.org/terms/user> . ?s <http://imeji.org/terms/userStatus> <http://imeji.org/terms/userStatus#INACTIVE>}";
  }

  /**
   * Remove all Filesize of all {@link Item}
   *
   * @return
   */
  public static final String deleteAllFileSize() {
    return "WITH <http://imeji.org/item> DELETE {?s <http://imeji.org/terms/fileSize> ?size} where{?s <http://imeji.org/terms/fileSize> ?size}";
  }

  /**
   * Update all {@link Item}. Remove all {@link Metadata} which doesn't have any content (no triples
   * as subject)
   *
   * @return
   */
  public static final String updateEmptyMetadata() {
    return "WITH <http://imeji.org/item> " + "DELETE {?mds <" + ImejiNamespaces.METADATA + "> ?s} "
        + "USING <http://imeji.org/item> " + "WHERE {?mds <" + ImejiNamespaces.METADATA
        + "> ?s . NOT EXISTS{?s ?p ?o}}";
  }


  public static final String selectContainerItemByFilename(URI containerURI, String filename) {
    filename = removeforbiddenCharacters(filename);
    return "SELECT DISTINCT ?s WHERE {?s <http://imeji.org/terms/filename> ?el . FILTER(regex(?el, '^"
        + filename + "\\\\..+', 'i')) .?s <http://imeji.org/terms/collection> <"
        + containerURI.toString() + "> . ?s <" + ImejiNamespaces.STATUS
        + "> ?status . FILTER (?status!=<" + Status.WITHDRAWN.getUriString() + ">)} LIMIT 2";
  }

  /**
   * Search for any file with the same checksum (in any collection)
   *
   * @return
   */
  public static final String selectItemByChecksum(URI containerURI, String checksum) {
    return "SELECT DISTINCT ?s WHERE {?s <http://imeji.org/terms/checksum> \"" + checksum
        + "\"^^<http://www.w3.org/2001/XMLSchema#string>. "
        + "?s <http://imeji.org/terms/collection> <" + containerURI.toString() + "> . " + "?s <"
        + ImejiNamespaces.STATUS + "> ?status . " + " FILTER (?status!=<"
        + Status.WITHDRAWN.getUriString() + ">)} LIMIT 1";

  }


  /**
   * Search for all Institute of all {@link User} . An institute is defined by the emai of the
   * {@link User}, for instance user@mpdl.mpg.de has institute mpdl.mpg.de
   *
   * @return
   */
  public static final String selectAllInstitutes() {
    return "SELECT DISTINCT ?s WHERE {?user <http://xmlns.com/foaf/0.1/email> ?email . let(?s := str(replace(?email, '(.)+@', '', 'i')))}";
  }

  /**
   * Search for all {@link Item} within a {@link CollectionImeji} belonging the the institute, and
   * sum all fileSize
   *
   * @param instituteName
   * @return
   */
  public static final String selectInstituteFileSize(String instituteName) {
    return "SELECT (SUM(?size) AS ?s) WHERE {?c <" + ImejiNamespaces.CREATOR
        + "> ?user . ?user <http://xmlns.com/foaf/0.1/email> ?email .filter(regex(?email, '"
        + instituteName
        + "', 'i')) . ?c a <http://imeji.org/terms/collection> . ?item <http://imeji.org/terms/collection> ?c . ?item <http://imeji.org/terms/fileSize> ?size}";
  }

  /**
   * Search for all {@link Item}s created by the {@link User}, and sum all fileSize
   *
   * @param user
   * @return
   */
  public static final String selectUserFileSize(String user) {
    return "SELECT (str(SUM(?size)) AS ?s) WHERE {?item <" + ImejiNamespaces.CREATOR + "> <" + user
        + "> . ?item <http://imeji.org/terms/fileSize> ?size}";
  }

  /**
   * Chararters ( and ) can not be accepted in the sparql query and must therefore removed
   *
   * @param s
   * @return
   */
  public static final String removeforbiddenCharacters(String s) {
    final String[] forbidden = {"(", ")", "'"};
    for (int i = 0; i < forbidden.length; i++) {
      s = s.replace(forbidden[i], ".");
    }
    return s;
  }

  /**
   *
   * @return
   */
  public static final String selectUserCompleteName(URI uri) {
    return "SELECT (str(?f + ', ' + ?g) as ?s) WHERE{ <" + uri.toString()
        + "> <http://xmlns.com/foaf/0.1/person>  ?p  .  "
        + "?p  <http://purl.org/escidoc/metadata/terms/0.1/family-name> ?f . "
        + "?p  <http://purl.org/escidoc/metadata/terms/0.1/given-name>  ?g}";
  }

  /**
   * Select the name of a collection
   * 
   * @param uri
   * @return
   */
  public static final String selectCollectionName(String uri) {
    return "SELECT (str(?name) as ?s) WHERE{<" + uri.toString()
        + "> <http://purl.org/dc/elements/1.1/title> ?name} LIMIT 1";
  }

  /**
   * Helpers
   */
  public static final String countTriplesAll() {
    return "SELECT (str(count(?ss)) as ?s) WHERE {?ss ?p ?o}";
  }

  /**
   * SElect the last modication date of an object
   *
   * @param id
   * @return
   */
  public static final String selectLastModifiedDate(URI id) {
    return "SELECT (str(?date) AS ?s) WHERE {<" + id.toString() + "> <"
        + ImejiNamespaces.LAST_MODIFICATION_DATE + "> ?date}";
  }

  public static final String selectUnusedContent() {
    return "SELECT ?s WHERE {?s a <http://imeji.org/terms/content> . not exists{ ?item <http://imeji.org/terms/contentId> ?contentId . FILTER(REGEX(str(?s), ?contentId, 'i'))}}";
  }

}
