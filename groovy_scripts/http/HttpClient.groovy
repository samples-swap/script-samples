package com.mzeng.gzbus



 
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
 
import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.logging.Log;
import org.apache.log4j.Logger;
import org.apache.commons.logging.Log;
import org.apache.log4j.*;

public class HttpClient {
  static Logger logger = Logger.getLogger(HttpClient.class);
	
	private static final String USER_AGENT = "Mozilla/5.0";
 
	public static void main(String[] args) throws Exception { 
		PropertyConfigurator.configure("log4j.properties");
		
		logger.debug("Testing 1 - Send Http GET request");
		println HttpClient.sendGet("http://www.google.com/search?q=mkyong");		
		
		logger.debug("\nTesting 2 - Send Http POST request");
		println HttpClient.sendPost("https://selfsolve.apple.com/wcResults.do", "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345");
 
	}
 
	// HTTP GET request
	public static String sendGet(String url) throws Exception { 
 
		URL obj = new URL(url);
		
		def con
		if(url.toLowerCase().indexOf("https")>=0)
		  con = (HttpsURLConnection) obj.openConnection();
		else
		  con =  (HttpURLConnection) obj.openConnection();
 
 
		// optional default is GET
		con.setRequestMethod("GET");
 
		//add request header
		con.setRequestProperty("User-Agent", USER_AGENT);
 
		int responseCode = con.getResponseCode();
		logger.debug("\nSending 'GET' request to URL : " + url);
		logger.debug("Response Code : " + responseCode);
 
		BufferedReader ins = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = ins.readLine()) != null) {
			response.append(inputLine);
		}
		ins.close();
 
		//print result
		def s = response.toString()
		logger.debug(s)
		return   s ;
 
	}
 
	// HTTP POST request
	public static String sendPost(String url,String urlParameters) throws Exception {
		 
		URL obj = new URL(url);
		
		def con
		if(url.toLowerCase().indexOf("https")>=0)
		  con = (HttpsURLConnection) obj.openConnection();
		else
		  con =  (HttpURLConnection) obj.openConnection();
 
		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
 
 
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
 
		int responseCode = con.getResponseCode();
		logger.debug("\nSending 'POST' request to URL : " + url);
		logger.debug("Post parameters : " + urlParameters);
		logger.debug("Response Code : " + responseCode);
 
		BufferedReader ins = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = ins.readLine()) != null) {
			response.append(inputLine);
		}
		ins.close();
 
		//print result
		def s = response.toString()
		logger.debug(s);
		return  s;
	}
 
}