package de.mpg.imeji.logic.search.elasticsearch.factory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.ImejiConfiguration;
import de.mpg.imeji.logic.config.ImejiFileTypes;
import de.mpg.imeji.logic.model.ImejiLicenses;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.StatementType;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.elasticsearch.ElasticSearch;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.model.SearchCollectionMetadata;

/**
 * Factory class to buid an {@link AbstractAggregationBuilder}
 * 
 * @author saquet
 *
 */
public class ElasticAggregationFactory {

  private final static int BUCKETS_MAX_SIZE = 100;
  private final static org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(ElasticAggregationFactory.class);

  public static Map<String, Aggregation> build(SearchObjectTypes... types) {

    if (SearchObjectTypes.ITEM.equals(types[0])) {
      return buildForItems();
    } else if (SearchObjectTypes.COLLECTION.equals(types[0])) {
      return buildForCollections();
    }

    return null;
  }

  private static Map<String, Aggregation> buildForItems() {


    Aggregation.Builder.ContainerBuilder systemAggregations = new Aggregation.Builder().filters(FiltersAggregation
        .of(fa -> fa.keyed(true).filters(f -> f.keyed(Collections.singletonMap("all", new MatchAllQuery.Builder().build()._toQuery())))));
    Aggregation.Builder.ContainerBuilder metadataAggregations = new Aggregation.Builder().nested(na -> na.path("metadata"));

    Map<String, Aggregation> aggregations = new LinkedHashMap<>();

    //FiltersAggregationBuilder systemAggregations =
    //    AggregationBuilders.filters("system", new FiltersAggregator.KeyedFilter("all", QueryBuilders.matchAllQuery()));
    //NestedAggregationBuilder metadataAggregations = AggregationBuilders.nested("metadata", "metadata");

    List<Facet> facets = new FacetService().retrieveAllFromCache();
    for (Facet facet : facets) {
      if (Facet.OBJECTTYPE_ITEM.equals(facet.getObjectType())) {

        String metadataField = getMetadataField(facet);
        if (metadataField != null) {
          metadataAggregations.aggregations(facet.getIndex(), getMetadataAggregation(facet, metadataField));
        } else if (SearchFields.filetype.name().equals(facet.getIndex())) {
          systemAggregations.aggregations(SearchFields.filetype.name(), getFiletypeAggregation(facet));
        } else if (SearchFields.collection.name().equals(facet.getIndex())) {
          systemAggregations.aggregations(facet.getIndex(), getCollectionAggregation(facet));
        } else if (SearchFields.license.name().equals(facet.getIndex())) {
          systemAggregations.aggregations(SearchFields.license.name(), getLicenseAggregation(facet));
        } else if (SearchFields.valueOfIndex(facet.getIndex()) == SearchFields.collection_author_organisation) {
          systemAggregations.aggregations(facet.getIndex(), getOrganizationsOfCollectionAggregation(facet));
        } else if (SearchFields.valueOfIndex(facet.getIndex()) == SearchFields.collection_author) {
          systemAggregations.aggregations(facet.getIndex(), getAuthorsOfCollectionAggregation(facet));
        } else {
          System.out.println("NOT CREATED AGGREGATION FOR FACET " + facet.getIndex());
        }
      }
    }
    aggregations.put("metadata", metadataAggregations.build());
    aggregations.put("system", systemAggregations.build());

    Aggregation itemAgg = FiltersAggregation
        .of(ag -> ag.filters(f -> f.keyed(
            Collections.singletonMap(Facet.ITEMS, TermQuery.of(t -> t.field(ElasticFields.JOIN_FIELD.field()).value("item"))._toQuery()))))
        ._toAggregation();
    aggregations.put(Facet.ITEMS, itemAgg);

    //aggregations.add(AggregationBuilders.filters(Facet.ITEMS,
    //    new FiltersAggregator.KeyedFilter(Facet.ITEMS, QueryBuilders.termQuery(ElasticFields.JOIN_FIELD.field(), "item"))));
    // new FiltersAggregator.KeyedFilter(Facet.ITEMS,
    // QueryBuilders.typeQuery(ElasticService.ElasticIndices.items.name()))));

    Aggregation subCollAgg = FiltersAggregation.of(ag -> ag.filters(f -> f.keyed(Collections.singletonMap(Facet.SUBCOLLECTIONS,
        TermQuery.of(t -> t.field(ElasticFields.JOIN_FIELD.field()).value("folder"))._toQuery()))))._toAggregation();
    aggregations.put(Facet.SUBCOLLECTIONS, subCollAgg);

    //aggregations.add(AggregationBuilders.filters(Facet.SUBCOLLECTIONS,
    //    new FiltersAggregator.KeyedFilter(Facet.SUBCOLLECTIONS, QueryBuilders.termQuery(ElasticFields.JOIN_FIELD.field(), "folder"))));
    // new FiltersAggregator.KeyedFilter(Facet.SUBCOLLECTIONS,
    // QueryBuilders.typeQuery(ElasticService.ElasticIndices.folders.name()))));

    return aggregations;
  }

  private static Map<String, Aggregation> buildForCollections() {



    Aggregation.Builder.ContainerBuilder systemAggregations =
        new Aggregation.Builder().filters(fa -> fa.keyed(true).filters(f -> f.keyed(Collections.singletonMap("all",
            BoolQuery.of(bq -> bq.mustNot(q -> q.exists(eq -> eq.field(ElasticFields.FOLDER.field()))))._toQuery()))));
    Aggregation.Builder.ContainerBuilder metadataAggregations = new Aggregation.Builder().nested(na -> na.path("info"));

    Map<String, Aggregation> aggregations = new LinkedHashMap<>();
    //NestedAggregationBuilder metadataAggregations = AggregationBuilders.nested("metadata", "info");

    //use only subcollections

    //FiltersAggregationBuilder systemAggregations = AggregationBuilders.filters("system", new FiltersAggregator.KeyedFilter("all",
    //    QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(ElasticFields.FOLDER.field()))));
    /*
    FiltersAggregationBuilder systemAggregations =
        AggregationBuilders.filters("system", new FiltersAggregator.KeyedFilter("all", QueryBuilders.matchAllQuery()));
    
    aggregations.add(systemAggregations);
    aggregations.add(metadataAggregations);
    */


    List<Facet> facets = new FacetService().retrieveAllFromCache();
    for (Facet facet : facets) {
      if (Facet.OBJECTTYPE_COLLECTION.equals(facet.getObjectType())) {
        if (SearchFields.author_organization_exact.getIndex().equals(facet.getIndex())) {
          //AggregationBuilders.terms(facet.getIndex()).size(BUCKETS_MAX_SIZE).field(ElasticFields.AUTHOR_ORGANIZATION_EXACT.field()));
          systemAggregations.aggregations(facet.getIndex(),
              TermsAggregation.of(ta -> ta.size(BUCKETS_MAX_SIZE).field(ElasticFields.AUTHOR_ORGANIZATION_EXACT.field()))._toAggregation());

        } else if (SearchFields.author_completename_exact.getIndex().equals(facet.getIndex())) {
          //AggregationBuilders.terms(facet.getIndex()).size(BUCKETS_MAX_SIZE).field(ElasticFields.AUTHOR_COMPLETENAME_EXACT.field()));
          systemAggregations.aggregations(facet.getIndex(),
              TermsAggregation.of(ta -> ta.size(BUCKETS_MAX_SIZE).field(ElasticFields.AUTHOR_COMPLETENAME_EXACT.field()))._toAggregation());


        } else if (SearchFields.collection_type.getIndex().equals(facet.getIndex())) {
          //AggregationBuilders.terms(facet.getIndex()).size(BUCKETS_MAX_SIZE).field(ElasticFields.COLLECTION_TYPE.field()));
          systemAggregations.aggregations(facet.getIndex(),
              TermsAggregation.of(ta -> ta.size(BUCKETS_MAX_SIZE).field(ElasticFields.COLLECTION_TYPE.field()))._toAggregation());

        } else if (facet.getIndex().startsWith("collection.md")) {
          ElasticFields collectionMdSearchField;
          //For collection metadata with label "Keywords", use the splitted field which stores the comma separated values of the keywords field
          if (ImejiConfiguration.COLLECTION_METADATA_KEYWORDS_LABEL.equals(SearchCollectionMetadata.indexToLabel(facet.getIndex()))) {
            collectionMdSearchField = ElasticFields.INFO_TEXT_SPLITTED;
          } else {
            collectionMdSearchField = ElasticFields.INFO_TEXT_EXACT;
          }

          metadataAggregations.aggregations(facet.getIndex(),
              Aggregation.of(agg -> agg.filter(TermQuery
                  .of(tq -> tq.field(ElasticFields.INFO_LABEL_EXACT.field()).value(SearchCollectionMetadata.indexToLabel(facet.getIndex())))
                  ._toQuery()).aggregations(facet.getName(),
                      Aggregation.of(agg2 -> agg2.terms(ta -> ta.field(collectionMdSearchField.field()).size(BUCKETS_MAX_SIZE))))));


          /*
          FilterAggregationBuilder fb = AggregationBuilders.filter(facet.getIndex(),
              QueryBuilders.termQuery(ElasticFields.INFO_LABEL_EXACT.field(), SearchCollectionMetadata.indexToLabel(facet.getIndex())));
          
          fb.subAggregations(AggregationBuilders.terms(facet.getName()).field(collectionMdSearchField.field()).size(BUCKETS_MAX_SIZE));
          metadataAggregations.aggregations(fb);
          
           */
        }

      }

    }

    aggregations.put("metadata", metadataAggregations.build());
    aggregations.put("system", systemAggregations.build());

    return aggregations;

  }

  /**
   * Create the aggregation for the license
   * 
   * @param facet
   * @return
   */
  private static Aggregation getLicenseAggregation(Facet facet) {
    List<String> licenses = Stream.of(ImejiLicenses.values()).map(l -> l.name()).collect(Collectors.toList());
    return Aggregation.of(ag -> ag.terms(at -> at.field(ElasticFields.LICENSE.field()).include(in -> in.terms(licenses))));


    /*
    licenses.add(ImejiLicenses.NO_LICENSE);
    IncludeExclude inex = new IncludeExclude(licenses.toArray(new String[0]), null);
    return AggregationBuilders.terms(SearchFields.license.name()).field(ElasticFields.LICENSE.field()).includeExclude(inex);
    
     */
  }

  /**
   * Create the aggregation for filetype
   * 
   * @param facet
   * @return
   */
  private static Aggregation getFiletypeAggregation(Facet facet) {

    Map<String, Query> keyedFilters = new LinkedHashMap<>();
    for (ImejiFileTypes.Type type : Imeji.CONFIG.getFileTypes().getTypes()) {
      BoolQuery filetypeQuery = BoolQuery.of(bq -> bq.should(Arrays.stream(type.getExtensionArray())
          .map(ext -> QueryStringQuery.of(qs -> qs.query(ElasticFields.NAME.field() + ".suggest:" + "*." + ext))._toQuery())
          .collect(Collectors.toList())));

      keyedFilters.put(type.getName(null), filetypeQuery._toQuery());
    }

    return Aggregation.of(ag -> ag.filters(fa -> fa.filters(fil -> fil.keyed(keyedFilters))));


    /*
    FiltersAggregationBuilder filetypeAggregation = null;
    List<KeyedFilter> filterList = new ArrayList<>();
    for (ImejiFileTypes.Type type : Imeji.CONFIG.getFileTypes().getTypes()) {
      BoolQueryBuilder filetypeQuery = QueryBuilders.boolQuery();
      for (String ext : type.getExtensionArray()) {
        filetypeQuery.should(QueryBuilders.queryStringQuery(ElasticFields.NAME.field() + ".suggest:" + "*." + ext));
      }
      KeyedFilter kf = new KeyedFilter(type.getName(null), filetypeQuery);
      filterList.add(kf);
    }
    filetypeAggregation = AggregationBuilders.filters(SearchFields.filetype.name(), filterList.toArray(new KeyedFilter[filterList.size()]));
    return filetypeAggregation;
    
     */
  }

  /**
   * Create the aggregation for the collection
   * 
   * @param facet
   * @return
   */
  private static Aggregation getCollectionAggregation(Facet facet) {

    return Aggregation.of(ag -> ag.terms(at -> at.field(ElasticFields.TITLE_WITH_ID_OF_COLLECTION.field()).size(BUCKETS_MAX_SIZE)));
    //return AggregationBuilders.terms(facet.getIndex()).size(BUCKETS_MAX_SIZE).field(ElasticFields.TITLE_WITH_ID_OF_COLLECTION.field());
  }

  private static Aggregation getAuthorsOfCollectionAggregation(Facet facet) {

    return Aggregation.of(ag -> ag.terms(at -> at.field(ElasticFields.AUTHORS_OF_COLLECTION.field()).size(BUCKETS_MAX_SIZE)));

    //return AggregationBuilders.terms(facet.getIndex()).size(BUCKETS_MAX_SIZE).field(ElasticFields.AUTHORS_OF_COLLECTION.field());
  }

  private static Aggregation getOrganizationsOfCollectionAggregation(Facet facet) {

    return Aggregation.of(ag -> ag.terms(at -> at.field(ElasticFields.ORGANIZATION_OF_COLLECTION.field()).size(BUCKETS_MAX_SIZE)));

    //return AggregationBuilders.terms(facet.getIndex()).size(BUCKETS_MAX_SIZE).field(ElasticFields.ORGANIZATION_OF_COLLECTION.field());
  }

  /**
   * Return the aggregation for metadata
   * 
   * @param facet
   * @param metadataField
   * @return
   */
  private static Aggregation getMetadataAggregation(Facet facet, String metadataField) {
    switch (StatementType.valueOf(facet.getType())) {
      case TEXT:
        return getMetadataTextAggregation(facet);
      case DATE:
        return getMetadataNumberAggregation(facet, metadataField);
      // return getMetadataDateAggregation(facet, metadataField);
      case NUMBER:
        return getMetadataNumberAggregation(facet, metadataField);
      case URL:
        return getMetadataTextAggregation(facet);
      case PERSON:
        return getMetadataTextAggregation(facet);
      default:
        return null;
    }
  }

  /**
   * Create aggregation for Date
   * 
   * @param facet
   * @param metadataField
   * @return
   */
  private static Aggregation getMetadataNumberAggregation(Facet facet, String metadataField) {
    return Aggregation
        .of(agg -> agg.filter(TermQuery.of(tq -> tq.field("metadata.index").value(getMetadataStatementIndex(facet.getIndex())))._toQuery())
            .aggregations(facet.getIndex(), Aggregation.of(subAgg -> subAgg.stats(stats -> stats.field(getMetadataField(facet)))))

        );

    /*
    FilterAggregationBuilder fb = AggregationBuilders.filter(facet.getIndex(),
        QueryBuilders.termQuery("metadata.index", getMetadataStatementIndex(facet.getIndex())));
    fb.subAggregation(AggregationBuilders.stats(facet.getIndex()).field(getMetadataField(facet)));
    return fb;
    */

  }

  /**
   * Create aggregation for Date
   * 
   * @param facet
   * @param metadataField
   * @return
   */
  private static Aggregation getMetadataDateAggregation(Facet facet, String metadataField) {

    return Aggregation.of(agg -> agg
        .filter(TermQuery.of(tq -> tq.field("metadata.index").value(getMetadataStatementIndex(facet.getIndex())))._toQuery())
        .aggregations(facet.getIndex(), Aggregation.of(
            subAgg -> subAgg.dateHistogram(dh -> dh.field(getMetadataField(facet)).calendarInterval(CalendarInterval.Year).format("yyyy"))))

    );
    /*
    FilterAggregationBuilder fb = AggregationBuilders.filter(facet.getIndex(),
        QueryBuilders.termQuery("metadata.index", getMetadataStatementIndex(facet.getIndex())));
    fb.subAggregation(AggregationBuilders.dateHistogram(facet.getIndex()).field(getMetadataField(facet))
        .dateHistogramInterval(DateHistogramInterval.YEAR).format("yyyy"));
    
    return fb;
    
     */
  }

  /**
   * Create Aggregation for metadata of type TEXT
   * 
   * @param facet
   * @param metadataField
   * @return
   */
  private static Aggregation getMetadataTextAggregation(Facet facet) {


    return Aggregation
        .of(agg -> agg.filter(TermQuery.of(tq -> tq.field("metadata.index").value(getMetadataStatementIndex(facet.getIndex())))._toQuery())
            .aggregations(facet.getName(),
                Aggregation.of(subAgg -> subAgg.terms(terms -> terms.field(getMetadataField(facet)).size(BUCKETS_MAX_SIZE))))

        );

    /*
    FilterAggregationBuilder fb = AggregationBuilders.filter(facet.getIndex(),
        QueryBuilders.termQuery("metadata.index", getMetadataStatementIndex(facet.getIndex())));
    fb.subAggregation(AggregationBuilders.terms(facet.getName()).field(getMetadataField(facet)).size(BUCKETS_MAX_SIZE));
    return fb;
    
     */
  }

  /**
   * Extract from the search index (for ex: md.title.text) the statement index and return it
   * 
   * @param searchIndex
   * @return
   */
  public static String getMetadataStatementIndex(String searchIndex) {
    return searchIndex.startsWith("md.") ? searchIndex.split("\\.")[1] : searchIndex;
  }

  /**
   * Return the field in elasticsearch
   * 
   * @param searchIndex
   * @return
   */
  public static String getMetadataField(Facet f) {
    if (!f.getIndex().startsWith("md.")) {
      return null;
    }
    switch (StatementType.valueOf(f.getType())) {
      case TEXT:
        return "metadata.text.exact";
      case DATE:
        return "metadata.date";
      case NUMBER:
        return "metadata.number";
      case URL:
        return "metadata.text.exact";
      case PERSON:
        return "metadata.text.exact";
      default:
        return null;
    }
  }
}
