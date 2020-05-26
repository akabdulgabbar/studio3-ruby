/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.ruby.internal.core;

import com.aptana.ruby.core.IRubyElement;

public class RubyImport extends RubyElement
{

	private String fValue;

	public RubyImport(String value, int start, int end)
	{
		super(start, end);
		fValue = value;
	}

	@Override
	public short getNodeType()
	{
		return IRubyElement.IMPORT_DECLARATION;
	}

	@Override
	public String getName()
	{
		return fValue;
	}

	@Override
	public String toString()
	{
		return getName();
	}
}
