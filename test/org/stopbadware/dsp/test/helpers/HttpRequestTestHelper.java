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
import org.codehaus.jackson.map.DeserializationConfig;

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
		mapper.configure(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
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
			return sendTestHttpRequest(pathAndQuery, data, method, null);
		} catch (IOException e) {
			System.out.println("IOException thrown: "+e.getMessage());
			return null;
		}
	}
	
	public TestResponse sendTestHttpRequest(
			String pathAndQuery, 
			Object data, 
			String method,
			Map<String, String> authHeaders) throws IOException {
		URL url = new URL(BASE + pathAndQuery);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(method);
		conn.setRequestProperty("Content-Type", "application/json");
		if (authHeaders == null || authHeaders.size() == 0) {
			authHeaders = createAuthHeaders(url.getPath().toString());
		}
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
		if (resCode == 200) {
			InputStream in = conn.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line = br.readLine()) != null) {
				body += line;
			}
		}
		conn.disconnect();
		return new TestResponse(resCode, body);
	}
	
	/**
	 * Creates valid authentication headers for an API request
	 * @param path the path portion of the API endpoint that will be accessed
	 * @return Map containing the required headers for authentication
	 */
	public Map<String, String> createAuthHeaders(String path) {
		Map<String, String> headers = new HashMap<>();
		String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
		String signature = SHA2.get256(TEST+timestamp+path+TEST);
		headers.put("SBW-Key", TEST);
		headers.put("SBW-Timestamp", timestamp);
		headers.put("SBW-Signature", signature);
		return headers;
	}
	
	/**
	 * Creates authentication headers for an API request based on the passed
	 * parameters. For all params a null value can be passed, in which case a suitable 
	 * and valid default will be used. 
	 * @param path path the path portion of the API endpoint that will be accessed
	 * @param key the account's public API key
	 * @param ts timestamp to use in the request (represented as a String)
	 * @param sec the account's secret API key
	 * @param sig the request signature, normally derived from the SHA2-256
	 * of the key, timestamp, path, and secret
	 * @return Map containing the required headers for authentication
	 */
	public Map<String, String> createAuthHeaders(
			String path,
			String key,
			String ts,
			String sec,
			String sig) {
		Map<String, String> headers = new HashMap<>();
		String urlPath = (path == null) ? VALID_API_PATH : path;
		String apiKey = (key==null) ? TEST : key;
		String secret = (sec==null) ? TEST : sec;
		String timestamp = (ts==null) ? String.valueOf(System.currentTimeMillis() / 1000) : ts;
		String signature = (sig==null) ? SHA2.get256(apiKey+timestamp+urlPath+secret) : sig;
		headers.put("SBW-Key", apiKey);
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
