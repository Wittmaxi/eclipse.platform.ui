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

/**
 * Use by FindReplaceLogic to signal warnings, errors and messages
 *
 * @since 3.17
 */
public final class FindReplaceLogicMessageStatus {
	private boolean error;
	private boolean warning;
	private String message;

	public FindReplaceLogicMessageStatus() {
		this("", false, false); //$NON-NLS-1$
	}

	/**
	 * Constructs a status object that can be used to communicate with an Interface
	 * that uses FindReplaceLogic
	 *
	 * @param message A user-readable message that can be displayed to inform the
	 *                user about the state of his Find/Replace operation
	 * @param error   Signals an Error. Can be expressed by an acoustic Signal and
	 *                using an error-font-color for the Message, for example.
	 * @param warning Signals a Warning. Can be expressed by an acoustic signal, for
	 *                example.
	 */
	public FindReplaceLogicMessageStatus(String message, boolean error, boolean warning) {
		this.message = message;
		this.error = error;
		this.warning = warning;
	}

	public String getMessage() {
		return message;
	}

	public boolean isError() {
		return error;
	}

	public boolean isWarning() {
		return warning;
	}

	public FindReplaceLogicMessageStatus setWarning(boolean newWarning) {
		return new FindReplaceLogicMessageStatus(this.message, this.error, newWarning);
	}

	public FindReplaceLogicMessageStatus setError(boolean newError) {
		return new FindReplaceLogicMessageStatus(this.message, newError, this.warning);
	}

	public FindReplaceLogicMessageStatus setMessage(String newMessage) {
		return new FindReplaceLogicMessageStatus(newMessage, this.error, this.warning);
	}

}