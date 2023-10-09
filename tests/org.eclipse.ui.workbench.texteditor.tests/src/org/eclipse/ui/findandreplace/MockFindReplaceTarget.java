package org.eclipse.ui.findandreplace;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IFindReplaceTarget;

public class MockFindReplaceTarget implements IFindReplaceTarget {

	public MockFindReplaceTarget() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean canPerformFind() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int findAndSelect(int widgetOffset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Point getSelection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSelectionText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEditable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void replaceSelection(String text) {
		// TODO Auto-generated method stub

	}

}
