package de.mpg.imeji.logic.search.elasticsearch.factory;

import java.util.ArrayList;
import java.util.List;

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
  private static final SortBuilder defaultSort = SortBuilders.fieldSort(ElasticFields.CREATED.field()).order(SortOrder.ASC);

  /**
   * Build a list of ElasticSearch {@link SortBuilder} from a list of Imeji {@link SortCriterion}
   * 
   * Goal: Multilevel sorting of results. Add the list of {@link SortBuilder} to an ElasticSerach
   * {@link SearchRequestBuilder} Sorting of results is then done descending the passed list, i.e.
   * Results are first be sorted by the first {@link SortCriterion} Elements that fall into the same
   * category are then sorted by the second {@link SortCriterion} and so on
   * 
   * @param sortCriteria list of {@link SortCriterion}
   * @return list of {@link SortBuilder}
   */
  public static List<SortBuilder> build(List<SortCriterion> sortCriteria) {

    ArrayList<SortBuilder> sortBuilderList = new ArrayList<SortBuilder>(sortCriteria.size());
    for (SortCriterion sortCriterion : sortCriteria) {
      if (sortCriterion != null) {
        sortBuilderList.add(build(sortCriterion));
      }
    }
    return sortBuilderList;
  }

  /**
   * Build an ElasticSearch {@link SortBuilder} from an Imeji {@link SortCriterion}
   *
   * @param sort
   * @return
   */
  public static SortBuilder build(SortCriterion sort) {
    if (sort == null) {
      return defaultSort;
      //return null;
    }
    final SearchFields index = sort.getField();
    switch (index) {
      case title:
        return makeBuilder(ElasticFields.NAME.field() + SORT_INDEX, sort, "keyword");
      case created:
        return makeBuilder(ElasticFields.CREATED.field(), sort, "long");
      case modified:
        return makeBuilder(ElasticFields.MODIFIED.field(), sort, "long");
      case filename:
        return makeBuilder(ElasticFields.NAME.field() + SORT_INDEX, sort, "keyword");
      case filetype:
        return makeBuilder(ElasticFields.FILETYPE.field(), sort, "keyword");
      case filesize:
        return makeBuilder(ElasticFields.SIZE.field(), sort, "long");
      case fileextension:
        return makeBuilder(ElasticFields.FILEEXTENSION.field() + SORT_INDEX, sort, "keyword");
      case creatorid:
        return makeBuilder(ElasticFields.CREATORS.field() + SORT_INDEX, sort, "keyword");
      case status:
        return makeBuilder(ElasticFields.STATUS.field(), sort, "keyword");
      case completename:
        return makeBuilder("completename" + SORT_INDEX, sort, "keyword");
      default:
        return defaultSort;
    }
  }

  /**
   * Construct an ElasticSearch {@link SortBuilder}
   *
   * @param field name of the search field
   * @param sortCriterion
   * @return
   */
  // unmappedType in order to prevent shard failures for missing sort fields
  private static SortBuilder makeBuilder(String field, SortCriterion sortCriterion, String unmappedType) {
    return SortBuilders.fieldSort(field).unmappedType(unmappedType).order(getSortOrder(sortCriterion));
  }

  /**
   * Return the {@link SortOrder} of the current sort criterion
   *
   * @param sort
   * @return
   */
  private static SortOrder getSortOrder(SortCriterion sort) {
    return sort.getSortOrder() == de.mpg.imeji.logic.search.model.SortCriterion.SortOrder.ASCENDING ? SortOrder.ASC : SortOrder.DESC;
  }
}
