package de.mpg.imeji.logic.search.elasticsearch.model;

public class ElasticJoinableContent {
  private ElasticContent content;
  private ElasticJoinField joinField;

  public ElasticContent getContent() {
    return content;
  }

  public void setContent(ElasticContent content) {
    this.content = content;
  }

  public ElasticJoinField getJoinField() {
    return joinField;
  }

  public void setJoinField(ElasticJoinField joinField) {
    this.joinField = joinField;
  }



}
