package de.mpg.imeji.presentation.admin;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Future;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.batch.AggregateMessages;
import de.mpg.imeji.logic.batch.CleanContentVOsJob;
import de.mpg.imeji.logic.batch.CleanInternalStorageJob;
import de.mpg.imeji.logic.batch.CleanTempFilesJob;
import de.mpg.imeji.logic.batch.ElasticReIndexJob;
import de.mpg.imeji.logic.batch.FulltextAndTechnicalMetadataJob;
import de.mpg.imeji.logic.batch.RefreshFileSizeJob;
import de.mpg.imeji.logic.batch.ResizeWebAndThumbnailJob;
import de.mpg.imeji.logic.batch.StorageUsageAnalyseJob;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.events.listener.ListenerService;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.notification.subscription.SubscriptionService;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.administrator.StorageAdministrator;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.presentation.beans.SuperBean;

/**
 * Bean for the administration page. Methods working on data
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "AdminBean")
@ViewScoped
public class AdminBean extends SuperBean {
  private static final long serialVersionUID = 777808298937503532L;
  private static final Logger LOGGER = Logger.getLogger(AdminBean.class);
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
   * Here are called all methods related to data cleaning
   *
   * @throws ImejiException
   *
   * @
   */
  public void clean() throws ImejiException {
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
   * Invoke all clean methods available
   *
   * @throws ImejiException
   *
   * @
   */
  private void invokeCleanMethods() throws ImejiException {
    Imeji.getEXECUTOR().submit(new CleanInternalStorageJob());
    Imeji.getEXECUTOR().submit(new CleanTempFilesJob());
    new ListenerService().init();
    cleanGrants();
    cleanSubscriptions();
    HierarchyService.reloadHierarchy();
    // cleanContent();
  }

  private void cleanSubscriptions() throws ImejiException {
    SubscriptionService service = new SubscriptionService();
    List<Subscription> subscriptions = new SubscriptionService().retrieveAll(Imeji.adminUser);
    for (Subscription s : subscriptions) {
      String collectionUri = ObjectHelper.getURI(CollectionImeji.class, s.getObjectId()).toString();
      CollectionImeji c = null;
      try {
        c = new CollectionService().retrieve(collectionUri, Imeji.adminUser);
      } catch (NotFoundException e) {
        LOGGER.error("Collection " + collectionUri + " not found, removing subscription");
        service.unSubscribe(s, Imeji.adminUser);
      }
      User user = null;
      try {
        user = new UserService().retrieve(URI.create(s.getUserId()), Imeji.adminUser);
      } catch (NotFoundException e) {
        LOGGER.error("User " + s.getUserId() + " not found, removing subscription");
        service.unSubscribe(s, Imeji.adminUser);
      }
      if (c != null && user != null && !SecurityUtil.authorization().read(user, c)) {
        LOGGER.error("User " + s.getUserId() + " is not allowed to subscribe to collection "
            + collectionUri + ", removing subscription");
        service.unSubscribe(s, Imeji.adminUser);
      }
    }
  }

  private void cleanContent() {
    Imeji.getEXECUTOR().submit(new CleanContentVOsJob());
  }

  /**
   * Clean grants which are not related to a user
   *
   * @
   */
  private void cleanGrants() {
    LOGGER.info("Cleaning grants...");
    System.out.println(JenaCustomQueries.removeGrantWithoutObject(Imeji.PROPERTIES.getBaseURI()));
    ImejiSPARQL
        .execUpdate(JenaCustomQueries.removeGrantWithoutObject(Imeji.PROPERTIES.getBaseURI()));
    LOGGER.info("...done!");
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

  public void recalculateWebAndThumbnail() {
    ResizeWebAndThumbnailJob job = new ResizeWebAndThumbnailJob();
    Imeji.getEXECUTOR().submit(job);
  }

  public void aggregateMessages() {
    Imeji.getEXECUTOR().submit(new AggregateMessages());
  }

}
