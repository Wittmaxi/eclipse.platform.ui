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
public class CaseSensitiveSearchContribution implements SearchContribution {

	@Override
	public StringMatcher getMatcher() {
		return new CaseSensitiveMatcher();
	}

	@Override
	public String getText() {
		return "Case"; //$NON-NLS-1$
	}

	@Override
	public boolean isActive(SearchContribution[] currentlyActiveContributions) {
		return true;
	}

	@Override
	public Image getImage() {
		ProblemViewImages images = new ProblemViewImages();

		return images.get(FindReplaceOverlayImages.OBJ_CASE_SENSITIVE);
	}

	@Override
	public String getToolTipText() {
		return "Perform a Case-Sensitive Search"; //$NON-NLS-1$
	}

}
