/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.export.format.Export;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.util.ObjectHelper;

/**
 * Manage {@link Export}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ExportManager {
  private OutputStream out;
  private Export export;
  private User user;
  private List<String> selectedItemsToExport = new ArrayList<String>();

  /**
   * Create a new {@link ExportManager} with url parameters, and perform the {@link Export} in the
   * specified {@link OutputStream}
   *
   * @param out
   * @param user
   * @param params
   * @throws HttpResponseException
   */
  public ExportManager(OutputStream out, User user, Map<String, String[]> params,
      List<String> selectedItems) throws HttpResponseException {
    this.out = out;
    this.user = user;
    export = Export.factory(params);
    export.setUser(user);
    this.selectedItemsToExport = selectedItems;
  }

  /**
   * Write in {@link OutputStream} the export
   *
   * @param sr
   *
   */
  public void export(SearchResult sr, User user) {
    if (export != null) {
      export.export(out, sr, user);
    } else {
      try {
        out.write("Unknown format".getBytes());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Search the element to export
   *
   * @param searchQuery
   * @return
   * @throws IOException
   * @throws ImejiException
   */
  public SearchResult search() throws IOException, ImejiException {
    String collectionId = export.getParam("col");
    String albumId = export.getParam("album");
    String spaceId = export.getParam("space");
    String query = export.getParam("q");
    export.getParam("id");
    String searchType = export.getParam("type");
    int maximumNumberOfRecords = 100;
    SearchQuery searchQuery = SearchQueryParser.parseStringQuery(query);
    if (export.getParam("n") != null) {
      maximumNumberOfRecords = Integer.parseInt(export.getParam("n"));
    }
    SearchResult result = null;
    if (!selectedItemsToExport.isEmpty()) {
      ItemController itemController = new ItemController();
      List<Item> itemResult =
          (List<Item>) itemController.retrieveBatch(selectedItemsToExport, 500, 0, user);
      List<String> sr = new ArrayList<String>();
      for (Item it : itemResult) {
        sr.add(it.getId().toString());
      }
      result = new SearchResult(sr, null);
    } else {
      if ("collection".equals(searchType) || "metadata".equals(searchType)) {
        CollectionController collectionController = new CollectionController();
        result = collectionController.search(searchQuery, null, maximumNumberOfRecords, 0, user,
            spaceId);
      } else if ("album".equals(searchType)) {
        AlbumController albumController = new AlbumController();
        result =
            albumController.search(searchQuery, user, null, maximumNumberOfRecords, 0, spaceId);
      } else if ("profile".equals(searchType)) {
        ProfileController pc = new ProfileController();
        result = pc.search(searchQuery, user, spaceId);
      } else if ("image".equals(searchType)) {
        ItemController itemController = new ItemController();
        if (collectionId != null) {
          result = itemController.search(ObjectHelper.getURI(CollectionImeji.class, collectionId),
              searchQuery, null, user, spaceId, -1, 0);
        } else if (albumId != null) {
          result = itemController.search(ObjectHelper.getURI(Album.class, albumId), searchQuery,
              null, user, spaceId, -1, 0);
        } else {
          result = itemController.search(null, searchQuery, null, user, spaceId, -1, 0);
        }
      }
    }
    if (result != null && result.getNumberOfRecords() > 0
        && result.getNumberOfRecords() > maximumNumberOfRecords) {
      result.setResults(result.getResults().subList(0, maximumNumberOfRecords));
    }
    return result;
  }

  /**
   * Return the content type of the {@link HttpResponse}
   *
   * @return
   */
  public String getContentType() {
    return export.getContentType();
  }

  public Export getExport() {
    return export;
  }

}
