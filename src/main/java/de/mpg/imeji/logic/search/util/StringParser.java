package de.mpg.imeji.logic.search.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to parse Strings
 *
 * @author bastiens
 *
 */
public class StringParser {
	private Matcher m;
	private final Pattern p;

	public StringParser(Pattern p) {
		this.p = p;
	}

	public boolean find(String s) {
		this.m = p.matcher(s);
		return m.find();
	}

	public String getGroup(int i) {
		return m.group(i);
	}
}
