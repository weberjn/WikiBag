package de.jwi.jspwiki.wikibag;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;

public class SendMulti
{

	public static void main(String[] args) throws Exception
	{
		URL url = new URL(WikiData.URL);

		String text = "some String";

		StringBuilder params = new StringBuilder("text=");
		params.append(URLEncoder.encode(text, StandardCharsets.UTF_8.name()));

		String authorization = Base64.getEncoder().encodeToString((WikiData.UserPass).getBytes(StandardCharsets.UTF_8));  

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		
		connection.setRequestProperty("Authorization", "Basic "+authorization);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Accept-Language", StandardCharsets.UTF_8.name());
		connection.setDoOutput(true);

		URL resource = SendMulti.class.getResource("/Schwaebisch_Hall_Historic_Center_and_Comburg.jpg");
		
		
		HttpEntity httpEntity = MultipartEntityBuilder.create().addBinaryBody("Schwaebisch_Hall.jpg", new File(resource.toURI())).build();
		

		connection.setRequestProperty("Content-Type", httpEntity.getContentType().getValue());
		OutputStream out = connection.getOutputStream();
		try {
			httpEntity.writeTo(out);
		} finally {
		    out.close();
		}
		int status = connection.getResponseCode();
		
		System.out.println(status);
	}

}
