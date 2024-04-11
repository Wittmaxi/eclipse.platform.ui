package org.eclipse.jface.text;

/**
 * @since 3.26
 */
public interface SearchContribution {

	public StringMatcher getMatcher();

	public String getText();

	public boolean isActive(SearchContribution[] currentlyActiveContributions);

}
