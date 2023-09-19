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

/**
 * Use by FindAndReplace to signal warnings, errors and messages to
 * FindAndReplaceDialog and FindAndReplaceOverlay.
 *
 * @since 3.17
 */
class FindAndReplaceMessageStatus {
	private boolean error;
	private boolean warning;
	private String message;

	public void resetStatus() {
		error = false;
		warning = false;
		setMessage(""); //$NON-NLS-1$
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

	public void setWarning(boolean warning) {
		this.warning = warning;
	}

	public boolean isWarning() {
		return warning;
	}

	@Override
	public FindAndReplaceMessageStatus clone() {
		FindAndReplaceMessageStatus ret = new FindAndReplaceMessageStatus();
		ret.setMessage(message);
		ret.setError(isError());
		ret.setWarning(isWarning());
		return ret;
	}

}