package org.eclipse.ui.texteditor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.IFindReplaceTargetExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.texteditor.NLSUtility;

/**
 * @since 3.18
 */
public class FindReplacer {
	private FindReplaceStatus status = new FindReplaceStatus();
	private IFindReplaceTarget fTarget;
	private IRegion fOldScope;
	private Point fIncrementalBaseLocation;

	/**
	 * Wether the search is incremental.
	 *
	 * TODO: what is this even doing?!
	 */
	boolean fIncrementalSearch;

	/**
	 * Tells whether an initial find operation is needed before the replace
	 * operation.
	 *
	 * @since 3.0
	 */
	private boolean fNeedsInitialFindBeforeReplace;

	/**
	 * Tells whether the target supports regular expressions. <code>true</code> if
	 * the target supports regular expressions
	 */
	private boolean fIsTargetSupportingRegEx;
	boolean fRegexSearch;
	boolean fForwardSearch;
	boolean fWholeWordSearch;
	boolean fWrapSearch;
	boolean fCaseSensitiveSearch;
	boolean fGlobalSearch;
	private boolean fTargetEditable;

	public Point getIncrementalBaseLocation() {
		return fIncrementalBaseLocation;
	}

	public void setIncrementalBaseLocation(Point incrementalBaseLocation) {
		fIncrementalBaseLocation = incrementalBaseLocation;
	}

	public boolean isGlobalSearch() {
		return fGlobalSearch;
	}

	public void setGlobalSearch(boolean globalSearch) {
		fGlobalSearch = globalSearch;
	}

	public boolean needsInitialFindBeforeReplace() {
		return fNeedsInitialFindBeforeReplace;
	}

	public void setNeedsInitialFindBeforeReplace(boolean needsInitialFindBeforeReplace) {
		fNeedsInitialFindBeforeReplace = needsInitialFindBeforeReplace;
	}

	public boolean isCaseSensitiveSearch() {
		return fCaseSensitiveSearch;
	}

	public void setCaseSensitiveSearch(boolean caseSensitiveSearch) {
		fCaseSensitiveSearch = caseSensitiveSearch;
	}

	public boolean isWrapSearch() {
		return fWrapSearch;
	}

	public void setWrapSearch(boolean wrapSearch) {
		fWrapSearch = wrapSearch;
	}

	public boolean isWholeWordSearchSetting() {
		return fWholeWordSearch;
	}

	public void setWholeWordSearchSetting(boolean wholeWordSearch) {
		fWholeWordSearch = wholeWordSearch;
	}

	public FindReplaceStatus getStatus() {
		return status;
	}

	public void setStatus(FindReplaceStatus status) {
		this.status = status;
	}

	public void resetStatus() {
		status.resetStatus();
	}

	/**
	 * Returns <code>true</code> if searching should be restricted to entire words,
	 * <code>false</code> if not. This is the case if the respective checkbox is
	 * turned on, regex is off, and the checkbox is enabled, i.e. the current find
	 * string is an entire word.
	 *
	 * @return <code>true</code> if the search is restricted to whole words
	 */
	private boolean isWholeWordSearch() {
		return isWholeWordSearchSetting() && !isRegExSearchAvailableAndChecked();
	}

	public boolean isForwardSearch() {
		return fForwardSearch;
	}

	public void setForwardSearch(boolean forwardSearch) {
		fForwardSearch = forwardSearch;
	}

	public boolean isTargetSupportingRegEx() {
		return fIsTargetSupportingRegEx;
	}

	public void setIsTargetSupportingRegEx(boolean isTargetSupportingRegEx) {
		fIsTargetSupportingRegEx = isTargetSupportingRegEx;
	}

	public boolean isIncrementalSearch() {
		return fIncrementalSearch;
	}

	public void setIncrementalSearch(boolean incrementalSearch) {
		fIncrementalSearch = incrementalSearch;
	}

	public boolean isRegexSearch() {
		return fRegexSearch;
	}

	public void setRegexSearch(boolean regexSearch) {
		fRegexSearch = regexSearch;
	}


	public boolean isRegExSearchAvailableAndChecked() {
		return isRegexSearch() && fIsTargetSupportingRegEx;
	}


	/**
	 * initializes the anchor used as starting point for incremental searching.
	 *
	 * @since 2.0
	 */
	public void initIncrementalBaseLocation() {
		if (fTarget != null && isIncrementalSearch() && !isRegExSearchAvailableAndChecked()) {
			fIncrementalBaseLocation = fTarget.getSelection();
		} else {
			fIncrementalBaseLocation = new Point(0, 0);
		}
	}

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

		IFindReplaceTargetExtension extensionTarget = (IFindReplaceTargetExtension) fTarget;

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

	/**
	 * Returns the status line manager of the active editor or <code>null</code> if
	 * there is no such editor.
	 *
	 * @return the status line manager of the active editor
	 */
	private IEditorStatusLine getStatusLineManager() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;

		IWorkbenchPage page = window.getActivePage();
		if (page == null)
			return null;

		IEditorPart editor = page.getActiveEditor();
		if (editor == null)
			return null;

		return editor.getAdapter(IEditorStatusLine.class);
	}


	/**
	 * Replaces all occurrences of the user's findString with the replace string.
	 * Indicate to the user the number of replacements that occur.
	 *
	 * @param replaceString
	 * @param findString
	 * @param display
	 */
	public void performReplaceAll(String replaceString, String findString, Display display) {

		int replaceCount = 0;

		if (findString != null && !findString.isEmpty()) {

			class ReplaceAllRunnable implements Runnable {
				public int numberOfOccurrences;

				@Override
				public void run() {
					numberOfOccurrences = replaceAll(findString, replaceString == null ? "" : replaceString, //$NON-NLS-1$
							isCaseSensitiveSearch(), isWholeWordSearch(), isRegExSearchAvailableAndChecked());
				}
			}

			try {
				ReplaceAllRunnable runnable = new ReplaceAllRunnable();
				BusyIndicator.showWhile(display, runnable);
				replaceCount = runnable.numberOfOccurrences;

				if (replaceCount != 0) {
					if (replaceCount == 1) { // not plural
						statusMessage(EditorMessages.FindReplace_Status_replacement_label);
					} else {
						String msg = EditorMessages.FindReplace_Status_replacements_label;
						msg = NLSUtility.format(msg, String.valueOf(replaceCount));
						statusMessage(msg);
					}
				} else {
					String msg = NLSUtility.format(EditorMessages.FindReplace_Status_noMatchWithValue_label,
							findString);
					statusMessage(false, EditorMessages.FindReplace_Status_noMatch_label, msg);
				}
			} catch (PatternSyntaxException ex) {
				statusError(ex.getLocalizedMessage());
			} catch (IllegalStateException ex) {
				// we don't keep state in this dialog
			}
		}
	}

	/**
	 * Replaces all occurrences of the user's findString with the replace string.
	 * Indicate to the user the number of replacements that occur.
	 */
	public void performSelectAll(String findString, Display display) {

		int selectCount = 0;

		if (findString != null && !findString.isEmpty()) {

			class SelectAllRunnable implements Runnable {
				public int numberOfOccurrences;

				@Override
				public void run() {
					numberOfOccurrences = selectAll(findString, isCaseSensitiveSearch(), isWholeWordSearch(),
							isRegExSearchAvailableAndChecked());
				}
			}

			try {
				SelectAllRunnable runnable = new SelectAllRunnable();
				BusyIndicator.showWhile(display, runnable);
				selectCount = runnable.numberOfOccurrences;

				if (selectCount != 0) {
					if (selectCount == 1) { // not plural
						statusMessage(EditorMessages.FindReplace_Status_selection_label);
					} else {
						String msg = EditorMessages.FindReplace_Status_selections_label;
						msg = NLSUtility.format(msg, String.valueOf(selectCount));
						statusMessage(msg);
					}
				} else {
					String msg = NLSUtility.format(EditorMessages.FindReplace_Status_noMatchWithValue_label,
							findString);
					statusMessage(false, EditorMessages.FindReplace_Status_noMatch_label, msg);
				}
			} catch (PatternSyntaxException ex) {
				statusError(ex.getLocalizedMessage());
			} catch (IllegalStateException ex) {
				// we don't keep state in this dialog
			}
		}
	}

	/**
	 * Validates the state of the find/replace target.
	 *
	 * @return <code>true</code> if target can be changed, <code>false</code>
	 *         otherwise
	 * @since 2.1
	 */
	public boolean validateTargetState() {

		if (fTarget instanceof IFindReplaceTargetExtension2) {
			IFindReplaceTargetExtension2 extension = (IFindReplaceTargetExtension2) fTarget;
			if (!extension.validateTargetState()) {
				statusError(EditorMessages.FindReplaceDialog_read_only);
				return false;
			}
		}
		return isEditable();
	}

	/**
	 * Replaces the current selection of the target with the user's replace string.
	 *
	 * @return <code>true</code> if the operation was successful
	 */
	public boolean performReplaceSelection(String replaceString) {

		if (!validateTargetState())
			return false;

		if (replaceString == null)
			replaceString = ""; //$NON-NLS-1$

		boolean replaced;
		try {
			replaceSelection(replaceString, isRegExSearchAvailableAndChecked());
			replaced = true;
		} catch (PatternSyntaxException ex) {
			statusError(ex.getLocalizedMessage());
			replaced = false;
		} catch (IllegalStateException ex) {
			replaced = false;
		}

		return replaced;
	}

	/**
	 * Locates the user's findString in the text of the target.
	 *
	 * @since 3.7
	 */
	public boolean performSearch(String searchString) {
		return performSearch(isIncrementalSearch() && !isRegExSearchAvailableAndChecked(), searchString);
	}

	/**
	 * Locates the user's findString in the text of the target.
	 *
	 * @param mustInitIncrementalBaseLocation <code>true</code> if base location
	 *                                        must be initialized
	 * @param beep                            if <code>true</code> beeps when search
	 *                                        does not find a match or needs to wrap
	 * @param forwardSearch                   the search direction
	 * @since 3.0
	 */
	public boolean performSearch(boolean mustInitIncrementalBaseLocation, String findString) {

		if (mustInitIncrementalBaseLocation)
			initIncrementalBaseLocation();

		boolean somethingFound = false;

		if (findString != null && !findString.isEmpty()) {

			try {
				somethingFound = findNext(findString, isForwardSearch());
			} catch (PatternSyntaxException ex) {
				statusError(ex.getLocalizedMessage());
			} catch (IllegalStateException ex) {
				// we don't keep state in this dialog
			}
		}
		return somethingFound;
	}

	/**
	 * Replaces all occurrences of the user's findString with the replace string.
	 * Returns the number of replacements that occur.
	 *
	 * @param findString    the string to search for
	 * @param replaceString the replacement string
	 * @param caseSensitive should the search be case sensitive
	 * @param wholeWord     does the search string represent a complete word
	 * @param regExSearch   if <code>true</code> findString represents a regular
	 *                      expression
	 * @return the number of occurrences
	 *
	 * @since 3.0
	 */
	public int replaceAll(String findString, String replaceString, boolean caseSensitive, boolean wholeWord,
			boolean regExSearch) {

		int replaceCount = 0;
		int findReplacePosition = 0;

		findReplacePosition = 0;

		if (!validateTargetState())
			return replaceCount;

		if (fTarget instanceof IFindReplaceTargetExtension)
			((IFindReplaceTargetExtension) fTarget).setReplaceAllMode(true);

		try {
			int index = 0;
			while (index != -1) {
				index = findAndSelect(findReplacePosition, findString);
				if (index != -1) { // substring not contained from current position
					Point selection = replaceSelection(replaceString, regExSearch);
					replaceCount++;
					findReplacePosition = selection.x + selection.y;
				}
			}
		} finally {
			if (fTarget instanceof IFindReplaceTargetExtension)
				((IFindReplaceTargetExtension) fTarget).setReplaceAllMode(false);
		}

		return replaceCount;
	}

	public int selectAll(String findString, boolean caseSensitive, boolean wholeWord, boolean regExSearch) {

		int replaceCount = 0;
		int position = 0;

		if (!validateTargetState())
			return replaceCount;

		List<Region> selectedRegions = new ArrayList<>();
		int index = 0;
		do {
			index = findAndSelect(position, findString);
			if (index != -1) { // substring not contained from current position
				Point selection = fTarget.getSelection();
				selectedRegions.add(new Region(selection.x, selection.y));
				replaceCount++;
				position = selection.x + selection.y;
			}
		} while (index != -1);
		if (fTarget instanceof IFindReplaceTargetExtension4) {
			((IFindReplaceTargetExtension4) fTarget).setSelection(selectedRegions.toArray(IRegion[]::new));
		}

		return replaceCount;
	}

	/**
	 * Returns the position of the specified search string, or <code>-1</code> if
	 * the string can not be found when searching using the given options.
	 *
	 * @param findString    the string to search for
	 * @param startPosition the position at which to start the search
	 * @param forwardSearch the direction of the search
	 * @param caseSensitive should the search be case sensitive
	 * @param wrapSearch    should the search wrap to the start/end if arrived at
	 *                      the end/start
	 * @param wholeWord     does the search string represent a complete word
	 * @param regExSearch   if <code>true</code> findString represents a regular
	 *                      expression
	 * @param beep          if <code>true</code> beeps when search does not find a
	 *                      match or needs to wrap
	 * @return the occurrence of the find string following the options or
	 *         <code>-1</code> if nothing found
	 * @since 3.0
	 */
	public int findIndex(String findString, int startPosition) {

		if (isForwardSearch()) {
			int index = findAndSelect(startPosition, findString);
			if (index == -1) {

				status.doBeep();

				if (isWrapSearch()) {
					statusMessage(EditorMessages.FindReplace_Status_wrapped_label);
					index = findAndSelect(-1, findString);
				}
			}
			return index;
		}

		// backward
		int index = startPosition == 0 ? -1
				: findAndSelect(startPosition - 1, findString);
		if (index == -1) {

			status.doBeep();

			if (isWrapSearch()) {
				statusMessage(EditorMessages.FindReplace_Status_wrapped_label);
				index = findAndSelect(-1, findString);
			}
		}
		return index;
	}

	/**
	 * Searches for a string starting at the given offset and using the specified
	 * search directives. If a string has been found it is selected and its start
	 * offset is returned.
	 *
	 * @param offset        the offset at which searching starts
	 * @param findString    the string which should be found
	 * @param forwardSearch the direction of the search
	 * @param caseSensitive <code>true</code> performs a case sensitive search,
	 *                      <code>false</code> an insensitive search
	 * @param wholeWord     if <code>true</code> only occurrences are reported in
	 *                      which the findString stands as a word by itself
	 * @param regExSearch   if <code>true</code> findString represents a regular
	 *                      expression
	 * @return the position of the specified string, or -1 if the string has not
	 *         been found
	 * @since 3.0
	 */
	public int findAndSelect(int offset, String findString) {
		if (fTarget instanceof IFindReplaceTargetExtension3)
			return ((IFindReplaceTargetExtension3) fTarget).findAndSelect(offset, findString, isForwardSearch(),
					isCaseSensitiveSearch(), isWholeWordSearch(), isRegexSearch());
		return fTarget.findAndSelect(offset, findString, isForwardSearch(), isCaseSensitiveSearch(),
				isWholeWordSearch());
	}

	/**
	 * Replaces the selection with <code>replaceString</code>. If
	 * <code>regExReplace</code> is <code>true</code>, <code>replaceString</code> is
	 * a regex replace pattern which will get expanded if the underlying target
	 * supports it. Returns the region of the inserted text; note that the returned
	 * selection covers the expanded pattern in case of regex replace.
	 *
	 * @param replaceString the replace string (or a regex pattern)
	 * @param regExReplace  <code>true</code> if <code>replaceString</code> is a
	 *                      pattern
	 * @return the selection after replacing, i.e. the inserted text
	 * @since 3.0
	 */
	public Point replaceSelection(String replaceString, boolean regExReplace) {
		if (fTarget instanceof IFindReplaceTargetExtension3)
			((IFindReplaceTargetExtension3) fTarget).replaceSelection(replaceString, regExReplace);
		else
			fTarget.replaceSelection(replaceString);

		return fTarget.getSelection();
	}

	/**
	 * Returns whether the specified search string can be found using the given
	 * options.
	 *
	 * @param findString    the string to search for
	 * @param forwardSearch the direction of the search
	 * @param caseSensitive should the search be case sensitive
	 * @param wrapSearch    should the search wrap to the start/end if arrived at
	 *                      the end/start
	 * @param wholeWord     does the search string represent a complete word
	 * @param incremental   is this an incremental search
	 * @param regExSearch   if <code>true</code> findString represents a regular
	 *                      expression
	 * @param beep          if <code>true</code> beeps when search does not find a
	 *                      match or needs to wrap
	 * @return <code>true</code> if the search string can be found using the given
	 *         options
	 *
	 * @since 3.0
	 */
	public boolean findNext(String findString, boolean forwardSearch) {

		if (fTarget == null)
			return false;

		Point r = null;
		if (isIncrementalSearch())
			r = fIncrementalBaseLocation;
		else
			r = fTarget.getSelection();

		int findReplacePosition = r.x;
		if (forwardSearch && !fNeedsInitialFindBeforeReplace || !forwardSearch && fNeedsInitialFindBeforeReplace)
			findReplacePosition += r.y;

		fNeedsInitialFindBeforeReplace = false;

		int index = findIndex(findString, findReplacePosition);

		if (index == -1) {
			String msg = NLSUtility.format(EditorMessages.FindReplace_Status_noMatchWithValue_label, findString);
			statusMessage(false, EditorMessages.FindReplace_Status_noMatch_label, msg);
			return false;
		}

		if (forwardSearch && index >= findReplacePosition || !forwardSearch && index <= findReplacePosition)
			statusMessage(""); //$NON-NLS-1$

		return true;
	}

	public boolean performFindFirstThenReplaceInASecondStep(String findString, String replaceString) {
		if (fNeedsInitialFindBeforeReplace) {
			performSearch(findString);
		}
		if (performReplaceSelection(replaceString)) {
			performSearch(findString);
			return true;
		}
		return false;
	}

	public boolean performSelectAndReplace(String findString, String replaceString) {
		if (fNeedsInitialFindBeforeReplace)
			performSearch(findString);
		return performReplaceSelection(replaceString);
	}

	public void updateTarget(IFindReplaceTarget target, boolean isTargetEditable) {
		fTargetEditable = isTargetEditable;
		fNeedsInitialFindBeforeReplace = true;

		if (target != fTarget) {
			if (fTarget != null && fTarget instanceof IFindReplaceTargetExtension)
				((IFindReplaceTargetExtension) fTarget).endSession();

			fTarget = target;
			if (fTarget != null)
				fIsTargetSupportingRegEx = fTarget instanceof IFindReplaceTargetExtension3;

			if (fTarget instanceof IFindReplaceTargetExtension) {
				((IFindReplaceTargetExtension) fTarget).beginSession();

				setGlobalSearch(true);
			}
		}

		initIncrementalBaseLocation();
	}

	public void endSession() {
		if (fTarget != null && fTarget instanceof IFindReplaceTargetExtension)
			((IFindReplaceTargetExtension) fTarget).endSession();

		fTarget = null;
	}


	public void deactivateScope() {
		if (fTarget != null && (fTarget instanceof IFindReplaceTargetExtension))
			((IFindReplaceTargetExtension) fTarget).setScope(null);

		fOldScope = null;
	}

	public String getCurrentSelection() {
		if (fTarget == null) {
			return null;
		}

		return fTarget.getSelectionText();
	}

	/**
	 * Returns whether the target is editable.
	 *
	 * @return <code>true</code> if target is editable
	 */
	public boolean isEditable() {
		boolean isEditable = (fTarget == null ? false : fTarget.isEditable());
		return fTargetEditable && isEditable;
	}

	public boolean supportsMultiSelection() {
		return fTarget instanceof IFindReplaceTargetExtension4;
	}

	public boolean isTargetAvailable() {
		return fTarget != null;
	}

	/**
	 * Sets the given status message in the status line.
	 *
	 * @param error         <code>true</code> if it is an error
	 * @param dialogMessage the message to display in the dialog's status line
	 * @param editorMessage the message to display in the editor's status line
	 */
	private void statusMessage(boolean error, String dialogMessage, String editorMessage) {
		status.setError(error);
		status.setMessage(dialogMessage);

		IEditorStatusLine statusLine = getStatusLineManager();
		if (statusLine != null)
			statusLine.setMessage(error, editorMessage, null);
	}

	/**
	 * Sets the given error message in the status line.
	 *
	 * @param message the message
	 */
	private void statusError(String message) {
		statusMessage(true, message, message);
	}

	/**
	 * Sets the given message in the status line.
	 *
	 * @param message the message
	 */
	private void statusMessage(String message) {
		statusMessage(false, message, message);
	}

	public void updateSearchResultAfterTextWasModified(String searchString) {
		if (isIncrementalSearch() && !isRegExSearchAvailableAndChecked()) {
			if (searchString.equals("") && fTarget != null) { //$NON-NLS-1$
				// empty selection at base location
				int offset = fIncrementalBaseLocation.x;

				if (isForwardSearch() && !fNeedsInitialFindBeforeReplace
						|| !isForwardSearch() && fNeedsInitialFindBeforeReplace)
					offset = offset + fIncrementalBaseLocation.y;

				fNeedsInitialFindBeforeReplace = false;
				findAndSelect(offset, ""); //$NON-NLS-1$
			} else {
				performSearch(false, searchString);
			}
		}
	}

}
