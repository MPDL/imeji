package de.mpg.imeji.presentation.facet;

import java.util.List;

/**
 * Abstract class for Facets
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class FacetsAbstract {
  /**
   * Initialize the {@link FacetsAbstract}
   */
  public abstract void init();

  /**
   * Return a {@link List} of {@link Facet}
   *
   * @return
   */
  public abstract List<List<Facet>> getFacets();
}
