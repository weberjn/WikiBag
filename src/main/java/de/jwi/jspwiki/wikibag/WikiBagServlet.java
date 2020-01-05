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
import org.apache.wiki.auth.permissions.PermissionFactory;

public class WikiBagServlet extends HttpServlet
{

	static Logger log = Logger.getLogger(WikiBagServlet.class);

	private WikiEngine wikiEngine;

	private MessageFormat textFormat;

	private MessageFormat attachmentFormat;

	static final String DEFAULT_PAGE = "WikiBag";

	static final String DEFAULT_TEXT_TEMPLATE = "{0}\\\\\n{1}\n----\n";

	static final String DEFAULT_ATTACHMENT_TEMPLATE = "[{0}]\\\\\\n{1}\\n----\\n";

	private String pageName;

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		wikiEngine = WikiEngine.getInstance(config);

		Properties wikiProperties = wikiEngine.getWikiProperties();

		pageName = wikiProperties.getProperty("de.jwi.WikiBag.page", DEFAULT_PAGE);

		String s =

				wikiProperties.getProperty("de.jwi.WikiBag.textTemplate", DEFAULT_TEXT_TEMPLATE);

		textFormat = MessageFormatFactory.createMessageFormat(s);

		s = wikiProperties.getProperty("de.jwi.WikiBag.attachmentTemplate", DEFAULT_ATTACHMENT_TEMPLATE);

		attachmentFormat = MessageFormatFactory.createMessageFormat(s);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String msg = WikiBagServlet.class.getSimpleName() + " is alive.";

		response.getWriter().print(msg);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try
		{
			String pathInfo = request.getPathInfo();

			log.info("doPost: " + pathInfo);

			String text = request.getParameter("text");

			log.info("doPost: text=" + text);

			WikiPage page = wikiEngine.getPage(pageName);

			if (page == null)
			{
				page = new WikiPage(wikiEngine, pageName);
			}

			String[] up = parseUserPassword(request);

			AuthenticationManager authenticationManager = wikiEngine.getAuthenticationManager();

			if (authenticationManager.isContainerAuthenticated())
			{
				request.login(up[0], up[1]);
			}
			
			WikiContext context = null;

			context = new WikiContext(wikiEngine, request, page);

			boolean loginOK;

			if (authenticationManager.isContainerAuthenticated())
			{
				authenticationManager.login(request);
			} else
			{
				WikiSession wikiSession = context.getWikiSession();

				authenticationManager.login(wikiSession, request, up[0], up[1]);
			}

//			log.info("loginOK: " + loginOK);
//
//			if (!loginOK)
//			{
//				throw new WikiSecurityException("Authentication failed");
//			}

			AuthorizationManager authorizationManager = wikiEngine.getAuthorizationManager();

			boolean isAllowed = authorizationManager.checkPermission(context.getWikiSession(),
					PermissionFactory.getPagePermission(page, "edit"));

			if (!isAllowed)
			{
				throw new ServletException("no edit permission");
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
			} else
			{
				Object[] messageArgs = { text, new Date() };
				String content = textFormat.format(messageArgs);
				addContent(context, content);
			}
		} catch (WikiException e)
		{
			throw new ServletException(e);
		}
	}

	private String[] parseUserPassword(HttpServletRequest request) throws ServletException
	{
		String authHeader = request.getHeader("Authorization");

		String BASIC_PREFIX = "Basic ";

		if (authHeader == null || !authHeader.startsWith(BASIC_PREFIX))
		{
			throw new ServletException("Require Basic Auth");
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
