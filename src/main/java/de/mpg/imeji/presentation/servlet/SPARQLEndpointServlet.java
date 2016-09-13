package de.mpg.imeji.presentation.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

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

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.security.util.SecurityUtil;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * Servlet implementing a sparql end point
 *
 * @author saquet
 *
 */
public class SPARQLEndpointServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(SPARQLEndpointServlet.class);

  /**
   *
   */
  private static final long serialVersionUID = 2718460776590689258L;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String q = req.getParameter("q");
    String format = req.getParameter("format");
    String model = req.getParameter("model");
    SessionBean session =
        (SessionBean) req.getSession(false).getAttribute(SessionBean.class.getSimpleName());
    if (!"".equals(q) && session.getUser() != null && SecurityUtil.isSysAdmin(session.getUser())) {
      try {
        Imeji.dataset.begin(ReadWrite.WRITE);
        Query sparql = QueryFactory.create(q, Syntax.syntaxARQ);
        QueryExecution exec = initQueryExecution(sparql, model);
        exec.getContext().set(TDB.symUnionDefaultGraph, true);
        ResultSet result = exec.execSelect();
        if ("table".equals(format)) {
          ResultSetFormatter.out(resp.getOutputStream(), result);
        } else {
          ResultSetFormatter.output(resp.getOutputStream(), result, getFormat(format));
        }
      } catch (Exception e) {
        LOGGER.error("spraql error: ", e);
        Imeji.dataset.abort();
      } finally {
        Imeji.dataset.commit();
        Imeji.dataset.end();
      }
    } else if (session.getUser() == null) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN,
          "imeji security: You need administration priviliges");
    } else if (SecurityUtil.isSysAdmin(session.getUser())) {
      resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "imeji security: You need to be signed-in");
    }
  }

  private QueryExecution initQueryExecution(Query sparql, String model) {
    String modelName = getModelName(model);
    if (modelName != null) {
      return QueryExecutionFactory.create(sparql, Imeji.dataset.getNamedModel(modelName));
    }
    return QueryExecutionFactory.create(sparql, Imeji.dataset);
  }

  private String getModelName(String name) {
    if ("item".equals(name)) {
      return Imeji.imageModel;
    } else if ("collection".equals(name)) {
      return Imeji.collectionModel;
    } else if ("album".equals(name)) {
      return Imeji.albumModel;
    } else if ("profile".equals(name)) {
      return Imeji.profileModel;
    } else if ("user".equals(name)) {
      return Imeji.userModel;
    }
    return null;
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
