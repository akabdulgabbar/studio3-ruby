/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.ruby.internal.core.build;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jrubyparser.CompatVersion;

import com.aptana.core.build.AbstractBuildParticipant;
import com.aptana.core.build.IProblem;
import com.aptana.core.build.ValidationItem;
import com.aptana.index.core.build.BuildContext;
import com.aptana.parsing.ast.IParseError;
import com.aptana.parsing.ast.IParseError.Severity;
import com.aptana.ruby.core.CollectingRubyWarnings;
import com.aptana.ruby.core.RubyParseState;
import com.aptana.ruby.launching.RubyLaunchingPlugin;

public class RubyValidator extends AbstractBuildParticipant
{
	private CompatVersion version;

	@Override
	public void buildStarting(IProject project, int kind, IProgressMonitor monitor)
	{
		super.buildStarting(project, kind, monitor);

		String rubyVersion = RubyLaunchingPlugin.getRubyVersionForProject(project);
		if (rubyVersion != null && rubyVersion.startsWith("ruby 1.9")) //$NON-NLS-1$
		{
			version = CompatVersion.RUBY1_9;
		}
		else if (rubyVersion != null && rubyVersion.startsWith("ruby 1.8")) //$NON-NLS-1$
		{
			version = CompatVersion.RUBY1_8;
		}
		else
		{
			version = CompatVersion.BOTH;
		}
	}

	@Override
	public void buildEnding(IProgressMonitor monitor)
	{
		version = CompatVersion.BOTH;
		super.buildEnding(monitor);
	}

	public void buildFile(BuildContext context, IProgressMonitor monitor)
	{
		String contents = null;
		String uri = context.getName();
		RubyParseState parseState = new RubyParseState();
		parseState.setFilename(uri);
		parseState.setStartingLineNumber(1);
		parseState.setVersion(version);
		try
		{
			contents = context.getContents();
			uri = context.getURI().toString();
			parseState.setWarnings(new CollectingRubyWarnings(uri));
			context.getAST(parseState);
		}
		catch (CoreException e)
		{
			// ignore, just forcing a parse
		}

		Collection<IProblem> problems = new ArrayList<IProblem>();
		for (IParseError parseError : context.getParseErrors())
		{
			int severity = (parseError.getSeverity() == Severity.ERROR) ? IMarker.SEVERITY_ERROR
					: IMarker.SEVERITY_WARNING;
			int line = -1;
			if (contents != null)
			{
				line = getLineNumber(parseError.getOffset(), contents);
			}
			problems.add(new ValidationItem(severity, parseError.getMessage(), parseError.getOffset(), parseError.getLength(), line, uri));
		}
		context.putProblems(IMarker.PROBLEM, problems);
	}

	public void deleteFile(BuildContext context, IProgressMonitor monitor)
	{
		context.removeProblems(IMarker.PROBLEM);
	}

}
