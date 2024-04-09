package org.eclipse.jface.text;

import org.eclipse.swt.graphics.Rectangle;

/**
 * @since 3.26
 */
public interface IFindReplaceTargetExtension5 {

	public Rectangle getFindReplaceOverlayBounds(int idealWidth, int idealHeight);

	public void attachMovementUpdater(Runnable callback);

	public void beginOverlaySession();

	public void endOverlaySession();

}
