/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.search.jenasearch;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.logic.ImejiNamespaces;
import de.mpg.imeji.logic.search.util.SearchUtils;
import de.mpg.imeji.logic.security.util.SecurityUtil;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;

/**
 * Simple security query add to any imeji sparql query, a security filter (according to user,
 * searchtype, etc)
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class JenaSecurityQuery {
  /**
   * Static factory for the security query. A {@link String} is returned which sould be added to the
   * complete sparql query
   *
   * @param user
   * @param pair
   * @param rdfType
   * @param includeWithdrawn
   * @return
   */
  public static String queryFactory(User user, String rdfType, Status status,
      boolean isUserSearch) {

    String statusFilter = getStatusAsFilter(status);

    if (Status.PENDING.equals(status) && !SecurityUtil.isSysAdmin(user)) {
      // add this explicitly in order to avoid too long queries. Only if Admin user should not be
      // set explicitly again
      isUserSearch = true;
    }
    if (Status.RELEASED.equals(status) || Status.WITHDRAWN.equals(status)) {
      // If searching for released or withdrawn objects, no grant filter
      // is needed (released and withdrawn objects
      // are all public)

      return statusFilter + " .";
    } else if (user != null && user.getGrants().isEmpty() && Status.PENDING.equals(status)) {
      // special case: a user without grants wants to see private objects:
      // not possible
      return "FILTER(false) .";
    }
    //
    else if ((user == null || user.getGrants().isEmpty())) {
      // if user is null or has no rights, then can only see the released
      // objects
      return "FILTER(?status=<" + Status.RELEASED.getUriString() + ">) .";
    }

    // Logic below is invoked for logged-in users. Grants must be always checked.
    // If user has no grants for requested objects, simply no data should be returned
    // that's why FILTER(false)
    String userGrantsAsFilterSimple = getUserGrantsAsFilterSimple(user, rdfType, isUserSearch);
    return userGrantsAsFilterSimple.equals("")
        ? (SecurityUtil.isSysAdmin(user) ? statusFilter : " FILTER(false) ")
        : userGrantsAsFilterSimple + statusFilter + " .";
  }

  /**
   * Return a SPARQL Filter with the allowed status of an object
   *
   * @param includeWithdrawn
   * @return
   */
  private static String getStatusAsFilter(Status status) {
    if (status == null) {
      return "FILTER (?status!=<" + Status.WITHDRAWN.getUriString() + ">)";
    } else {
      return "FILTER (?status=<" + status.getUriString() + ">)";
    }
  }

  /**
   * Simple Security query (is experimental, must be tested)
   *
   * @param user
   * @param rdfType
   * @return
   */
  private static String getUserGrantsAsFilterSimple(User user, String rdfType,
      boolean isUserSearch) {

    if (SearchUtils.isSysAdmin(user) && !isUserSearch) {
      return "";
    }
    return getAllowedContainersFilter(user, rdfType, isUserSearch);
  }

  /**
   * Return ths SPARQL filter with all container the {@link User} is allowed to view
   *
   * @param user
   * @param rdfType
   * @return
   */
  private static String getAllowedContainersFilter(User user, String rdfType,
      boolean isUserSearch) {
    List<String> uris = new ArrayList<>();

    if (J2JHelper.getResourceNamespace(new Album()).equals(rdfType)) {
      uris = SecurityUtil.getListOfAllowedAlbums(user);
    } else {
      uris = SecurityUtil.getListOfAllowedCollections(user);
    }

    String s = "";
    boolean addReleasedStatus = !isUserSearch;

    StringBuilder builder = new StringBuilder();
    String allowedContainerString = "";
    if (J2JHelper.getResourceNamespace(new Item()).equals(rdfType)) {
      int i = 0;
      for (String uri : uris) {
        i++;
        builder.append(
            (i == 1 ? "{ " : " UNION {") + "?s " + getPredicateName(rdfType) + " <" + uri + "> }");
      }
      allowedContainerString = builder.toString();
      if (addReleasedStatus) {
        s = allowedContainerString + (uris.size() > 0 ? " UNION " : "") + " { ?s <"
            + ImejiNamespaces.STATUS + "> <" + Status.RELEASED.getUriString() + "> }";
      }
    } else if ((J2JHelper.getResourceNamespace(new CollectionImeji()).equals(rdfType)
        || J2JHelper.getResourceNamespace(new Album()).equals(rdfType))) {
      int j = 0;
      for (String uri : uris) {
        j++;
        builder.append(" <" + uri + "> " + (j == uris.size() ? "" : ","));
      }
      allowedContainerString =
          uris.size() > 0 ? "  FILTER (?s in (" + builder.toString() + ") " : "";

      if (addReleasedStatus) {
        allowedContainerString =
            allowedContainerString + (uris.size() > 0 ? " || " : ". FILTER ") + "( ?status = <"
                + Status.RELEASED.getUriString() + "> )" + (uris.size() > 0 ? "" : ". ");
      }

      s = allowedContainerString + (uris.size() > 0 ? ") ." : "");
    }


    if (J2JHelper.getResourceNamespace(new Item()).equals(rdfType)) {
      // searching for items. Add to the Filter the item for which the
      // user has extra rights as well as the item which are public
      StringBuilder builderItems = new StringBuilder();
      int itNo = 0;
      List<String> allowedItems = SecurityUtil.getListOfAllowedItem(user);
      String allowedItemsString = "";
      if (allowedItems.size() > 0) {
        for (String uri : allowedItems) {
          itNo++;
          builderItems.append(" <" + uri + "> " + (itNo == allowedItems.size() ? "" : ","));
        }

        allowedItemsString = ((!"".equals(s)) ? " UNION " : "") + " { ?s <" + ImejiNamespaces.STATUS
            + "> ?status. FILTER (?s in (" + builderItems.toString() + ")) }";
      }

      s += allowedItemsString;
    }

    if (J2JHelper.getResourceNamespace(new MetadataProfile()).equals(rdfType)) {
      // searching for profiles. Add to the Filter the profiles for which the
      // user has extra rights as well as the item which are public

      StringBuilder builderProfiles = new StringBuilder();
      int pNo = 0;
      List<String> allowedProfiles = SecurityUtil.getListOfAllowedProfiles(user);
      String allowedProfilesString = "";
      String releasedStatusFilter = "( ?status = <" + Status.RELEASED.getUriString() + "> )";
      if (allowedProfiles.size() > 0) {
        for (String uri : allowedProfiles) {
          pNo++;
          builderProfiles.append(" <" + uri + "> " + (pNo == allowedProfiles.size() ? "" : ","));
        }

        allowedProfilesString = " { ?s <" + ImejiNamespaces.STATUS + "> ?status. FILTER (?s in ("
            + builderProfiles.toString() + ") || " + releasedStatusFilter + ") }";
      }

      allowedProfilesString = " { ?s <" + ImejiNamespaces.STATUS
          + "> ?status. FILTER (" + ((allowedProfiles.size() > 0)
              ? (" ?s in (" + builderProfiles.toString() + ") || ") : "")
          + releasedStatusFilter + ") }";
      s += allowedProfilesString;

    }
    return s;
  }

  /**
   * Return the variable name of for the object on with the security is checked
   *
   * @param rdfType
   * @return
   */
  public static String getVariableName(String rdfType) {
    if (J2JHelper.getResourceNamespace(new CollectionImeji()).equals(rdfType)
        || J2JHelper.getResourceNamespace(new Album()).equals(rdfType)) {
      return "s";
    } else {
      return "c";
    }
  }

  /**
   * Return the predicate for the object on with the security is checked
   *
   * @param rdfType
   * @return
   */
  public static String getPredicateName(String rdfType) {
    if (J2JHelper.getResourceNamespace(new CollectionImeji()).equals(rdfType)
        || J2JHelper.getResourceNamespace(new Album()).equals(rdfType)) {
      return " a ";
    } else {
      return "<" + ImejiNamespaces.COLLECTION + ">";
    }
  }
}
