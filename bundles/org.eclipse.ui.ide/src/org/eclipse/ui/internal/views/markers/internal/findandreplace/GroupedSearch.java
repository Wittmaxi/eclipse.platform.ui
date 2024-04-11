package org.eclipse.ui.internal.views.markers.internal.findandreplace;

import org.eclipse.jface.text.StringMatcher;

public class GroupedSearch implements StringMatcher {
	StringMatcher otherMatcher = null;
	String lastMatch = new String();

	public GroupedSearch() {

	}

	private GroupedSearch(StringMatcher other) {
		otherMatcher = other;
	}

	@Override
	public boolean doesStringMatch(String sourceString, String searchString) {
		boolean otherMatched = true;
		if (otherMatcher != null) {
			otherMatched = otherMatcher.doesStringMatch(sourceString, searchString);
		}

		if (lastMatch.equals(sourceString)) {
			return false;
		}

		if (otherMatched) {
			lastMatch = sourceString;
		}
		return otherMatched;
	}


	@Override
	public StringMatcher chain(StringMatcher chainedMatcher) {
		return new GroupedSearch(chainedMatcher);
	}

}