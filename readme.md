## WikiBag Servlet for JSPWiki

Creates a Web Share Target to send URLs, Texts and Images to the Wiki page WikiBag.

# installation

add the Wikibag jar to the JSPWiki in WEB-INF/lib

Enable container-managed authentication in jspwiki WEB-INF/web.xml

# Android Installion

open in Chrome

https://your.host/JSPWiki/wikibag.html

This will only work if this is a valid https site.

Chrome might immediately offer to install it. If not, use the install option of the Android three dots menu.
Then in the Android apps list should be a Wikibag icon. Also it should appear in the list of send targets.

Uninstall: from Settings - Apps
	
# Test

Share an URL, a text selection or an image to WikiBag.

The WikiBag wiki page should now contain the URL and an image attachment.

The WikiBag accepts all the file types in the files list of src/main/resources/META-INF/resources/manifest.json


# License

> This software is licensed under the Apache License, version 2.
 
	