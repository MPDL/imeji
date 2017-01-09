/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.upload;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.storage.Storage;

/**
 * The Servlet to Read files from imeji {@link Storage}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@WebServlet("/uploadServlet")
public class UploadServlet extends HttpServlet {
  private static final long serialVersionUID = -4879871986174193049L;
  private static final Logger LOGGER = Logger.getLogger(UploadServlet.class);


  @Override
  public void init() {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    System.out.println("POST !!!!!!!! Upload Servlet !!!!!!!");
    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "ups");
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    System.out.println("GET !!!!!!!! Upload Servlet !!!!!!!");
  }

}
