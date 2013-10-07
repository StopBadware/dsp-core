package org.stopbadware.dsp.rest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.stopbadware.dsp.json.EventReports;
import org.stopbadware.dsp.json.Response;
import org.stopbadware.lib.util.SHA2;

public class AddTest {
	
	@Test
	public void addEventsTest() {
		String path = "/v2/add/events";
		Object data = new EventReports();
		try {
			int response = sendTestHttpRequest(path, data);
			System.out.println(data);		//DELME DATA-120
			System.out.println(response);	//DELME DATA-120
		} catch (IOException e) {
			e.printStackTrace();			//DELME DATA-120
			fail("IOException thrown: "+e.getMessage());
		}
		
		assertTrue(false);
	}
	
	private int sendTestHttpRequest(String pathAndQuery, Object data) throws IOException {
		URL url = new URL("http://127.0.0.1:5000"+pathAndQuery);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		Map<String, String> authHeaders = createAuthHeaders(url.getPath().toString());
		for (String key : authHeaders.keySet()) {
			conn.setRequestProperty(key, authHeaders.get(key));
		}
		conn.setDoOutput(true);
		//
		PrintStream out = new PrintStream(conn.getOutputStream());
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(out, data);
//		OutputStream out = conn.getOutputStream();
//		out.write(data. getBytes("UTF-8"));
//		out.flush();
//		out.close();
		int resCode = conn.getResponseCode();
		conn.disconnect();
		return resCode;
	}
	
	private Map<String, String> createAuthHeaders(String path) {
		Map<String, String> headers = new HashMap<>();
		String test = "TEST";
		String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
		String signature = SHA2.get256(test+timestamp+path+test);
		headers.put("SBW-Key", test);
		headers.put("SBW-Timestamp", timestamp);
		headers.put("SBW-Signature", signature);
		return headers;
	}

}
