package de.mpg.imeji.rest.to;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"query", "totalNumberOfResults", "numberOfResults", "offset", "size", "results"})
@JsonInclude(JsonInclude.Include.NON_NULL)
/**
 * A TO to wrap Search results
 *
 * @author bastiens
 *
 */
public class SearchResultTO<T> implements Serializable {

  private static final long serialVersionUID = -9046921435794271874L;
  private int totalNumberOfResults;
  private int numberOfResults;
  private int size;
  private int offset;
  private String query;
  private List<T> results = new ArrayList<>();

  public SearchResultTO() {}

  /**
   * Builder for {@link SearchResultTO}
   *
   * @author bastiens
   *
   */
  public static class Builder<T> {
    private int totalNumberOfResults;
    private int numberOfResults;
    private int size;
    private int offset;
    private String query;
    private List<T> results = new ArrayList<>();

    public SearchResultTO<T> build() {
      return new SearchResultTO<T>(this);
    }

    public Builder<T> totalNumberOfRecords(int i) {
      this.totalNumberOfResults = i;
      return this;
    }

    public Builder<T> numberOfRecords(int i) {
      this.numberOfResults = i;
      return this;
    }

    public Builder<T> size(int i) {
      this.size = i;
      return this;
    }

    public Builder<T> offset(int i) {
      this.offset = i;
      return this;
    }

    public Builder<T> query(String s) {
      this.query = s;
      return this;
    }

    public Builder<T> results(List<T> l) {
      this.results = l;
      return this;
    }

  }

  public SearchResultTO(Builder<T> builder) {
    this.query = builder.query;
    this.numberOfResults = builder.numberOfResults;
    this.offset = builder.offset;
    this.results = builder.results;
    this.size = builder.size;
    this.totalNumberOfResults = builder.totalNumberOfResults;
  }

  /**
   * @return the totalNumberOfResults
   */
  public int getTotalNumberOfResults() {
    return totalNumberOfResults;
  }

  /**
   * @param totalNumberOfResults the totalNumberOfResults to set
   */
  public void setTotalNumberOfResults(int totalNumberOfResults) {
    this.totalNumberOfResults = totalNumberOfResults;
  }

  /**
   * @return the numberOfResults
   */
  public int getNumberOfResults() {
    return numberOfResults;
  }

  /**
   * @param numberOfResults the numberOfResults to set
   */
  public void setNumberOfResults(int numberOfResults) {
    this.numberOfResults = numberOfResults;
  }

  /**
   * @return the query
   */
  public String getQuery() {
    return query;
  }

  /**
   * @param query the query to set
   */
  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * @return the results
   */
  public List<T> getResults() {
    return results;
  }

  /**
   * @param results the results to set
   */
  public void setResults(List<T> results) {
    this.results = results;
  }

  /**
   * @return the size
   */
  public int getSize() {
    return size;
  }

  /**
   * @param size the size to set
   */
  public void setSize(int size) {
    this.size = size;
  }

  /**
   * @return the offset
   */
  public int getOffset() {
    return offset;
  }

  /**
   * @param offset the offset to set
   */
  public void setOffset(int offset) {
    this.offset = offset;
  }

}
