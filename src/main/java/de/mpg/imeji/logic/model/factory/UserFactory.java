package de.mpg.imeji.logic.model.factory;

import java.util.ArrayList;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.util.StringHelper;

public class UserFactory {

  private final User user = new User();

  public UserFactory setEmail(String email) {
    user.setEmail(email);
    return this;
  }

  public UserFactory setPerson(String givenName, String familyName, String organization) {
    user.getPerson().setGivenName(givenName);
    user.getPerson().setFamilyName(familyName);
    Organization org = new Organization();
    org.setName(organization);
    ArrayList<Organization> orgList = new ArrayList<Organization>();
    orgList.add(org);
    user.getPerson().setOrganizations(orgList);
    return this;
  }

  public UserFactory setPassword(String pwd) throws ImejiException {
    user.setEncryptedPassword(StringHelper.md5(pwd));
    return this;
  }

  public UserFactory setQuota(long quota) {
    user.setQuota(quota);
    return this;
  }

  public User build() {
    return user;
  }

}
