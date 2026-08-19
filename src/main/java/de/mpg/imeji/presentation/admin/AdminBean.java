package de.mpg.imeji.presentation.admin;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.batch.*;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.emailcontent.ImejiExternalEmailContent;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.db.repositories.UserDbRepository;
import de.mpg.imeji.logic.events.listener.ListenerService;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.notification.subscription.SubscriptionService;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.administrator.StorageAdministrator;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.update.UpdateAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.faces.bean.ManagedBean;
import jakarta.faces.bean.ViewScoped;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

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
  private static final Logger LOGGER = LogManager.getLogger(AdminBean.class);

  private String sparqlUpdateQuery;
  private boolean dryRunDeleteUsersWithoutGroups = true;

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
    Imeji.getEXECUTOR().submit(new CleanInternalStorageJob());
    Imeji.getEXECUTOR().submit(new CleanTempFilesJob());
    new ListenerService().init();
    cleanGrants();
    cleanSubscriptions();
    HierarchyService.reloadHierarchy();
  }

  /**
   * Reindex all data
   */
  public void reindex() {
    Imeji.getEXECUTOR().submit(new ElasticReIndexJob());
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
        LOGGER.error("User " + s.getUserId() + " is not allowed to subscribe to collection " + collectionUri + ", removing subscription");
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
    try {
      new UserDbRepository().removeZombieGrants();
    } catch (ImejiException e) {
      throw new RuntimeException(e);
    }
    //System.out.println(JenaCustomQueries.removeGrantWithoutObject(Imeji.PROPERTIES.getBaseURI()));
    //ImejiSPARQL.execUpdate(JenaCustomQueries.removeGrantWithoutObject(Imeji.PROPERTIES.getBaseURI()));
    LOGGER.info("...done!");
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

  public void recalculateWebAndThumbnail() {
    ResizeWebAndThumbnailJob job = new ResizeWebAndThumbnailJob();
    Imeji.getEXECUTOR().submit(job);
  }

  public void reGenerateFullWebThumbnailImages() {
    ReGenerateFullWebThumbnailJob job = new ReGenerateFullWebThumbnailJob();
    Imeji.getEXECUTOR().submit(job);
  }

  public void aggregateMessages() {
    Imeji.getEXECUTOR().submit(new AggregateMessages());
  }

  public void makeEmailTextsEditable() {
    ImejiExternalEmailContent.copyEmailContentToExternalXMLFiles();
  }

  public String getSparqlUpdateQuery() {
    return sparqlUpdateQuery;
  }

  public void setSparqlUpdateQuery(String sparqlUpdateQuery) {
    this.sparqlUpdateQuery = sparqlUpdateQuery;
  }

  public void runSparqlUpdateQuery() {
    final Dataset dataset = Imeji.dataset;
    try {
      dataset.begin(ReadWrite.WRITE);
      UpdateAction.parseExecute(sparqlUpdateQuery, dataset);
      dataset.commit();
      BeanHelper.info("SPARQL update query successfully executed");
    } catch (Exception e) {
      dataset.abort();
      BeanHelper.error(e.getMessage());
      LOGGER.error(e);
    }

  }


  public String deleteAllUsersWithoutGrant() {
    LOGGER.info((dryRunDeleteUsersWithoutGroups ? "DRY RUN -- " : "") + " Deleting all users without user groups and default permissions");
    final UserService controller = new UserService();
    List<User> allUsers = new ArrayList<>();
    try {
      allUsers = controller.retrieveAll();
    } catch (ImejiException e) {
      LOGGER.error("Error retrieving all users", e);
    }
    int count = 0;
    for (User fullUser : allUsers) {
      //LOGGER.info(user.getEmail() + user.getGroups() + user.getGrants());
      try {
        LocalDate modDate = LocalDate.ofInstant(fullUser.getModified().getTime().toInstant(), ZoneId.systemDefault());
        LocalDate today = LocalDate.now();
        boolean isOlderThan1Month = modDate.isBefore(today.minusMonths(1));
        //LOGGER.info("User " + fullUser.getEmail() + " ("+ fullUser.getPerson().getCompleteName()+") " + fullUser.getGrants() + " " + fullUser.getGroups() + " "+ isOlderThan1Month);
        if (fullUser.getGrants().size() == 2 && (fullUser.getGrants().stream().anyMatch(i -> i.equals("READ,http://imeji.org/")))
            && (fullUser.getGrants().stream().anyMatch(i -> i.startsWith("ADMIN,http://imeji.org/user/")))
            && (fullUser.getGroups() == null || fullUser.getGroups().isEmpty()) && isOlderThan1Month) {

          if (dryRunDeleteUsersWithoutGroups) {
            LOGGER.info("DRY RUN DELETE USER: " + fullUser.getEmail() + "; " + fullUser.getPerson().getCompleteName() + "; "
                + fullUser.getId() + "; " + fullUser.getGrants());
          } else {
            controller.delete(fullUser);
            LOGGER.info("Successfully deleted user " + fullUser.getEmail() + "; " + fullUser.getPerson().getCompleteName() + "; "
                + fullUser.getId() + "; " + fullUser.getGrants());
          }

          count++;
        }
      } catch (Exception e) {
        String userMessage =
            "Error deleting user: " + fullUser.getEmail() + "; " + fullUser.getPerson().getCompleteName() + "; " + fullUser.getId();
        LOGGER.error(userMessage, e);
      }


    }
    String userMessage = count + " users deleted successfully";
    BeanHelper.info(userMessage);
    LOGGER.info(userMessage);

    /*
    final String email = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("email");
    final UserService controller = new UserService();
    try {
      controller.delete(controller.retrieve(email, getSessionUser()));
      reload();
    } catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
      String userMessage = "Error deleting user: " + exceptionWithMessage.getUserMessage(getLocale());
      BeanHelper.error(userMessage);
      if (exceptionWithMessage.getMessage() != null) {
        LOGGER.error(exceptionWithMessage.getMessage(), exceptionWithMessage);
      } else {
        LOGGER.error(userMessage, exceptionWithMessage);
      }
    } catch (final Exception e) {
      BeanHelper.error("Error deleting user");
      LOGGER.error("Error deleting user", e);
    }
    
     */
    return "";


  }


  public boolean isDryRunDeleteUsersWithoutGroups() {
    return dryRunDeleteUsersWithoutGroups;
  }

  public void setDryRunDeleteUsersWithoutGroups(boolean dryRunDeleteUsersWithoutGroups) {
    this.dryRunDeleteUsersWithoutGroups = dryRunDeleteUsersWithoutGroups;
  }
}
