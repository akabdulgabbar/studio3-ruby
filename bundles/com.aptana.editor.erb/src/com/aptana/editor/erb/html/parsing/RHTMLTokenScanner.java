/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.erb.html.parsing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;

import com.aptana.editor.erb.IERBConstants;
import com.aptana.editor.erb.parsing.lexer.ERBTokens;
import com.aptana.editor.html.parsing.HTMLTokenScanner;

public class RHTMLTokenScanner extends HTMLTokenScanner
{

	private static final String[] RUBY_START = { IERBConstants.OPEN_EVALUATE_TAG, IERBConstants.OPEN_INSERT_TAG };
	private static final String[] RUBY_END = new String[] { IERBConstants.CLOSE_NO_NEWLINE_TAG,
			IERBConstants.CLOSE_W_NEWLINE_TAG };

	public RHTMLTokenScanner()
	{
		List<IRule> rules = new ArrayList<IRule>();
		// adds rules for finding the ruby start and end sequences
		WordRule wordRule = new WordRule(new RubyStartDetector(), Token.UNDEFINED);
		IToken token = createToken(getERBTokenName(ERBTokens.RUBY));
		for (String word : RUBY_START)
		{
			wordRule.addWord(word, token);
		}
		rules.add(wordRule);
		wordRule = new WordRule(new RubyEndDetector(), Token.UNDEFINED);
		token = createToken(getERBTokenName(ERBTokens.RUBY_END));
		for (String word : RUBY_END)
		{
			wordRule.addWord(word, token);
		}
		rules.add(wordRule);

		for (IRule rule : fRules)
		{
			rules.add(rule);
		}

		setRules(rules.toArray(new IRule[rules.size()]));
	}

	private static String getERBTokenName(short token)
	{
		return ERBTokens.getTokenName(token);
	}

	private static final class RubyStartDetector implements IWordDetector
	{

		public boolean isWordPart(char c)
		{
			switch (c)
			{
				case '<':
				case '%':
				case '=':
					return true;
			}
			return false;
		}

		public boolean isWordStart(char c)
		{
			return c == '<';
		}
	}

	private static final class RubyEndDetector implements IWordDetector
	{

		public boolean isWordPart(char c)
		{
			switch (c)
			{
				case '-':
				case '%':
				case '>':
					return true;
			}
			return false;
		}

		public boolean isWordStart(char c)
		{
			return c == '-' || c == '%';
		}
	}
}
