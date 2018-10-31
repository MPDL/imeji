package de.mpg.imeji.logic.search.facet.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.reader.ReaderFacade;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.generic.ImejiControllerAbstract;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.facet.model.Facet;

public class FacetController extends ImejiControllerAbstract<Facet> {
	private static final ReaderFacade READER = new ReaderFacade(Imeji.facetModel);
	private static final WriterFacade WRITER = new WriterFacade(Imeji.facetModel);

	@Override
	public List<Facet> createBatch(List<Facet> l, User user) throws ImejiException {
		WRITER.create(toObjectList(l), user);
		return l;
	}

	@Override
	public List<Facet> retrieveBatch(List<String> ids, User user) throws ImejiException {
		List<Facet> facets = initializeEmtpyList(ids);
		READER.read(toObjectList(facets), user);
		return facets;
	}

	@Override
	public List<Facet> retrieveBatchLazy(List<String> ids, User user) throws ImejiException {
		List<Facet> facets = initializeEmtpyList(ids);
		READER.readLazy(toObjectList(facets), user);
		return facets;
	}

	@Override
	public List<Facet> updateBatch(List<Facet> l, User user) throws ImejiException {
		WRITER.update(toObjectList(l), user, true);
		return l;
	}

	@Override
	public void deleteBatch(List<Facet> l, User user) throws ImejiException {
		WRITER.delete(toObjectList(l), user);
	}

	/**
	 * Initialize a list of empty Facets with their id
	 *
	 * @param ids
	 * @return
	 */
	private List<Facet> initializeEmtpyList(List<String> ids) {
		final List<Facet> facets = new ArrayList<>(ids.size());
		for (final String id : ids) {
			final Facet f = new Facet();
			f.setUri(URI.create(id));
			facets.add(f);
		}
		return facets;
	}
}
