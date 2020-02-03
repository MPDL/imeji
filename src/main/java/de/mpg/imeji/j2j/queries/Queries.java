package de.mpg.imeji.j2j.queries;

import java.util.LinkedList;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.tdb.TDB;

/**
 * Utility class for executing SPARQL queries from within Jena transaction.
 * 
 * @author breddin
 *
 */
public class Queries {

  /**
   * Execute a given SPARQL query on a given model. SPARQL query searches one or more URIs.
   * 
   * @param sparqlQuery
   * @param modelName
   * @return query results: list of URIs
   */
  public static LinkedList<String> executeSPARQLQueryAndGetResults(String sparqlQuery, Dataset dataset, String modelName) {

    final Query q = QueryFactory.create(sparqlQuery, Syntax.syntaxARQ);
    // set the model for more efficient handling
    final QueryExecution qexec = QueryExecutionFactory.create(q, dataset.getNamedModel(modelName));
    qexec.getContext().set(TDB.symUnionDefaultGraph, true);
    qexec.setTimeout(-1);

    LinkedList<String> resultURIs = new LinkedList<String>();
    try {
      final ResultSet resultSet = qexec.execSelect();
      while (resultSet.hasNext()) {
        QuerySolution sol = resultSet.nextSolution();
        RDFNode subject = sol.get("s");
        if (subject != null) {
          String result = subject.isURIResource() ? subject.asResource().getURI() : subject.asLiteral().toString();
          resultURIs.add(result);
        }
      }
    } finally {
      qexec.close();
    }

    return resultURIs;
  }



}
