package org.eclipse.ui.internal.views.markers.internal.findandreplace;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.SearchContribution;
import org.eclipse.jface.text.StringMatcher;

import org.eclipse.ui.texteditor.FindReplaceOverlayImages;


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

	@Override
	public Image getImage() {
		return FindReplaceOverlayImages.get(FindReplaceOverlayImages.OBJ_WHOLE_WORD);
	}

	@Override
	public String getToolTipText() {
		return "Only search for whole words"; //$NON-NLS-1$
	}

}
