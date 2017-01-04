/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.admin;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Resource;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.batch.CleanContentVOsJob;
import de.mpg.imeji.logic.batch.ElasticReIndexJob;
import de.mpg.imeji.logic.batch.FulltextAndTechnicalMetadataJob;
import de.mpg.imeji.logic.batch.ImportFileFromEscidocToInternalStorageJob;
import de.mpg.imeji.logic.batch.RefreshFileSizeJob;
import de.mpg.imeji.logic.batch.StorageUsageAnalyseJob;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.db.reader.ReaderFacade;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.administrator.StorageAdministrator;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.SuperBean;

/**
 * Bean for the administration page. Methods working on data
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class AdminBean extends SuperBean {
  private static final long serialVersionUID = 777808298937503532L;
  private static final Logger LOGGER = Logger.getLogger(AdminBean.class);
  private boolean clean = false;
  private String numberOfFilesInStorage;
  private String sizeOfFilesinStorage;
  private String freeSpaceInStorage;
  private String lastUpdateStorageStatistics;
  private Future<Integer> storageAnalyseStatus;
  private String cleanDatabaseReport = "";

  public AdminBean() {
    try {
      final StorageUsageAnalyseJob storageUsageAnalyse = new StorageUsageAnalyseJob();
      this.numberOfFilesInStorage = Integer.toString(storageUsageAnalyse.getNumberOfFiles());
      this.sizeOfFilesinStorage =
          FileUtils.byteCountToDisplaySize(storageUsageAnalyse.getStorageUsed());
      this.freeSpaceInStorage =
          FileUtils.byteCountToDisplaySize(storageUsageAnalyse.getFreeSpace());
      this.lastUpdateStorageStatistics = storageUsageAnalyse.getLastUpdate();
    } catch (IOException | URISyntaxException e) {
      LOGGER.error("Error constructing StorageUsageAnalyseJob", e);
    }

  }

  /**
   * Refresh the file size of all items
   *
   * @return
   */
  public String refreshFileSize() {
    Imeji.getEXECUTOR().submit(new RefreshFileSizeJob());
    return "";
  }

  /**
   * Reindex the Files, i.e., parse the fulltext and the technicale metadata and index it
   *
   * @return
   */
  public String extractFulltextAndTechnicalMetadata() {
    Imeji.getEXECUTOR().submit(new FulltextAndTechnicalMetadataJob());
    return "";
  }

  /**
   * Clean the {@link Storage}
   *
   * @return
   */
  public String cleanStorage() {
    final StorageController controller = new StorageController();
    controller.getAdministrator().clean();
    return "pretty:";
  }

  /**
   * Return the location of the internal storage
   *
   * @return
   * @throws IOException
   * @throws URISyntaxException
   */
  public String getInternalStorageLocation() throws IOException, URISyntaxException {
    return PropertyReader.getProperty("imeji.storage.path");
  }

  /**
   * Make the same as clean, but doesn't remove the resources
   *
   * @throws ImejiException
   *
   * @
   */
  public void status() throws ImejiException {
    clean = false;
    invokeCleanMethods();
  }

  /**
   * Here are called all methods related to data cleaning
   *
   * @throws ImejiException
   *
   * @
   */
  public void clean() throws ImejiException {
    clean = true;
    invokeCleanMethods();
  }

  /**
   * Start the job {@link StorageUsageAnalyseJob}
   *
   * @throws IOException
   * @throws URISyntaxException
   */
  public String analyseStorageUsage() throws IOException, URISyntaxException {
    storageAnalyseStatus = Imeji.getEXECUTOR().submit(new StorageUsageAnalyseJob());
    return "";
  }

  /**
   * Reindex all data
   */
  public void reindex() {
    Imeji.getEXECUTOR().submit(new ElasticReIndexJob());
  }

  /**
   * Import the files in an external storage (for instance escidoc) into the internal storage
   *
   * @
   */
  public String importToInternalStorage() {
    Imeji.getEXECUTOR().submit(new ImportFileFromEscidocToInternalStorageJob(getSessionUser()));
    return "";
  }

  /**
   * Invoke all clean methods available
   *
   * @throws ImejiException
   *
   * @
   */
  private void invokeCleanMethods() throws ImejiException {
    cleanStatement();
    cleanGrants();
    cleanContent();
  }

  private void cleanContent() {
    if (clean) {
      Imeji.getEXECUTOR().submit(new CleanContentVOsJob());
    }
  }

  /**
   * Clean {@link Statement} which are not bound a {@link MetadataProfile}
   *
   * @throws ImejiException
   *
   * @
   */
  private void cleanStatement() throws ImejiException {
    LOGGER.info("Searching for statement without profile...");
    final Search search = SearchFactory.create();
    final List<String> uris = search
        .searchString(JenaCustomQueries.selectStatementUnbounded(), null, null, 0, -1).getResults();
    LOGGER.info("...found " + uris.size());
    cleanDatabaseReport += "Statement without any profile " + uris.size() + " found  <br/> ";
    removeResources(uris, Imeji.profileModel, new Statement());
  }

  /**
   * Clean grants which are not related to a user
   *
   * @
   */
  private void cleanGrants() {
    if (clean) {
      ImejiSPARQL.execUpdate(JenaCustomQueries.removeGrantWithoutObject());
      ImejiSPARQL.execUpdate(JenaCustomQueries.removeGrantWithoutUser());
      ImejiSPARQL.execUpdate(JenaCustomQueries.removeGrantEmtpy());
    }
    LOGGER.info("Searching for problematic grants...");
    final Search search = SearchFactory.create();
    List<String> uris = search
        .searchString(JenaCustomQueries.selectGrantWithoutUser(), null, null, 0, -1).getResults();
    cleanDatabaseReport += "Grants without users: " + uris.size() + " found  <br/>";
    uris = search.searchString(JenaCustomQueries.selectGrantWithoutObjects(), null, null, 0, -1)
        .getResults();
    cleanDatabaseReport += "Grants on non existing objects: " + uris.size() + " found <br/>";
    uris =
        search.searchString(JenaCustomQueries.selectGrantEmtpy(), null, null, 0, -1).getResults();
    cleanDatabaseReport += "Empty Grants: " + uris.size() + " found  <br/>";
    LOGGER.info("...done");
  }

  /**
   * Remove Exception a {@link List} of {@link Resource}
   *
   * @param uris
   * @param modelName
   * @throws ImejiException
   * @throws IllegalAccessException
   * @throws InstantiationException @
   */
  private synchronized void removeResources(List<String> uris, String modelName, Object obj)
      throws ImejiException {
    if (clean) {
      removeObjects(loadResourcesAsObjects(uris, modelName, obj), modelName);
    }
  }

  /**
   * Load the {@link Resource} as {@link Object}
   *
   * @param uris
   * @param modelName
   * @param obj
   * @return
   */
  private List<Object> loadResourcesAsObjects(List<String> uris, String modelName, Object obj) {
    final ReaderFacade reader = new ReaderFacade(modelName);
    final List<Object> l = new ArrayList<Object>();
    for (final String uri : uris) {
      try {
        LOGGER.info("Resource to be removed: " + uri);
        l.add(reader.read(uri, getSessionUser(), obj.getClass().newInstance()));
      } catch (final Exception e) {
        LOGGER.error("ERROR LOADING RESOURCE " + uri + " !!!!!", e);
      }
    }
    return l;
  }

  /**
   * Remove an {@link Object}, it must have a {@link j2jId}
   *
   * @param l
   * @param modelName @
   * @throws ImejiException
   */
  private void removeObjects(List<Object> l, String modelName) throws ImejiException {
    if (clean) {
      final WriterFacade writer = new WriterFacade(modelName);
      writer.delete(l, getSessionUser());
    }
  }

  /**
   * return count of all {@link Album}
   *
   * @return
   */
  public int getAllAlbumsSize() {
    final Search search =
        SearchFactory.create(SearchObjectTypes.ALBUM, SEARCH_IMPLEMENTATIONS.JENA);
    return search.searchString(JenaCustomQueries.selectAlbumAll(), null, null, 0, -1)
        .getNumberOfRecords();
  }

  /**
   * return count of all {@link CollectionImeji}
   *
   * @return
   */
  public int getAllCollectionsSize() {
    final Search search =
        SearchFactory.create(SearchObjectTypes.COLLECTION, SEARCH_IMPLEMENTATIONS.JENA);
    return search.searchString(JenaCustomQueries.selectCollectionAll(), null, null, 0, -1)
        .getNumberOfRecords();
  }

  /**
   * return count of all {@link Item}
   *
   * @return
   */
  public int getAllImagesSize() {
    final Search search = SearchFactory.create(SearchObjectTypes.ITEM, SEARCH_IMPLEMENTATIONS.JENA);
    return search.searchString(JenaCustomQueries.selectItemAll(), null, null, 0, -1)
        .getNumberOfRecords();
  }

  /**
   * True if the current {@link Storage} has implemted a {@link StorageAdministrator}
   *
   * @return
   */
  public boolean isAdministrate() {
    final StorageController sc = new StorageController();
    return sc.getAdministrator() != null;
  }

  /**
   * Return all {@link User}
   *
   * @return
   */
  public List<User> getAllUsers() {
    final UserService uc = new UserService();
    return (List<User>) uc.searchUserByName("");
  }

  /**
   * return count of all {@link User}
   *
   * @return
   */
  public int getAllUsersSize() {
    try {
      return this.getAllUsers().size();
    } catch (final Exception e) {
      return 0;
    }
  }

  public String getNumberOfFilesInStorage() {
    return numberOfFilesInStorage;
  }

  public String getSizeOfFilesinStorage() {
    return sizeOfFilesinStorage;
  }

  public String getFreeSpaceInStorage() {
    return freeSpaceInStorage;
  }

  public String getLastUpdateStorageStatistics() {
    return lastUpdateStorageStatistics;
  }

  public boolean getStorageAnalyseStatus() {
    if (storageAnalyseStatus != null) {
      return storageAnalyseStatus.isDone();
    }
    return true;
  }

  public String getCleanDatabaseReport() {
    return cleanDatabaseReport;
  }
}
