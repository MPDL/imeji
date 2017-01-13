package de.mpg.imeji.logic.search.elasticsearch.model;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.User;

/**
 * Indexed user
 *
 * @author saquet
 *
 */
public class ElasticUser {
  private final String id;
  private final String email;
  private final String apiKey;
  private final ElasticPerson person;
  private final List<String> read;
  private final List<String> upload;

  public ElasticUser(User user) {
    this.id = user.getId().toString();
    this.email = user.getEmail();
    this.apiKey = user.getApiKey();
    this.person = new ElasticPerson(user.getPerson());
    this.read = new ArrayList<>();
    this.upload = new ArrayList<>();
    for (final String g : user.getGrants()) {
      final String[] grantString = g.split(",");
      this.read.add(grantString[1]);
      if (GrantType.valueOf(grantString[0]) == GrantType.EDIT
          || GrantType.valueOf(grantString[0]) == GrantType.ADMIN) {
        this.upload.add(grantString[1]);
      }
    }
  }

  public String getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public ElasticPerson getPerson() {
    return person;
  }

  public String getApiKey() {
    return apiKey;
  }

  public List<String> getRead() {
    return read;
  }

  public List<String> getUpload() {
    return upload;
  }
}
