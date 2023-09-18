package org.eclipse.ui.texteditor;

/**
 * @since 3.18
 */
public class FindReplaceStatus {
	private boolean error;
	private String message;
	private boolean beep;

	public void resetStatus() {
		error = false;
		beep = false;
		setMessage(""); //$NON-NLS-1$
	}

	public boolean shouldBeep() {
		return beep;
	}

	public void doBeep() {
		setBeep(true);
	}

	public void setBeep(boolean beep) {
		this.beep = beep;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

}