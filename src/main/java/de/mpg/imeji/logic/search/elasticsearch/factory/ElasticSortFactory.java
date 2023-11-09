package de.mpg.imeji.logic.search.elasticsearch.factory;

import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.model.SortCriterion;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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
  //private static final SortBuilder defaultSort = SortBuilders.fieldSort(ElasticFields.CREATED.field()).order(SortOrder.ASC);
  private static final SortOptions defaultSort =
      SortOptions.of(so -> so.field(fs -> fs.field(ElasticFields.CREATED.field()).order(SortOrder.Asc)));

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
  public static List<SortOptions> build(List<SortCriterion> sortCriteria) {


    ArrayList<SortOptions> sortBuilderList = new ArrayList<SortOptions>(sortCriteria.size());
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
  public static SortOptions build(SortCriterion sort) {
    if (sort == null) {
      return defaultSort;
      //return null;
    }
    final SearchFields index = sort.getField();
    switch (index) {
      case title:
        return makeBuilder(ElasticFields.NAME.field() + SORT_INDEX, sort, FieldType.Keyword);
      case created:
        return makeBuilder(ElasticFields.CREATED.field(), sort, FieldType.Long);
      case modified:
        return makeBuilder(ElasticFields.MODIFIED.field(), sort, FieldType.Long);
      case filename:
        return makeBuilder(ElasticFields.NAME.field() + SORT_INDEX, sort, FieldType.Keyword);
      case filetype:
        return makeBuilder(ElasticFields.FILETYPE.field(), sort, FieldType.Keyword);
      case filesize:
        return makeBuilder(ElasticFields.SIZE.field(), sort, FieldType.Long);
      case fileextension:
        return makeBuilder(ElasticFields.FILEEXTENSION.field() + SORT_INDEX, sort, FieldType.Keyword);
      case creatorid:
        return makeBuilder(ElasticFields.CREATORS.field() + SORT_INDEX, sort, FieldType.Keyword);
      case status:
        return makeBuilder(ElasticFields.STATUS.field(), sort, FieldType.Keyword);
      case completename:
        return makeBuilder("completename" + SORT_INDEX, sort, FieldType.Keyword);
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
  private static SortOptions makeBuilder(String field, SortCriterion sortCriterion, FieldType unmappedType) {
    return SortOptions.of(so -> so.field(fs -> fs.field(field).unmappedType(unmappedType).order(getSortOrder(sortCriterion))));

  }

  /**
   * Return the {@link SortOrder} of the current sort criterion
   *
   * @param sort
   * @return
   */
  private static SortOrder getSortOrder(SortCriterion sort) {
    return sort.getSortOrder() == de.mpg.imeji.logic.search.model.SortCriterion.SortOrder.ASCENDING ? SortOrder.Asc : SortOrder.Desc;
  }
}
