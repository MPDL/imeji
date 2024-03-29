package de.mpg.imeji.logic.search.jenasearch;

import java.util.List;

import de.mpg.imeji.logic.search.SearchIndexer;

/**
 * {@link SearchIndexer} for {@link JenaSearch}
 *
 * @author bastiens
 *
 */
public class JenaIndexer implements SearchIndexer {


  @Override
  public void indexBatch(List<?> l) {
    // No indexation needed, since search is done directly on jena Database with
    // sparql queries
  }

  @Override
  public void deleteBatch(List<?> l) {
    // No indexation needed, since search is done directly on jena Database with
    // sparql queries
  }


  @Override
  public void updateIndexBatch(List<?> l) {
    // No indexation needed, since search is done directly on jena Database with
    // sparql queries
  }

}
