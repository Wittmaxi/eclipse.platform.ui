/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

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
 * @since 3.17
 */
class FindReplaceLogic implements IFindReplaceLogic {
	private FindAndReplaceMessageStatus status = new FindAndReplaceMessageStatus();
	private IFindReplaceTarget target;
	private IRegion oldScope;
	private Point incrementalBaseLocation;

	/**
	 * Whether the search is incremental. (Search-as-you-type). The incremental
	 * search will find the next occurrence after
	 * <code>incrementalBaseLocation</code>
	 */
	private boolean isSearchAsYouType;

	/**
	 * Tells whether an initial find operation is needed before the replace
	 * operation.
	 *
	 * @since 3.0
	 */
	private boolean nextReplactionOperationNeedsFindOperationFirst;

	/**
	 * Tells whether the target supports regular expressions. <code>true</code> if
	 * the target supports regular expressions
	 */
	private boolean fIsTargetSupportingRegEx;
	private boolean useRegExSearch;
	private boolean searchFoward;
	private boolean searchInWholeWords;
	private boolean wrapSearch;
	private boolean respectCaseInSearch;
	private boolean searchEntireDocument;
	private boolean isTargetEditable;

	/**
	 * A global search in files searches in the whole file, as opposed to only
	 * searching in a part of the selection.
	 *
	 * @return boolean whether the search is global
	 */
	@Override
	public boolean isGlobalSearch() {
		return searchEntireDocument;
	}

	@Override
	public void setGlobalSearch(boolean globalSearch) {
		searchEntireDocument = globalSearch;
	}

	@Override
	public boolean needsInitialFindBeforeReplace() {
		return nextReplactionOperationNeedsFindOperationFirst;
	}

	@Override
	public void setNeedsInitialFindBeforeReplace(boolean needsInitialFindBeforeReplace) {
		nextReplactionOperationNeedsFindOperationFirst = needsInitialFindBeforeReplace;
	}

	@Override
	public boolean isCaseSensitiveSearch() {
		return respectCaseInSearch;
	}

	@Override
	public void setCaseSensitiveSearch(boolean caseSensitiveSearch) {
		respectCaseInSearch = caseSensitiveSearch;
	}

	@Override
	public boolean isWrapSearch() {
		return wrapSearch;
	}

	@Override
	public void setWrapSearch(boolean wrapSearch) {
		this.wrapSearch = wrapSearch;
	}

	@Override
	public boolean isWholeWordSearchSetting() {
		return searchInWholeWords;
	}

	@Override
	public void setWholeWordSearchSetting(boolean wholeWordSearch) {
		searchInWholeWords = wholeWordSearch;
	}

	/**
	 * Returns the current status of FindReplaceLogic. Assumes that
	 * <code>resetStatus</code> was run before the last operation. The Status can
	 * inform about events such as an error happening, a warning happening (e.g.:
	 * the search-string wasn't found) and can including a message that the UI may
	 * choose to print.
	 *
	 * @return FindAndReplaceMessageStatus
	 */
	@Override
	public FindAndReplaceMessageStatus getStatus() {
		return status;
	}

	/**
	 * Call before running an operation of FindReplaceLogic. Resets the internal
	 * status.
	 */
	@Override
	public void resetStatus() {
		status = new FindAndReplaceMessageStatus();
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

	@Override
	public boolean isForwardSearch() {
		return searchFoward;
	}

	@Override
	public void setForwardSearch(boolean forwardSearch) {
		searchFoward = forwardSearch;
	}

	@Override
	public boolean isTargetSupportingRegEx() {
		return fIsTargetSupportingRegEx;
	}

	@Override
	public void setIsTargetSupportingRegEx(boolean isTargetSupportingRegEx) {
		fIsTargetSupportingRegEx = isTargetSupportingRegEx;
	}

	@Override
	public boolean isIncrementalSearch() {
		return isSearchAsYouType;
	}

	@Override
	public void setIncrementalSearch(boolean incrementalSearch) {
		this.isSearchAsYouType = incrementalSearch;
	}

	@Override
	public boolean isRegexSearch() {
		return useRegExSearch;
	}

	@Override
	public void setRegexSearch(boolean regexSearch) {
		useRegExSearch = regexSearch;
	}


	@Override
	public boolean isRegExSearchAvailableAndChecked() {
		return isRegexSearch() && fIsTargetSupportingRegEx;
	}


	/**
	 * Initializes the anchor used as starting point for incremental searching.
	 *
	 * @since 2.0
	 */
	@Override
	public void initIncrementalBaseLocation() {
		if (target != null && isIncrementalSearch() && !isRegExSearchAvailableAndChecked()) {
			incrementalBaseLocation = target.getSelection();
		} else {
			incrementalBaseLocation = new Point(0, 0);
		}
	}

	/**
	 * Tells the dialog to perform searches only in the scope given by the actually
	 * selected lines.
	 *
	 * @param selectedLines <code>true</code> if selected lines should be used
	 * @since 2.0
	 */
	@Override
	public void useSelectedLines(boolean selectedLines) {
		if (isIncrementalSearch() && !isRegExSearchAvailableAndChecked())
			initIncrementalBaseLocation();

		if (target == null || !(target instanceof IFindReplaceTargetExtension))
			return;

		IFindReplaceTargetExtension extensionTarget = (IFindReplaceTargetExtension) target;

		if (selectedLines) {

			IRegion scope;
			if (oldScope == null) {
				Point lineSelection = extensionTarget.getLineSelection();
				scope = new Region(lineSelection.x, lineSelection.y);
			} else {
				scope = oldScope;
				oldScope = null;
			}

			int offset = isForwardSearch() ? scope.getOffset() : scope.getOffset() + scope.getLength();

			extensionTarget.setSelection(offset, 0);
			extensionTarget.setScope(scope);
		} else {
			oldScope = extensionTarget.getScope();
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
	 * @param findString    The string that will be replaced
	 * @param replaceString The string that will replace the findString
	 * @param display       the UI's Display
	 */
	@Override
	public void performReplaceAll(String findString, String replaceString, Display display) {

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
	 * Selects all occurrences of findString.
	 *
	 * @param findString The String to find and select
	 * @param display    The UI's Display The UI's Display
	 */
	@Override
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
				// we don't keep state
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
	@Override
	public boolean validateTargetState() {

		if (target instanceof IFindReplaceTargetExtension2) {
			IFindReplaceTargetExtension2 extension = (IFindReplaceTargetExtension2) target;
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
	 * @param replaceString the String to replace the selection with
	 *
	 * @return <code>true</code> if the operation was successful
	 */
	@Override
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
	 * Locates the user's findString in the target
	 *
	 * @param searchString the String to search for
	 * @return Whether the string was found in the target
	 *
	 * @since 3.7
	 */
	@Override
	public boolean performSearch(String searchString) {
		return performSearch(isIncrementalSearch() && !isRegExSearchAvailableAndChecked(), searchString);
	}

	/**
	 * Locates the user's findString in the text of the target.
	 *
	 * @param mustInitIncrementalBaseLocation <code>true</code> if base location
	 *                                        must be initialized
	 * @param findString                      the String to search for
	 * @return Whether the string was found in the target
	 * @since 3.0
	 */
	@Override
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
	@Override
	public int replaceAll(String findString, String replaceString, boolean caseSensitive, boolean wholeWord,
			boolean regExSearch) {

		int replaceCount = 0;
		int findReplacePosition = 0;

		findReplacePosition = 0;

		if (!validateTargetState())
			return replaceCount;

		if (target instanceof IFindReplaceTargetExtension)
			((IFindReplaceTargetExtension) target).setReplaceAllMode(true);

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
			if (target instanceof IFindReplaceTargetExtension)
				((IFindReplaceTargetExtension) target).setReplaceAllMode(false);
		}

		return replaceCount;
	}

	/**
	 * @param findString    the String to select as part of the search
	 * @param caseSensitive Should case be respected in searching?
	 * @param wholeWord     Should the find String be searched as a whole word?
	 * @param regExSearch   Should the findString be interpreted as RegEx?
	 * @return The amount of selected Elements
	 */
	@Override
	public int selectAll(String findString, boolean caseSensitive, boolean wholeWord, boolean regExSearch) {

		int selectCount = 0;
		int position = 0;

		if (!validateTargetState())
			return selectCount;

		List<Region> selectedRegions = new ArrayList<>();
		int index = 0;
		do {
			index = findAndSelect(position, findString);
			if (index != -1) { // substring not contained from current position
				Point selection = target.getSelection();
				selectedRegions.add(new Region(selection.x, selection.y));
				selectCount++;
				position = selection.x + selection.y;
			}
		} while (index != -1);
		if (target instanceof IFindReplaceTargetExtension4) {
			((IFindReplaceTargetExtension4) target).setSelection(selectedRegions.toArray(IRegion[]::new));
		}

		return selectCount;
	}

	/**
	 * Returns the position of the specified search string, or <code>-1</code> if
	 * the string can not be found when searching using the given options.
	 *
	 * @param findString    the string to search for
	 * @param startPosition the position at which to start the search
	 * @return the occurrence of the find string following the options or
	 *         <code>-1</code> if nothing found
	 * @since 3.0
	 */
	@Override
	public int findIndex(String findString, int startPosition) {

		if (isForwardSearch()) {
			int index = findAndSelect(startPosition, findString);
			if (index == -1) {

				status = status.setWarning(true);

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

			status = status.setWarning(true);

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
	 * @return the position of the specified string, or -1 if the string has not
	 *         been found
	 * @since 3.0
	 */
	@Override
	public int findAndSelect(int offset, String findString) {
		if (target instanceof IFindReplaceTargetExtension3)
			return ((IFindReplaceTargetExtension3) target).findAndSelect(offset, findString, isForwardSearch(),
					isCaseSensitiveSearch(), isWholeWordSearch(), isRegexSearch());
		return target.findAndSelect(offset, findString, isForwardSearch(), isCaseSensitiveSearch(),
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
	@Override
	public Point replaceSelection(String replaceString, boolean regExReplace) {
		if (target instanceof IFindReplaceTargetExtension3)
			((IFindReplaceTargetExtension3) target).replaceSelection(replaceString, regExReplace);
		else
			target.replaceSelection(replaceString);

		return target.getSelection();
	}

	/**
	 * Returns whether the specified search string can be found using the given
	 * options.
	 *
	 * @param findString    the string to search for
	 * @param forwardSearch the direction of the search
	 * @return <code>true</code> if the search string can be found using the given
	 *         options
	 *
	 * @since 3.0
	 */
	@Override
	public boolean findNext(String findString, boolean forwardSearch) {

		if (target == null)
			return false;

		Point r = null;
		if (isIncrementalSearch())
			r = incrementalBaseLocation;
		else
			r = target.getSelection();

		int findReplacePosition = r.x;
		if (forwardSearch && !nextReplactionOperationNeedsFindOperationFirst || !forwardSearch && nextReplactionOperationNeedsFindOperationFirst)
			findReplacePosition += r.y;

		nextReplactionOperationNeedsFindOperationFirst = false;

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

	/**
	 * Replaces the selection and jumps to the next occurrence of findString
	 * instantly. If another operation annotated that we need to select the
	 * occurrence of findString first before replacing, this method does so. (eg,
	 * after replacing once, we automatically perform <code> findAndSelect </code>
	 * once before being able to replace again).
	 */
	@Override
	public boolean performReplaceAndFind(String findString, String replaceString) {
		if (getCurrentSelection() != findString) {
			performSearch(findString);
		}
		if (performReplaceSelection(replaceString)) {
			performSearch(findString);
			return true;
		}
		return false;
	}

	@Override
	public boolean performSelectAndReplace(String findString, String replaceString) {
		if (nextReplactionOperationNeedsFindOperationFirst)
			performSearch(findString);
		return performReplaceSelection(replaceString);
	}

	@Override
	public void updateTarget(IFindReplaceTarget newTarget, boolean canEditTarget) {
		this.isTargetEditable = canEditTarget;
		nextReplactionOperationNeedsFindOperationFirst = true;

		if (this.target != newTarget) {
			if (newTarget != null && newTarget instanceof IFindReplaceTargetExtension)
				((IFindReplaceTargetExtension) newTarget).endSession();

			this.target = newTarget;
			if (newTarget != null)
				fIsTargetSupportingRegEx = newTarget instanceof IFindReplaceTargetExtension3;

			if (newTarget instanceof IFindReplaceTargetExtension) {
				((IFindReplaceTargetExtension) newTarget).beginSession();

				setGlobalSearch(true);
			}
		}

		initIncrementalBaseLocation();
	}

	@Override
	public void endSession() {
		if (target != null && target instanceof IFindReplaceTargetExtension)
			((IFindReplaceTargetExtension) target).endSession();

		target = null;
	}


	@Override
	public void deactivateScope() {
		if (target != null && (target instanceof IFindReplaceTargetExtension))
			((IFindReplaceTargetExtension) target).setScope(null);

		oldScope = null;
	}

	@Override
	public String getCurrentSelection() {
		if (target == null) {
			return null;
		}

		return target.getSelectionText();
	}

	/**
	 * Returns whether the target is editable.
	 *
	 * @return <code>true</code> if target is editable
	 */
	@Override
	public boolean isEditable() {
		boolean isEditable = (target == null ? false : target.isEditable());
		return isTargetEditable && isEditable;
	}

	/**
	 * @return Whether the target supports multiple selections
	 */
	@Override
	public boolean supportsMultiSelection() {
		return target instanceof IFindReplaceTargetExtension4;
	}

	/**
	 * @return Whether the target is available
	 */
	@Override
	public boolean isTargetAvailable() {
		return target != null;
	}

	/**
	 * Sets the given status message in the status line.
	 *
	 * @param error         <code>true</code> if it is an error
	 * @param dialogMessage the message to display in the dialog's status line
	 * @param editorMessage the message to display in the editor's status line
	 */
	private void statusMessage(boolean error, String dialogMessage, String editorMessage) {
		status = status.setError(error).setMessage(dialogMessage);

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

	/**
	 * Updates the search result after the Text was Modified. Used in combination
	 * with <code>setIncrementalSearch(true)</code>. This method specifically allows
	 * for "search-as-you-type"
	 *
	 * "Search-as-you-type" is not compatible with RegEx-search. This will
	 * initialize the base-location for search (if not initialized already) but will
	 * not update it, meaning that incrementally searching the same string twice in
	 * a row will always yield the same result, unless the Base location was
	 * modified (eg., by performing "find next")
	 *
	 * @param searchString the String that is to be searched
	 */
	@Override
	public void performIncrementalSearch(String searchString) {
		if (isIncrementalSearch() && !isRegExSearchAvailableAndChecked()) {
			if (searchString.equals("") && target != null) { //$NON-NLS-1$
				// empty selection at base location
				int offset = incrementalBaseLocation.x;

				if (isForwardSearch() && !nextReplactionOperationNeedsFindOperationFirst
						|| !isForwardSearch() && nextReplactionOperationNeedsFindOperationFirst)
					offset = offset + incrementalBaseLocation.y;

				nextReplactionOperationNeedsFindOperationFirst = false;
				findAndSelect(offset, ""); //$NON-NLS-1$
			} else {
				performSearch(false, searchString);
			}
		}
	}

}
