package org.stopbadware.dsp.test.helpers;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.stopbadware.dsp.test.helpers.TestVals.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.lib.util.SHA2;

/**
 * Helper class for testing responses to HTTP requests. Requires a running
 * instance of DSP-CORE (defaults to 127.0.0.1:5000 if SBW_CORE_HOST is not
 * set as an environment variable)
 *
 */
public class HttpRequestTestHelper {
	
	private static final String BASE = (System.getenv("SBW_CORE_HOST") != null) ? System.getenv("SBW_CORE_HOST") : "http://127.0.0.1:5000";
	
	public HttpRequestTestHelper() {
		
	}
	
	public TestResponse getTest(String pathAndQuery) {
		return sendTest(pathAndQuery, null, "GET");
	}
	
	public void searchTest(String pathAndQuery) {
		TestResponse res = getTest(pathAndQuery);
		assertTrue(res != null);
		assertTrue(res.code == OK);
		ObjectMapper mapper = new ObjectMapper();
		try {
			SearchResults results = mapper.readValue(res.body, SearchResults.class);
			assertTrue(results.getCode() == SearchResults.OK);
		} catch (IOException e) {
			fail("IOException thrown: " + e.getMessage());
		}
	}
	
	public void errorTest(String pathAndQuery, int expectedResponse) {
		TestResponse res = getTest(pathAndQuery);
		assertTrue(res != null);
		assertTrue(res.code == OK);
		ObjectMapper mapper = new ObjectMapper();
		try {
			Error results = mapper.readValue(res.body, Error.class);
			assertTrue(results.getCode() == expectedResponse);
		} catch (IOException e) {
			fail("IOException thrown: " + e.getMessage());
		}
	}
	
	public boolean postTest(String pathAndQuery, Object data, int expectedResponse) {
		return sendTest(pathAndQuery, data, "POST").code == expectedResponse;
	}
	
	private TestResponse sendTest(String pathAndQuery, Object data, String method)  {
		try {
			return sendTestHttpRequest(pathAndQuery, data, method);
		} catch (IOException e) {
			System.out.println("IOException thrown: "+e.getMessage());
			return null;
		}
	}
	
	public TestResponse sendTestHttpRequest(String pathAndQuery, Object data, String method) throws IOException {
		URL url = new URL(BASE + pathAndQuery);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(method);
		conn.setRequestProperty("Content-Type", "application/json");
		Map<String, String> authHeaders = createAuthHeaders(url.getPath().toString());
		for (String key : authHeaders.keySet()) {
			conn.setRequestProperty(key, authHeaders.get(key));
		}
		if (data != null) {
			conn.setDoOutput(true);
			PrintStream out = new PrintStream(conn.getOutputStream());
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(out, data);
		}
		int resCode = conn.getResponseCode();
		String body = "";
		InputStream in = conn.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line = null;
		while ((line = br.readLine()) != null) {
			body += line;
		}
		conn.disconnect();
		return new TestResponse(resCode, body);
	}
	
	private Map<String, String> createAuthHeaders(String path) {
		Map<String, String> headers = new HashMap<>();
		String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
		String signature = SHA2.get256(TEST+timestamp+path+TEST);
		headers.put("SBW-Key", TEST);
		headers.put("SBW-Timestamp", timestamp);
		headers.put("SBW-Signature", signature);
		return headers;
	}
	
	public class TestResponse {
		public final int code;
		public final String body;
		public TestResponse(int code, String body) {
			this.code = code;
			this.body = body;
		}
	}

}
