/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.apache.log4j.lf5.util.StreamUtils;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.ImejiInitializer;
import de.mpg.imeji.logic.jobs.ElasticReIndexJob;
import de.mpg.imeji.logic.jobs.ReadMaxPlanckIPMappingJob;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Initialize application on server start
 *
 * @author saquet
 */
public class InitializerServlet extends HttpServlet {
  private static final long serialVersionUID = -3826737851602585061L;
  private static final Logger LOGGER = Logger.getLogger(InitializerServlet.class);

  @Override
  public void init() throws ServletException {
    try {
      super.init();
      Imeji.locksSurveyor.start();
      initModel();
      reindex();
      Imeji.getExecutor().submit(new ReadMaxPlanckIPMappingJob());
    } catch (Exception e) {
      LOGGER.error("imeji didn't initialize correctly", e);
    }
  }

  /**
   * Initialize the imeji jena tdb
   *
   * @throws URISyntaxException
   * @throws IOException
   * @throws ImejiException
   */
  public void initModel() throws IOException, URISyntaxException, ImejiException {
    ImejiInitializer.init();
    runMigration();
  }

  /**
   * Reindex the data
   */
  private void reindex() {
    if (Imeji.STARTUP.doReIndex()) {
      LOGGER.info("Doing reindex...");
      Imeji.getExecutor().submit(new ElasticReIndexJob());
    }
  }


  /**
   * look to the migration File (migration.txt)
   *
   * @throws IOException
   */
  private void runMigration() throws IOException {
    File f = new File(Imeji.tdbPath + StringHelper.urlSeparator + "migration.txt");
    FileInputStream in = null;
    try {
      in = new FileInputStream(f);
    } catch (FileNotFoundException e) {
      LOGGER.info("No " + f.getAbsolutePath() + " found, no migration runs");
    }
    if (in != null) {
      String migrationRequests = new String(StreamUtils.getBytes(in), "UTF-8");
      migrationRequests =
          migrationRequests.replaceAll("XXX_BASE_URI_XXX", Imeji.PROPERTIES.getBaseURI());
      migrationRequests = addNewIdToMigration(migrationRequests);
      LOGGER.info("Running migration with query: ");
      LOGGER.info(migrationRequests);
      ImejiSPARQL.execUpdate(migrationRequests);
      LOGGER.info("Migration done!");
    }
  }

  /**
   * Replace XXX_NEW_ID_XXX by a new ID in Migration File
   *
   * @param migrationRequests
   * @return
   */
  private String addNewIdToMigration(String migrationRequests) {
    Pattern p = Pattern.compile("XXX_NEW_ID_XXX");
    Matcher m = p.matcher(migrationRequests);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      m.appendReplacement(sb, IdentifierUtil.newId());
    }
    m.appendTail(sb);
    return sb.toString();
  }

  @Override
  public void destroy() {
    ImejiInitializer.shutdown();
    super.destroy();
  }
}
