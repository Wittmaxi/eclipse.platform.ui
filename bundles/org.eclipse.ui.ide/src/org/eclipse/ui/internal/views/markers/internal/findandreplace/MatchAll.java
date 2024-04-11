package org.eclipse.ui.internal.views.markers.internal.findandreplace;

import org.eclipse.jface.text.StringMatcher;

public class MatchAll implements StringMatcher {
	StringMatcher otherMatcher = null;

	private MatchAll(StringMatcher other) {
		otherMatcher = other;
	}

	/**
	 *
	 */
	public MatchAll() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean doesStringMatch(String sourceString, String searchString) {
		if (otherMatcher != null) {
			return otherMatcher.doesStringMatch(sourceString, searchString);
		}
		return true;
	}

	@Override
	public StringMatcher chain(StringMatcher chainedMatcher) {
		return new MatchAll(chainedMatcher);
	}

}