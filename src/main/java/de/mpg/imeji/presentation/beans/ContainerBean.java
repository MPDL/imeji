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
package de.mpg.imeji.presentation.beans;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.doi.DoiService;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.util.ImejiFactory;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.ContainerAdditionalInfo;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.util.ObjectHelper;
import de.mpg.imeji.presentation.album.AlbumBean;
import de.mpg.imeji.presentation.collection.CollectionBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Super Java Bean for containers bean {@link AlbumBean} and {@link CollectionBean}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class ContainerBean extends SuperBean implements Serializable {
  private static final Logger LOGGER = Logger.getLogger(ContainerBean.class);
  private static final long serialVersionUID = 3377874537531738442L;
  private int authorPosition;
  private int organizationPosition;
  private int size;
  private List<Item> items;
  private List<Item> discardedItems;
  private int sizeDiscarded;

  /**
   * Types of containers
   *
   * @author saquet (initial creation)
   * @author $Author$ (last modification)
   * @version $Revision$ $LastChangedDate$
   */
  public enum CONTAINER_TYPE {
    COLLECTION, ALBUM;
  }

  /**
   * return the {@link CONTAINER_TYPE} of the current bean
   *
   * @return
   */
  public abstract String getType();

  /**
   * Return the container
   *
   * @return
   */
  public abstract Container getContainer();

  /**
   * Return the String used for redirection
   *
   * @return
   */
  protected abstract String getNavigationString();

  /**
   * Return the bundle of the message when not orga is set
   *
   * @return
   */
  protected abstract String getErrorMessageNoAuthor();

  /**
   * The url of the current page
   *
   * @return
   */
  protected abstract String getPageUrl();

  /**
   * Find the first {@link Item} of the current {@link Container} (fast method)
   *
   * @param user
   * @param size
   */
  protected void findItems(User user, int size) {
    ItemController ic = new ItemController();
    ic.searchAndSetContainerItems(getContainer(), user, size, 0);
  }

  /**
   * Count the size the {@link Container}
   *
   * @param hasgrant
   * @return
   */
  protected void countItems() {
    ItemController ic = new ItemController();
    size = ic.search(getContainer().getId(), null, null, Imeji.adminUser, null, 0, 0)
        .getNumberOfRecords();
  }

  /**
   * Load the {@link Item} of the {@link Container}
   *
   * @throws ImejiException
   */
  protected void loadItems(User user, int size) throws ImejiException {
    setItems(new ArrayList<Item>());
    if (getContainer() != null) {
      List<String> uris = new ArrayList<String>();
      for (URI uri : getContainer().getImages()) {
        uris.add(uri.toString());
      }
      ItemController ic = new ItemController();
      setItems((List<Item>) ic.retrieveBatchLazy(uris, size, 0, user));
    }
  }


  /**
   * Load the {@link Item} of the {@link Container}
   */
  public void countDiscardedItems(User user) {
    if (getContainer() != null) {
      ItemController ic = new ItemController();
      SearchQuery q = new SearchQuery();
      try {
        q.addPair(new SearchPair(SearchFields.status, SearchOperators.EQUALS,
            Status.WITHDRAWN.getUriString(), false));
      } catch (UnprocessableError e) {
        LOGGER.error("Error creating query to search for discarded items of a container", e);
      }
      setSizeDiscarded(
          ic.search(getContainer().getId(), q, null, user, null, -1, 0).getNumberOfRecords());
    } else {
      setSizeDiscarded(0);
    }
  }

  /**
   * Get Person String
   *
   * @return
   */
  public String getPersonString() {
    String personString = "";
    for (Person p : getContainer().getMetadata().getPersons()) {
      if (!"".equalsIgnoreCase(personString)) {
        personString += ", ";
      }
      personString += p.getFamilyName() + " " + p.getGivenName() + " ";
    }
    return personString;
  }

  /**
   * @return
   */
  public String getAuthorsWithOrg() {
    String personString = "";
    for (Person p : getContainer().getMetadata().getPersons()) {
      if (!"".equalsIgnoreCase(personString)) {
        personString += ", ";
      }
      personString += p.getCompleteName();
      if (!p.getOrganizationString().equals("")) {
        personString += " (" + p.getOrganizationString() + ")";
      }
    }
    return personString;
  }

  public String getCitation() {
    String url = getDoiUrl().isEmpty() ? getPageUrl() : getDoiUrl();
    return getAuthorsWithOrg() + ". " + getContainer().getMetadata().getTitle() + ". <a href=\""
        + url + "\">" + url + "</a>";
  }

  /**
   * The Url to view the DOI
   *
   * @return
   */
  public String getDoiUrl() {
    return getContainer().getDoi().isEmpty() ? ""
        : DoiService.DOI_URL_RESOLVER + getContainer().getDoi();
  }

  /**
   * Add an addtionial Info at the passed position
   *
   * @param pos
   */
  public void addAdditionalInfo(int pos) {
    getContainer().getMetadata().getAdditionalInformations().add(pos,
        new ContainerAdditionalInfo("", "", ""));
  }

  /**
   * Remove the nth additional Info
   *
   * @param pos
   */
  public void removeAdditionalInfo(int pos) {
    getContainer().getMetadata().getAdditionalInformations().remove(pos);
  }

  /**
   * Add a new author to the {@link CollectionImeji}
   *
   * @param authorPosition
   * @return
   */
  public String addAuthor(int authorPosition) {
    List<Person> c = (List<Person>) getContainer().getMetadata().getPersons();
    Person p = ImejiFactory.newPerson();
    p.setPos(authorPosition + 1);
    c.add(authorPosition + 1, p);
    return "";
  }

  /**
   * Remove an author of the {@link CollectionImeji}
   *
   * @return
   */
  public String removeAuthor(int authorPosition) {
    List<Person> c = (List<Person>) getContainer().getMetadata().getPersons();
    if (c.size() > 1) {
      c.remove(authorPosition);
    } else {
      BeanHelper.error(getErrorMessageNoAuthor());
    }
    return "";
  }

  /**
   * Add an organization to an author of the {@link CollectionImeji}
   *
   * @param authorPosition
   * @param organizationPosition
   * @return
   */
  public String addOrganization(int authorPosition, int organizationPosition) {
    List<Person> persons = (List<Person>) getContainer().getMetadata().getPersons();
    List<Organization> orgs = (List<Organization>) persons.get(authorPosition).getOrganizations();
    Organization o = ImejiFactory.newOrganization();
    o.setPos(organizationPosition + 1);
    orgs.add(organizationPosition + 1, o);
    return "";
  }

  /**
   * Remove an organization to an author of the {@link CollectionImeji}
   *
   * @return
   */
  public String removeOrganization(int authorPosition, int organizationPosition) {
    List<Person> persons = (List<Person>) getContainer().getMetadata().getPersons();
    List<Organization> orgs = (List<Organization>) persons.get(authorPosition).getOrganizations();
    if (orgs.size() > 1) {
      orgs.remove(organizationPosition);
    } else {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_author_need_one_organization",
          BeanHelper.getLocale()));
    }
    return "";
  }

  /**
   * getter
   *
   * @return
   */
  public int getAuthorPosition() {
    return authorPosition;
  }

  /**
   * setter
   *
   * @param pos
   */
  public void setAuthorPosition(int pos) {
    this.authorPosition = pos;
  }

  /**
   * @return the collectionPosition
   */
  public int getOrganizationPosition() {
    return organizationPosition;
  }

  /**
   * @param collectionPosition the collectionPosition to set
   */
  public void setOrganizationPosition(int organizationPosition) {
    this.organizationPosition = organizationPosition;
  }

  /**
   * @return the items
   */
  public List<Item> getItems() {
    return items;
  }

  /**
   * @return the discarded items
   */
  public List<Item> getDiscardedItems() {
    return discardedItems;
  }

  /**
   * @param items the items to set
   */
  public void setItems(List<Item> items) {
    this.items = items;
  }

  /**
   * @param discarded items setter
   */
  public void setDiscardedItems(List<Item> items) {
    this.discardedItems = items;
  }

  /**
   * @return the size
   */
  public int getSize() {
    return size;
  }

  /**
   * @param size the size to set
   */
  public void setSize(int size) {
    this.size = size;
  }

  /**
   * @param size the size to set
   */
  public void setSizeDiscarded(int size) {
    this.sizeDiscarded = size;
  }

  public int getSizeDiscarded() {
    return sizeDiscarded;
  }

  /**
   * True if the current {@link User} is the creator of the {@link Container}
   *
   * @return
   */
  public boolean isOwner() {
    SessionBean sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    if (getContainer() != null && getContainer().getCreatedBy() != null
        && sessionBean.getUser() != null) {
      return getContainer().getCreatedBy()
          .equals(ObjectHelper.getURI(User.class, sessionBean.getUser().getEmail()));
    }
    return false;
  }

  public boolean isCollectionType() {
    return getType().equals(CONTAINER_TYPE.COLLECTION.toString());
  }

  public boolean isAlbumType() {
    return getType().equals(CONTAINER_TYPE.ALBUM.toString());
  }

  /**
   * Remove an author of the {@link CollectionImeji}
   *
   * @return
   */
  public String removeContainerLogo() {
    getContainer().setLogoUrl(null);
    return "";
  }

}
