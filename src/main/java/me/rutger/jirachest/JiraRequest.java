package me.rutger.jirachest;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JiraRequest {

    // Define some global vars
    String subdomain = JiraChest.plugin.getConfig().getString("subdomain");
    String user = JiraChest.plugin.getConfig().getString("user");
    String key = JiraChest.plugin.getConfig().getString("key");

    String BASE_URL = "https://%s.atlassian.net";
    String ENDPOINT_ISSUE = "/rest/api/2/issue/%s";
    String ENDPOINT_SEARCH = "/rest/api/2/search?jql=%s";
    String ENDPOINT_TRANSITIONS_FORMAT = "/rest/api/2/issue/%s/transitions";
    String ENDPOINT_COMMENT_FORMAT = "/rest/api/2/issue/%s/comment";

    public String getIssue(String issue) throws IOException {
        String endpoint = getUri(String.format(ENDPOINT_ISSUE, issue) );
        return getRequest(endpoint);
    }

    public String getAllIssues() throws IOException {
        String query = "project = HEET and status != Done ORDER BY created desc";

        String endpoint = getUri(String.format(ENDPOINT_SEARCH, urlEncode(query)) );
        return getRequest(endpoint);
    }

    public String transition(String issue, String lane) throws  IOException {
        String endpoint = getUri(String.format(ENDPOINT_TRANSITIONS_FORMAT, issue));

        // Define json request body
        JSONObject json = new JSONObject();
        JSONObject item = new JSONObject();
        item.put("id", lane);
        json.put("transition", item);

        return postRequest(endpoint,json);
    }

    // General get method
    public String getRequest(String endpoint) throws IOException {

        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Request request = new Request.Builder()
                .url(endpoint)
                .method("GET", null)
                .addHeader("Authorization", "Basic " + getEncoding())
                .build();

        // Define response
        Response response = client.newCall(request).execute();

        // Return response body (json)
        return response.body().string();
    }

    // General post method
    public String postRequest(String endpoint, JSONObject payload) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder().build();

        // Set json as our request body
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, payload.toString());

        Request request = new Request.Builder()
                .url(endpoint)
                .method("POST", body)
                .addHeader("Authorization", "Basic " + getEncoding())
                .addHeader("Content-Type", "application/json")
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    // Format base url
    public String getUri(String endpoint) {
        String apiurl = String.format(BASE_URL, subdomain);
        return new String(apiurl + endpoint);
    }

    // Generate basic auth
    public String getEncoding() {
        byte[] bytes = String.format("%s:%s", user, key).getBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    // Urlencode string
    public String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }
}

