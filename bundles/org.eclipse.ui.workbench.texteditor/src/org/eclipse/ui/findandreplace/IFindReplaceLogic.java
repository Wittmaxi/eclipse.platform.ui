package org.eclipse.ui.findandreplace;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.IFindReplaceTarget;

interface IFindReplaceLogic {

	/**
	 * A global search in files searches in the whole file, as opposed to only
	 * searching in a part of the selection.
	 *
	 * @return boolean whether the search is global
	 */
	boolean isGlobalSearch();

	void setGlobalSearch(boolean globalSearch);

	boolean needsInitialFindBeforeReplace();

	void setNeedsInitialFindBeforeReplace(boolean needsInitialFindBeforeReplace);

	boolean isCaseSensitiveSearch();

	void setCaseSensitiveSearch(boolean caseSensitiveSearch);

	boolean isWrapSearch();

	void setWrapSearch(boolean wrapSearch);

	boolean isWholeWordSearchSetting();

	void setWholeWordSearchSetting(boolean wholeWordSearch);

	/**
	 * Returns the current status of FindReplaceLogic. Assumes that
	 * <code>resetStatus</code> was run before the last operation. The Status can
	 * inform about events such as an error happening, a warning happening (e.g.:
	 * the search-string wasn't found) and can including a message that the UI may
	 * choose to print.
	 *
	 * @return FindAndReplaceMessageStatus
	 */
	FindReplaceLogicMessageStatus getStatus();

	/**
	 * Call before running an operation of FindReplaceLogic. Resets the internal
	 * status.
	 */
	void resetStatus();

	boolean isForwardSearch();

	void setForwardSearch(boolean forwardSearch);

	boolean isTargetSupportingRegEx();

	void setIsTargetSupportingRegEx(boolean isTargetSupportingRegEx);

	boolean isIncrementalSearch();

	void setIncrementalSearch(boolean incrementalSearch);

	boolean isRegexSearch();

	void setRegexSearch(boolean regexSearch);

	boolean isRegExSearchAvailableAndChecked();

	/**
	 * Initializes the anchor used as starting point for incremental searching.
	 *
	 * @since 2.0
	 */
	void initIncrementalBaseLocation();

	/**
	 * Tells the dialog to perform searches only in the scope given by the actually
	 * selected lines.
	 *
	 * @param selectedLines <code>true</code> if selected lines should be used
	 * @since 2.0
	 */
	void useSelectedLines(boolean selectedLines);

	/**
	 * Replaces all occurrences of the user's findString with the replace string.
	 * Indicate to the user the number of replacements that occur.
	 *
	 * @param findString    The string that will be replaced
	 * @param replaceString The string that will replace the findString
	 * @param display       the display on which the busy feedback should be
	 *                      displayed. If the display is null, the Display for the
	 *                      current thread will be used. If there is no Display for
	 *                      the current thread,the runnable code will be executed
	 *                      and no busy feedback will be displayed.y
	 */
	void performReplaceAll(String findString, String replaceString, Display display);

	/**
	 * Selects all occurrences of findString.
	 *
	 * @param findString The String to find and select
	 * @param display    The UI's Display The UI's Display
	 */
	void performSelectAll(String findString, Display display);

	/**
	 * Validates the state of the find/replace target.
	 *
	 * @return <code>true</code> if target can be changed, <code>false</code>
	 *         otherwise
	 * @since 2.1
	 */
	boolean validateTargetState();

	/**
	 * Locates the user's findString in the target
	 *
	 * @param searchString the String to search for
	 * @return Whether the string was found in the target
	 *
	 * @since 3.7
	 */
	boolean performSearch(String searchString);

	/**
	 * Locates the user's findString in the text of the target.
	 *
	 * @param mustInitIncrementalBaseLocation <code>true</code> if base location
	 *                                        must be initialized
	 * @param findString                      the String to search for
	 * @return Whether the string was found in the target
	 * @since 3.0
	 */
	boolean performSearch(boolean mustInitIncrementalBaseLocation, String findString);

	/**
	 * @param findString    the String to select as part of the search
	 * @param caseSensitive Should case be respected in searching?
	 * @param wholeWord     Should the find String be searched as a whole word?
	 * @param regExSearch   Should the findString be interpreted as RegEx?
	 * @return The amount of selected Elements
	 */
	int selectAll(String findString, boolean caseSensitive, boolean wholeWord, boolean regExSearch);

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
	int findIndex(String findString, int startPosition);

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
	int findAndSelect(int offset, String findString);

	/**
	 * Replaces the selection and jumps to the next occurrence of findString
	 * instantly. Will not fail in case the selection is invalidated, eg. after a
	 * replace operation or after the target was updated
	 *
	 * @param findString    the string to replace
	 * @param replaceString the string to put in place of findString
	 * @return whether a replacement has been performed
	 */
	boolean performReplaceAndFind(String findString, String replaceString);

	boolean performSelectAndReplace(String findString, String replaceString);

	void updateTarget(IFindReplaceTarget newTarget, boolean canEditTarget);

	void endSession();

	void deactivateScope();

	String getCurrentSelection();

	/**
	 * Returns whether the target is editable.
	 *
	 * @return <code>true</code> if target is editable
	 */
	boolean isEditable();

	/**
	 * @return Whether the target supports multiple selections
	 */
	boolean supportsMultiSelection();

	/**
	 * @return Whether the target is available
	 */
	boolean isTargetAvailable();

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
	void performIncrementalSearch(String searchString);

	/*
	 * @return the Target that FindReplaceLogic operates on
	 */
	IFindReplaceTarget getTarget();

}