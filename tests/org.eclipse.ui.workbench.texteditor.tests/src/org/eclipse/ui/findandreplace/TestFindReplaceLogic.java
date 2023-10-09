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

package org.eclipse.ui.findandreplace;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;

public class TestFindReplaceLogic {

	Shell parentShell;
	IFindReplaceLogic findReplaceLogic;
	TextViewer textViewer;

	public void setupFindReplaceLogicObject() {
		setupFindReplaceLogicObject(null);
	}

	public void setupFindReplaceLogicObject(TextViewer target) {
		findReplaceLogic= new FindReplaceLogic();
		if (target != null) {
			findReplaceLogic.updateTarget(target.getFindReplaceTarget(), true);
		}
	}

	public void setupTextViewer(String contentText) {
		parentShell= new Shell();
		textViewer= new TextViewer(parentShell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		textViewer.setDocument(new Document(contentText));
		textViewer.getControl().setFocus();
	}

	@After
	public void tearDownObjects() {
		if (textViewer != null) {
			textViewer.getControl().dispose();
			textViewer= null;
		}
		parentShell.dispose();
	}

	@SuppressWarnings("boxing")
	private void expectStatusIs(FindReplaceLogicMessageStatus test, FindReplaceLogicMessageStatus expected) {
		assertEquals("Status message is not the expected message", test.getMessage(), expected.getMessage());
		assertEquals("Status warning flag is not the expected value", test.isWarning(), expected.isWarning());
		assertEquals("Status error flag is not the expected value", test.isError(), expected.isError());
		findReplaceLogic.resetStatus(); // sketchy side-effect of this function.
	}

	private final String baseReplaceAllTestCaseSetupString= "aaaa";
	/**
	 * Expects the TextViewer to contain a document with "aaaa"
	 */
	private void performReplaceAllBaseTestcases() {
		Display display= parentShell.getDisplay();

		findReplaceLogic.performReplaceAll("a", "b", display);
		assertEquals("Text was not correctly replaced", "bbbb", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("4 matches replaced", false, false));

		findReplaceLogic.performReplaceAll("b", "aa", display);
		assertEquals("Larger text was not correctly replaced", "aaaaaaaa", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("4 matches replaced", false, false));

		findReplaceLogic.performReplaceAll("b", "c", display);
		assertEquals("We expect nothing to be replaced", "aaaaaaaa", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("String not found", false, false));

		findReplaceLogic.performReplaceAll("aaaaaaaa", "d", display); // https://github.com/eclipse-platform/eclipse.platform.ui/issues/1203
		assertEquals("We expect everything to be replaced by a single character", "d", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("1 match replaced", false, false));

		findReplaceLogic.performReplaceAll("d", null, display);
		assertEquals("We expect everything to be deleted", "", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("1 match replaced", false, false));

		textViewer.getDocument().set("f");
		findReplaceLogic.performReplaceAll("f", "", display);
		assertEquals("We expect everything to be deleted", "", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("1 match replaced", false, false));

		findReplaceLogic.updateTarget(new MockFindReplaceTarget() {
			@Override
			public boolean isEditable() {
				return false;
			}
		}, false);
		findReplaceLogic.performReplaceAll("a", "b", display);
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("String not found", false, false));
	}

	@Test
	public void testPerformReplaceAllBackwards() {
		setupTextViewer(baseReplaceAllTestCaseSetupString);
		setupFindReplaceLogicObject(textViewer);

		performReplaceAllBaseTestcases();
	}

	@Test
	public void testPerformReplaceAllForwards() {
		setupTextViewer(baseReplaceAllTestCaseSetupString);
		setupFindReplaceLogicObject(textViewer);
		findReplaceLogic.setForwardSearch(true);

		performReplaceAllBaseTestcases();
	}

	@Test
	public void testPerformReplaceAllForwardRegEx() {
		setupTextViewer("hello@eclipse.com looks.almost@like_an_email");
		setupFindReplaceLogicObject(textViewer);
		findReplaceLogic.setRegexSearch(true);
		findReplaceLogic.setForwardSearch(true);

		findReplaceLogic.performReplaceAll(".+\\@.+\\.com", "", parentShell.getDisplay());
		assertEquals("We expect the regex-pattern-match to be replaced", " looks.almost@like_an_email", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("1 match replaced", false, false));

		findReplaceLogic.performReplaceAll("( looks.)|(like_)", "", parentShell.getDisplay());
		assertEquals("We expect the regex-pattern-matches (multiple) to be replaced", "almost@an_email", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("2 matches replaced", false, false));

		findReplaceLogic.performReplaceAll("[", "", parentShell.getDisplay());
		assertEquals("We expect nothing to be replaced since the pattern is invalid", "almost@an_email", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("Unclosed character class near index 0\r\n"
				+ "[\r\n"
				+ "^", true, false));
	}

	@Test
	public void testPerformReplaceAllForward() {
		setupTextViewer("hello@eclipse.com looks.almost@like_an_email");
		setupFindReplaceLogicObject(textViewer);
		findReplaceLogic.setRegexSearch(true);
		findReplaceLogic.setForwardSearch(true);

		findReplaceLogic.performReplaceAll(".+\\@.+\\.com", "", parentShell.getDisplay());
		assertEquals("We expect the regex-pattern-match to be replaced", " looks.almost@like_an_email", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("1 match replaced", false, false));

		findReplaceLogic.performReplaceAll("( looks.)|(like_)", "", parentShell.getDisplay());
		assertEquals("We expect the regex-pattern-matches (multiple) to be replaced", "almost@an_email", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("2 matches replaced", false, false));

		findReplaceLogic.performReplaceAll("[", "", parentShell.getDisplay());
		assertEquals("We expect nothing to be replaced since the pattern is invalid", "almost@an_email", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("Unclosed character class near index 0\r\n"
				+ "[\r\n"
				+ "^", true, false));
	}

	@Test
	public void testPerformSelectAndReplace() {
		setupTextViewer("Hello<replace>World<replace>!");
		setupFindReplaceLogicObject(textViewer);
		findReplaceLogic.setForwardSearch(true);

		findReplaceLogic.performSearch("<replace>"); // select first, then replace. We don't need to perform a second search
		findReplaceLogic.performSelectAndReplace("<replace>", " ");
		assertEquals("We expect the first occurence to be replaced", "Hello World<replace>!", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("", false, false));

		findReplaceLogic.performSelectAndReplace("<replace>", " "); // perform the search yourself and replace that automatically
		assertEquals("We expect the second occurence to be replaced", "Hello World !", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("", false, false));
	}

	@Test
	public void testPerformSelectAndReplaceBackward() {
		setupTextViewer("Hello<replace>World<replace>!");
		setupFindReplaceLogicObject(textViewer);
		findReplaceLogic.setForwardSearch(false);
		findReplaceLogic.setWrapSearch(true); // this only works if the search was wrapped

		findReplaceLogic.performSearch("<replace>"); // select first, then replace. We don't need to perform a second search
		findReplaceLogic.performSelectAndReplace("<replace>", " ");
		assertEquals("We expect the first occurence to be replaced", "Hello<replace>World !", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("Wrapped search", false, true));

		findReplaceLogic.performSelectAndReplace("<replace>", " "); // perform the search yourself and replace that automatically
		assertEquals("We expect the second occurence to be replaced", "Hello World !", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("", false, false));
	}

	@SuppressWarnings("boxing")
	@Test
	public void testPerformReplaceAndFind() {
		setupTextViewer("Hello<replace>World<replace>!");
		setupFindReplaceLogicObject(textViewer);
		findReplaceLogic.setForwardSearch(true);

		boolean status = findReplaceLogic.performReplaceAndFind("<replace>", " ");
		assertEquals("Status wasn't correctly returned", true, status);
		assertEquals("First occurence wasn't replaced", "Hello World<replace>!", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("", false, false));
		assertEquals("The next occurence was not automatically selected as expected", "<replace>", findReplaceLogic.getTarget().getSelectionText());

		status= findReplaceLogic.performReplaceAndFind("<replace>", " ");
		assertEquals("Status wasn't correctly returned", true, status);
		assertEquals("Text second occurence wasn't replaced", "Hello World !", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("String not found", false, true));

		status= findReplaceLogic.performReplaceAndFind("<replace>", " ");
		assertEquals("Status wasn't correctly returned", false, status);
		assertEquals("Text shouldn't have been changed", "Hello World !", textViewer.getDocument().get());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("String not found", false, true));
	}

	@Test
	public void testPerformSelectAllForward() {
		setupTextViewer("AbAbAbAb");
		setupFindReplaceLogicObject(textViewer);
		findReplaceLogic.setForwardSearch(true);

		findReplaceLogic.performSelectAll("b", parentShell.getDisplay());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("4 matches selected", false, false));
		// I don't have access to getAllSelectionPoints or similar (not yet implemented), so I cannot really test for correct behavior
		// related to https://github.com/eclipse-platform/eclipse.platform.ui/issues/1047

		findReplaceLogic.performSelectAll("AbAbAbAb", parentShell.getDisplay());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("1 match selected", false, false));
	}


	@Test
	public void testPerformSelectAllBackward() {
		setupTextViewer("AbAbAbAb");
		setupFindReplaceLogicObject(textViewer);
		findReplaceLogic.setForwardSearch(false);

		findReplaceLogic.performSelectAll("b", parentShell.getDisplay()); // https://github.com/eclipse-platform/eclipse.platform.ui/issues/1203 maybe related?
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("4 matches selected", false, false));
		// I don't have access to getAllSelectionPoints or similar (not yet implemented), so I cannot really test for correct behavior
		// related to https://github.com/eclipse-platform/eclipse.platform.ui/issues/1047

		findReplaceLogic.performSelectAll("AbAbAbAb", parentShell.getDisplay());
		expectStatusIs(findReplaceLogic.getStatus(), new FindReplaceLogicMessageStatus("1 match selected", false, false));
	}

}
