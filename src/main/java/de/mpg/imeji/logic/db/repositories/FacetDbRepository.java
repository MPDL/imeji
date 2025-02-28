package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.search.facet.model.Facet;

public class FacetDbRepository extends DbRepository<Facet> {

    public FacetDbRepository() {
        super(Facet.class);
    }
}
