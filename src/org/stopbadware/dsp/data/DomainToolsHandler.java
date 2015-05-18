package org.stopbadware.dsp.data;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DomainToolsHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DomainToolsHandler.class);

    public class DTSigner {
        private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

        private String api_username;
        private String api_key;
        private SimpleDateFormat timeFormatter;

        public DTSigner(String api_username, String api_key) {
            this.api_username = api_username;
            this.api_key = api_key;
            this.timeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        }

        public String timestamp() {
            Date now = new Date();
            return this.timeFormatter.format(now).replace("+0000","Z");
        }

        public String getHexString(byte[] b) {
            String result = "";
            for (int i = 0; i < b.length; i++) {
                result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            }
            return result;
        }

        public String sign(String timestamp, String uri)
                throws java.security.SignatureException {
            String Result;
            try {
                String data = new String(this.api_username + timestamp + uri);
                SecretKeySpec signingKey = new SecretKeySpec(this.api_key.getBytes(),
                        HMAC_SHA1_ALGORITHM);
                Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
                mac.init(signingKey);
                byte[] rawSignature = mac.doFinal(data.getBytes());
                Result = this.getHexString(rawSignature);
            } catch (Exception e) {
                throw new java.security.SignatureException("Failed to generate HMAC : "
                        + e.getMessage());
            }
            return Result;
        }
    }
    public String getWhoisForHost(String host) {
        String api_username = (System.getenv("DT_USERNAME")!=null) ? System.getenv("DT_USERNAME") : "";
        String api_key = (System.getenv("DT_API_KEY")!=null) ? System.getenv("DT_API_KEY") : "";
        String uri = "/v1/"+host+"/whois/parsed";

        try {
            DTSigner signer = new DTSigner(api_username, api_key);
            String timestamp = signer.timestamp();
            String signature = signer.sign(timestamp, uri);
            String query = uri + "?api_username=" +
                    api_username + "&signature=" + signature + "&timestamp="
                    + timestamp;
            LOG.debug("Domain tools whois lookup for URI {} is signed");
            return sendQueryToDomainTools(query);

        } catch(SignatureException e) {
            LOG.error("Error trying to sign whois query", e);
        } catch(IOException e) {
            LOG.error("Error querying domaintools for host {}",host,e);
        }
        return null;
    }
    private String sendQueryToDomainTools(String query) throws IOException {
        String dtHost = "api.domaintools.com";
        URL url = new URL("http://" + dtHost + query);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");
        int responseCode = conn.getResponseCode();
        LOG.debug("Sending 'GET' request to URL : " + url);
        LOG.debug("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        LOG.info("Response = "+response.toString());
        return response.toString();
    }

}