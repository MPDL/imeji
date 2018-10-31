package de.mpg.imeji.presentation.util;

import java.util.List;

/**
 * Utility class for List
 *
 * @author bastiens
 *
 */
public class ListUtils {

	private ListUtils() {
		// private Constructor
	}

	/**
	 * Compare 2 arrays ignoring order, i.e. both arrays are compared in their
	 * natural order
	 *
	 * @param l1
	 * @param l2
	 */
	public static boolean equalsIgnoreOrder(List<String> l1, List<String> l2) {
		l1.sort(null);
		l2.sort(null);
		return l1.equals(l2);
	}
}
