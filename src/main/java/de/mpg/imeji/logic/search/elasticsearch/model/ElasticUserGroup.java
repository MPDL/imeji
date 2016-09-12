package de.mpg.imeji.logic.search.elasticsearch.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.logic.vo.Grant.GrantType;

/**
 * Elastic Object for UserGroup
 * 
 * @author saquet
 *
 */
public class ElasticUserGroup {
  private final String name;
  private final List<String> read = new ArrayList<>();
  private final List<String> upload = new ArrayList<>();
  private final List<String> users = new ArrayList<>();

  /**
   * Constructor for one group
   * 
   * @param group
   */
  public ElasticUserGroup(UserGroup group) {
    this.name = group.getName();
    for (URI uri : group.getUsers()) {
      users.add(uri.toString());
    }
    for (Grant g : group.getGrants()) {
      if (g.asGrantType() == GrantType.READ) {
        this.read.add(g.getGrantFor().toString());
      } else if (g.asGrantType() == GrantType.CREATE) {
        this.upload.add(g.getGrantFor().toString());
      }
    }
  }

  public String getName() {
    return name;
  }

  public List<String> getRead() {
    return read;
  }

  public List<String> getUpload() {
    return upload;
  }

  public List<String> getUsers() {
    return users;
  }
}
