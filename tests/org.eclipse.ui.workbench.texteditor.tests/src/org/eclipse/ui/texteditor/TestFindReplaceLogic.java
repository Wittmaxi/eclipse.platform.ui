package org.eclipse.ui.texteditor;

import org.junit.After;
import org.junit.Test;

import org.eclipse.swt.SWT;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.TextViewer;

import org.eclipse.ui.PlatformUI;

public class TestFindReplaceLogic {

	IFindReplaceLogic findReplaceLogic;

	TextViewer textViewer;

	public void setupFindReplaceLogicObject() {
		setupFindReplaceLogicObject(null);
	}

	public void setupFindReplaceLogicObject(TextViewer target) {
		findReplaceLogic= new FindReplaceLogic();
		if (target != null) {
			findReplaceLogic.updateTarget((IFindReplaceTarget) target, true);
		}
	}

	public void setupTextViewer(String contentText) {
		textViewer= new TextViewer(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		textViewer.setDocument(new Document(contentText));
		textViewer.getControl().setFocus();
	}

	@After
	public void tearDownObjects() {
		if (textViewer != null) {
			textViewer.getControl().dispose();
			textViewer= null;
		}
	}

	/**
	 * The Whole-Word-Search is a bit more subtle, since we cannot search in whole words while
	 * searching in RegExes
	 */
	@Test
	public void testUseSelectedLines() {
		setupTextViewer("");
		setupFindReplaceLogicObject(textViewer);

	}

}
