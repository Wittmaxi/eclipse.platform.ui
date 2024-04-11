package org.eclipse.ui.internal.views.markers.internal.findandreplace;

import org.eclipse.jface.text.SearchContribution;
import org.eclipse.jface.text.StringMatcher;

/**
 * @since 3.4
 *
 */
public class WholeWordSearchContribution implements SearchContribution {

	@Override
	public StringMatcher getMatcher() {
		return new WholeWordMatcher();
	}

	@Override
	public String getText() {
		return "whole word"; //$NON-NLS-1$
	}

	@Override
	public boolean isActive(SearchContribution[] currentlyActiveContributions) {
		return true;
	}

}
