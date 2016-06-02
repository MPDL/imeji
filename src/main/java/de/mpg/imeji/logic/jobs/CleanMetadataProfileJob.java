package de.mpg.imeji.logic.jobs;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.jenasearch.JenaSearch;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Properties.Status;

/**
 * Clean unused {@link MetadataProfile}
 *
 * @author saquet
 *
 */
public class CleanMetadataProfileJob implements Callable<Integer> {

  private boolean delete = false;
  private List<MetadataProfile> profiles = new ArrayList<MetadataProfile>();

  public CleanMetadataProfileJob(boolean delete) {
    this.delete = delete;
  }

  @Override
  public Integer call() throws Exception {
    Search s = new JenaSearch(SearchObjectTypes.ALL, null);
    List<String> r =
        s.searchString(JenaCustomQueries.selectUnusedMetadataProfiles(), null, null, 0, -1)
            .getResults();
    ProfileController pc = new ProfileController();
    for (String uri : r) {
      profiles.add(pc.retrieve(URI.create(uri), Imeji.adminUser));
    }
    if (delete) {
      for (MetadataProfile mdp : profiles) {
        //NB.02.06. A Hack to allow purging released metadata profiles which are not used, 
        //Default metadata profile will never be deleted (Profile controller adds exception) 
        mdp.setStatus(Status.PENDING);
        pc.delete(mdp, Imeji.adminUser);
      }
    }
    return 1;
  }

  public List<MetadataProfile> getProfiles() {
    return profiles;
  }

}
