package org.stopbadware.dsp.test.helpers;

import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
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
	
	public int sendTestHttpRequest(String pathAndQuery, Object data) throws IOException {
		URL url = new URL(BASE + pathAndQuery);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		Map<String, String> authHeaders = createAuthHeaders(url.getPath().toString());
		for (String key : authHeaders.keySet()) {
			conn.setRequestProperty(key, authHeaders.get(key));
		}
		conn.setDoOutput(true);
		PrintStream out = new PrintStream(conn.getOutputStream());
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(out, data);
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
