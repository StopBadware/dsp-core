package org.stopbadware.dsp.test.helpers;

import java.net.HttpURLConnection;

/**
 * Convenience class to provide frequently used test values  
 *
 */
public class TestVals {
	
	public static final String TEST = "TEST";
	public static final int PRIVATE_AS_RANGE_START = 64512;
	public static final int PRIVATE_AS_RANGE_END = 65534;
	public static final String TEST_REPORT_UID = TEST+"-"+TEST+"-"+1701;
	public static final String VALID_API_PATH = "/v2/events/timeoflast/test";
	
	public static final int OK = HttpURLConnection.HTTP_OK;
	public static final int BAD_REQUEST = HttpURLConnection.HTTP_BAD_REQUEST;
	public static final int FORBIDDEN = HttpURLConnection.HTTP_FORBIDDEN;
	public static final int NOT_FOUND = HttpURLConnection.HTTP_NOT_FOUND;
	public static final int INT_ERROR = HttpURLConnection.HTTP_INTERNAL_ERROR;

}
