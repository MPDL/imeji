package de.mpg.imeji.logic.search.elasticsearch.model;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.User;

/**
 * Indexed user
 *
 * @author saquet
 *
 */
public class ElasticUser extends ElasticPerson {
  private final String id;
  private final String email;
  private final String apiKey;
  private final List<String> read;
  private final List<String> upload;

  public ElasticUser(User user) {
    super(user.getPerson());
    this.id = user.getId().toString();
    this.email = user.getEmail();
    this.apiKey = user.getApiKey();
    this.read = new ArrayList<>();
    this.upload = new ArrayList<>();
    for (final String g : user.getGrants()) {
      final Grant grant = new Grant(g);
      this.read.add(grant.getGrantFor());
      if (grant.asGrantType() == GrantType.EDIT || grant.asGrantType() == GrantType.ADMIN) {
        this.upload.add(grant.getGrantFor());
      }
    }
  }

  public String getId() {
    return id;
  }

  public String getEmail() {
    return email;
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
