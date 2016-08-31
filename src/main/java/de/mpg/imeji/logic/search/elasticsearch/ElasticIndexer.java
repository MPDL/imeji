package de.mpg.imeji.logic.search.elasticsearch;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.search.SearchIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticAnalysers;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticAlbum;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFolder;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticItem;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticSpace;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticUser;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticUserGroup;
import de.mpg.imeji.logic.search.elasticsearch.util.ElasticSearchUtil;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Properties;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;

/**
 * Indexer for ElasticSearch
 *
 * @author bastiens
 *
 */
public class ElasticIndexer implements SearchIndexer {
  private static final Logger LOGGER = Logger.getLogger(ElasticIndexer.class);
  private static final ObjectMapper mapper = new ObjectMapper();
  private String index = "data";
  private String dataType;
  private ElasticAnalysers analyser;
  private String mappingFile = "elasticsearch/Elastic_TYPE_Mapping.json";

  public ElasticIndexer(String indexName, ElasticTypes dataType, ElasticAnalysers analyser) {
    this.index = indexName;
    this.dataType = dataType.name();
    this.analyser = analyser;
    this.mappingFile = mappingFile.replace("_TYPE_", StringUtils.capitalize(this.dataType));
  }


  @Override
  public void index(Object obj) {
    List<String> collectionsToReindex = new ArrayList<String>();
    try {
      addSpaceForldersToRedindex(collectionsToReindex, obj);
      indexJSON(getId(obj), toJson(obj, dataType));
      commit();
      reindexFoldersItems(collectionsToReindex);
    } catch (Exception e) {
      LOGGER.error("Error indexing object ", e);
    }
  }


  @Override
  public void indexBatch(List<?> l) {
    List<String> collectionsToReindex = new ArrayList<String>();
    try {
      for (Object obj : l) {
        addSpaceForldersToRedindex(collectionsToReindex, obj);
        indexJSON(getId(obj), toJson(obj, dataType));
      }
      commit();
      reindexFoldersItems(collectionsToReindex);

    } catch (Exception e) {
      LOGGER.error("error indexing object ", e);
    }
  }


  @Override
  public void delete(Object obj) {
    String id = getId(obj);
    if (id != null) {
      ElasticService.client.prepareDelete(index, dataType, id).execute().actionGet();
      commit();
    }
  }

  @Override
  public void deleteBatch(List<?> l) {
    for (Object obj : l) {
      String id = getId(obj);
      if (id != null) {
        ElasticService.client.prepareDelete(index, dataType, id).execute().actionGet();
      }

    }
    commit();
  }

  /**
   * Transform an object to a json
   *
   * @param obj
   * @return
   * @throws UnprocessableError
   */
  public static String toJson(Object obj, String dataType) throws UnprocessableError {
    try {
      return mapper.writeValueAsString(toESEntity(obj, dataType));
    } catch (JsonProcessingException e) {
      throw new UnprocessableError("Error serializing object to json", e);
    }
  }

  /**
   * Index in Elasticsearch the passed json with the given id
   *
   * @param id
   * @param json
   */
  public void indexJSON(String id, String json) {
    if (id != null) {
      ElasticService.client.prepareIndex(index, dataType).setId(id).setSource(json).execute()
          .actionGet();
    }
  }

  /**
   * Make all changes done searchable. Kind of a commit. Might be important if data needs to be
   * immediately available for other tasks
   */
  public void commit() {
    ElasticService.client.admin().indices().prepareRefresh(index).execute().actionGet();
  }

  /**
   * Remove all indexed data
   */
  public static void clear(String index) {
    DeleteIndexResponse delete =
        ElasticService.client.admin().indices().delete(new DeleteIndexRequest(index)).actionGet();
    if (!delete.isAcknowledged()) {
      // Error
    }
  }

  /**
   * Transform a model Entity into an Elasticsearch Entity
   *
   * @param obj
   * @return
   */
  private static Object toESEntity(Object obj, String dataType) {
    if (obj instanceof Item) {
      obj = setAlbums((Item) obj);
      return new ElasticItem((Item) obj,
          getSpace((Item) obj, ElasticTypes.folders.name(), ElasticService.DATA_ALIAS));
    }
    if (obj instanceof CollectionImeji) {
      ElasticFolder ef = new ElasticFolder((CollectionImeji) obj);
      return ef;
    }
    if (obj instanceof Album) {
      return new ElasticAlbum((Album) obj);
    }
    if (obj instanceof Space) {
      return new ElasticSpace((Space) obj);
    }
    if (obj instanceof User) {
      return new ElasticUser((User) obj);
    }
    if (obj instanceof UserGroup) {
      return new ElasticUserGroup((UserGroup) obj);
    }
    return obj;
  }

  /**
   * Set the albums of an item
   *
   * @param item
   * @return
   */
  private static Item setAlbums(Item item) {
    AlbumController c = new AlbumController();
    SearchQuery q = new SearchQuery();
    try {
      q.addPair(new SearchPair(SearchFields.member, SearchOperators.EQUALS, item.getId().toString(),
          false));
    } catch (UnprocessableError e) {
      LOGGER.error("Error searching for albums of item " + item.getIdString(), e);
    }
    item.setAlbums(c.search(q, Imeji.adminUser, null, -1, 0, null).getResults());
    return item;
  }

  /**
   * Get the Id of an Object
   *
   * @param obj
   * @return
   */
  private String getId(Object obj) {
    if (obj instanceof Properties) {
      return ((Properties) obj).getId().toString();
    }
    if (obj instanceof User) {
      return ((User) obj).getId().toString();
    }
    if (obj instanceof UserGroup) {
      return ((UserGroup) obj).getId().toString();
    }
    return null;
  }

  /**
   * Add a mapping to the fields (important to have a better search)
   */
  public void addMapping() {
    try {
      String jsonMapping = new String(
          Files.readAllBytes(
              Paths.get(ElasticIndexer.class.getClassLoader().getResource(mappingFile).toURI())),
          "UTF-8").replace("XXX_ANALYSER_XXX", analyser.name());
      ElasticService.client.admin().indices().preparePutMapping(this.index).setType(dataType)
          .setSource(jsonMapping).execute().actionGet();
    } catch (Exception e) {
      LOGGER.error("Error initializing the Elastic Search Mapping", e);
    }
  }


  /**
   * Retrieve the space of the Item depending of its folder
   *
   * @param item
   * @return
   */
  private static String getSpace(Item item, String dataType, String index) {
    return ElasticSearchUtil.readFieldAsString(item.getCollection().toString(), ElasticFields.SPACE,
        dataType, index);
  }

  /**
   * True if a the space of a collection is different than the space in index
   *
   * @param ef
   * @param dataType
   * @param index
   * @return
   */
  private static boolean isSpaceCollectionChanged(CollectionImeji col, String dataType,
      String index) {
    String indexedValue = ElasticSearchUtil.readFieldAsString(col.getId().toString(),
        ElasticFields.SPACE, ElasticTypes.folders.name(), index);
    String newValue = col.getSpace() != null ? col.getSpace().toString() : "";
    return !indexedValue.equals(newValue);
  }



  /**
   * Reindex all Items of these Folders
   *
   * @param collectionsToReindex
   * @throws URISyntaxException
   * @throws IOException
   * @throws ImejiException
   */
  private void reindexFoldersItems(List<String> collectionsToReindex) {
    if (collectionsToReindex.size() > 0) {
      try {
        for (String collectionR : collectionsToReindex) {
          reindexItemsInContainer(collectionR);
        }
      } catch (Exception e) {
        LOGGER.error("There has been an error during reindexing of Folder Items!", e);
      }
    }
  }

  /**
   * Reindex all {@link Item} stored in the database
   *
   * @throws ImejiException
   * @throws URISyntaxException
   * @throws IOException
   *
   */
  private void reindexItemsInContainer(String containerUri) {
    ElasticIndexer indexer = new ElasticIndexer(index, ElasticTypes.items, analyser);
    ItemController controller = new ItemController();
    try {
      List<Item> items = controller.searchAndRetrieve(new URI(containerUri), (SearchQuery) null,
          null, Imeji.adminUser, null, -1, -1);
      indexer.indexBatch(items);
    } catch (Exception e) {
      LOGGER.error("There has been an error during reindexing of items in a container! ", e);
    }
  }

  /**
   * Find Folders which need to be updated, because they have been added/removed toa space
   *
   * @param collectionsToReindex
   * @param obj
   */
  private void addSpaceForldersToRedindex(List<String> collectionsToReindex, Object obj) {
    if (dataType.equals(ElasticTypes.folders.name())) {
      // reindex items and collections in Space (check first if this has been changed)
      if (isSpaceCollectionChanged((CollectionImeji) obj, dataType, index)) {
        collectionsToReindex.add(getId(obj));
      }
    }
  }

}
