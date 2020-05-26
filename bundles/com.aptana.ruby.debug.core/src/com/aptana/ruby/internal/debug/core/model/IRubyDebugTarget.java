package com.aptana.ruby.internal.debug.core.model;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.model.IDebugTarget;

import com.aptana.ruby.internal.debug.core.RubyDebuggerProxy;
import com.aptana.ruby.internal.debug.core.SuspensionPoint;

public interface IRubyDebugTarget extends IDebugTarget
{
	public void suspensionOccurred(SuspensionPoint suspensionPoint);

	public void updateThreads();

	public void setRubyDebuggerProxy(RubyDebuggerProxy rubyDebuggerProxy);

	public int getPort();

	public String getHost();

	/**
	 * @deprecated Will be removed once code is cleaned up.
	 * @return
	 */
	public RubyDebuggerProxy getRubyDebuggerProxy();

	public IStatus load(String filename);

}
