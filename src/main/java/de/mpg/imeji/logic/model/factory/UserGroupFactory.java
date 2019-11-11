package de.mpg.imeji.logic.model.factory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;

public class UserGroupFactory {

  private final UserGroup userGroup = new UserGroup();

  public UserGroupFactory() {
    userGroup.setUsers(new ArrayList<URI>());
    userGroup.setGrants(new ArrayList<String>());
  }

  public UserGroup build() {
    return userGroup;
  }

  public UserGroupFactory setName(String name) {
    userGroup.setName(name);
    return this;
  }

  public UserGroupFactory setUsers(Collection<URI> users) {
    userGroup.setUsers(users);
    return this;
  }

  public UserGroupFactory addUsers(User... users) {
    for (User u : users) {
      userGroup.getUsers().add(u.getId());
    }
    return this;
  }

  public UserGroupFactory addUsers(URI... users) {
    for (URI u : users) {
      userGroup.getUsers().add(u);
    }
    return this;
  }

  public UserGroupFactory setGrants(Collection<String> grants) {
    userGroup.setGrants(grants);
    return this;
  }

  /**
   * Add multiple grants to the UserGroup.
   * 
   * Grants must be of the form "ShareRoles_type,collection_URI".
   * 
   * @param grants The grants to add
   * @return the UserGroupFactory
   */
  public UserGroupFactory addGrants(String... grants) {
    for (String g : grants) {
      userGroup.getGrants().add(g);
    }
    return this;
  }

}
