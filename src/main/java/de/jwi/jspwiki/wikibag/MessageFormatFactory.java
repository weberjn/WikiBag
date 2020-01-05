package de.jwi.jspwiki.wikibag;

import java.text.MessageFormat;

public class MessageFormatFactory {

	public static MessageFormat createMessageFormat(String pattern)
	{
		pattern=pattern.replace("\\n", "\n");
		pattern=pattern.replace("\\t", "\t");
		
		MessageFormat mf = new MessageFormat(pattern);
		
		return mf;
	}

	
}
