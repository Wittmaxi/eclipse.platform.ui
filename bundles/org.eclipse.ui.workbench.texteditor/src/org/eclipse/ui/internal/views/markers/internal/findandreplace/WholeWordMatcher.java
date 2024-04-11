package org.eclipse.ui.internal.views.markers.internal.findandreplace;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.StringMatcher;

public class WholeWordMatcher implements StringMatcher {
	StringMatcher otherMatcher = null;

	public WholeWordMatcher() {

	}

	private WholeWordMatcher(StringMatcher other) {
		otherMatcher = other;
	}

	@Override
	public boolean doesStringMatch(String sourceString, String searchString) {
		boolean doesOtherMatch = true;
		if (otherMatcher != null) {
			doesOtherMatch = otherMatcher.doesStringMatch(sourceString, searchString);
		}
		String firstLine = sourceString.split(System.lineSeparator())[0];
		List<String> words = Arrays.asList(firstLine.split(" ")); //$NON-NLS-1$
		return words.contains(searchString) && doesOtherMatch;
	}


	@Override
	public StringMatcher chain(StringMatcher chainedMatcher) {
		return new WholeWordMatcher(chainedMatcher);
	}
}