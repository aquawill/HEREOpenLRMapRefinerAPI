package org.foobar.mapRefiner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RouteMatcher {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String HERE_API_KEY = System.getenv("HERE_API_KEY");

    private static final String ROUTE_MATCHING_URL = "https://routematching.hereapi.com/v8/match/routelinks?"
            + "apiKey=" + HERE_API_KEY
            + "&routeMatch=1&mode=car&mapMatchRadius=20";
    public static List<double[]> fetchRouteShape(JsonNode matchedRouteResponse) {
        List<double[]> shape = new ArrayList<>();
        JsonNode legs = matchedRouteResponse.path("response").path("route").path(0).path("leg");

        if (legs.isArray()) {
            for (JsonNode leg : legs) {
                for (JsonNode link : leg.path("link")) {
                    JsonNode shapeNode = link.path("shape");
                    for (int i = 0; i < shapeNode.size(); i += 2) {
                        double lat = shapeNode.get(i).asDouble();
                        double lon = shapeNode.get(i + 1).asDouble();
                        shape.add(new double[]{lat, lon});
                    }
                }
            }
        }
        return shape;
    }

    public static List<String> fetchLinkIds(JsonNode matchedRouteResponse) {
        List<String> linkIds = new ArrayList<>();
        JsonNode legs = matchedRouteResponse.path("response").path("route").path(0).path("leg");

        if (legs.isArray()) {
            for (JsonNode leg : legs) {
                for (JsonNode link : leg.path("link")) {
                    linkIds.add(link.path("linkId").asText());
                }
            }
        }
        return linkIds;
    }

    public static List<String> fetchSegmentRefs(JsonNode matchedRouteResponse) {
        List<String> segmentRefs = new ArrayList<>();
        JsonNode legs = matchedRouteResponse.path("response").path("route").path(0).path("leg");

        if (legs.isArray()) {
            for (JsonNode leg : legs) {
                for (JsonNode link : leg.path("link")) {
                    segmentRefs.add(link.path("segmentRef").asText());
                }
            }
        }
        return segmentRefs;
    }


    public static JsonNode fetchMatchedRoute(String inputJson) throws Exception {
        URL url = new URL(ROUTE_MATCHING_URL);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");


        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = inputJson.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
            os.flush();
        }


        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("Route Matching API failed: HTTP " + responseCode);
        }


        try (InputStream is = conn.getInputStream()) {
            return objectMapper.readTree(is);
        }
    }

}
