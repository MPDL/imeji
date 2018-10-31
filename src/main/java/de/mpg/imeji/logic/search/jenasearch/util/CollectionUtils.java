package de.mpg.imeji.logic.search.jenasearch.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Based on {@link de.mpg.imeji.logic.search.jenasearch.util.CollectionUtils}
 * <br/>
 * Introduced order to
 * {@link de.mpg.imeji.logic.search.jenasearch.util.CollectionUtils}<br/>
 * Much faster than other classes
 *
 * @author saquet
 */
public class CollectionUtils {
	private static Integer INTEGER_ONE = new Integer(1);

	/**
	 * Make UNION operation between 2 {@link Collection}
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Collection union(final Collection a, final Collection b) {
		final ArrayList list = new ArrayList();
		final Map mapa = getCardinalityMap(a);
		final Map mapb = getCardinalityMap(b);
		final Set elts = new LinkedHashSet(a);
		elts.addAll(b);
		final Iterator it = elts.iterator();
		while (it.hasNext()) {
			final Object obj = it.next();
			for (int i = 0, m = Math.max(getFreq(obj, mapa), getFreq(obj, mapb)); i < m; i++) {
				list.add(obj);
			}
		}
		return list;
	}

	/**
	 * Make INTERSECTION operation between 2 {@link Collection}
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Collection intersection(final Collection a, final Collection b) {
		final ArrayList list = new ArrayList();
		final Map mapa = getCardinalityMap(a);
		final Map mapb = getCardinalityMap(b);
		final Set elts = new LinkedHashSet(a);
		elts.addAll(b);
		final Iterator it = elts.iterator();
		while (it.hasNext()) {
			final Object obj = it.next();
			for (int i = 0, m = Math.min(getFreq(obj, mapa), getFreq(obj, mapb)); i < m; i++) {
				list.add(obj);
			}
		}
		return list;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Map getCardinalityMap(final Collection coll) {
		final Map count = new HashMap();
		for (final Iterator it = coll.iterator(); it.hasNext();) {
			final Object obj = it.next();
			final Integer c = (Integer) (count.get(obj));
			if (c == null) {
				count.put(obj, INTEGER_ONE);
			} else {
				count.put(obj, new Integer(c.intValue() + 1));
			}
		}
		return count;
	}

	@SuppressWarnings({"rawtypes"})
	private static final int getFreq(final Object obj, final Map freqMap) {
		final Integer count = (Integer) freqMap.get(obj);
		if (count != null) {
			return count.intValue();
		}
		return 0;
	}
}
