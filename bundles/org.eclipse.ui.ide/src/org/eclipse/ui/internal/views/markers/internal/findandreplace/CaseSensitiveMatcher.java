package org.eclipse.ui.internal.views.markers.internal.findandreplace;

import org.eclipse.jface.text.StringMatcher;

public class CaseSensitiveMatcher implements StringMatcher {
	StringMatcher otherMatcher = null;

	public CaseSensitiveMatcher() {

	}

	private CaseSensitiveMatcher(StringMatcher other) {
		otherMatcher = other;
	}

	@Override
	public boolean doesStringMatch(String sourceString, String searchString) {
		boolean doesOtherMatch = true;
		if (otherMatcher != null) {
			doesOtherMatch = otherMatcher.doesStringMatch(sourceString, searchString);
		}
		return sourceString.split(System.lineSeparator())[0].contains(searchString) && doesOtherMatch;
	}

	@Override
	public StringMatcher chain(StringMatcher chainedMatcher) {
		return new CaseSensitiveMatcher(chainedMatcher);
	}

}