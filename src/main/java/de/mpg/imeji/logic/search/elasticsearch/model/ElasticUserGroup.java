package de.mpg.imeji.logic.search.elasticsearch.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.UserGroup;

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
    for (final URI uri : group.getUsers()) {
      users.add(uri.toString());
    }
    for (final String g : group.getGrants()) {
      final String[] grantString = g.split(",");
      this.read.add(grantString[1]);
      if (GrantType.valueOf(grantString[0]) == GrantType.EDIT
          || GrantType.valueOf(grantString[0]) == GrantType.ADMIN) {
        this.upload.add(grantString[1]);
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
