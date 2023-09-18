package org.eclipse.ui.texteditor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.jface.text.IFindReplaceTarget;

import org.eclipse.ui.IWorkbenchPart;

/**
 * All Find/Replace UI's extend this class.
 *
 * @since 3.18
 */
public abstract class FindReplaceUI extends Dialog {

	public FindReplaceUI(Shell parentShell) {
		super(parentShell);
	}

	public abstract void updateTarget(IFindReplaceTarget target, boolean isEditable, boolean b);

	public abstract void updateTarget(IFindReplaceTarget target, boolean isEditable, boolean b,
			IWorkbenchPart activePart);

	@Override
	protected abstract boolean isResizable();

	@Override
	public void create() {
		super.create();
	}

	@Override
	public Shell getParentShell() {
		return super.getParentShell();
	}


}
