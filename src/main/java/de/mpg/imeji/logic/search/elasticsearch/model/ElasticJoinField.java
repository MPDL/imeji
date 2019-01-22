package de.mpg.imeji.logic.search.elasticsearch.model;

public class ElasticJoinField {
  private String parent;
  private String name;

  public void setParent(String parent) {
    this.parent = parent;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ElasticJoinField(String name, String parent) {
    this.name = name;
    this.parent = parent;
  }

  public ElasticJoinField() {
    // TODO Auto-generated constructor stub
  }

  public String getParent() {
    return parent;
  }

  public String getName() {
    return name;
  }
}
