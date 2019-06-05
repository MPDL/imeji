package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jList;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.ImejiNamespaces;
import de.mpg.imeji.logic.model.aspects.AccessMember;
import de.mpg.imeji.logic.model.aspects.CloneURI;
import de.mpg.imeji.logic.model.aspects.ResourceLastModified;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.util.URIListHelper;

/**
 * A User group
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/userGroup")
@j2jModel("userGroup")
@j2jId(getMethod = "getId", setMethod = "setId")
public class UserGroup implements Serializable, ResourceLastModified, CloneURI, AccessMember {
  private static final long serialVersionUID = 7770992777121385741L;
  private URI id = IdentifierUtil.newURI(UserGroup.class);
  @j2jLiteral("http://xmlns.com/foaf/0.1/name")
  private String name;
  @j2jList("http://imeji.org/terms/grant")
  private Collection<String> grants = new ArrayList<String>();
  @j2jList("http://xmlns.com/foaf/0.1/member")
  private Collection<URI> users = new ArrayList<URI>();
  @j2jLiteral(ImejiNamespaces.LAST_MODIFICATION_DATE)
  private Calendar modified;

  private static final Logger LOGGER = LogManager.getLogger(UserGroup.class);


  @Override
  public UserGroup cloneURI() {
    UserGroup clonedUserGroup = new UserGroup();
    clonedUserGroup.setId(id);
    return clonedUserGroup;
  }


  @Override
  public void setModified(Calendar calendar) {
    this.modified = calendar;
  }

  @Override
  public Calendar getModified() {
    return this.modified;
  }

  @Override
  public Field getTimeStampField() {
    try {
      Field timeStampField = UserGroup.class.getDeclaredField("modified");
      return timeStampField;
    } catch (NoSuchFieldException | SecurityException e) {
      LOGGER.error(e);
    }
    return null;
  }


  @Override
  public void accessMember(ChangeMember changeMember) {

    // access members:
    // (a) add/edit/delete user from users list.
    // (b) add/edit/delete grant form grants list.

    try {
      Field grantsListField = UserGroup.class.getDeclaredField("grants");
      Field usersListField = UserGroup.class.getDeclaredField("users");

      if (changeMember.getField().equals(grantsListField) && changeMember.getValue() instanceof Grant) {
        String grantString = ((Grant) changeMember.getValue()).toGrantString();
        int indexOfExistingGrant = Grant.getGrantPositionInGrantsList((List<String>) this.grants, grantString);
        URIListHelper.addEditRemoveElementOfURIList((List<String>) this.grants, changeMember.getAction(), grantString,
            indexOfExistingGrant);
      } else if (changeMember.getField().equals(usersListField) && changeMember.getValue() instanceof URI) {
        URI userURI = (URI) changeMember.getValue();
        int indexOfExistingUser = ((List<URI>) this.users).indexOf(userURI);
        URIListHelper.addEditRemoveElementOfURIList((List<URI>) this.users, changeMember.getAction(), userURI, indexOfExistingUser);
      } else {
        LOGGER.info("Could not set member in UserGroup. Check and implement your case (if applicable).");
      }

    } catch (NoSuchFieldException | SecurityException e) {
      LOGGER.error(e);
    }
  }



  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the grants
   */
  public Collection<String> getGrants() {
    return grants;
  }

  /**
   * @param grants the grants to set
   */
  public void setGrants(Collection<String> grants) {
    this.grants = grants;
  }

  /**
   * @return the users
   */
  public Collection<URI> getUsers() {
    return users;
  }

  /**
   * @param users the users to set
   */
  public void setUsers(Collection<URI> users) {
    this.users = users;
  }

  /**
   * @return the id
   */
  public URI getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(URI id) {
    this.id = id;
  }


}
