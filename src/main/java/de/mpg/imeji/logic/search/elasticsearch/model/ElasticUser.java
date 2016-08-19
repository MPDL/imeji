package de.mpg.imeji.logic.search.elasticsearch.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.vo.Grant;
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
  private final List<URI> read;

  public ElasticUser(User user) {
    this.id = user.getId().toString();
    this.email = user.getEmail();
    this.apiKey = user.getApiKey();
    this.person = new ElasticPerson(user.getPerson());
    this.read = new ArrayList<>();
    for (Grant g : user.getGrants()) {
      this.read.add(g.getGrantFor());
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

  public List<URI> getRead() {
    return read;
  }
}
