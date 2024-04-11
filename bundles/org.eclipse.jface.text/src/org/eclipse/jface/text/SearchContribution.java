package org.eclipse.jface.text;

import org.eclipse.swt.graphics.Image;

/**
 * @since 3.26
 */
public interface SearchContribution {

	public StringMatcher getMatcher();

	public String getText();

	public boolean isActive(SearchContribution[] currentlyActiveContributions);

	public Image getImage();

	public String getToolTipText(); // Todo separate logic from UI

}
