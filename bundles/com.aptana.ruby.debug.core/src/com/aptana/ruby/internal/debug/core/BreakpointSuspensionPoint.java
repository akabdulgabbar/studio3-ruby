package com.aptana.ruby.internal.debug.core;

public class BreakpointSuspensionPoint extends SuspensionPoint
{
	public String toString()
	{
		return "Breakpoint at " + this.getPosition(); //$NON-NLS-1$
	}

	public boolean isBreakpoint()
	{
		return true;
	}

	public boolean isException()
	{
		return false;
	}

	public boolean isStep()
	{
		return false;
	}

}
