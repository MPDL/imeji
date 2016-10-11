package de.mpg.imeji.logic.search.elasticsearch;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.Client;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.controller.resource.ContentController;
import de.mpg.imeji.logic.search.SearchIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticAnalysers;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.elasticsearch.factory.util.ElasticSearchFactoryUtil;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticAlbum;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFolder;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticItem;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticSpace;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticUser;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticUserGroup;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.ContentVO;
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
  private final String index;
  private final String dataType;
  private final ElasticAnalysers analyser;
  private String mappingFile = "elasticsearch/Elastic_TYPE_Mapping.json";
  // private final Client client;

  public ElasticIndexer(String indexName, ElasticTypes dataType, ElasticAnalysers analyser) {
    this.index = indexName;
    this.dataType = dataType.name();
    this.analyser = analyser;
    this.mappingFile = mappingFile.replace("_TYPE_", StringUtils.capitalize(this.dataType));
  }


  @Override
  public void index(Object obj) {
    try {
      indexJSON(getId(obj), toJson(obj, dataType, index));
    } catch (Exception e) {
      LOGGER.error("Error indexing object ", e);
    }
  }


  @Override
  public void indexBatch(List<?> l) {
    try {
      for (Object obj : l) {
        indexJSON(getId(obj), toJson(obj, dataType, index));
      }
      commit();
    } catch (Exception e) {
      LOGGER.error("error indexing object ", e);
    }
  }


  @Override
  public void delete(Object obj) {
    String id = getId(obj);
    if (id != null) {
      ElasticService.getClient().prepareDelete(index, dataType, id).execute().actionGet();
      commit();
    }
  }

  @Override
  public void deleteBatch(List<?> l) {
    for (Object obj : l) {
      String id = getId(obj);
      if (id != null) {
        ElasticService.getClient().prepareDelete(index, dataType, id).execute().actionGet();
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
  public static String toJson(Object obj, String dataType, String index) throws UnprocessableError {
    try {
      return mapper.setSerializationInclusion(Include.NON_NULL)
          .writeValueAsString(toESEntity(obj, dataType, index));
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
      ElasticService.getClient().prepareIndex(index, dataType).setId(id).setSource(json).execute()
          .actionGet();
    }
  }

  /**
   * Make all changes done searchable. Kind of a commit. Might be important if data needs to be
   * immediately available for other tasks
   */
  public void commit() {
    ElasticService.getClient().admin().indices().prepareRefresh(index).execute().actionGet();
  }

  /**
   * Remove all indexed data
   */
  public static void clear(Client client, String index) {
    DeleteIndexResponse delete =
        client.admin().indices().delete(new DeleteIndexRequest(index)).actionGet();
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
  private static Object toESEntity(Object obj, String dataType, String index) {
    if (obj instanceof Item) {
      return new ElasticItem((Item) obj, getSpace((Item) obj, ElasticTypes.folders.name(), index),
          getContentVO((Item) obj));
    }
    if (obj instanceof CollectionImeji) {
      return new ElasticFolder((CollectionImeji) obj);
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
    if (obj instanceof ContentVO) {
      return new ElasticItem((ContentVO) obj);
    }
    return obj;
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
      ElasticService.getClient().admin().indices().preparePutMapping(this.index).setType(dataType)
          .setSource(jsonMapping).execute().actionGet();
    } catch (Exception e) {
      LOGGER.error("Error initializing the Elastic Search Mapping " + mappingFile, e);
    }
  }


  /**
   * Retrieve the space of the Item depending of its folder
   *
   * @param item
   * @return
   */
  private static String getSpace(Item item, String dataType, String index) {
    return ElasticSearchFactoryUtil.readFieldAsString(item.getCollection().toString(),
        ElasticFields.SPACE, dataType, index);
  }


  /**
   * Return the content of an item
   * 
   * @param item
   * @return
   */
  private static ContentVO getContentVO(Item item) {
    try {
      return new ContentController().read(item.getContentId());
    } catch (ImejiException e) {
      return new ContentVO();
    }
  }


  @Override
  public void updatePartial(String id, Object obj) {
    if (id != null) {
      try {
        ElasticService.getClient().prepareUpdate(index, dataType, id)
            .setDoc(toJson(obj, dataType, index)).execute().actionGet();
      } catch (UnprocessableError e) {
        LOGGER.error("Error index partial update ", e);
      }

    }
  }
}
