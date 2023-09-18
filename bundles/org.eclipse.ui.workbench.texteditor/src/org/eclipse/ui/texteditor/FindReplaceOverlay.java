package org.eclipse.ui.texteditor;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.text.IFindReplaceTarget;

import org.eclipse.ui.IWorkbenchPart;

/**
 * The better-integrated version of the FindReplaceDialog. Implemented after
 * <a href=
 * "https://github.com/eclipse-platform/eclipse.platform.ui/issues/1090">this
 * proposal</a>.
 *
 * @since 3.18
 */
public class FindReplaceOverlay extends FindReplaceUI {

	Timer timer = new Timer();
	private final long waitForUserDoneTypingDelay = 300;
	boolean currentlyActive = false;
	IWorkbenchPart targetPart;
	IFindReplaceTarget target;

	boolean dialogCreated = false;
	Composite container;
	Composite searchContainer;
	Text searchBar;
	Button findAllButton;
	Button searchUpButton;
	Button searchDownButton;
	Button wholeWordSearchButton;
	Button caseSensitiveSearchButton;
	Button regexSearchButton;
	Button openReplaceButton;
	Label searchLabel;

	Composite replaceContainer;
	Composite replaceOptions;
	Text replaceBar;
	Button replaceButton;
	Button replaceAllButton;

	protected FindReplaceOverlay(Shell parentShell) {
		super(parentShell);

	}

	IWorkbenchPart fAssignedPart;
	IFindReplaceTarget fTarget;

	Shell fShell;

	@Override
	public void updateTarget(IFindReplaceTarget target, boolean isEditable, boolean b) {
		fTarget = target;
		// TODO: FindReplacer
	}

	@Override
	public void updateTarget(IFindReplaceTarget target, boolean isEditable, boolean b, IWorkbenchPart activePart) {
		fTarget = target;
		fAssignedPart = activePart;
		updateLayout();
	}

	@Override
	protected boolean isResizable() {
		return false;
	}

	@Override
	public void create() {
		super.create();

		container = new Composite(getShell(), SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		GridData findContainerGD = new GridData();
		findContainerGD.grabExcessHorizontalSpace = true;
		findContainerGD.horizontalAlignment = GridData.FILL;
		findContainerGD.verticalAlignment = GridData.FILL;
		container.setLayoutData(findContainerGD);

		searchContainer = new Composite(container, SWT.BORDER); // The border looks funny. TODO
		GridData searchContainerGD = new GridData();
		searchContainerGD.grabExcessHorizontalSpace = true;
		searchContainerGD.horizontalAlignment = GridData.FILL;
		searchContainerGD.verticalAlignment = GridData.FILL;
		searchContainer.setLayoutData(searchContainerGD);
		searchContainer.setLayout(new GridLayout(8, false));
		searchBar = new Text(searchContainer, SWT.SINGLE);
		searchLabel = new Label(searchContainer, SWT.NONE);
		searchLabel.setText(" ");
		GridData searchBarGridData = new GridData();
		searchBarGridData.grabExcessHorizontalSpace = true;
		searchBarGridData.grabExcessVerticalSpace = true;
		searchBarGridData.horizontalAlignment = GridData.FILL;
		searchBarGridData.verticalAlignment = GridData.CENTER;
		searchBar.setLayoutData(searchBarGridData);
		searchBar.forceFocus();
		searchBar.selectAll();
		searchUpButton = new Button(searchContainer, SWT.TOGGLE);
		searchUpButton.setText(" ⬆️ "); //$NON-NLS-1$
		searchUpButton.setToolTipText("Search backward");
//		searchUpButton.addSelectionListener(new SelectionListener() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				searchUpButton.setSelection(true);
//				searchDownButton.setSelection(false);
//				// findReplacer.setForwardSearch(false);
//				search();
//			}
//
//			@Override
//			public void widgetDefaultSelected(SelectionEvent e) {
//				// TODO Auto-generated method stub
//
//			}
//		});
		searchDownButton = new Button(searchContainer, SWT.TOGGLE);
		searchDownButton.setSelection(true); // by default, search down
		searchDownButton.setText(" ⬇️ "); //$NON-NLS-1$
		searchDownButton.setToolTipText("Search forward");
//		searchDownButton.addSelectionListener(new SelectionListener() {
//
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				searchUpButton.setSelection(false);
//				searchDownButton.setSelection(true);
//				findReplacer.setForwardSearch(true);
//				search();
//			}
//
//			@Override
//			public void widgetDefaultSelected(SelectionEvent e) {
//				// TODO Auto-generated method stub
//
//			}
//		});
		wholeWordSearchButton = new Button(searchContainer, SWT.TOGGLE);
		wholeWordSearchButton.setText(" ♊️ ");
		wholeWordSearchButton.setToolTipText("Only find in whole words");
//		wholeWordSearchButton.addSelectionListener(new SelectionListener() {
//
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				findReplacer.setWholeWord(wholeWordSearchButton.getSelection());
//			}
//
//			@Override
//			public void widgetDefaultSelected(SelectionEvent e) {
//				// TODO Auto-generated method stub
//
//			}
//		});
		caseSensitiveSearchButton = new Button(searchContainer, SWT.TOGGLE);
		caseSensitiveSearchButton.setText(" 🔠 ");
		caseSensitiveSearchButton.setToolTipText("Match case");
//		caseSensitiveSearchButton.addSelectionListener(new SelectionListener() {
//
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				findReplacer.setCaseSensitive(caseSensitiveSearchButton.getSelection());
//			}
//
//			@Override
//			public void widgetDefaultSelected(SelectionEvent e) {
//				// TODO Auto-generated method stub
//
//			}
//		});
		regexSearchButton = new Button(searchContainer, SWT.TOGGLE);
		regexSearchButton.setText(" ⚙️ "); //$NON-NLS-1$
		regexSearchButton.setToolTipText("Search for a regular expression"); //$NON-NLS-1$
//		regexSearchButton.addSelectionListener(new SelectionListener() {
//
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				findReplacer.setRegExSearch(regexSearchButton.getSelection());
//			}
//
//			@Override
//			public void widgetDefaultSelected(SelectionEvent e) {
//				// TODO Auto-generated method stub
//			}
//		});

//		if (fTarget.isEditable()) {
//			openReplaceButton = new Button(searchContainer, SWT.TOGGLE);
//			openReplaceButton.setText(" 🔄 "); //$NON-NLS-1$
////			openReplaceButton.addSelectionListener(new SelectionListener() {
////				@Override
////				public void widgetSelected(SelectionEvent e) {
////					if (openReplaceButton.getSelection()) {
////						createReplaceDialog();
////					} else {
////						hideReplace();
////					}
////				}
////
////				@Override
////				public void widgetDefaultSelected(SelectionEvent e) {
////					// TODO Auto-generated method stub
////
////				}
////			});
//		}

		// Problem: shift+8 ("(") will not be typed into the search Bar since it is
		// already a bound key. We need to override this behaviour FIXME
		// Problem also generalizes to other keybinds: Ctrl + backspace, ...
		// We need to convince the editor that we aren't writing to it.
		searchBar.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(final TraverseEvent event) {
				if (event.detail == SWT.TRAVERSE_RETURN) {
//					search();
				}
			}
		});
		searchBar.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				timer.cancel();
				timer = new Timer();
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						Display.getDefault().asyncExec(new Runnable() { // https://wiki.eclipse.org/FAQ_Why_do_I_get_an_invalid_thread_access_exception%3F
							@Override
							public void run() {
//								selectAll();
							}
						});
					}

				}, waitForUserDoneTypingDelay);
			}

		});
		searchBar.setMessage(" 🔎 Find"); //$NON-NLS-1$

//		setNormalTablist();
		container.layout();
//		((IFindReplaceTargetExtension5) targetPart).updateLayout();
		dialogCreated = true;

	}

	public void createReplaceDialog() {
		Composite parent = container;
		replaceContainer = new Composite(parent, SWT.BORDER);
		replaceContainer.setLayout(new GridLayout(3, false));
		GridData replaceContainerLayoutData = new GridData();
		replaceContainerLayoutData.grabExcessHorizontalSpace = true;
		replaceContainerLayoutData.horizontalAlignment = SWT.FILL;
		replaceContainer.setLayoutData(replaceContainerLayoutData);

		replaceBar = new Text(replaceContainer, SWT.SINGLE);
		GridData replaceBarData = new GridData();
		replaceBarData.grabExcessHorizontalSpace = true;
		replaceBarData.horizontalAlignment = SWT.FILL;
		replaceBar.setLayoutData(replaceBarData);
		replaceBar.setMessage(" 🔄 Replace"); //$NON-NLS-1$

		replaceOptions = new Composite(replaceContainer, SWT.NONE);
		replaceOptions.setLayout(new GridLayout(3, false));
		replaceButton = new Button(replaceOptions, SWT.PUSH);
		replaceButton.setText(" 🔂 "); //$NON-NLS-1$
		replaceButton.setToolTipText("Replace");
//		replaceButton.addSelectionListener(new SelectionListener() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				replaceNext();
//			}
//
//			@Override
//			public void widgetDefaultSelected(SelectionEvent e) {
//			}
//		});
		replaceAllButton = new Button(replaceOptions, SWT.PUSH);
		replaceAllButton.setText(" 🔁 "); //$NON-NLS-1$
		replaceAllButton.setToolTipText("Replace All");
//		replaceAllButton.addSelectionListener(new SelectionListener() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				replaceAll();
//			}
//
//			@Override
//			public void widgetDefaultSelected(SelectionEvent e) {
//				// TODO Auto-generated method stub
//
//			}
//		});
		replaceBar.forceFocus();

//		setReplaceTablist();
//		((IFindReplaceTargetExtension5) targetPart).updateLayout();
	}

	public void updateLayout() {
		System.out.println(fAssignedPart);
	}
}
