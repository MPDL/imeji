package de.mpg.imeji.logic.batch;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticInitializer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;

/**
 * REindex data from the database into elastic search
 *
 * @author bastiens
 *
 */
public class ElasticReIndexJob implements Callable<Integer> {

  private static final Logger LOGGER = Logger.getLogger(ElasticReIndexJob.class);

  @Override
  public Integer call() {
    try {
      LOGGER.info("Reindex started!");
      // Check if the alias is used by only 1 index. If not, reset completely the indexes
      ElasticInitializer.getIndexNameFromAliasName(ElasticService.DATA_ALIAS);
      final String index = ElasticInitializer.createIndex();
      addAllMappings(index);
      reindexUsers(index);
      reindexUserGroups(index);
      reindexItems(index);
      reindexContents(index);
      reindexFolders(index);
      ElasticInitializer.setNewIndexAndRemoveOldIndex(index);
      LOGGER.info("Reindex done!");
    } catch (final Exception e) {
      LOGGER.error("Error by reindex", e);
    }

    return null;
  }

  /**
   * Add the Mapping first, to avoid conflicts in mappings
   *
   * @param index
   */
  private void addAllMappings(String index) {
    for (final ElasticTypes type : ElasticTypes.values()) {
      new ElasticIndexer(index, type, ElasticService.ANALYSER).addMapping();
    }
  }

  /**
   * Reindex all the {@link CollectionImeji} stored in the database
   *
   * @throws ImejiException
   */
  private void reindexFolders(String index) throws ImejiException {
    final CollectionService c = new CollectionService();
    c.reindex(index);
  }

  /**
   * Reindex all {@link Item} stored in the database
   *
   * @throws ImejiException
   *
   */
  private void reindexItems(String index) throws ImejiException {
    final ItemService controller = new ItemService();
    controller.reindex(index);
  }

  /**
   * Reindex all ContentVO stored in the database
   *
   * @throws ImejiException
   *
   */
  private void reindexContents(String index) throws ImejiException {
    new ContentService().reindex(index);
  }

  /**
   * Reindex all users
   *
   * @param index
   * @throws ImejiException
   */
  private void reindexUsers(String index) throws ImejiException {
    new UserService().reindex(index);
  }

  /**
   * Reindex all usergroups
   *
   * @param index
   * @throws ImejiException
   */
  private void reindexUserGroups(String index) throws ImejiException {
    new UserGroupService().reindex(index);
  }

}
