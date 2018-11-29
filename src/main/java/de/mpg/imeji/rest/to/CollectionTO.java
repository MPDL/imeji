package de.mpg.imeji.rest.to;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonInclude;

@XmlRootElement
@XmlType(propOrder = {"versionOf"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollectionTO extends PropertiesTO implements Serializable {
  private static final long serialVersionUID = 7039960402363523772L;
  private String collectionId;
  private String title;
  private String description;
  private List<PersonTO> contributors = new ArrayList<PersonTO>();
  private List<ContainerAdditionalInformationTO> additionalInfos = new ArrayList<>();

  /**
   * @return the collectionId
   */
  public String getCollectionId() {
    return collectionId;
  }

  /**
   * @param collectionId the collectionId to set
   */
  public void setCollectionId(String collectionId) {
    this.collectionId = collectionId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<PersonTO> getContributors() {
    return contributors;
  }

  public void setContributors(List<PersonTO> contributors) {
    this.contributors = contributors;
  }

  /**
   * @return the additionalInformations
   */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public List<ContainerAdditionalInformationTO> getAdditionalInfos() {
    return additionalInfos;
  }

  /**
   * @param additionalInformations the additionalInformations to set
   */
  public void setAdditionalInfos(List<ContainerAdditionalInformationTO> additionalInformations) {
    this.additionalInfos = additionalInformations;
  }

}
