package de.mpg.imeji.logic.statistic;

import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.db.repositories.UserDbRepository;
import de.mpg.imeji.logic.export.ZIPExport;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.jenasearch.JenaSearch;
import de.mpg.imeji.logic.security.user.util.QuotaUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Controller for all actions which are related to statistics
 *
 * @author saquet
 *
 */
public class StatisticsService {

  private static final Logger LOGGER = LogManager.getLogger(StatisticsService.class);

  /**
   * Return the all institute names (define by the suffix of emails users)
   *
   * @return
   */
  public List<String> getAllInstitute() {
      try {
          return  new UserDbRepository().retrieveAllDomains();
      } catch (ImejiException e) {
          throw new RuntimeException(e);
      }
/*
      final Search s = new JenaSearch(SearchObjectTypes.USER, null);
    return s.searchString(JenaCustomQueries.selectAllInstitutes(), null, null, Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS)
        .getResults();

 */
  }

  /**
   * Return the size of the storage used by one institute (i.e. the size of all files in collections
   * belonging the institut)
   *
   * @param instituteName
   * @return
   */
  public long getUsedStorageSizeForInstitute(String instituteName) {
    try {
      return  new UserDbRepository().getFileSizeForDomain(instituteName);
    } catch (ImejiException e) {
      throw new RuntimeException(e);
    }
    /*

    final Search s = new JenaSearch(SearchObjectTypes.ALL, null);
    final List<String> result = s.searchString(JenaCustomQueries.selectInstituteFileSize(instituteName), null, null,
        Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS).getResults();
    LOGGER.info(result);
    if (result.size() == 1 && result.get(0) != null) {
      final String size = result.get(0).replace("^^xsd:integer", "").replace("\"", "");
      return Long.parseLong(size);
    }
    return 0;

     */
  }

  public long getAllFileSize() {

    try {
      return  new UserDbRepository().getFileSizeForAll();
    } catch (ImejiException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return the total file size used by the user
   * 
   * @param user
   * @return
   */
  public long getUsedStorageForUser(User user) {
    return QuotaUtil.getUsedQuota(user);
  }
}
