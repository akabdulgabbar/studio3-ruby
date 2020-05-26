/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package com.aptana.ruby.internal.debug.core.launching;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

import com.aptana.core.ShellExecutable;
import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.ResourceUtil;
import com.aptana.core.util.StringUtil;
import com.aptana.ruby.debug.core.RubyDebugCorePlugin;
import com.aptana.ruby.debug.core.launching.IRubyLaunchConfigurationConstants;
import com.aptana.ruby.internal.debug.core.RubyDebuggerProxy;
import com.aptana.ruby.internal.debug.core.model.RubyDebugTarget;
import com.aptana.ruby.internal.debug.core.model.RubyProcessingException;
import com.aptana.ruby.launching.RubyLaunchingPlugin;

/**
 * Launches Ruby program on a Ruby interpreter
 */
public class RubyDebuggerLaunchDelegate extends LaunchConfigurationDelegate
{

	private static final String RDEBUG_IDE = "rdebug-ide"; //$NON-NLS-1$
	private static final String DEBUGGER_PORT_SWITCH = "--port"; //$NON-NLS-1$
	/**
	 * Switch/arguments that tells ruby/debugger that we're done passing switches/arguments to it.
	 */
	private static final String END_OF_ARGUMENTS_DELIMETER = "--"; //$NON-NLS-1$

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration,
	 * java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException
	{
		List<String> commandList = new ArrayList<String>();
		// Ruby binary
		IPath rubyExecutablePath = rubyExecutable(configuration);
		commandList.add(rubyExecutablePath.toOSString());
		// Arguments to ruby
		commandList.addAll(interpreterArguments(rubyExecutablePath, configuration));

		// Set up debugger
		String host = configuration.getAttribute(IRubyLaunchConfigurationConstants.ATTR_REMOTE_HOST,
				IRubyLaunchConfigurationConstants.DEFAULT_REMOTE_HOST);
		int port = -1;
		if (mode.equals(ILaunchManager.DEBUG_MODE))
		{
			// TODO Grab port from configuration?
			port = findFreePort();
			if (port == -1)
			{
				abort(Messages.RubyDebuggerLaunchDelegate_0, null);
			}
			commandList.addAll(debugArguments(rubyExecutablePath, host, port, configuration));
		}

		IPath workingDir = getWorkingDirectory(configuration);

		// File to run
		// if the file to launch is "Rakefile", we actually need to run "rake" on the parent
		String fileToLaunch = fileToLaunch(configuration);
		if (fileToLaunch.equals(RubyLaunchingPlugin.RAKEFILE)
				|| fileToLaunch.endsWith(File.separator + RubyLaunchingPlugin.RAKEFILE))
		{
			IPath rakeFilePath = Path.fromOSString(fileToLaunch);
			IPath parent = rakeFilePath.removeLastSegments(1);

			IPath rakePath = RubyLaunchingPlugin.getRakePath(parent);
			String rakePathString = (rakePath == null) ? RubyLaunchingPlugin.RAKE : rakePath.toOSString();
			commandList.add(rakePathString);
			workingDir = parent;
		}
		else
		{
			commandList.add(fileToLaunch);
		}

		// Args to file
		commandList.addAll(programArguments(configuration));

		// Now actually launch the process!
		Process process = DebugPlugin.exec(commandList.toArray(new String[commandList.size()]),
				(workingDir == null) ? null : workingDir.toFile(), getEnvironment(configuration));
		// FIXME Build a label from args?
		String label = commandList.get(0);

		// Set process type to "ruby" so our linetracker hyperlink stuff works
		Map<String, String> map = new HashMap<String, String>();
		map.put(IProcess.ATTR_PROCESS_TYPE, IRubyLaunchConfigurationConstants.PROCESS_TYPE);

		IProcess p = DebugPlugin.newProcess(launch, process, label, map);
		if (mode.equals(ILaunchManager.DEBUG_MODE))
		{
			RubyDebugTarget target = new RubyDebugTarget(launch, host, port);
			target.setProcess(p);
			RubyDebuggerProxy proxy = new RubyDebuggerProxy(target, true);
			try
			{
				proxy.start();
				launch.addDebugTarget(target);
			}
			catch (RubyProcessingException e)
			{
				RubyDebugCorePlugin.log(e);
				target.terminate();
			}
			catch (IOException e)
			{
				RubyDebugCorePlugin.log(e);
				target.terminate();
			}
		}
	}

	private Collection<? extends String> programArguments(ILaunchConfiguration configuration) throws CoreException
	{
		List<String> commandList = new ArrayList<String>();
		String programArgs = configuration.getAttribute(IRubyLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
				(String) null);
		if (programArgs != null)
		{
			for (String arg : DebugPlugin.parseArguments(programArgs))
			{
				commandList.add(arg);
			}
		}
		return commandList;
	}

	private String fileToLaunch(ILaunchConfiguration configuration) throws CoreException
	{
		String program = configuration.getAttribute(IRubyLaunchConfigurationConstants.ATTR_FILE_NAME, (String) null);
		if (program == null)
		{
			abort(Messages.RubyDebuggerLaunchDelegate_1, null);
		}
		// check for absolute path
		File file = new File(program);
		if (file.exists())
			return program;

		// check for relative to workspace root
		IFile iFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(program));
		if (iFile == null || !iFile.exists())
		{
			abort(MessageFormat.format(Messages.RubyDebuggerLaunchDelegate_2, program), null);
		}
		// TODO What about checking relative to working dir?
		return iFile.getLocation().toOSString();
	}

	private Collection<? extends String> debugArguments(IPath rubyExecutablePath, String host, int port,
			ILaunchConfiguration configuration) throws CoreException
	{
		IPath workingDir = getWorkingDirectory(configuration);
		List<String> commandList = new ArrayList<String>();
		IPath rdebug = RubyLaunchingPlugin.getBinaryScriptPath(RDEBUG_IDE, getRDebugIDELocations(rubyExecutablePath),
				workingDir);
		// FIXME What if user is using RVM? We need to respect which version of rdebug-ide we need to use!
		if (rdebug == null)
		{
			abort(Messages.RubyDebuggerLaunchDelegate_3, null);
		}
		commandList.add(rdebug.toOSString());
		commandList.add(DEBUGGER_PORT_SWITCH);
		commandList.add(Integer.toString(port));
		commandList.add(END_OF_ARGUMENTS_DELIMETER);
		return commandList;
	}

	private List<IPath> getRDebugIDELocations(IPath rubyExecutablePath)
	{
		List<IPath> locations = new ArrayList<IPath>();
		// check in bin dir alongside where our ruby exe is!
		if (rubyExecutablePath != null)
		{
			locations.add(rubyExecutablePath.removeLastSegments(1));
		}
		// TODO Check gem executable directory! (we need to get this from 'gem environment')
		locations.add(Path.fromOSString(System.getProperty("user.home")).append(".gem/ruby/1.8/bin")); //$NON-NLS-1$ //$NON-NLS-2$
		locations.add(Path.fromOSString(System.getProperty("user.home")).append(".gem/ruby/1.9/bin")); //$NON-NLS-1$ //$NON-NLS-2$
		locations.add(Path.fromOSString("/opt/local/bin")); //$NON-NLS-1$
		locations.add(Path.fromOSString("/usr/local/bin")); //$NON-NLS-1$
		locations.add(Path.fromOSString("/usr/bin")); //$NON-NLS-1$
		locations.add(Path.fromOSString("/bin")); //$NON-NLS-1$
		return locations;
	}

	private Collection<? extends String> interpreterArguments(IPath rubyExecutablePath,
			ILaunchConfiguration configuration) throws CoreException
	{
		List<String> arguments = new ArrayList<String>();
		// Add special VM args if we're under jruby!
		String rubyVersion = RubyLaunchingPlugin.getRubyVersion(rubyExecutablePath);
		if (rubyVersion != null && (rubyVersion.contains("jruby") || rubyVersion.contains("java"))) //$NON-NLS-1$ //$NON-NLS-2$
		{
			arguments.add("--debug"); //$NON-NLS-1$
			arguments.add("-X+O"); //$NON-NLS-1$
		}

		String interpreterArgs = configuration.getAttribute(IRubyLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
				(String) null);
		if (interpreterArgs != null)
		{
			String[] raw = DebugPlugin.parseArguments(interpreterArgs);
			for (int i = 0; i < raw.length; i++)
			{
				String arg = raw[i];
				if ((arg.equals("-e") || arg.equals("-X") || arg.equals("-F")) && (raw.length > (i + 1))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{
					arguments.add(arg + " " + raw[i + 1]); //$NON-NLS-1$
					i++;
				}
				else
				{
					arguments.add(arg);
				}
			}
		}

		URL url = FileLocator.find(RubyLaunchingPlugin.getDefault().getBundle(),
				Path.fromPortableString("ruby/sync.rb"), null); //$NON-NLS-1$
		try
		{
			File file = ResourceUtil.resourcePathToFile(url);
			String filePath = file.getParent();
			arguments.add("-I"); //$NON-NLS-1$
			arguments.add(filePath);
			arguments.add("-rsync"); //$NON-NLS-1$
		}
		catch (Exception e)
		{
			IdeLog.logError(RubyLaunchingPlugin.getDefault(), e);
		}
		arguments.add(END_OF_ARGUMENTS_DELIMETER);
		return arguments;
	}

	protected IPath rubyExecutable(ILaunchConfiguration configuration) throws CoreException
	{
		IPath path = RubyLaunchingPlugin.rubyExecutablePath(getWorkingDirectory(configuration));
		// TODO If we can't find one, should we just try plain "ruby"?
		if (path == null)
		{
			abort(Messages.RubyDebuggerLaunchDelegate_13, null);
		}
		if (!path.toFile().exists())
		{
			abort(MessageFormat.format(Messages.RubyDebuggerLaunchDelegate_14, path), null);
		}
		return path;
	}

	private String[] getEnvironment(ILaunchConfiguration configuration) throws CoreException
	{
		Map<String, String> env = new HashMap<String, String>();
		// Only grab shell environment if we're not on Windows. This isn't running inside cygwin!
		if (!Platform.OS_WIN32.equals(Platform.getOS()))
		{
			env.putAll(ShellExecutable.getEnvironment(getWorkingDirectory(configuration)));
		}
		String[] envp = DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);
		if (envp != null)
		{
			for (String envstring : envp)
			{
				if (envstring.indexOf((int) '\u0000') != -1)
				{
					envstring = envstring.replaceFirst("\u0000.*", StringUtil.EMPTY); //$NON-NLS-1$
				}
				int eqlsign = envstring.indexOf('=');
				if (eqlsign != -1)
				{
					env.put(envstring.substring(0, eqlsign), envstring.substring(eqlsign + 1));
				}
			}
		}
		if (env.isEmpty())
		{
			return null;
		}
		List<String> list = new ArrayList<String>();
		for (Map.Entry<String, String> entry : env.entrySet())
		{
			list.add(entry.getKey() + "=" + entry.getValue()); //$NON-NLS-1$
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Return a File pointing at the working directory for the launch. Return null if no value specified, or specified
	 * location does not exist or is not a directory.
	 * 
	 * @param configuration
	 * @return
	 * @throws CoreException
	 */
	protected IPath getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException
	{
		// TODO Cache once we grab it once?
		String workingDirVal = configuration.getAttribute(IRubyLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY,
				(String) null);
		if (workingDirVal == null)
		{
			return null;
		}
		IPath workingDirectory = Path.fromOSString(workingDirVal);
		if (!workingDirectory.toFile().isDirectory())
		{
			IdeLog.logError(RubyDebugCorePlugin.getDefault(),
					"Specified working directory does not appear to be a valid directory: " //$NON-NLS-1$
							+ workingDirVal);
			return null;
		}
		return workingDirectory;
	}

	/**
	 * Throws an exception with a new status containing the given message and optional exception.
	 * 
	 * @param message
	 *            error message
	 * @param e
	 *            underlying exception
	 * @throws CoreException
	 */
	private void abort(String message, Throwable e) throws CoreException
	{
		throw new CoreException(new Status(IStatus.ERROR, IRubyLaunchConfigurationConstants.ID_RUBY_DEBUG_MODEL, 0,
				message, e));
	}

	/**
	 * Returns a free port number on localhost, or -1 if unable to find a free port.
	 * 
	 * @return a free port number on localhost, or -1 if unable to find a free port
	 */
	private static int findFreePort()
	{
		ServerSocket socket = null;
		try
		{
			socket = new ServerSocket(0);
			return socket.getLocalPort();
		}
		catch (IOException e)
		{
		}
		finally
		{
			if (socket != null)
			{
				try
				{
					socket.close();
				}
				catch (IOException e)
				{
				}
			}
		}
		return -1;
	}
}
