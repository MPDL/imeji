package de.mpg.imeji.logic.controller.business;

import java.util.List;

import de.mpg.imeji.logic.controller.ImejiController;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.jenasearch.JenaSearch;

/**
 * Controller for all actions which are related to statistics
 *
 * @author saquet
 *
 */
public class StatisticsBusinessController extends ImejiController {

  /**
   * Return the all institute names (define by the suffix of emails users)
   *
   * @return
   */
  public List<String> getAllInstitute() {
    Search s = new JenaSearch(SearchObjectTypes.USER, null);
    return s.searchString(JenaCustomQueries.selectAllInstitutes(), null, null, 0, -1).getResults();
  }

  /**
   * Return the size of the storage used by one institute (i.e. the size of all files in collections
   * belonging the institut)
   *
   * @param instituteName
   * @return
   */
  public long getUsedStorageSizeForInstitute(String instituteName) {
    Search s = new JenaSearch(SearchObjectTypes.ALL, null);
    List<String> result =
        s.searchString(JenaCustomQueries.selectInstituteFileSize(instituteName), null, null, 0, -1)
            .getResults();
    if (result.size() == 1 && result.get(0) != null) {
      String size = result.get(0).replace("^^http://www.w3.org/2001/XMLSchema#integer", "");
      return Long.parseLong(size);
    }
    return 0;
  }
}
