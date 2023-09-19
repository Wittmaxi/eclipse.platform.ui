/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     SAP SE, christian.georgi@sap.com - Bug 487357: Make find dialog content scrollable
 *     Pierre-Yves B., pyvesdev@gmail.com - Bug 121634: [find/replace] status bar must show the string being searched when "String Not Found"
 *******************************************************************************/
package org.eclipse.ui.texteditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.FrameworkUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.util.Util;

import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.internal.texteditor.SWTUtil;


/**
 * Find/Replace dialog. The dialog is opened on a particular
 * target but can be re-targeted. Internally used by the <code>FindReplaceAction</code>
 */
class FindReplaceDialog extends Dialog {

	private static final int CLOSE_BUTTON_ID = 101;
	private FindReplaceLogic findReplacer;

	/**
	 * Updates the find replace dialog on activation changes.
	 */
	class ActivationListener extends ShellAdapter {
		@Override
		public void shellActivated(ShellEvent e) {
			fActiveShell= (Shell)e.widget;
			updateButtonState();

			if (fGiveFocusToFindField && getShell() == fActiveShell && okToUse(fFindField))
				fFindField.setFocus();

		}

		@Override
		public void shellDeactivated(ShellEvent e) {
			fGiveFocusToFindField= false;

			storeSettings();

			fGlobalRadioButton.setSelection(true);
			fSelectedRangeRadioButton.setSelection(false);
			findReplacer.setGlobalSearch(false);

			findReplacer.deactivateScope();

			fActiveShell= null;
			updateButtonState();
		}
	}

	private final FindModifyListener fFindModifyListener = new FindModifyListener();

	/**
	 * Modify listener to update the search result in case of incremental search.
	 * @since 2.0
	 */
	private class FindModifyListener implements ModifyListener {

		// XXX: Workaround for Combo bug on Linux (see bug 404202 and bug 410603)
		private boolean fIgnoreNextEvent;
		private void ignoreNextEvent() {
			fIgnoreNextEvent = true;
		}

		@Override
		public void modifyText(ModifyEvent e) {

			// XXX: Workaround for Combo bug on Linux (see bug 404202 and bug 410603)
			if (fIgnoreNextEvent) {
				fIgnoreNextEvent = false;
				return;
			}

			findReplacer.updateSearchResultAfterTextWasModified(getFindString());

			updateButtonState(!findReplacer.isIncrementalSearch());
		}
	}

	/** The size of the dialogs search history. */
	private static final int HISTORY_SIZE= 15;

	private List<String> fFindHistory;
	private List<String> fReplaceHistory;

	private Shell fParentShell;
	private Shell fActiveShell;

	private final ActivationListener fActivationListener= new ActivationListener();

	private Label fReplaceLabel, fStatusLabel;
	private Button fForwardRadioButton, fGlobalRadioButton, fSelectedRangeRadioButton;
	private Button fCaseCheckBox, fWrapCheckBox, fWholeWordCheckBox, fIncrementalCheckBox;

	/**
	 * Checkbox for selecting whether the search string is a regular expression.
	 * @since 3.0
	 */
	private Button fIsRegExCheckBox;

	private Button fReplaceSelectionButton, fReplaceFindButton, fFindNextButton, fReplaceAllButton, fSelectAllButton;
	private Combo fFindField, fReplaceField;

	/**
	 * Find and replace command adapters.
	 * @since 3.3
	 */
	private ContentAssistCommandAdapter fContentAssistFindField, fContentAssistReplaceField;

	private Rectangle fDialogPositionInit;

	private IDialogSettings fDialogSettings;
	/**
	 * <code>true</code> if the find field should receive focus the next time the
	 * dialog is activated, <code>false</code> otherwise.
	 *
	 * @since 3.0
	 */
	private boolean fGiveFocusToFindField= true;

	/**
	 * Holds the mnemonic/button pairs for all buttons.
	 * @since 3.7
	 */
	private HashMap<Character, Button> fMnemonicButtonMap= new HashMap<>();


	/**
	 * Creates a new dialog with the given shell as parent.
	 * @param parentShell the parent shell
	 */
	public FindReplaceDialog(Shell parentShell) {
		super(parentShell);
		findReplacer = new FindReplaceLogic();

		fParentShell= null;

		fDialogPositionInit= null;
		fFindHistory= new ArrayList<>(HISTORY_SIZE);
		fReplaceHistory= new ArrayList<>(HISTORY_SIZE);

		readConfiguration();
		updateButtonState();

		setShellStyle(getShellStyle() ^ SWT.APPLICATION_MODAL | SWT.MODELESS);
		setBlockOnOpen(false);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	/**
	 * Returns this dialog's parent shell.
	 * @return the dialog's parent shell
	 */
	@Override
	public Shell getParentShell() {
		return super.getParentShell();
	}


	/**
	 * Returns <code>true</code> if control can be used.
	 *
	 * @param control the control to be checked
	 * @return <code>true</code> if control can be used
	 */
	private boolean okToUse(Control control) {
		return control != null && !control.isDisposed();
	}

	@Override
	public void create() {

		super.create();

		Shell shell= getShell();
		shell.addShellListener(fActivationListener);

		// set help context
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IAbstractTextEditorHelpContextIds.FIND_REPLACE_DIALOG);

		// fill in combo contents
		fFindField.removeModifyListener(fFindModifyListener);
		updateCombo(fFindField, fFindHistory);
		fFindField.addModifyListener(fFindModifyListener);
		updateCombo(fReplaceField, fReplaceHistory);

		// get find string
		initFindStringFromSelection();

		// set dialog position
		if (fDialogPositionInit != null)
			shell.setBounds(fDialogPositionInit);

		shell.setText(EditorMessages.FindReplace_title);
		// shell.setImage(null);
	}

	/**
	 * Create the button section of the find/replace dialog.
	 *
	 * @param parent the parent composite
	 * @return the button section
	 */
	private Composite createButtonSection(Composite parent) {

		Composite panel= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= -2; // this is intended
		panel.setLayout(layout);

		fFindNextButton= makeButton(panel, EditorMessages.FindReplace_FindNextButton_label, 102, true, new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (findReplacer.isIncrementalSearch() && !findReplacer.isRegExSearchAvailableAndChecked())
					findReplacer.initIncrementalBaseLocation();

				findReplacer.setNeedsInitialFindBeforeReplace(false);
				boolean somethingFound = findReplacer.performSearch(getFindString());
				writeSelection();
				updateButtonState(!somethingFound);
				updateFindHistory();
				evaluateFindReplacerStatus();
			}
		});
		setGridData(fFindNextButton, SWT.FILL, true, SWT.FILL, false);

		fSelectAllButton = makeButton(panel, EditorMessages.FindReplace_SelectAllButton_label, 106, false,
				new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						findReplacer.performSelectAll(getFindString(), fActiveShell.getDisplay());
						writeSelection();
						updateButtonState();
						updateFindAndReplaceHistory();
						evaluateFindReplacerStatus();
					}
				});
		setGridData(fSelectAllButton, SWT.FILL, true, SWT.FILL, false);

		new Label(panel, SWT.NONE); // filler

		fReplaceFindButton= makeButton(panel, EditorMessages.FindReplace_ReplaceFindButton_label, 103, false, new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (findReplacer.performFindFirstThenReplaceInASecondStep(getFindString(), getReplaceString())) {
					writeSelection();
				}
				updateButtonState();
				updateFindAndReplaceHistory();
				evaluateFindReplacerStatus();
			}
		});
		setGridData(fReplaceFindButton, SWT.FILL, false, SWT.FILL, false);

		fReplaceSelectionButton= makeButton(panel, EditorMessages.FindReplace_ReplaceSelectionButton_label, 104, false, new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (findReplacer.performSelectAndReplace(getFindString(), getReplaceString())) {
					writeSelection();
				}
				updateButtonState();
				updateFindAndReplaceHistory();
				evaluateFindReplacerStatus();
			}
		});
		setGridData(fReplaceSelectionButton, SWT.FILL, false, SWT.FILL, false);

		fReplaceAllButton= makeButton(panel, EditorMessages.FindReplace_ReplaceAllButton_label, 105, false, new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				findReplacer.performReplaceAll(getFindString(), getReplaceString(), fActiveShell.getDisplay());
				writeSelection();
				updateButtonState();
				updateFindAndReplaceHistory();
				evaluateFindReplacerStatus();
			}
		});
		setGridData(fReplaceAllButton, SWT.FILL, true, SWT.FILL, false);

		// Make the all the buttons the same size as the Remove Selection button.
		fReplaceAllButton.setEnabled(findReplacer.isEditable());

		return panel;
	}

	/**
	 * Creates the options configuration section of the find replace dialog.
	 *
	 * @param parent the parent composite
	 * @return the options configuration section
	 */
	private Composite createConfigPanel(Composite parent) {

		Composite panel= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.makeColumnsEqualWidth= true;
		panel.setLayout(layout);

		Composite directionGroup= createDirectionGroup(panel);
		setGridData(directionGroup, SWT.FILL, true, SWT.FILL, false);

		Composite scopeGroup= createScopeGroup(panel);
		setGridData(scopeGroup, SWT.FILL, true, SWT.FILL, false);

		Composite optionsGroup= createOptionsGroup(panel);
		setGridData(optionsGroup, SWT.FILL, true, SWT.FILL, true);
		((GridData)optionsGroup.getLayoutData()).horizontalSpan= 2;

		return panel;
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite panel= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.makeColumnsEqualWidth= true;
		panel.setLayout(layout);
		setGridData(panel, SWT.FILL, true, SWT.FILL, true);

		ScrolledComposite scrolled= new ScrolledComposite(panel, SWT.V_SCROLL);
		setGridData(scrolled, SWT.FILL, true, SWT.FILL, true);

		Composite mainArea = new Composite(scrolled, SWT.NONE);
		setGridData(mainArea, SWT.FILL, true, SWT.FILL, true);
		mainArea.setLayout(new GridLayout(1, true));

		Composite inputPanel= createInputPanel(mainArea);
		setGridData(inputPanel, SWT.FILL, true, SWT.TOP, false);

		Composite configPanel= createConfigPanel(mainArea);
		setGridData(configPanel, SWT.FILL, true, SWT.TOP, true);

		scrolled.setContent(mainArea);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		scrolled.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				scrolled.setMinHeight(mainArea.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
			}
		});

		Composite buttonPanelB= createButtonSection(panel);
		setGridData(buttonPanelB, SWT.RIGHT, true, SWT.BOTTOM, false);

		Composite statusBar= createStatusAndCloseButton(panel);
		setGridData(statusBar, SWT.FILL, true, SWT.BOTTOM, false);

		panel.addTraverseListener(e -> {
			if (e.detail == SWT.TRAVERSE_RETURN) {
				if (!Util.isMac()) {
					Control controlWithFocus= getShell().getDisplay().getFocusControl();
					if (controlWithFocus != null && (controlWithFocus.getStyle() & SWT.PUSH) == SWT.PUSH)
						return;
				}
				Event event1= new Event();
				event1.type= SWT.Selection;
				event1.stateMask= e.stateMask;
				fFindNextButton.notifyListeners(SWT.Selection, event1);
				e.doit= false;
			}
			else if (e.detail == SWT.TRAVERSE_MNEMONIC) {
				Character mnemonic= Character.valueOf(Character.toLowerCase(e.character));
				if (fMnemonicButtonMap.containsKey(mnemonic)) {
					Button button= fMnemonicButtonMap.get(mnemonic);
					if ((fFindField.isFocusControl() || fReplaceField.isFocusControl() || (button.getStyle() & SWT.PUSH) != 0)
							&& button.isEnabled()) {
						Event event2= new Event();
						event2.type= SWT.Selection;
						event2.stateMask= e.stateMask;
						if ((button.getStyle() & SWT.RADIO) != 0) {
							Composite buttonParent= button.getParent();
							if (buttonParent != null) {
								for (Control child : buttonParent.getChildren())
									((Button)child).setSelection(false);
							}
							button.setSelection(true);
						} else {
							button.setSelection(!button.getSelection());
						}
						button.notifyListeners(SWT.Selection, event2);
						e.detail= SWT.TRAVERSE_NONE;
						e.doit= true;
					}
				}
			}
		});

		updateButtonState();

		applyDialogFont(panel);

		return panel;
	}

	private void setContentAssistsEnablement(boolean enable) {
		fContentAssistFindField.setEnabled(enable);
		fContentAssistReplaceField.setEnabled(enable);
	}

	/**
	 * Creates the direction defining part of the options defining section
	 * of the find replace dialog.
	 *
	 * @param parent the parent composite
	 * @return the direction defining part
	 */
	private Composite createDirectionGroup(Composite parent) {

		Composite panel= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		panel.setLayout(layout);

		Group group= new Group(panel, SWT.SHADOW_ETCHED_IN);
		group.setText(EditorMessages.FindReplace_Direction);
		GridLayout groupLayout= new GridLayout();
		group.setLayout(groupLayout);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		SelectionListener selectionListener= new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (findReplacer.isIncrementalSearch() && !findReplacer.isRegExSearchAvailableAndChecked())
					findReplacer.initIncrementalBaseLocation();

				findReplacer.setForwardSearch(fForwardRadioButton.getSelection());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Do nothing
			}
		};

		fForwardRadioButton= new Button(group, SWT.RADIO | SWT.LEFT);
		fForwardRadioButton.setText(EditorMessages.FindReplace_ForwardRadioButton_label);
		setGridData(fForwardRadioButton, SWT.LEFT, false, SWT.CENTER, false);
		fForwardRadioButton.addSelectionListener(selectionListener);
		storeButtonWithMnemonicInMap(fForwardRadioButton);

		Button backwardRadioButton= new Button(group, SWT.RADIO | SWT.LEFT);
		backwardRadioButton.setText(EditorMessages.FindReplace_BackwardRadioButton_label);
		setGridData(backwardRadioButton, SWT.LEFT, false, SWT.CENTER, false);
		backwardRadioButton.addSelectionListener(selectionListener);
		storeButtonWithMnemonicInMap(backwardRadioButton);

		findReplacer.setForwardSearch(true); // search forward by default
		backwardRadioButton.setSelection(!findReplacer.isForwardSearch());
		fForwardRadioButton.setSelection(findReplacer.isForwardSearch());

		return panel;
	}

	/**
	 * Creates the scope defining part of the find replace dialog.
	 *
	 * @param parent the parent composite
	 * @return the scope defining part
	 * @since 2.0
	 */
	private Composite createScopeGroup(Composite parent) {

		Composite panel= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		panel.setLayout(layout);

		Group group= new Group(panel, SWT.SHADOW_ETCHED_IN);
		group.setText(EditorMessages.FindReplace_Scope);
		GridLayout groupLayout= new GridLayout();
		group.setLayout(groupLayout);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		fGlobalRadioButton= new Button(group, SWT.RADIO | SWT.LEFT);
		fGlobalRadioButton.setText(EditorMessages.FindReplace_GlobalRadioButton_label);
		setGridData(fGlobalRadioButton, SWT.LEFT, false, SWT.CENTER, false);
		fGlobalRadioButton.setSelection(findReplacer.isGlobalSearch());
		fGlobalRadioButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!fGlobalRadioButton.getSelection() || findReplacer.isGlobalSearch())
					return;
				findReplacer.setGlobalSearch(true);
				findReplacer.useSelectedLines(false);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		storeButtonWithMnemonicInMap(fGlobalRadioButton);

		fSelectedRangeRadioButton= new Button(group, SWT.RADIO | SWT.LEFT);
		fSelectedRangeRadioButton.setText(EditorMessages.FindReplace_SelectedRangeRadioButton_label);
		setGridData(fSelectedRangeRadioButton, SWT.LEFT, false, SWT.CENTER, false);
		fSelectedRangeRadioButton.setSelection(!findReplacer.isGlobalSearch());
		fSelectedRangeRadioButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!fSelectedRangeRadioButton.getSelection() || !findReplacer.isGlobalSearch())
					return;
				findReplacer.setGlobalSearch(false);
				findReplacer.useSelectedLines(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		storeButtonWithMnemonicInMap(fSelectedRangeRadioButton);

		return panel;
	}


	/**
	 * Creates the panel where the user specifies the text to search
	 * for and the optional replacement text.
	 *
	 * @param parent the parent composite
	 * @return the input panel
	 */
	private Composite createInputPanel(Composite parent) {

		ModifyListener listener= e -> updateButtonState();

		Composite panel= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		panel.setLayout(layout);

		Label findLabel= new Label(panel, SWT.LEFT);
		findLabel.setText(EditorMessages.FindReplace_Find_label);
		setGridData(findLabel, SWT.LEFT, false, SWT.CENTER, false);

		// Create the find content assist field
		ComboContentAdapter contentAdapter= new ComboContentAdapter();
		FindReplaceDocumentAdapterContentProposalProvider findProposer= new FindReplaceDocumentAdapterContentProposalProvider(true);
		fFindField= new Combo(panel, SWT.DROP_DOWN | SWT.BORDER);
		fContentAssistFindField= new ContentAssistCommandAdapter(
				fFindField,
				contentAdapter,
				findProposer,
				ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS,
				new char[0],
				true);
		setGridData(fFindField, SWT.FILL, true, SWT.CENTER, false);
		addDecorationMargin(fFindField);
		fFindField.addModifyListener(fFindModifyListener);

		fReplaceLabel= new Label(panel, SWT.LEFT);
		fReplaceLabel.setText(EditorMessages.FindReplace_Replace_label);
		setGridData(fReplaceLabel, SWT.LEFT, false, SWT.CENTER, false);

		// Create the replace content assist field
		FindReplaceDocumentAdapterContentProposalProvider replaceProposer= new FindReplaceDocumentAdapterContentProposalProvider(false);
		fReplaceField= new Combo(panel, SWT.DROP_DOWN | SWT.BORDER);
		fContentAssistReplaceField= new ContentAssistCommandAdapter(
				fReplaceField,
				contentAdapter, replaceProposer,
				ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS,
				new char[0],
				true);
		setGridData(fReplaceField, SWT.FILL, true, SWT.CENTER, false);
		addDecorationMargin(fReplaceField);
		fReplaceField.addModifyListener(listener);

		return panel;
	}

	/**
	 * Creates the functional options part of the options defining
	 * section of the find replace dialog.
	 *
	 * @param parent the parent composite
	 * @return the options group
	 */
	private Composite createOptionsGroup(Composite parent) {

		Composite panel= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		panel.setLayout(layout);

		Group group= new Group(panel, SWT.SHADOW_NONE);
		group.setText(EditorMessages.FindReplace_Options);
		GridLayout groupLayout= new GridLayout();
		groupLayout.numColumns= 2;
		groupLayout.makeColumnsEqualWidth= true;
		group.setLayout(groupLayout);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		SelectionListener selectionListener= new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setupFindReplacer();
				storeSettings();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};

		fCaseCheckBox= new Button(group, SWT.CHECK | SWT.LEFT);
		fCaseCheckBox.setText(EditorMessages.FindReplace_CaseCheckBox_label);
		setGridData(fCaseCheckBox, SWT.LEFT, false, SWT.CENTER, false);
		fCaseCheckBox.setSelection(findReplacer.isCaseSensitiveSearch());
		fCaseCheckBox.addSelectionListener(selectionListener);
		storeButtonWithMnemonicInMap(fCaseCheckBox);

		fWrapCheckBox= new Button(group, SWT.CHECK | SWT.LEFT);
		fWrapCheckBox.setText(EditorMessages.FindReplace_WrapCheckBox_label);
		setGridData(fWrapCheckBox, SWT.LEFT, false, SWT.CENTER, false);
		fWrapCheckBox.setSelection(findReplacer.isWrapSearch());
		fWrapCheckBox.addSelectionListener(selectionListener);
		storeButtonWithMnemonicInMap(fWrapCheckBox);

		fWholeWordCheckBox= new Button(group, SWT.CHECK | SWT.LEFT);
		fWholeWordCheckBox.setText(EditorMessages.FindReplace_WholeWordCheckBox_label);
		setGridData(fWholeWordCheckBox, SWT.LEFT, false, SWT.CENTER, false);
		fWholeWordCheckBox.setSelection(findReplacer.isWholeWordSearchSetting());
		fWholeWordCheckBox.addSelectionListener(selectionListener);
		storeButtonWithMnemonicInMap(fWholeWordCheckBox);

		fIncrementalCheckBox= new Button(group, SWT.CHECK | SWT.LEFT);
		fIncrementalCheckBox.setText(EditorMessages.FindReplace_IncrementalCheckBox_label);
		setGridData(fIncrementalCheckBox, SWT.LEFT, false, SWT.CENTER, false);
		fIncrementalCheckBox.setSelection(findReplacer.isIncrementalSearch());
		fIncrementalCheckBox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (findReplacer.isIncrementalSearch() && !findReplacer.isRegexSearch())
					findReplacer.initIncrementalBaseLocation();

				setupFindReplacer();
				storeSettings();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		storeButtonWithMnemonicInMap(fIncrementalCheckBox);

		fIsRegExCheckBox= new Button(group, SWT.CHECK | SWT.LEFT);
		fIsRegExCheckBox.setText(EditorMessages.FindReplace_RegExCheckbox_label);
		setGridData(fIsRegExCheckBox, SWT.LEFT, false, SWT.CENTER, false);
		((GridData)fIsRegExCheckBox.getLayoutData()).horizontalSpan= 2;
		fIsRegExCheckBox.setSelection(findReplacer.isRegexSearch());
		fIsRegExCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean newState= fIsRegExCheckBox.getSelection();
				fIncrementalCheckBox.setEnabled(!newState);
				setupFindReplacer();
				storeSettings();
				updateButtonState();
				setContentAssistsEnablement(newState);
			}
		});
		storeButtonWithMnemonicInMap(fIsRegExCheckBox);
		fWholeWordCheckBox.setEnabled(!findReplacer.isRegExSearchAvailableAndChecked());
		fWholeWordCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateButtonState();
			}
		});
		fIncrementalCheckBox.setEnabled(!findReplacer.isRegExSearchAvailableAndChecked());
		return panel;
	}

	/**
	 * Creates the status and close section of the dialog.
	 *
	 * @param parent the parent composite
	 * @return the status and close button
	 */
	private Composite createStatusAndCloseButton(Composite parent) {

		Composite panel= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		panel.setLayout(layout);

		fStatusLabel= new Label(panel, SWT.LEFT);
		setGridData(fStatusLabel, SWT.FILL, true, SWT.CENTER, false);

		String label= EditorMessages.FindReplace_CloseButton_label;
		Button closeButton = createButton(panel, CLOSE_BUTTON_ID, label, false);
		setGridData(closeButton, SWT.RIGHT, false, SWT.BOTTOM, false);

		return panel;
	}

	/*
	 * @see Dialog#buttonPressed
	 */
	@Override
	protected void buttonPressed(int buttonID) {
		if (buttonID == 101)
			close();
	}



	// ------- action invocation ---------------------------------------


	/**
	 * Returns the dialog's boundaries.
	 * @return the dialog's boundaries
	 */
	private Rectangle getDialogBoundaries() {
		if (okToUse(getShell()))
			return getShell().getBounds();
		return fDialogPositionInit;
	}

	/**
	 * Returns the dialog's history.
	 * @return the dialog's history
	 */
	private List<String> getFindHistory() {
		return fFindHistory;
	}

	// ------- accessors ---------------------------------------

	/**
	 * Retrieves the string to search for from the appropriate text input field and returns it.
	 * @return the search string
	 */
	private String getFindString() {
		if (okToUse(fFindField)) {
			return fFindField.getText();
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Returns the dialog's replace history.
	 * @return the dialog's replace history
	 */
	private List<String> getReplaceHistory() {
		return fReplaceHistory;
	}

	/**
	 * Retrieves the replacement string from the appropriate text input field and returns it.
	 * @return the replacement string
	 */
	private String getReplaceString() {
		if (okToUse(fReplaceField)) {
			return fReplaceField.getText();
		}
		return ""; //$NON-NLS-1$
	}

	// ------- init / close ---------------------------------------

	/**
	 * Returns the first line of the given selection.
	 *
	 * @param selection the selection
	 * @return the first line of the selection
	 */
	private String getFirstLine(String selection) {
		if (!selection.isEmpty()) {
			int delimiterOffset = TextUtilities.nextDelimiter(selection, 0).delimiterIndex;
			if (delimiterOffset > 0)
				return selection.substring(0, delimiterOffset);
			else if (delimiterOffset == -1)
				return selection;
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jface.window.Window#close()
	 */
	@Override
	public boolean close() {
		handleDialogClose();
		return super.close();
	}

	/**
	 * Removes focus changed listener from browser and stores settings for re-open.
	 */
	private void handleDialogClose() {

		// remove listeners
		if (okToUse(fFindField)) {
			fFindField.removeModifyListener(fFindModifyListener);
		}

		if (fParentShell != null) {
			fParentShell.removeShellListener(fActivationListener);
			fParentShell= null;
		}

		getShell().removeShellListener(fActivationListener);

		// store current settings in case of re-open
		storeSettings();

		findReplacer.endSession();

		// prevent leaks
		fActiveShell= null;
	}

	/**
	 * Writes the current selection to the dialog settings.
	 * @since 3.0
	 */
	private void writeSelection() {
		String selection = findReplacer.getCurrentSelection();
		if (selection == null)
			return;

		IDialogSettings s= getDialogSettings();
		s.put("selection", selection); //$NON-NLS-1$
	}

	/**
	 * Stores the current state in the dialog settings.
	 * @since 2.0
	 */
	private void storeSettings() {
		fDialogPositionInit= getDialogBoundaries();

		writeConfiguration();
	}

	/**
	 * Initializes the string to search for and the appropriate
	 * text in the Find field based on the selection found in the
	 * action's target.
	 */
	private void initFindStringFromSelection() {
		String fullSelection = findReplacer.getCurrentSelection();
		if (fullSelection != null && okToUse(fFindField)) {
			boolean isRegEx = findReplacer.isRegExSearchAvailableAndChecked();
			fFindField.removeModifyListener(fFindModifyListener);
			if (!fullSelection.isEmpty()) {
				String firstLine= getFirstLine(fullSelection);
				String pattern= isRegEx ? FindReplaceDocumentAdapter.escapeForRegExPattern(fullSelection) : firstLine;
				fFindField.setText(pattern);
				if (!firstLine.equals(fullSelection)) {
					// multiple lines selected
					findReplacer.useSelectedLines(true);
					fGlobalRadioButton.setSelection(false);
					fSelectedRangeRadioButton.setSelection(true);
				}
			} else {
				if ("".equals(fFindField.getText())) { //$NON-NLS-1$
					if (!fFindHistory.isEmpty())
						fFindField.setText(fFindHistory.get(0));
					else
						fFindField.setText(""); //$NON-NLS-1$
				}
			}
			fFindField.setSelection(new Point(0, fFindField.getText().length()));
			fFindField.addModifyListener(fFindModifyListener);
		}
	}


	/**
	 * Creates a button.
	 * @param parent the parent control
	 * @param label the button label
	 * @param id the button id
	 * @param dfltButton is this button the default button
	 * @param listener a button pressed listener
	 * @return the new button
	 */
	private Button makeButton(Composite parent, String label, int id, boolean dfltButton, SelectionListener listener) {
		Button button= createButton(parent, id, label, dfltButton);
		button.addSelectionListener(listener);
		storeButtonWithMnemonicInMap(button);
		return button;
	}

	/**
	 * Stores the button and its mnemonic in {@link #fMnemonicButtonMap}.
	 *
	 * @param button button whose mnemonic has to be stored
	 * @since 3.7
	 */
	private void storeButtonWithMnemonicInMap(Button button) {
		char mnemonic= LegacyActionTools.extractMnemonic(button.getText());
		if (mnemonic != LegacyActionTools.MNEMONIC_NONE)
			fMnemonicButtonMap.put(Character.valueOf(Character.toLowerCase(mnemonic)), button);
	}


	// ------- UI creation ---------------------------------------

	/**
	 * Attaches the given layout specification to the <code>component</code>.
	 *
	 * @param component the component
	 * @param horizontalAlignment horizontal alignment
	 * @param grabExcessHorizontalSpace grab excess horizontal space
	 * @param verticalAlignment vertical alignment
	 * @param grabExcessVerticalSpace grab excess vertical space
	 */
	private void setGridData(Control component, int horizontalAlignment, boolean grabExcessHorizontalSpace, int verticalAlignment, boolean grabExcessVerticalSpace) {
		GridData gd;
		if (component instanceof Button && (((Button)component).getStyle() & SWT.PUSH) != 0) {
			SWTUtil.setButtonDimensionHint((Button)component);
			gd= (GridData)component.getLayoutData();
		} else {
			gd= new GridData();
			component.setLayoutData(gd);
			gd.horizontalAlignment= horizontalAlignment;
			gd.grabExcessHorizontalSpace= grabExcessHorizontalSpace;
		}
		gd.verticalAlignment= verticalAlignment;
		gd.grabExcessVerticalSpace= grabExcessVerticalSpace;
	}

	/**
	 * Adds enough space in the control's layout data margin for the content assist
	 * decoration.
	 * @param control the control that needs a margin
	 * @since 3.3
	 */
	private void addDecorationMargin(Control control) {
		Object layoutData= control.getLayoutData();
		if (!(layoutData instanceof GridData))
			return;
		GridData gd= (GridData)layoutData;
		FieldDecoration dec= FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		gd.horizontalIndent= dec.getImage().getBounds().width;
	}

	/**
	 * Updates the enabled state of the buttons.
	 */
	private void updateButtonState() {
		updateButtonState(false);
	}

	/**
	 * Updates the enabled state of the buttons.
	 *
	 * @param disableReplace <code>true</code> if replace button must be disabled
	 * @since 3.0
	 */
	private void updateButtonState(boolean disableReplace) {
		if (okToUse(getShell()) && okToUse(fFindNextButton)) {

			boolean hasActiveSelection = false;
			String selection = findReplacer.getCurrentSelection();
			if (selection != null)
				hasActiveSelection = !selection.isEmpty();

			boolean enable = findReplacer.isTargetAvailable()
					&& (fActiveShell == fParentShell || fActiveShell == getShell());
			String str= getFindString();
			boolean findString= str != null && !str.isEmpty();

			fWholeWordCheckBox.setEnabled(isWord(str) && !findReplacer.isRegExSearchAvailableAndChecked());
			fFindNextButton.setEnabled(enable && findString);
			fSelectAllButton.setEnabled(enable && findString && findReplacer.supportsMultiSelection());
			fReplaceSelectionButton
					.setEnabled(!disableReplace && enable && findReplacer.isEditable() && hasActiveSelection
							&& (!findReplacer.needsInitialFindBeforeReplace()
									|| !findReplacer.isRegExSearchAvailableAndChecked()));
			fReplaceFindButton
					.setEnabled(!disableReplace && enable && findReplacer.isEditable() && findString
							&& hasActiveSelection
							&& (!findReplacer.needsInitialFindBeforeReplace()
									|| !findReplacer.isRegExSearchAvailableAndChecked()));
			fReplaceAllButton.setEnabled(enable && findReplacer.isEditable() && findString);
		}
	}

	/**
	 * Tests whether each character in the given string is a letter.
	 *
	 * @param str the string to check
	 * @return <code>true</code> if the given string is a word
	 * @since 3.0
	 */
	private boolean isWord(String str) {
		if (str == null || str.isEmpty())
			return false;

		for (int i= 0; i < str.length(); i++) {
			if (!Character.isJavaIdentifierPart(str.charAt(i)))
				return false;
		}
		return true;
	}

	/**
	 * Updates the given combo with the given content.
	 * @param combo combo to be updated
	 * @param content to be put into the combo
	 */
	private void updateCombo(Combo combo, List<String> content) {
		combo.removeAll();
		for (String element : content) {
			combo.add(element.toString());
		}
	}

	// ------- open / reopen ---------------------------------------

	/**
	 * Called after executed find/replace action to update the history.
	 */
	private void updateFindAndReplaceHistory() {
		updateFindHistory();
		if (okToUse(fReplaceField)) {
			updateHistory(fReplaceField, fReplaceHistory);
		}

	}

	/**
	 * Called after executed find action to update the history.
	 */
	private void updateFindHistory() {
		if (okToUse(fFindField)) {
			fFindField.removeModifyListener(fFindModifyListener);

			// XXX: Workaround for Combo bug on Linux (see bug 404202 and bug 410603)
			if (Util.isLinux())
				fFindModifyListener.ignoreNextEvent();

			updateHistory(fFindField, fFindHistory);
			fFindField.addModifyListener(fFindModifyListener);
		}
	}

	/**
	 * Updates the combo with the history.
	 * @param combo to be updated
	 * @param history to be put into the combo
	 */
	private void updateHistory(Combo combo, List<String> history) {
		String findString= combo.getText();
		int index= history.indexOf(findString);
		if (index != 0) {
			if (index != -1) {
				history.remove(index);
			}
			history.add(0, findString);
			Point selection= combo.getSelection();
			updateCombo(combo, history);
			combo.setText(findString);
			combo.setSelection(selection);
		}
	}

	/**
	 * Updates this dialog because of a different target.
	 * @param target the new target
	 * @param isTargetEditable <code>true</code> if the new target can be modified
	 * @param initializeFindString <code>true</code> if the find string of this dialog should be initialized based on the viewer's selection
	 * @since 2.0
	 */
	public void updateTarget(IFindReplaceTarget target, boolean isTargetEditable, boolean initializeFindString) {
		findReplacer.updateTarget(target, isTargetEditable);

		boolean globalSearch = findReplacer.isGlobalSearch();
		fGlobalRadioButton.setSelection(globalSearch);
		boolean useSelectedLines = !globalSearch;
		fSelectedRangeRadioButton.setSelection(useSelectedLines);
		findReplacer.useSelectedLines(useSelectedLines);

		if (okToUse(fIsRegExCheckBox))
			fIsRegExCheckBox.setEnabled(findReplacer.isTargetSupportingRegEx());

		if (okToUse(fWholeWordCheckBox))
			fWholeWordCheckBox.setEnabled(!findReplacer.isRegExSearchAvailableAndChecked());

		if (okToUse(fIncrementalCheckBox))
			fIncrementalCheckBox.setEnabled(!findReplacer.isRegExSearchAvailableAndChecked());

		if (okToUse(fReplaceLabel)) {
			fReplaceLabel.setEnabled(findReplacer.isEditable());
			fReplaceField.setEnabled(findReplacer.isEditable());
			if (initializeFindString) {
				initFindStringFromSelection();
				fGiveFocusToFindField = true;
			}
		}

		updateButtonState();

		setContentAssistsEnablement(findReplacer.isRegExSearchAvailableAndChecked());
	}

	/**
	 * Sets the parent shell of this dialog to be the given shell.
	 *
	 * @param shell the new parent shell
	 */
	@Override
	public void setParentShell(Shell shell) {
		if (shell != fParentShell) {

			if (fParentShell != null)
				fParentShell.removeShellListener(fActivationListener);

			fParentShell= shell;
			fParentShell.addShellListener(fActivationListener);
		}

		fActiveShell= shell;
	}


	//--------------- configuration handling --------------

	/**
	 * Returns the dialog settings object used to share state
	 * between several find/replace dialogs.
	 *
	 * @return the dialog settings to be used
	 */
	private IDialogSettings getDialogSettings() {
		IDialogSettings settings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(FindReplaceDialog.class)).getDialogSettings();
		fDialogSettings= settings.getSection(getClass().getName());
		if (fDialogSettings == null)
			fDialogSettings= settings.addNewSection(getClass().getName());
		return fDialogSettings;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		String sectionName= getClass().getName() + "_dialogBounds"; //$NON-NLS-1$
		IDialogSettings settings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(FindReplaceDialog.class)).getDialogSettings();
		IDialogSettings section= settings.getSection(sectionName);
		if (section == null)
			section= settings.addNewSection(sectionName);
		return section;
	}

	@Override
	protected int getDialogBoundsStrategy() {
		return DIALOG_PERSISTLOCATION | DIALOG_PERSISTSIZE;
	}

	/**
	 * Initializes itself from the dialog settings with the same state
	 * as at the previous invocation.
	 */
	private void readConfiguration() {
		IDialogSettings s= getDialogSettings();

		findReplacer.setWrapSearch(s.get("wrap") == null || s.getBoolean("wrap")); //$NON-NLS-1$ //$NON-NLS-2$
		findReplacer.setCaseSensitiveSearch(s.getBoolean("casesensitive")); //$NON-NLS-1$
		findReplacer.setWholeWordSearchSetting(s.getBoolean("wholeword")); //$NON-NLS-1$
		findReplacer.setIncrementalSearch(s.getBoolean("incremental")); //$NON-NLS-1$
		findReplacer.setRegexSearch(s.getBoolean("isRegEx")); //$NON-NLS-1$

		String[] findHistory= s.getArray("findhistory"); //$NON-NLS-1$
		if (findHistory != null) {
			List<String> history= getFindHistory();
			history.clear();
			Collections.addAll(history, findHistory);
		}

		String[] replaceHistory= s.getArray("replacehistory"); //$NON-NLS-1$
		if (replaceHistory != null) {
			List<String> history= getReplaceHistory();
			history.clear();
			Collections.addAll(history, replaceHistory);
		}
	}

	private void setupFindReplacer() {
		findReplacer.setWrapSearch(fWrapCheckBox.getSelection());
		findReplacer.setCaseSensitiveSearch(fCaseCheckBox.getSelection());
		findReplacer.setWholeWordSearchSetting(fWholeWordCheckBox.getSelection());
		findReplacer.setIncrementalSearch(fIncrementalCheckBox.getSelection());
		findReplacer.setRegexSearch(fIsRegExCheckBox.getSelection());
	}

	/**
	 * Stores its current configuration in the dialog store.
	 */
	private void writeConfiguration() {
		IDialogSettings s= getDialogSettings();

		s.put("wrap", findReplacer.isWrapSearch()); //$NON-NLS-1$
		s.put("casesensitive", findReplacer.isCaseSensitiveSearch()); //$NON-NLS-1$
		s.put("wholeword", findReplacer.isWholeWordSearchSetting()); //$NON-NLS-1$
		s.put("incremental", findReplacer.isIncrementalSearch()); //$NON-NLS-1$
		s.put("isRegEx", findReplacer.isRegexSearch()); //$NON-NLS-1$

		List<String> history= getFindHistory();
		String findString= getFindString();
		if (!findString.isEmpty())
			history.add(0, findString);
		writeHistory(history, s, "findhistory"); //$NON-NLS-1$

		history= getReplaceHistory();
		String replaceString= getReplaceString();
		if (!replaceString.isEmpty())
			history.add(0, replaceString);
		writeHistory(history, s, "replacehistory"); //$NON-NLS-1$
	}

	/**
	 * Writes the given history into the given dialog store.
	 *
	 * @param history the history
	 * @param settings the dialog settings
	 * @param sectionName the section name
	 * @since 3.2
	 */
	private void writeHistory(List<String> history, IDialogSettings settings, String sectionName) {
		int itemCount= history.size();
		Set<String> distinctItems= new HashSet<>(itemCount);
		for (int i= 0; i < itemCount; i++) {
			String item= history.get(i);
			if (distinctItems.contains(item)) {
				history.remove(i--);
				itemCount--;
			} else {
				distinctItems.add(item);
			}
		}

		while (history.size() > HISTORY_SIZE)
			history.remove(HISTORY_SIZE);

		String[] names= new String[history.size()];
		history.toArray(names);
		settings.put(sectionName, names);

	}

	private void evaluateFindReplacerStatus() {
		FindAndReplaceMessageStatus status = findReplacer.getStatus();

		String dialogMessage = status.getMessage();
		boolean error = status.isError();
		boolean warning = status.isWarning();

		fStatusLabel.setText(dialogMessage);

		if (error) {
			fStatusLabel.setForeground(JFaceColors.getErrorText(fStatusLabel.getDisplay()));
		}
		else {
			fStatusLabel.setForeground(null);
		}

		if ((error || warning) && okToUse(getShell())) {
			getShell().getDisplay().beep();
		}

		findReplacer.resetStatus();
	}
}
