package org.eclipse.jface.text;

import java.util.List;

/**
 * @since 3.26
 */
public interface IFindReplaceTargetExtension6 {

	public boolean useCustomSearchContributions();

	public List<SearchContribution> getSearchContributions();

}
