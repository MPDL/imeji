package de.mpg.imeji.j2j.transaction;

import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.tdb.TDB;

import de.mpg.imeji.exceptions.ImejiException;

/**
 * {@link Transaction} for search operation
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchTransaction extends Transaction {
  private final String searchQuery;
  private final List<String> results;
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
  public SearchTransaction(String modelName, String searchQuery, List<String> results, boolean count) {
    super(null);
    this.searchQuery = searchQuery;
    this.results = results;
    this.count = count;
    this.modelName = modelName;
  }

  @Override
  protected void execute(Dataset ds) throws ImejiException {
    final Query q = QueryFactory.create(searchQuery, Syntax.syntaxARQ);
    final QueryExecution qexec = initQueryExecution(ds, q);
    qexec.getContext().set(TDB.symUnionDefaultGraph, true);
    qexec.setTimeout(-1);
    try {
      final ResultSet rs = qexec.execSelect();
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
      final QuerySolution qs = rs.next();
      final Literal l = qs.getLiteral("?.1");
      final int c = l.getInt();
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
    final QuerySolution qs = results.nextSolution();
    final RDFNode rdfNode = qs.get("sort0");
    if (rdfNode != null) {
      String sortValue = "";
      if (rdfNode.isLiteral()) {
        sortValue = rdfNode.asLiteral().toString();
      } else if (rdfNode.isURIResource()) {
        sortValue = rdfNode.asResource().getURI();
      }
      return addSortValue(qs.getResource("s").toString(), sortValue);
    }
    final RDFNode sVariable = qs.get("s");
    final RDFNode oVariable = qs.get("o");

    String result = null;
    if (sVariable != null) {
      result = sVariable.isURIResource() ? sVariable.asResource().getURI() : sVariable.asLiteral().toString();
    }
    if (oVariable != null) {
      result += "|" + (oVariable.isURIResource() ? oVariable.asResource().getURI() : oVariable.asLiteral().toString());
    }
    return result;
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
