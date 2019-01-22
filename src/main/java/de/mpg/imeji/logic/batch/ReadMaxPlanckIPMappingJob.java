package de.mpg.imeji.logic.batch;

import java.util.concurrent.Callable;

import de.mpg.imeji.logic.util.MaxPlanckInstitutUtils;

/**
 * Read the IP Mapping for the Max Planck Planck Institutes
 *
 * @author bastiens
 *
 */
public class ReadMaxPlanckIPMappingJob implements Callable<Integer> {

  @Override
  public Integer call() throws Exception {
    MaxPlanckInstitutUtils.initMPINameMap();
    MaxPlanckInstitutUtils.initIdMap();
    return 1;
  }

}
