package de.mpg.imeji.logic.messaging.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.reflections.Reflections;

import de.mpg.imeji.logic.messaging.MessageService;

/**
 * Service managing {@link Aggregation}
 * 
 * @author saquet
 *
 */
public class AggregationService {
  private static Logger LOGGER = Logger.getLogger(AggregationService.class);

  /**
   * Run all aggregations defined in imeji and then remove all messages
   */
  public void runAllAggregations() {
    long start = System.currentTimeMillis();
    findAllAggregations().stream().forEach(a -> a.aggregate());
    new MessageService().deleteOldMessages(start);
  }


  /**
   * Find all aggregation defined in imeji
   * 
   * @return
   */
  private List<Aggregation> findAllAggregations() {
    Reflections reflections = new Reflections("de.mpg.imeji");
    Set<Class<? extends Aggregation>> aggregationClasses =
        reflections.getSubTypesOf(Aggregation.class);
    List<Aggregation> l = new ArrayList<>();
    for (Class<? extends Aggregation> c : aggregationClasses) {
      try {
        l.add(c.newInstance());
      } catch (InstantiationException | IllegalAccessException e) {
        LOGGER.error("Error instancing " + c, e);
      }
    }
    return l;
  }

}
