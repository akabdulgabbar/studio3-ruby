package com.aptana.ruby.internal.debug.core.parsing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.model.IVariable;
import org.xmlpull.v1.XmlPullParser;

import com.aptana.core.util.StringUtil;
import com.aptana.ruby.debug.core.RubyDebugCorePlugin;
import com.aptana.ruby.debug.core.model.IRubyStackFrame;
import com.aptana.ruby.debug.core.model.IRubyVariable;
import com.aptana.ruby.internal.debug.core.model.RubyProcessingException;
import com.aptana.ruby.internal.debug.core.model.RubyVariable;

@SuppressWarnings("nls")
public class VariableReader extends XmlStreamReader
{

	private IRubyStackFrame stackFrame;
	private IRubyVariable parent;
	private List<IVariable> variables;
	private String exceptionMessage;
	private String exceptionType;

	public VariableReader(XmlPullParser xpp)
	{
		super(xpp);
	}

	public VariableReader(AbstractReadStrategy readStrategy)
	{
		super(readStrategy);
	}

	public RubyVariable[] readVariables(IRubyVariable variable) throws RubyProcessingException
	{
		return readVariables(variable.getStackFrame(), variable);
	}

	public RubyVariable[] readVariables(IRubyStackFrame stackFrame) throws RubyProcessingException
	{
		return readVariables(stackFrame, null);
	}

	public synchronized RubyVariable[] readVariables(IRubyStackFrame stackFrame, IRubyVariable parent)
			throws RubyProcessingException
	{
		this.stackFrame = stackFrame;
		this.parent = parent;
		this.variables = new ArrayList<IVariable>();
		try
		{
			this.read();
		}
		catch (Exception ex)
		{
			RubyDebugCorePlugin.log(ex);
			return new RubyVariable[0];
		}
		if (exceptionMessage != null)
		{
			throw new RubyProcessingException(exceptionType, exceptionMessage);
		}
		else if (isWaitTimeExpired())
		{
			throw new RubyProcessingException("Timeout: Could not read result.");
		}
		return variables.toArray(new RubyVariable[variables.size()]);
	}

	protected boolean processStartElement(XmlPullParser xpp)
	{
		String name = xpp.getName();
		if (name.equals("variables"))
		{
			return true;
		}
		if (name.equals("variable"))
		{
			String varName = xpp.getAttributeValue(StringUtil.EMPTY, "name");
			String varValue = xpp.getAttributeValue(StringUtil.EMPTY, "value");
			String kind = xpp.getAttributeValue(StringUtil.EMPTY, "kind");
			RubyVariable newVariable;
			if (varValue == null)
			{
				newVariable = new RubyVariable(stackFrame, varName, kind);
			}
			else
			{
				String typeName = xpp.getAttributeValue(StringUtil.EMPTY, "type");
				boolean hasChildren = xpp.getAttributeValue(StringUtil.EMPTY, "hasChildren").equals("true");
				String objectId = xpp.getAttributeValue(StringUtil.EMPTY, "objectId");
				newVariable = new RubyVariable(stackFrame, varName, kind, varValue, typeName, hasChildren, objectId);
			}
			newVariable.setParent(parent);
			variables.add(newVariable);
			return true;
		}
		if (name.equals("processingException"))
		{
			exceptionMessage = xpp.getAttributeValue(StringUtil.EMPTY, "message");
			exceptionType = xpp.getAttributeValue(StringUtil.EMPTY, "type");
			return true;
		}
		return false;
	}

	protected boolean processEndElement(XmlPullParser xpp)
	{
		return !xpp.getName().equals("variable");
	}
}
