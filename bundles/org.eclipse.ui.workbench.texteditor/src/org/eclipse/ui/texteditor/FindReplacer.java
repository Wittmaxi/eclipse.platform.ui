package org.eclipse.ui.texteditor;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * @since 3.18
 */
public class FindReplacer {
	IFindReplaceTarget fTarget;
	private IRegion fOldScope;

	/**
	 * Tells the dialog to perform searches only in the scope given by the actually
	 * selected lines.
	 *
	 * @param selectedLines <code>true</code> if selected lines should be used
	 * @since 2.0
	 */
	public void useSelectedLines(boolean selectedLines) {
		if (isIncrementalSearch() && !isRegExSearchAvailableAndChecked())
			initIncrementalBaseLocation();

		if (fTarget == null || !(fTarget instanceof IFindReplaceTargetExtension))
			return;

		IFindReplaceTargetExtension extensionTarget = fTarget;

		if (selectedLines) {

			IRegion scope;
			if (fOldScope == null) {
				Point lineSelection = extensionTarget.getLineSelection();
				scope = new Region(lineSelection.x, lineSelection.y);
			} else {
				scope = fOldScope;
				fOldScope = null;
			}

			int offset = isForwardSearch() ? scope.getOffset() : scope.getOffset() + scope.getLength();

			extensionTarget.setSelection(offset, 0);
			extensionTarget.setScope(scope);
		} else {
			fOldScope = extensionTarget.getScope();
			extensionTarget.setScope(null);
		}
	}

}
