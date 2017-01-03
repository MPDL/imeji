package de.mpg.imeji.logic.jobs;

import java.util.concurrent.Callable;

/**
 * Update all uris with the new Domain. This method should be used when the domain
 *
 * @author bastiens
 *
 */
public class UpdateUrisDomainJob implements Callable<Integer> {
  private final String domain;

  public UpdateUrisDomainJob(String domain) {
    this.domain = domain;
  }

  @Override
  public Integer call() throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

}
