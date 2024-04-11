package org.eclipse.ui.internal.views.markers.internal.findandreplace;

import org.eclipse.jface.text.SearchContribution;
import org.eclipse.jface.text.StringMatcher;

/**
 * @since 3.4
 *
 */
public class GroupedSearchContribution implements SearchContribution {

	private final GroupedSearch matcher = new GroupedSearch();

	@Override
	public StringMatcher getMatcher() {
		return matcher;
	}

	@Override
	public String getText() {
		return "grouped"; //$NON-NLS-1$
	}

	@Override
	public boolean isActive(SearchContribution[] currentlyActiveContributions) {
		return true;
	}

}
