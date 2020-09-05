package de.jwi.jspwiki.wikibag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SendPlainTest
{

	public static void main(String[] args) throws IOException
	{
		URL url = new URL(WikiData.URL);

		String text = "Ísland Straße Mädchen ça va résoudre les problèmes";

		Charset charset = StandardCharsets.UTF_8;
//		Charset charset = StandardCharsets.ISO_8859_1;
		
		StringBuilder params = new StringBuilder("text=");
		params.append(URLEncoder.encode(text, charset.name()));

		String authorization = Base64.getEncoder().encodeToString((WikiData.UserPass).getBytes(charset));  

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		
		connection.setRequestProperty("Authorization", "Basic "+authorization);

		
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset="+charset.name());
		connection.setRequestProperty("Content-Length", "" + params.length());  
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
