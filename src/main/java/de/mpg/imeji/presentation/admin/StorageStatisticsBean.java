package de.mpg.imeji.presentation.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.bean.ManagedBean;

import de.mpg.imeji.logic.controller.business.StatisticsBusinessController;

@ManagedBean
public class StorageStatisticsBean {

  private final List<Institute> institutes = new ArrayList<>();

  public StorageStatisticsBean() {
    final StatisticsBusinessController controller = new StatisticsBusinessController();
    for (final String institute : controller.getAllInstitute()) {
      institutes
          .add(new Institute(institute, controller.getUsedStorageSizeForInstitute(institute)));
    }
  }

  public ArrayList<Institute> getInstitutes() {
    Collections.sort(institutes, new Comparator<Institute>() {
      @Override
      public int compare(Institute institute1, Institute institute2) {
        if ((institute2.getStorage() - institute1.getStorage()) >= 0) {
          return 1;
        } else {
          return -1;
        }
      }
    });
    return (ArrayList<Institute>) institutes;

  }

}
