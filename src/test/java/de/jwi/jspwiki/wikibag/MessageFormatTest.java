package de.jwi.jspwiki.wikibag;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.jwi.jspwiki.wikibag.MessageFormatFactory;
import de.jwi.jspwiki.wikibag.WikiBagServlet;

public class MessageFormatTest {

	private static Properties p;

	public static void main(String[] args) {
		Object[] testArgs = { 31415L, "SchnelleLotte", "X" };

		String pattern = "Anzahl Dateien auf der Festplatte \"{1}\": \\n\\t{0}. -.";

		pattern = pattern.replace("\\n", "\n");
		pattern = pattern.replace("\\t", "\t");

		MessageFormat form = new MessageFormat(pattern);
		System.out.println(form.format(testArgs));

	}

	@BeforeClass
	public static void setup() throws IOException {
		InputStream is = MessageFormatTest.class.getResourceAsStream("/MessageFormatTest.properties");
		p = new Properties();
		p.load(is);
		is.close();

	}

	@Test
	public void testText() {
		String pattern = "{0}{1}";
		MessageFormat mf = MessageFormatFactory.createMessageFormat(pattern);

		Object[] messageArgs = { "X", "Y" };
		String m = mf.format(messageArgs);

		Assert.assertEquals("XY", m);

	}

	@Test
	public void testNewLines() {
		String pattern = "{0}\\n{1}";
		MessageFormat mf = MessageFormatFactory.createMessageFormat(pattern);

		Object[] messageArgs = { "X", "Y" };
		String m = mf.format(messageArgs);

		Assert.assertEquals("X\nY", m);

	}

	@Test
	public void testNewLinesProperties()  {
	
		String pattern = p.getProperty("textpattern");
		MessageFormat mf = MessageFormatFactory.createMessageFormat(pattern);

		Object[] messageArgs = { "X", "Y" };
		String m = mf.format(messageArgs);

		Assert.assertEquals("X\nY", m);

	}

	@Test
	public void textDefault() {
		String pattern = WikiBagServlet.DEFAULT_TEXT_TEMPLATE;
		MessageFormat mf = MessageFormatFactory.createMessageFormat(pattern);

		Object[] messageArgs = { "X", "Y" };
		String m = mf.format(messageArgs);

		String defaultresult = p.getProperty("textdefaultresult");
		
		
		Assert.assertEquals(defaultresult, m);

	}

	@Test
	public void urlDefault() {
		String pattern = WikiBagServlet.DEFAULT_URL_TEMPLATE;
		MessageFormat mf = MessageFormatFactory.createMessageFormat(pattern);

		Object[] messageArgs = { "X", "Y", "Z" };
		String m = mf.format(messageArgs);

		String defaultresult = p.getProperty("urldefaultresult");
		
		
		Assert.assertEquals(defaultresult, m);

	}

	@Test
	public void attachmentDefault() {
		String pattern = WikiBagServlet.DEFAULT_ATTACHMENT_TEMPLATE;
		MessageFormat mf = MessageFormatFactory.createMessageFormat(pattern);

		Object[] messageArgs = { "X", "Y" };
		String m = mf.format(messageArgs);

		String defaultresult = p.getProperty("attachmentdefaultresult");
		
		
		Assert.assertEquals(defaultresult, m);

	}

	
	
}
