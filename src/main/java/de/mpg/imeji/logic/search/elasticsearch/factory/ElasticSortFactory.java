package de.mpg.imeji.logic.search.elasticsearch.factory;

import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.model.SortCriterion;

/**
 * Factory for {@link SortBuilder}
 *
 * @author bastiens
 *
 */
public class ElasticSortFactory {
  /**
   * Prefix used to index the value used by the sorting. See
   * https://www.elastic.co/guide/en/elasticsearch/guide/current/multi-fields.html
   */
  private static final String SORT_INDEX = ".sort";

  /**
   * Default Sorting value
   */
  private static final SortBuilder defaultSort =
      SortBuilders.fieldSort(ElasticFields.CREATED.field()).order(SortOrder.ASC);

  /**
   * Build a {@link SortBuilder} from a {@link SortCriterion}
   *
   * @param sort
   * @return
   */
  public static SortBuilder build(SortCriterion sort) {
    if (sort == null) {
      return defaultSort;
    }
    final SearchFields index = sort.getField();
    switch (index) {
      case title:
        return makeBuilder(ElasticFields.NAME.field() + SORT_INDEX, sort);
      case modified:
        return makeBuilder(ElasticFields.MODIFIED.field(), sort);
      case filename:
        return makeBuilder(ElasticFields.NAME.field() + SORT_INDEX, sort);
      case filetype:
        return makeBuilder(ElasticFields.FILETYPE.field(), sort);
      case filesize:
        return makeBuilder(ElasticFields.SIZE.field(), sort);
      case creatorid:
        return makeBuilder(ElasticFields.CREATORS.field() + SORT_INDEX, sort);
      case status:
        return makeBuilder(ElasticFields.STATUS.field(), sort);
      default:
        return defaultSort;
    }
  }

  /**
   * Construct a Sortbuilder
   *
   * @param field
   * @param sortCriterion
   * @return
   */
  private static SortBuilder makeBuilder(String field, SortCriterion sortCriterion) {
    return SortBuilders.fieldSort(field).order(getSortOrder(sortCriterion));
  }

  /**
   * Return the {@link SortOrder} of the current sort criterion
   *
   * @param sort
   * @return
   */
  private static SortOrder getSortOrder(SortCriterion sort) {
    return sort.getSortOrder() == de.mpg.imeji.logic.search.model.SortCriterion.SortOrder.ASCENDING
        ? SortOrder.ASC : SortOrder.DESC;
  }
}
