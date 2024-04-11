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
package org.eclipse.ui.internal.views.markers;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

public class ProblemViewImages {

	public ProblemViewImages() {
		declareImages();
	}

	static final String PREFIX_ELCL = "org.eclipse.ui.internal.views.markers.elcl."; //$NON-NLS-1$

	static final String PREFIX_OBJ = "org.eclipse.ui.internal.views.markers.obj."; //$NON-NLS-1$

	static final String ELCL_FIND_NEXT = PREFIX_ELCL + "select_next.png"; //$NON-NLS-1$

	static final String ELCL_FIND_PREV = PREFIX_ELCL + "select_prev.png"; //$NON-NLS-1$

	static final String OBJ_FIND_REGEX = PREFIX_OBJ + "regex_gear.gif"; //$NON-NLS-1$

	static final String OBJ_REPLACE = PREFIX_OBJ + "replace.png"; //$NON-NLS-1$

	static final String OBJ_REPLACE_ALL = PREFIX_OBJ + "replace_all.png"; //$NON-NLS-1$

	static final String OBJ_WHOLE_WORD = PREFIX_OBJ + "whole_word.png"; //$NON-NLS-1$

	static final String OBJ_CASE_SENSITIVE = PREFIX_OBJ + "case_sensitive.png"; //$NON-NLS-1$

	static final String OBJ_OPEN_REPLACE = PREFIX_OBJ + "open_replace.png"; //$NON-NLS-1$

	static final String OBJ_CLOSE_REPLACE = PREFIX_OBJ + "close_replace.png"; //$NON-NLS-1$

	static final String OBJ_SEARCH_ALL = PREFIX_OBJ + "search_all.png"; //$NON-NLS-1$

	static final String OBJ_SEARCH_IN_AREA = PREFIX_OBJ + "search_in_selection.png"; //$NON-NLS-1$

	private static String ICONS_PATH = "$nl$/icons/full/"; //$NON-NLS-1$

	// Use IPath and toOSString to build the names to ensure they have the
	// slashes correct
	private final static String ELCL = ICONS_PATH + "elcl16/"; //$NON-NLS-1$

	private final static String OBJ = ICONS_PATH + "obj16/"; //$NON-NLS-1$

	/**
	 * Declare all images
	 */
	private void declareImages() {
		declareRegistryImage(ELCL_FIND_NEXT, ELCL + "select_next.png"); //$NON-NLS-1$
		declareRegistryImage(ELCL_FIND_PREV, ELCL + "select_prev.png"); //$NON-NLS-1$
		declareRegistryImage(OBJ_FIND_REGEX, OBJ + "regex.png"); //$NON-NLS-1$
		declareRegistryImage(OBJ_REPLACE_ALL, OBJ + "replace_all.png"); //$NON-NLS-1$
		declareRegistryImage(OBJ_REPLACE, OBJ + "replace.png"); //$NON-NLS-1$
		declareRegistryImage(OBJ_WHOLE_WORD, OBJ + "whole_word.png"); //$NON-NLS-1$
		declareRegistryImage(OBJ_CASE_SENSITIVE, OBJ + "case_sensitive.png"); //$NON-NLS-1$
		declareRegistryImage(OBJ_OPEN_REPLACE, OBJ + "open_replace.png"); //$NON-NLS-1$
		declareRegistryImage(OBJ_CLOSE_REPLACE, OBJ + "close_replace.png"); //$NON-NLS-1$
		declareRegistryImage(OBJ_SEARCH_ALL, OBJ + "search_all.png"); //$NON-NLS-1$
		declareRegistryImage(OBJ_SEARCH_IN_AREA, OBJ + "search_in_area.png"); //$NON-NLS-1$
	}

	/**
	 * Declare an Image in the registry table.
	 *
	 * @param key  the key to use when registering the image
	 * @param path the path where the image can be found. This path is relative to
	 *             where this plugin class is found (i.e. typically the packages
	 *             directory)
	 */
	private final void declareRegistryImage(String key, String path) {
		ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
		Bundle bundle = Platform.getBundle("org.eclipse.ui.internal.views.markers"); //$NON-NLS-1$
		URL url = null;
		if (bundle != null) {
			url = FileLocator.find(bundle, IPath.fromOSString(path), null);
			desc = ImageDescriptor.createFromURL(url);
		}
		imageRegistry.put(key, desc);
	}

	/**
	 * Returns a new image registry for this plugin-in. The registry will be used to
	 * manage images which are frequently used by the plugin-in.
	 * <p>
	 * The default implementation of this method creates an empty registry.
	 * Subclasses may override this method if needed.
	 * </p>
	 *
	 * @return ImageRegistry the resulting registry.
	 * @see #getImageRegistry
	 */
	protected ImageRegistry createImageRegistry() {
		// Use display of workbench if available
		if (PlatformUI.isWorkbenchRunning()) {
			return new ImageRegistry(PlatformUI.getWorkbench().getDisplay());
		}

		// Otherwise use display of the current thread if available
		if (Display.getCurrent() != null) {
			return new ImageRegistry(Display.getCurrent());
		}

		// Invalid thread access if it is not the UI Thread
		// and the workbench is not created.
		throw new SWTError(SWT.ERROR_THREAD_INVALID_ACCESS);
	}

	private ImageRegistry imageRegistry;

	private ImageRegistry getNewImageRegistry() {
		if (imageRegistry == null) {
			imageRegistry = createImageRegistry();
		}
		return imageRegistry;
	}

	/**
	 * Returns the image managed under the given key in this registry.
	 *
	 * @param key the image's key
	 * @return the image managed under the given key
	 */
	public Image get(String key) {
		return getNewImageRegistry().get(key);
	}

	/**
	 * Returns the image descriptor for the given key in this registry.
	 *
	 * @param key the image's key
	 * @return the image descriptor for the given key
	 */
	public ImageDescriptor getDescriptor(String key) {
		return getNewImageRegistry().getDescriptor(key);
	}
}
