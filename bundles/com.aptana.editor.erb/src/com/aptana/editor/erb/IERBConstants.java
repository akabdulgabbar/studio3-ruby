/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.editor.erb;

/**
 * @author Max Stepanov
 * @author cwilliams
 */
public interface IERBConstants
{

	public String CONTENT_TYPE_HTML_ERB = "com.aptana.contenttype.html.erb"; //$NON-NLS-1$
	public String CONTENT_TYPE_XML_ERB = "com.aptana.contenttype.xml.erb"; //$NON-NLS-1$

	// Tags
	public static final String OPEN_INSERT_TAG = "<%="; //$NON-NLS-1$
	public static final String OPEN_EVALUATE_TAG = "<%"; //$NON-NLS-1$
	public static final String CLOSE_NO_NEWLINE_TAG = "-%>"; //$NON-NLS-1$
	public static final String CLOSE_W_NEWLINE_TAG = "%>"; //$NON-NLS-1$

	/**
	 * Scope names for RHTML scopes.
	 */
	public static final String EMBEDDED_CSS_SCOPE = "source.css.embedded.html"; //$NON-NLS-1$
	public static final String EMBEDDED_JS_SCOPE = "source.js.embedded.html"; //$NON-NLS-1$
	public static final String EMBEDDED_RUBY_SCOPE = "source.ruby.rails.embedded.html"; //$NON-NLS-1$
	public static final String TOPLEVEL_RHTML_SCOPE = "text.html.ruby"; //$NON-NLS-1$
	public static final String EMBEDDED_RUBY_TAG_SCOPE = "source.erb.embedded.html"; //$NON-NLS-1$
	public static final String EMBEDDED_RUBY_TRANSITION_SCOPE = "punctuation.section.embedded.ruby"; //$NON-NLS-1$

	/**
	 * Scope names for RXML scopes
	 */
	public static final String TOPLEVEL_RXML_SCOPE = "text.xml.ruby"; //$NON-NLS-1$
	public static final String EMBEDDED_RUBY_RXML_SCOPE = "source.erb.embedded.xml"; //$NON-NLS-1$
}
