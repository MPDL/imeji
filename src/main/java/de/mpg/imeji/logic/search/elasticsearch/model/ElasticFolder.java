package de.mpg.imeji.logic.search.elasticsearch.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContainerAdditionalInfo;
import de.mpg.imeji.logic.model.Person;

/**
 * The elastic Version of a {@link CollectionImeji}
 *
 * @author bastiens
 *
 */
public final class ElasticFolder extends ElasticProperties {
  private final String name;
  private final String folder;
  private final String description;
  private final String creators;
  private final List<String> pid;
  private final String filetype = "text/directory";
  private final List<ElasticPerson> author = new ArrayList<>();
  private final List<ElasticContainerAdditionalInfo> info = new ArrayList<>();

  public ElasticFolder(CollectionImeji c) {
    super(c);
    this.name = c.getTitle();
    this.description = c.getDescription();
    this.pid = c.getDoi() != null ? Arrays.asList(c.getDoi()) : new ArrayList<String>();
    this.folder = c.getCollection() != null ? c.getCollection().toString() : null;
    this.creators =
        c.getPersons().stream().map(p -> p.getCompleteName()).collect(Collectors.joining(";"));
    for (final Person p : c.getPersons()) {
      author.add(new ElasticPerson(p));
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

  public List<ElasticPerson> getAuthor() {
    return author;
  }

  public List<String> getPid() {
    return pid;
  }

  /**
   * @return the creators
   */
  public String getCreators() {
    return creators;
  }

  /**
   * @return the folder
   */
  public String getFolder() {
    return folder;
  }

}
