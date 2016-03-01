package de.mpg.imeji.logic.jobs;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.ProfileController;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.jenasearch.JenaSearch;
import de.mpg.imeji.logic.vo.MetadataProfile;

/**
 * Clean empty {@link MetadataProfile}, which are not referenced by any collection
 * 
 * @author saquet
 *
 */
public class CleanEmptyMetadataProfileJob implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(CleanEmptyMetadataProfileJob.class);

  @Override
  public Integer call() throws Exception {
    LOGGER.info("Cleaning not referenced empty profiles...");
    Search s = new JenaSearch(SearchObjectTypes.ALL, null);
    List<String> r = s.searchString(JenaCustomQueries.selectUnusedMetadataProfiles(), null,
        Imeji.adminUser, 0, -1).getResults();
    ProfileController pc = new ProfileController();
    for (String uri : r) {
      MetadataProfile mdp = pc.retrieve(URI.create(uri), Imeji.adminUser);
      if (mdp.getStatements().isEmpty()) {
        pc.delete(mdp, Imeji.adminUser);
      }
    }
    LOGGER.info("...done!");
    return 1;
  }
}
