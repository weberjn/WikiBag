## WikiBag Servlet for JSPWiki

WikiBag Servlet listens for posts that contain text or multipart data (as attachment) and adds it to the wiki page WikiBag. 

Post calls must have an Authorization Basic Header, the user needs PagePermission "edit".

# installation

add the Wikibag jar to the JSPWiki in WEB-INF/lib and add the Servlet to JSPWiki's web.xml

    <servlet>
       <servlet-name>wikibag</servlet-name>
	   <multipart-config/>		
       <servlet-class>de.jwi.jspwiki.wikibag.WikiBagServlet</servlet-class>
    </servlet
    
     <servlet-mapping>
        <servlet-name>wikibag</servlet-name>
		<url-pattern>/wikibag</url-pattern>
    </servlet-mapping>
	
# test

http://localhost:8080/wiki/wikibag

should give

    WikiBagServlet is alive.

send a string:

    curl -v -u wikibag:secret -X POST --data "text=a string" http://localhost:8080/JSPWiki/wikibag

send an image:

    curl -v -u wikibag:secret -F upload=@src/test/resources/Schwaebisch_Hall_Historic_Center_and_Comburg.jpg http://localhost:8080/JSPWiki/wikibag 

The WikiBag wiki page should now contain the text string and an image attachment.


# License

> This software is licensed under the Apache License, version 2.
 
	