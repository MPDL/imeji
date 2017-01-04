package de.mpg.imeji.logic.batch;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.collection.CollectionController;
import de.mpg.imeji.logic.content.ContentController;
import de.mpg.imeji.logic.controller.AlbumController;
import de.mpg.imeji.logic.controller.SpaceController;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticInitializer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.usergroup.UserGroupService;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;

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
      reindexAlbums(index);
      reindexItems(index);
      reindexContents(index);
      reindexFolders(index);
      reindexSpaces(index);
      ElasticInitializer.setNewIndexAndRemoveOldIndex(index);
      // IMPORTANT: Albums must be reindex after Items
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
    final CollectionController c = new CollectionController();
    c.reindex(index);
  }

  /**
   * Reindex all the {@link Album} stored in the database
   *
   * @throws ImejiException
   */
  private void reindexAlbums(String index) throws ImejiException {
    final AlbumController controller = new AlbumController();
    controller.reindex(index);
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
    new ContentController().reindex(index);
  }

  /**
   * Reindex all {@link Item} stored in the database
   *
   * @throws ImejiException
   *
   */
  private void reindexSpaces(String index) throws ImejiException {
    final SpaceController controller = new SpaceController();
    controller.reindex(index);
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
