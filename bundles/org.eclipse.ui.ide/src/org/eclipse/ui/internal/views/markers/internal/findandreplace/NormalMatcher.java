package org.eclipse.ui.internal.views.markers.internal.findandreplace;

import org.eclipse.jface.text.StringMatcher;

public class NormalMatcher implements StringMatcher {
	StringMatcher otherMatcher = null;

	public NormalMatcher() {

	}

	private NormalMatcher(StringMatcher other) {
		otherMatcher = other;
	}

	@Override
	public boolean doesStringMatch(String sourceString, String searchString) {
		boolean doesOtherMatch = true;
		if (otherMatcher != null) {
			doesOtherMatch = otherMatcher.doesStringMatch(sourceString, searchString);
		}
		String lowerCaseSource = sourceString.toLowerCase();
		String lowerCaseSearch = searchString.toLowerCase();
		return lowerCaseSource.split(System.lineSeparator())[0].contains(lowerCaseSearch) && doesOtherMatch;
	}


	@Override
	public StringMatcher chain(StringMatcher chainedMatcher) {
		return new NormalMatcher(chainedMatcher);
	}

}