package de.mpg.imeji.logic.workflow.status.impl;

import java.util.List;

import de.mpg.imeji.logic.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.vo.Properties;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.workflow.status.StatusUtil;
import de.mpg.imeji.logic.workflow.status.VersionReader;

/**
 * {@link VersionReader} implementation for Jena Backend
 * 
 * @author bastiens
 *
 */
public class JenaVersionReader implements VersionReader {

  @Override
  public Status getStatus(Properties p) {
    List<String> statusString =
        ImejiSPARQL.exec(JenaCustomQueries.selectStatus(p.getId().toString()), null);
    if (statusString.size() == 1) {
      return StatusUtil.parseStatus(statusString.get(0));
    }
    return null;
  }

  @Override
  public int getVersion(Properties p) {
    List<String> statusString =
        ImejiSPARQL.exec(JenaCustomQueries.selectVersion(p.getId().toString()), null);
    if (statusString.size() == 1) {
      return Integer.parseInt(statusString.get(0));
    }
    return 0;
  }

}
