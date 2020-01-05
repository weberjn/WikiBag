package de.jwi.jspwiki.wikibag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SendPlainTest
{

	public static void main(String[] args) throws IOException
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

		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
		outputStreamWriter.write(params.toString());
		outputStreamWriter.flush();

		String line;

		BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

		while ((line = br.readLine()) != null)
		{
			System.out.println(line);
		}

		br.close();

		int status = connection.getResponseCode();
		
		System.out.println(status);

	}

}
