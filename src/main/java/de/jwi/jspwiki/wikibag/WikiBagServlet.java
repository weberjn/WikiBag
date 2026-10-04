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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.pages.PageManager;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

public class WikiBagServlet extends HttpServlet
{
	private static final Logger log = LogManager.getLogger(WikiBagServlet.class);

	private MessageFormat textFormat;
	
	private MessageFormat urlFormat;

	private MessageFormat attachmentFormat;

	static final String DEFAULT_PAGE = "WikiBag";

	static final String DEFAULT_TEXT_TEMPLATE = "{0}\\\\\n{1}\n----\n";
	
	static final String DEFAULT_URL_TEMPLATE =  "{0}\\\\\n[{1}]\\\\\n{2}\n----\n";

	static final String DEFAULT_ATTACHMENT_TEMPLATE = "[{0}]\\\\\\n{1}\\n----\\n";

	private String pageName;

	private Enumeration<String> headerNames;

	private String header;

	private File tempDir;

	private Engine engine;

	private AuthenticationManager authenticationManager;

	private AuthorizationManager authorizationManager;

	private PageManager pageManager;

	private AttachmentManager attachmentManager;
	
	

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		String s; 
		
		ServletContext servletContext = config.getServletContext();
		
		tempDir = (File)servletContext.getAttribute(ServletContext.TEMPDIR); 
		
		engine = Wiki.engine().find(config);
		
		Properties wikiProperties = engine.getWikiProperties();

		pageName = wikiProperties.getProperty("de.jwi.WikiBag.page", DEFAULT_PAGE);

		s =	wikiProperties.getProperty("de.jwi.WikiBag.textTemplate", DEFAULT_TEXT_TEMPLATE);

		textFormat = MessageFormatFactory.createMessageFormat(s);

		s = wikiProperties.getProperty("de.jwi.WikiBag.urlTemplate", DEFAULT_URL_TEMPLATE);

		urlFormat = MessageFormatFactory.createMessageFormat(s);
		
		s = wikiProperties.getProperty("de.jwi.WikiBag.attachmentTemplate", DEFAULT_ATTACHMENT_TEMPLATE);

		attachmentFormat = MessageFormatFactory.createMessageFormat(s);
		
		authenticationManager = engine.getManager(AuthenticationManager.class);
		
		authorizationManager = engine.getManager(AuthorizationManager.class);
		
		pageManager = engine.getManager(PageManager.class);
		
		attachmentManager = engine.getManager(AttachmentManager.class);
		
		boolean containerAuthenticated = authenticationManager.isContainerAuthenticated();		
		log.info("containerAuthenticated: " + containerAuthenticated);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		log.info("doPost()");
		
		String shareInfo = "";
		
		if (request.getContentType() != null
				&& request.getContentType().toLowerCase().indexOf("multipart/form-data") > -1)
		{
			Collection<Part> parts = request.getParts();
			int n = parts.size();
			
			Map<String, String> textContent = new HashMap<>();
			
			for (Part part : parts)
			{
				String name = part.getName();
				String contentType = part.getContentType();
				
				if ("text/plain".equals(contentType))
				{
					String s = null;
					try(InputStream is = part.getInputStream())
					{
					    s = new String(is.readAllBytes(), StandardCharsets.UTF_8);
					}

					log.info("part: " + name + ": " +s);
					
					textContent.put(name, s);
				}
				else
				{
					// image/jpeg and everything else specified in manifest.json
					
					long size = part.getSize();
					String submittedFileName = part.getSubmittedFileName();
					
					try 
					{
						Attachment attachment = addAttachment(request, part);
						
						Object[] messageArgs = { submittedFileName, new Date() };
						String content = attachmentFormat.format(messageArgs);
						addContent(request, content);
						
						shareInfo = String.format("%s %d bytes", attachment.toString(), size);
						
					} 
					catch (WikiException | IOException e) 
					{
						throw new ServletException(e);
					}
					
					log.info(submittedFileName);
				}
			}
			
			if (textContent.size() == 2)
			{
				String contentName = textContent.get("name");
				String contentDescription = textContent.get("description");
				
				String content = null;
				
				if (contentDescription.startsWith("http://") || contentDescription.startsWith("https://"))
				{
					Object[] messageArgs = { contentName, contentDescription, new Date() };
					content = urlFormat.format(messageArgs);
				}
				else
				{
					Object[] messageArgs = { contentDescription, new Date() };
					content = textFormat.format(messageArgs);
				}					
				
				try 
				{
					addContent(request, content);
				} 
				catch (WikiException e) 
				{
					throw new ServletException(e);
				}
				
				shareInfo = contentDescription;
			}

		} 

		response.setContentType("text/html");
		response.getWriter().write("share to page " + pageName + " sucess. " + shareInfo);

	}

	private Page getAuthorizedPage(HttpServletRequest request) throws WikiSecurityException
	{
		String remoteUser = request.getRemoteUser();
		
		// make RemoteUser known to Wiki
		authenticationManager.login(request);
		
		Session session = Wiki.session().find(engine, request);
		
		Page page = pageManager.getPage(pageName);

		if (page == null)
		{
			page = Wiki.contents().page(engine, pageName);
		}
		
		Context context = Wiki.context().create(engine, request, page);
		
		Principal user = context.getCurrentUser();

		boolean isAllowed = authorizationManager.checkPermission(context.getWikiSession(),
				PermissionFactory.getPagePermission(page, "edit"));

		if (!isAllowed)
		{
			log.info("no edit permission for: " + user.getName());
			throw new WikiSecurityException("no edit permission");
		}

		return page;
	}

	private void addContent(HttpServletRequest request, String content) throws WikiException
	{
		Page page = getAuthorizedPage(request);
		
		Context context = Wiki.context().create(engine, request, page);
		
		Principal user = context.getCurrentUser();
		
		page.setAuthor(user.getName());
		
		String pureText = pageManager.getPureText(page);

		pureText = content + '\n' + pureText;
		
		pageManager.saveText(context, pureText);
	}
	
	
	private Attachment addAttachment(HttpServletRequest request, Part part)
			throws ProviderException, IOException, WikiSecurityException
	{
		Page page = getAuthorizedPage(request);
		
		Context context = Wiki.context().create(engine, request, page);
		
		Principal user = context.getCurrentUser();
		
		boolean isAllowed = authorizationManager.checkPermission(context.getWikiSession(),
				PermissionFactory.getPagePermission(page, "edit"));

		if (!isAllowed)
		{
			log.info("no edit permission for: " + user.getName());
			throw new WikiSecurityException("no edit permission");
		}
		
		
		String contentType = part.getContentType();
		long partSize = part.getSize();
		
		String submittedFileName = part.getSubmittedFileName();
		
		
		String attachmentPageName = page.getName();

		InputStream is = part.getInputStream();

		Attachment attachment = Wiki.contents().attachment(engine, attachmentPageName, submittedFileName);
		
		attachment.setAuthor(user.getName());

		attachment.setSize(partSize);

		attachmentManager.storeAttachment(attachment, is);

		
		Attachment attachmentInfo = attachmentManager
				.getAttachmentInfo(page.getName() + "/" + submittedFileName);
		
		return attachmentInfo;
	}

}
