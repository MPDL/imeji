package de.mpg.imeji.logic.jobs;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.controller.resource.SpaceController;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticInitializer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.user.controller.GroupBusinessController;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;

/**
 * REindex data from the database into elastic search
 *
 * @author bastiens
 *
 */
public class ElasticReIndexJob implements Callable<Integer> {

  private static final Logger LOGGER = Logger.getLogger(ElasticReIndexJob.class);

  @Override
  public Integer call() throws Exception {
    LOGGER.info("Reindex started!");
    // Check if the alias is used by only 1 index. If not, reset completely the indexes
    ElasticInitializer.getIndexNameFromAliasName(ElasticService.DATA_ALIAS);
    String index = ElasticInitializer.createIndex();
    addAllMappings(index);
    reindexUsers(index);
    reindexUserGroups(index);
    reindexAlbums(index);
    reindexItems(index);
    reindexFolders(index);
    reindexSpaces(index);
    ElasticInitializer.setNewIndexAndRemoveOldIndex(index);
    // IMPORTANT: Albums must be reindex after Items
    LOGGER.info("Reindex done!");
    return null;
  }

  /**
   * Add the Mapping first, to avoid conflicts in mappings
   *
   * @param index
   */
  private void addAllMappings(String index) {
    for (ElasticTypes type : ElasticTypes.values()) {
      new ElasticIndexer(index, type, ElasticService.ANALYSER).addMapping();
    }
  }

  /**
   * Reindex all the {@link CollectionImeji} stored in the database
   *
   * @throws ImejiException
   */
  private void reindexFolders(String index) throws ImejiException {
    LOGGER.info("Indexing Folders...");
    ElasticIndexer indexer =
        new ElasticIndexer(index, ElasticTypes.folders, ElasticService.ANALYSER);
    CollectionController c = new CollectionController();
    List<CollectionImeji> collections = (List<CollectionImeji>) c.retrieveAll(Imeji.adminUser);
    indexer.indexBatch(collections);
    indexer.commit();
    LOGGER.info("Folders reindexed!");
  }

  /**
   * Reindex all the {@link Album} stored in the database
   *
   * @throws ImejiException
   */
  private void reindexAlbums(String index) throws ImejiException {
    LOGGER.info("Indexing Albums...");
    ElasticIndexer indexer =
        new ElasticIndexer(index, ElasticTypes.albums, ElasticService.ANALYSER);
    AlbumController controller = new AlbumController();
    List<Album> albums = controller.retrieveAll(Imeji.adminUser);
    indexer.indexBatch(albums);
    indexer.commit();
    LOGGER.info("Albums reindexed!");
  }

  /**
   * Reindex all {@link Item} stored in the database
   *
   * @throws ImejiException
   *
   */
  private void reindexItems(String index) throws ImejiException {
    LOGGER.info("Indexing Items...");
    ElasticIndexer indexer = new ElasticIndexer(index, ElasticTypes.items, ElasticService.ANALYSER);
    ItemController controller = new ItemController();
    List<Item> items = (List<Item>) controller.retrieveAll(Imeji.adminUser);
    LOGGER.info("+++ " + items.size() + " items to index +++");
    indexer.indexBatch(items);
    indexer.commit();
    LOGGER.info("Items reindexed!");
  }

  /**
   * Reindex all {@link Item} stored in the database
   *
   * @throws ImejiException
   *
   */
  private void reindexSpaces(String index) throws ImejiException {
    LOGGER.info("Indexing Spaces...");
    ElasticIndexer indexer =
        new ElasticIndexer(index, ElasticTypes.spaces, ElasticService.ANALYSER);
    SpaceController controller = new SpaceController();
    List<Space> items = controller.retrieveAll();
    LOGGER.info("+++ " + items.size() + " items to index +++");
    indexer.indexBatch(items);
    indexer.commit();
    LOGGER.info("Spaces reindexed!");
  }

  /**
   * Reindex all users
   * 
   * @param index
   * @throws ImejiException
   */
  private void reindexUsers(String index) throws ImejiException {
    LOGGER.info("Indexing users...");
    ElasticIndexer indexer = new ElasticIndexer(index, ElasticTypes.users, ElasticService.ANALYSER);
    List<User> users = new UserBusinessController().retrieveAll();
    LOGGER.info("+++ " + users.size() + " users to index +++");
    indexer.indexBatch(users);
    indexer.commit();
    LOGGER.info("...users reindexed!");
  }

  /**
   * Reindex all usergroups
   * 
   * @param index
   * @throws ImejiException
   */
  private void reindexUserGroups(String index) throws ImejiException {
    LOGGER.info("Indexing users...");
    ElasticIndexer indexer =
        new ElasticIndexer(index, ElasticTypes.usergroups, ElasticService.ANALYSER);
    List<UserGroup> groups = (List<UserGroup>) new GroupBusinessController().retrieveAll();
    LOGGER.info("+++ " + groups.size() + " user groups to index +++");
    indexer.indexBatch(groups);
    indexer.commit();
    LOGGER.info("...user groups reindexed!");
  }

}
