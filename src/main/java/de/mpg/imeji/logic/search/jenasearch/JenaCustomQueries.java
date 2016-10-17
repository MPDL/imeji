/*
 *
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the Common Development and Distribution
 * License, Version 1.0 only (the "License"). You may not use this file except in compliance with
 * the License.
 *
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions and limitations under the
 * License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License
 * file at license/ESCIDOC.LICENSE. If applicable, add the following below this CDDL HEADER, with
 * the fields enclosed by brackets "[]" replaced with your own identifying information: Portions
 * Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */
/*
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft für
 * wissenschaftlich-technische Information mbH and Max-Planck- Gesellschaft zur Förderung der
 * Wissenschaft e.V. All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.logic.search.jenasearch;

import java.net.URI;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.ImejiNamespaces;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;

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
   * Select default {@link MetadataProfile}
   *
   * @return
   */
  public static final String selectDefaultMetadataProfile() {
    return X_PATH_FUNCTIONS_DECLARATION + "  SELECT DISTINCT ?s WHERE { "
        + "?s a <http://imeji.org/terms/mdprofile> . ?s <http://imeji.org/terms/default> true"
        + "}";
  }

  /**
   * Select {@link MetadataProfile} which are not used by any {@link CollectionImeji}
   *
   * @return
   */
  public static final String selectUnusedMetadataProfiles() {
    return " SELECT DISTINCT ?s WHERE { "
        + "?s a <http://imeji.org/terms/mdprofile> . not exists{?c <http://imeji.org/terms/mdprofile> ?s}"
        + "}";
  }

  /**
   * Checks if provided {@link MetadataProfile} uri has other references than the reference in the
   * provided resource {@link CollectionImeji}
   *
   * @return
   */
  public static final String hasOtherMetadataProfileReferences(String profileUri,
      String resourceUri) {
    String q = " SELECT ?s WHERE { ?s ?p ?o ." + "?s <http://imeji.org/terms/mdprofile> <"
        + profileUri + ">." + " FILTER (?s != <" + resourceUri + ">  && ?s != <" + profileUri
        + ">) " + " NOT EXISTS { " + "?item <http://imeji.org/terms/metadataSet> ?s. "
        + "?s <http://imeji.org/terms/mdprofile> ?o. "
        + "?item <http://imeji.org/terms/collection> ?collection." + "FILTER (?collection = <"
        + resourceUri + ">) }" + "} LIMIT 1";
    return q;

  }

  /**
   * Checks if provided {@link MetadataProfile} uri has any collection references
   * {@link CollectionImeji}
   *
   * @return
   */
  public static final String hasMetadataProfileReferences(String profileUri) {
    return " SELECT ?s WHERE { " + "?s <http://imeji.org/terms/mdprofile> <" + profileUri + ">."
        + " FILTER (?s != <" + profileUri + "> )} LIMIT 1";

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
        + "  SELECT DISTINCT ?s WHERE {OPTIONAL{ ?s <http://imeji.org/terms/grant> ?g . ?g <http://imeji.org/terms/grantType> <"
        + Grant.toGrantTypeURI(GrantType.ADMIN).toString()
        + ">. ?g <http://imeji.org/terms/grantFor> <" + Imeji.PROPERTIES.getBaseURI()
        + ">} . filter(bound(?g)) . ?s a <http://imeji.org/terms/user>}";
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
   * Select {@link CollectionImeji} of space
   *
   * @param uri
   * @return
   */
  public static final String selectCollectionImejiOfSpace(String uri) {
    return X_PATH_FUNCTIONS_DECLARATION
        + "  SELECT DISTINCT ?s WHERE {?s a <http://imeji.org/terms/collection> . ?s <http://imeji.org/terms/space> <"
        + uri + ">}";
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
   * @param fileUrl
   * @return
   */
  public static final String selectCollectionIdOfFile(String fileUrl) {
    return X_PATH_FUNCTIONS_DECLARATION + "  SELECT DISTINCT ?s WHERE {" + "optional{"
        + "?it <http://imeji.org/terms/webImageUrl> <" + fileUrl
        + ">} . optional {?it <http://imeji.org/terms/thumbnailImageUrl> <" + fileUrl
        + ">} . optional{ ?it <http://imeji.org/terms/fullImageUrl> <" + fileUrl + ">}"
        + " . ?it <http://imeji.org/terms/collection> ?s . } LIMIT 1 ";
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
   * Find the profile of a collection and return its uri
   *
   * @param collectionUri
   * @return
   */
  public static final String selectProfileIdOfCollection(String collectionUri) {
    return " SELECT DISTINCT ?s WHERE {<" + collectionUri
        + "> <http://imeji.org/terms/mdprofile> ?s} LIMIT 1 ";
  }

  /**
   * @param fileUrl
   * @return
   */
  public static final String selectItemIdOfFileUrl(String fileUrl) {
    String path = URI.create(fileUrl).getPath();
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
  public static final String selectItemOfFile(String storageId) {
    return X_PATH_FUNCTIONS_DECLARATION + XSD_DECLARATION + "SELECT DISTINCT ?s WHERE { ?s <"
        + ImejiNamespaces.STORAGE_ID + "> '" + storageId + "'^^xsd:string } limit 1";
  }

  /**
   * Select the Status of the item which got this file
   *
   * @param fileUrl
   * @return
   */
  public static final String selectItemStatusOfFile(String storageId) {
    return X_PATH_FUNCTIONS_DECLARATION + XSD_DECLARATION + "SELECT DISTINCT ?s WHERE { ?s <"
        + ImejiNamespaces.STORAGE_ID + "> '" + storageId + "'^^xsd:string . ?s <"
        + ImejiNamespaces.STATUS + "> <" + Status.RELEASED.getUriString() + ">} limit 1";
  }

  /**
   * @param fileUrl
   * @return
   */
  public static final String selectSpaceIdOfFileOrCollection(String fileUrl) {
    return X_PATH_FUNCTIONS_DECLARATION + "  SELECT DISTINCT ?s WHERE {"
        + "?s <http://imeji.org/terms/logoUrl> <" + fileUrl + "> } LIMIT 1 ";
  }


  /**
   * Select all {@link Metadata} which are not related to a statement. Happens when a
   * {@link Statement} is removed from a {@link MetadataProfile}
   *
   * @return
   */
  public static final String selectMetadataUnbounded() {
    return X_PATH_FUNCTIONS_DECLARATION + "  SELECT DISTINCT ?s ?sort0 WHERE {?mds <"
        + ImejiNamespaces.METADATA + "> ?s"
        + " . ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?sort0 . ?s <http://imeji.org/terms/statement> ?st"
        + " . not exists{?p a <http://imeji.org/terms/mdprofile> . ?p <http://imeji.org/terms/statement> ?st}}";
  }

  /**
   * Select all {@link Statement} which are not bounded to a {@link MetadataProfile}. Should not
   * happen...
   *
   * @return
   */
  public static final String selectStatementUnbounded() {
    return X_PATH_FUNCTIONS_DECLARATION
        + "  SELECT DISTINCT ?s WHERE {?s <http://purl.org/dc/terms/type> ?type"
        + " . not exists{ ?p a <http://imeji.org/terms/mdprofile> . ?p <http://imeji.org/terms/statement> ?s}}";
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
   * Select all {@link Space} available imeji
   *
   * @return
   */
  public static final String selectSpaceAll() {
    return "SELECT ?s WHERE { ?s a <http://imeji.org/terms/space>}";
  }

  /**
   * Select a {@link Space} by its label
   *
   * @return
   */
  public static final String getSpaceByLabel(String spaceId) {
    return "SELECT ?s WHERE { ?s <http://imeji.org/terms/slug> \"" + spaceId
        + "\"^^<http://www.w3.org/2001/XMLSchema#string>. ?s a <http://imeji.org/terms/space> }";
  }

  public static final String selectCollectionsOfSpace(URI id) {
    return "SELECT DISTINCT ?s WHERE{ ?s <http://imeji.org/terms/space> " + "<" + id.toString()
        + "> " + " . ?s a <http://imeji.org/terms/collection> }";
  }

  public static final String selectSpaceOfCollection(URI id) {
    return "SELECT DISTINCT ?s WHERE{  <" + id.toString() + "> <http://imeji.org/terms/space> "
        + "?s " + " . ?s a <http://imeji.org/terms/space> } LIMIT 1";
  }

  public static final String selectCollectionsNotInSpace() {
    return "SELECT DISTINCT ?s  WHERE {"
        + " FILTER NOT EXISTS {?s <http://imeji.org/terms/space> ?o} ."
        + "  ?s a <http://imeji.org/terms/collection> ." + " FILTER NOT EXISTS {?s <"
        + ImejiNamespaces.STATUS + "> <" + Status.WITHDRAWN.getUriString() + "> }" + "} ";
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
   * Update all {@link Item}. Remove the {@link Metadata} which have a non existing
   * {@link Statement}
   *
   * @return
   */
  public static final String updateRemoveAllMetadataWithoutStatement(String profileURI) {
    String profileQuery = profileURI != null ? "<" + profileURI + ">" : "?profile";
    return "WITH <http://imeji.org/item> " + "DELETE {?mds <" + ImejiNamespaces.METADATA
        + "> ?s . ?s ?p ?o } " + "USING <http://imeji.org/item> "
        + "USING <http://imeji.org/metadataProfile> "
        + "WHERE {?mds <http://imeji.org/terms/mdprofile> " + profileQuery + " . ?mds <"
        + ImejiNamespaces.METADATA + "> ?s . ?s <http://imeji.org/terms/statement> ?st"
        + " . NOT EXISTS{" + profileQuery + " <http://imeji.org/terms/statement> ?st}"
        + " . ?s ?p ?o }";
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
    String[] forbidden = {"(", ")", "'"};
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
    return "SELECT (str(?cn) as ?s) WHERE{ <" + uri.toString() + "> "
        + "<http://xmlns.com/foaf/0.1/person> ?o "
        + ". ?o <http://purl.org/escidoc/metadata/terms/0.1/complete-name> ?cn}";
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
