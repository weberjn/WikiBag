/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package de.jwi.jspwiki.wikibag;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Date;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.permissions.PermissionFactory;

public class WikiBagServlet extends HttpServlet
{

	static Logger log = Logger.getLogger(WikiBagServlet.class);

	private WikiEngine wikiEngine;

	private MessageFormat textFormat;
	
	private MessageFormat urlFormat;

	private MessageFormat attachmentFormat;

	static final String DEFAULT_PAGE = "WikiBag";

	static final String DEFAULT_TEXT_TEMPLATE = "{0}\\\\\n{1}\n----\n";
	
	static final String DEFAULT_URL_TEMPLATE =  "{0}\\\\\n[{1}]\\\\\n{2}\n----\n";

	static final String DEFAULT_ATTACHMENT_TEMPLATE = "[{0}]\\\\\\n{1}\\n----\\n";

	private String pageName;

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		String s; 
		
		wikiEngine = WikiEngine.getInstance(config);
		
		Properties wikiProperties = wikiEngine.getWikiProperties();

		pageName = wikiProperties.getProperty("de.jwi.WikiBag.page", DEFAULT_PAGE);

		s =	wikiProperties.getProperty("de.jwi.WikiBag.textTemplate", DEFAULT_TEXT_TEMPLATE);

		textFormat = MessageFormatFactory.createMessageFormat(s);

		s = wikiProperties.getProperty("de.jwi.WikiBag.urlTemplate", DEFAULT_URL_TEMPLATE);

		urlFormat = MessageFormatFactory.createMessageFormat(s);
		
		s = wikiProperties.getProperty("de.jwi.WikiBag.attachmentTemplate", DEFAULT_ATTACHMENT_TEMPLATE);

		attachmentFormat = MessageFormatFactory.createMessageFormat(s);
		
		boolean containerAuthenticated = wikiEngine.getAuthenticationManager().isContainerAuthenticated();		
		log.info("containerAuthenticated: " + containerAuthenticated);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String s = request.getContextPath() + request.getServletPath();
		PrintWriter out = response.getWriter();
		out.print("<html><body>");
		out.print("Wikibag: <p>");
		out.print("<form action='" + s + "' method='post'>");
		out.print("text: <input type='text' name='text' /> <p>");
		out.print("user: <input type='text' name='user' /> <p>");  
		out.print("pass: <input type='password ' name='password' /> <p>");  
		out.print("<input type='submit' value='post' />");  
		out.print("</form></body></html>");  				
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try
		{
			String userp = request.getParameter("user");
			String passwordp = request.getParameter("password");
			
			WikiPage page = wikiEngine.getPage(pageName);

			if (page == null)
			{
				page = new WikiPage(wikiEngine, pageName);
			}

			String[] up = {userp, passwordp};
			
			if (userp ==null || passwordp == null)
			{
				up = parseUserPassword(request);
			}
			

			AuthenticationManager authenticationManager = wikiEngine.getAuthenticationManager();

			if (authenticationManager.isContainerAuthenticated())
			{
				String remoteUser = request.getRemoteUser();
				try {
					request.login(up[0], up[1]);
				} catch (ServletException e) {
					log.info("login failed for " + up[0], e);
					throw new WikiSecurityException("Authentication failed");
				}
			}
			
			WikiContext context = new WikiContext(wikiEngine, request, page);

			boolean loginOK = false;
			
			if (authenticationManager.isContainerAuthenticated())
			{
				// checks for container authenticated request
				
				loginOK = authenticationManager.login(request);
				// always false, Bug
			}
			else
			{
				WikiSession wikiSession = context.getWikiSession();

				// this always authenticates against UserDatabaseLoginModule
				loginOK = authenticationManager.login(wikiSession, request, up[0], up[1]);
				if (!loginOK)
				{
					log.info("Authentication failed for: " + up[0]);
					throw new WikiSecurityException("Authentication failed");
				}
			}
			
			

//			WikiContext context = new WikiContext(wikiEngine, request, page);
//			
//			WikiSession wikiSession = context.getWikiSession();
//			
//			AuthenticationManager authenticationManager = wikiEngine.getAuthenticationManager();
//			
//			boolean loginOK = authenticationManager.login(wikiSession, request, up[0], up[1]);
//			if (!loginOK)
//			{
//				log.info("Authentication failed for: " + up[0]);
//				throw new WikiSecurityException("Authentication failed");
//			}

			AuthorizationManager authorizationManager = wikiEngine.getAuthorizationManager();

			boolean isAllowed = authorizationManager.checkPermission(context.getWikiSession(),
					PermissionFactory.getPagePermission(page, "edit"));

			if (!isAllowed)
			{
				log.info("no edit permission for: " + up[0]);
				throw new WikiSecurityException("no edit permission");
			}

			Principal user = context.getCurrentUser();

			if (request.getContentType() != null
					&& request.getContentType().toLowerCase().indexOf("multipart/form-data") > -1)
			{

				for (Part part : request.getParts())
				{
					String name = part.getName();
					String contentType = part.getContentType();
					log.info(name);

					String disposition = part.getHeader("Content-Disposition");
					String fileName = disposition.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");

					addAttachment(part, fileName, page, user);

					Object[] messageArgs = { fileName, new Date() };
					String content = attachmentFormat.format(messageArgs);
					addContent(context, content);
				}
			} 
			else
			{
				String content; 
				
				String charset = request.getCharacterEncoding() != null ? request.getCharacterEncoding() : StandardCharsets.ISO_8859_1.name();
				
				String text = request.getParameter("text");

				text = URLDecoder.decode(text, charset);
				log.info("doPost: text=" + text);

				if (text.startsWith("http://") || text.startsWith("https://"))
				{
					String subject = request.getParameter("subject");
					log.info("doPost: subject=" + subject);

					subject = subject != null ? subject : "";
					
					Object[] messageArgs = { subject, text, new Date() };
					content = urlFormat.format(messageArgs);
				}
				else
				{
					Object[] messageArgs = { text, new Date() };
					content = textFormat.format(messageArgs);
				}
				addContent(context, content);
			}
		} 
		catch (WikiSecurityException e)
		{
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		}
		catch (WikiException e)
		{
			throw new ServletException(e);
		}
	}

	private String[] parseUserPassword(HttpServletRequest request) throws WikiSecurityException
	{
		String authHeader = request.getHeader("Authorization");

		String BASIC_PREFIX = "Basic ";

		if (authHeader == null || !authHeader.startsWith(BASIC_PREFIX))
		{
			throw new WikiSecurityException("Require Basic Auth");
		}

		String userPassBase64 = authHeader.substring(BASIC_PREFIX.length());
		Decoder decoder = Base64.getDecoder();
		byte[] decode = decoder.decode(userPassBase64);
		String userPass = new String(decode); // => user:pass

		String[] s = userPass.split(":");

		return s;
	}


	private void addAttachment(Part part, String fileName, WikiPage page, Principal user)
			throws ProviderException, IOException
	{
		String contentType = part.getContentType();
		long partSize = part.getSize();

		InputStream is = part.getInputStream();

		Attachment attachment = new Attachment(wikiEngine, page.getName(), fileName);

		attachment.setAuthor(user.getName());

		attachment.setSize(partSize);

		wikiEngine.getAttachmentManager().storeAttachment(attachment, is);

		Attachment attachmentInfo = wikiEngine.getAttachmentManager()
				.getAttachmentInfo(page.getName() + "/" + fileName);

		int version = attachmentInfo.getVersion();

		log.info(version + "");
	}

	private void addContent(WikiContext wikiContext, String content) throws WikiException
	{

		WikiPage page = wikiContext.getPage();

		String pureText = wikiEngine.getPureText(page);

		pureText = content + '\n' + pureText;

		wikiEngine.saveText(wikiContext, pureText);
	}

}
