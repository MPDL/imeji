package de.mpg.imeji.logic.search.elasticsearch.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.ContainerAdditionalInfo;
import de.mpg.imeji.logic.vo.Person;

/**
 * The elastic Version of a {@link CollectionImeji}
 *
 * @author bastiens
 *
 */
public final class ElasticFolder extends ElasticProperties {
  private final String name;
  private final String description;
  private final List<String> pid;
  private final List<ElasticPerson> person = new ArrayList<>();
  private final List<ElasticContainerAdditionalInfo> info = new ArrayList<>();

  public ElasticFolder(CollectionImeji c) {
    super(c);
    this.name = c.getTitle();
    this.description = c.getDescription();
    this.pid = c.getDoi() != null ? Arrays.asList(c.getDoi()) : new ArrayList<String>();
    for (final Person p : c.getPersons()) {
      person.add(new ElasticPerson(p));
    }
    for (final ContainerAdditionalInfo i : c.getAdditionalInformations()) {
      info.add(new ElasticContainerAdditionalInfo(i));
    }
  }

  public List<ElasticContainerAdditionalInfo> getInfo() {
    return info;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  public List<ElasticPerson> getPerson() {
    return person;
  }

  public List<String> getPid() {
    return pid;
  }

}
