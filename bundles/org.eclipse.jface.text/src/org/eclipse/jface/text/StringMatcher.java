package org.eclipse.jface.text;

/**
 * @since 3.26
 */
public interface StringMatcher { // an abstract class probably makes more sense here!
	public boolean doesStringMatch(String sourceString, String searchString);

	public StringMatcher chain(StringMatcher chainedMatcher);
}