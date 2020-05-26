/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.ruby.internal.core;

import com.aptana.parsing.ast.IParseNode;
import com.aptana.ruby.core.IImportContainer;
import com.aptana.ruby.core.IRubyElement;

public class RubyImportContainer extends RubyElement implements IImportContainer
{

	private static final String NAME = "require/load declarations"; //$NON-NLS-1$

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public short getNodeType()
	{
		return IRubyElement.IMPORT_CONTAINER;
	}

	@Override
	public int getStart()
	{
		if (getChildCount() == 0)
		{
			return super.getStart();
		}
		return getChild(0).getStartingOffset();
	}

	@Override
	public int getEnd()
	{
		int size = getChildCount();
		if (size == 0)
		{
			return super.getEnd();
		}
		return getChild(size - 1).getEndingOffset();
	}

	@Override
	public IParseNode getNodeAtOffset(int offset)
	{
		IParseNode[] children = getChildren();
		for (IParseNode child : children)
		{
			if (child.getStartingOffset() <= offset && offset <= child.getEndingOffset())
			{
				return child.getNodeAtOffset(offset);
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		return getName();
	}
}
