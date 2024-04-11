package org.eclipse.ui.internal.views.markers.internal.findandreplace;

import org.eclipse.jface.text.SearchContribution;
import org.eclipse.jface.text.StringMatcher;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.internal.views.markers.ProblemViewImages;
import org.eclipse.ui.texteditor.FindReplaceOverlayImages;

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

	@Override
	public Image getImage() {
		ProblemViewImages images = new ProblemViewImages();

		return images.get(FindReplaceOverlayImages.OBJ_REPLACE_ALL);
	}

	@Override
	public String getToolTipText() {
		return "Perform a grouped search: skip results where the same text appears two times in a row"; //$NON-NLS-1$
	}

}
