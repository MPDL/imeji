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
	public void index(Object obj) {
		// No indexation needed, since search is done directly on jena Database with
		// sparql queries
	}

	@Override
	public void indexBatch(List<?> l) {
		// No indexation needed, since search is done directly on jena Database with
		// sparql queries
	}

	@Override
	public void delete(Object obj) {
		// No indexation needed, since search is done directly on jena Database with
		// sparql queries
	}

	@Override
	public void deleteBatch(List<?> l) {
		// No indexation needed, since search is done directly on jena Database with
		// sparql queries
	}

	@Override
	public void updatePartial(String id, Object obj) {
		// No indexation needed, since search is done directly on jena Database with
		// sparql queries
	}

	@Override
	public void updateIndexBatch(List<?> l) {
		// No indexation needed, since search is done directly on jena Database with
		// sparql queries
	}

	@Override
	public void index(String name, Object obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void indexBatch(String name, List<?> l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateIndexBatch(String name, List<?> l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(String name, Object obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteBatch(String name, List<?> l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updatePartial(String name, String id, Object obj) {
		// TODO Auto-generated method stub

	}

}
