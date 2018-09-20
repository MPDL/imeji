package de.mpg.imeji.presentation.admin;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger; 
import org.apache.logging.log4j.LogManager;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;
import com.hp.hpl.jena.tdb.TDB;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * Servlet implementing a sparql end point
 *
 * @author saquet
 *
 */
@WebServlet("/sparql")
public class SPARQLEndpointServlet extends HttpServlet {
  private static final Logger LOGGER = LogManager.getLogger(SPARQLEndpointServlet.class);
  private static final long serialVersionUID = 2718460776590689258L;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    final String q = req.getParameter("q");
    final String format = req.getParameter("format");
    final String model = req.getParameter("model");
    final SessionBean session =
        (SessionBean) req.getSession(false).getAttribute(SessionBean.class.getSimpleName());
    if (!"".equals(q) && session.getUser() != null
        && SecurityUtil.authorization().isSysAdmin(session.getUser())) {
      try {
        Imeji.dataset.begin(ReadWrite.WRITE);
        final Query sparql = QueryFactory.create(q, Syntax.syntaxARQ);
        final QueryExecution exec = initQueryExecution(sparql, model);
        exec.getContext().set(TDB.symUnionDefaultGraph, true);
        final ResultSet result = exec.execSelect();
        if ("table".equals(format)) {
          ResultSetFormatter.out(resp.getOutputStream(), result);
        } else {
          ResultSetFormatter.output(resp.getOutputStream(), result, getFormat(format));
        }
      } catch (final Exception e) {
        LOGGER.error("spraql error: ", e);
        Imeji.dataset.abort();
      } finally {
        Imeji.dataset.commit();
        Imeji.dataset.end();
      }
    } else if (session.getUser() == null) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN,
          "imeji security: You need administration priviliges");
    } else if (SecurityUtil.authorization().isSysAdmin(session.getUser())) {
      resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "imeji security: You need to be signed-in");
    }
  }

  private QueryExecution initQueryExecution(Query sparql, String model) {
    final String modelName = getModelName(model);
    if (!StringHelper.isNullOrEmptyTrim(modelName)) {
      return QueryExecutionFactory.create(sparql, Imeji.dataset.getNamedModel(modelName));
    }
    return QueryExecutionFactory.create(sparql, Imeji.dataset);
  }

  private String getModelName(String name) {
    if ("item".equals(name)) {
      return Imeji.imageModel;
    } else if ("collection".equals(name)) {
      return Imeji.collectionModel;
    } else if ("statement".equals(name)) {
      return Imeji.statementModel;
    } else if ("facet".equals(name)) {
      return Imeji.facetModel;
    } else if ("user".equals(name)) {
      return Imeji.userModel;
    } else if ("content".equals(name)) {
      return Imeji.contentModel;
    }
    return name;
  }

  private ResultsFormat getFormat(String format) {
    if ("csv".equals(format)) {
      return ResultsFormat.FMT_RS_CSV;
    } else if ("json".equals(format)) {
      return ResultsFormat.FMT_RS_JSON;
    } else if ("tsv".equals(format)) {
      return ResultsFormat.FMT_RS_TSV;
    } else if ("ttl".equals(format)) {
      return ResultsFormat.FMT_RDF_TTL;
    } else if ("bio".equals(format)) {
      return ResultsFormat.FMT_RS_BIO;
    }
    return ResultsFormat.FMT_RDF_XML;
  }
}
