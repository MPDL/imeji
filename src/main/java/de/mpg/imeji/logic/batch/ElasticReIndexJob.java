package de.mpg.imeji.logic.batch;

import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticInitializer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticAnalysers;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;

/**
 * REindex data from the database into elastic search
 *
 * @author bastiens
 *
 */
public class ElasticReIndexJob implements Callable<Integer> {

  private static final Logger LOGGER = LogManager.getLogger(ElasticReIndexJob.class);

  @Override
  public Integer call() {
    try {
      LOGGER.info("Reindex started!");
      // ElasticService.ANALYSER =
      // ElasticAnalysers.valueOf(PropertyReader.getProperty("elastic.analyser"));
      // Check if the alias is used by only 1 index. If not, reset completely the
      // indexes
      // ElasticInitializer.getIndexNameFromAliasName(ElasticService.DATA_ALIAS);
      // final String index = ElasticInitializer.createIndex();
      // addAllMappings(index);
      for (final ElasticIndices index : ElasticIndices.values()) {
        ElasticInitializer.initializeIndex(index);
        new ElasticIndexer(index.name()).addMapping();
      }

      try {
        reindexUsers(ElasticIndices.users.name());
      } catch (Exception e) {
        LOGGER.error("Error while reindexing users", e);
      }

      try {
        reindexUserGroups(ElasticIndices.usergroups.name());
      } catch (Exception e) {
        LOGGER.error("Error while reindexing usergroups", e);
      }

      try {
        reindexItems(ElasticIndices.items.name());
      } catch (Exception e) {
        LOGGER.error("Error while reindexing items", e);
      }

      try {
        reindexContents(ElasticIndices.items.name());
      } catch (Exception e) {
        LOGGER.error("Error while reindexing contents", e);
      }

      try {
        reindexFolders(ElasticIndices.folders.name());
      } catch (Exception e) {
        LOGGER.error("Error while reindexing folders", e);
      }
      // ElasticInitializer.setNewIndexAndRemoveOldIndex(index);
      LOGGER.info("Reindex done!");

    } catch (final Exception e) {
      LOGGER.error("Error while initializing indexes, stopping reindex", e);
    }

    return null;
  }

  /**
   * Add the Mapping first, to avoid conflicts in mappings
   *
   * @param index
   */
  /*
   * private void addAllMappings(String index) { for (final ElasticIndices type :
   * ElasticIndices.values()) { new ElasticIndexer(index, type,
   * ElasticService.ANALYSER).addMapping(); } }
   */

  /**
   * Reindex all the {@link CollectionImeji} stored in the database
   *
   * @throws ImejiException
   */
  private void reindexFolders(String index) throws Exception {
    final CollectionService c = new CollectionService();
    c.reindex(index);
  }

  /**
   * Reindex all {@link Item} stored in the database
   *
   * @throws ImejiException
   *
   */
  private void reindexItems(String index) throws Exception {
    final ItemService controller = new ItemService();
    controller.reindex(index);
  }

  /**
   * Reindex all ContentVO stored in the database
   *
   * @throws ImejiException
   *
   */
  private void reindexContents(String index) throws Exception {
    new ContentService().reindex(index);
  }

  /**
   * Reindex all users
   *
   * @param index
   * @throws ImejiException
   */
  private void reindexUsers(String index) throws Exception {
    new UserService().reindex(index);
  }

  /**
   * Reindex all usergroups
   *
   * @param index
   * @throws ImejiException
   */
  private void reindexUserGroups(String index) throws Exception {
    new UserGroupService().reindex(index);
  }

}
