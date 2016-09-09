package de.mpg.imeji.j2j.transaction;

import java.util.List;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.tdb.TDB;

import de.mpg.imeji.exceptions.ImejiException;

/**
 * {@link Transaction} for search operation
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchTransaction extends Transaction {
  private String searchQuery;
  private List<String> results;
  private String modelName = null;
  private boolean count = false;
  public final static String SORT_VALUE_REGEX = "XXX_SORT_VALUE_PATTERN_XXX";

  /**
   * Construct a new {@link SearchTransaction}
   *
   * @param modelName
   * @param searchQuery
   * @param results
   * @param count
   */
  public SearchTransaction(String modelName, String searchQuery, List<String> results,
      boolean count) {
    super(null);
    this.searchQuery = searchQuery;
    this.results = results;
    this.count = count;
    this.modelName = modelName;
  }

  @Override
  protected void execute(Dataset ds) throws ImejiException {
    Query q = QueryFactory.create(searchQuery, Syntax.syntaxARQ);
    QueryExecution qexec = initQueryExecution(ds, q);
    qexec.getContext().set(TDB.symUnionDefaultGraph, true);
    qexec.setTimeout(100000);
    try {
      ResultSet rs = qexec.execSelect();
      setResults(rs);
      count = true;
    } finally {
      qexec.close();
      count = false;
    }
  }

  /**
   * Initialize a new a {@link QueryExecution} for a SPARQL query
   *
   * @param ds
   * @param q
   * @return
   */
  private QueryExecution initQueryExecution(Dataset ds, Query q) {
    if (modelName != null) {
      return QueryExecutionFactory.create(q, ds.getNamedModel(modelName));
    }
    return QueryExecutionFactory.create(q, ds);
  }

  /**
   * Set the results according to the search type
   *
   * @param rs
   */
  private void setResults(ResultSet rs) {
    if (count) {
      setCountResults(rs);
    } else {
      setExecResults(rs);
    }
  }

  /**
   * set results results for count results
   *
   * @param rs
   */
  private void setCountResults(ResultSet rs) {
    if (rs.hasNext()) {
      QuerySolution qs = rs.next();
      Literal l = qs.getLiteral("?.1");
      int c = l.getInt();
      results.add(Integer.toString(c));
    }
  }

  /**
   * Set results for exec search
   *
   * @param rs
   */
  private void setExecResults(ResultSet rs) {
    for (; rs.hasNext();) {
      results.add(0, readResult(rs));
    }
  }

  /**
   * Parse the {@link ResultSet}
   *
   * @param results
   * @return
   */
  private String readResult(ResultSet results) {
    QuerySolution qs = results.nextSolution();
    RDFNode rdfNode = qs.get("sort0");
    if (rdfNode != null) {
      String sortValue = "";
      if (rdfNode.isLiteral()) {
        sortValue = rdfNode.asLiteral().toString();
      } else if (rdfNode.isURIResource()) {
        sortValue = rdfNode.asResource().getURI();
      }
      return addSortValue(qs.getResource("s").toString(), sortValue);
    }
    RDFNode node = qs.get("s");

    /*
     * Was causing internal Server error when node was Null (i.e. there were no results),
     *
     * see https://github.com/imeji-community/imeji/issues/1010
     */
    if (node != null) {
      return node.isURIResource() ? node.asResource().getURI() : node.asLiteral().toString();
    } else {
      return null;
    }

  }

  @Override
  protected ReadWrite getLockType() {
    return ReadWrite.READ;
  }

  /**
   * A a sort value to a {@link String}
   *
   * @param s
   * @param sortValue
   * @return
   */
  private String addSortValue(String s, String sortValue) {
    return s + SORT_VALUE_REGEX + sortValue;
  }
}
