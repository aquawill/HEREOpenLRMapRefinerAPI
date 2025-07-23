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


    public static List<double[]> getTrimmedShape(List<double[]> shape, int positiveOffset, int negativeOffset) {
        double totalLength = 0.0;

        List<Double> segmentLengths = new ArrayList<>();

        for (int i = 0; i < shape.size() - 1; i++) {
            double distance = haversineDistance(shape.get(i), shape.get(i + 1));
            segmentLengths.add(distance);
            totalLength += distance;
        }

        double startTrimmedLength = 0.0;
        int startIndex = 0;
        while (startIndex < segmentLengths.size() && startTrimmedLength + segmentLengths.get(startIndex) < positiveOffset) {
            startTrimmedLength += segmentLengths.get(startIndex);
            startIndex++;
        }

        // 若 negativeOffset == 0，計算 positiveOffset 在 shape 上的準確座標
        if (negativeOffset == -1) {
            if (startIndex == 0) {
                return List.of(shape.get(0)); // 直接返回第一個點
            }
            if (startIndex >= shape.size()) {
                return List.of(shape.get(shape.size() - 1)); // 如果超過範圍，回傳最後一個點
            }

            double remainingDistance = positiveOffset - startTrimmedLength;
            double[] startPoint = shape.get(startIndex - 1);
            double[] endPoint = shape.get(startIndex);

            double segmentDistance = segmentLengths.get(startIndex - 1);
            if (segmentDistance == 0) {
                return List.of(startPoint); // 避免除以零，直接返回該點
            }

            double fraction = remainingDistance / segmentDistance;
            double interpolatedLat = startPoint[0] + fraction * (endPoint[0] - startPoint[0]);
            double interpolatedLon = startPoint[1] + fraction * (endPoint[1] - startPoint[1]);

            return List.of(new double[]{interpolatedLat, interpolatedLon});
        } else if (negativeOffset == 0) {
            return shape.subList(startIndex, segmentLengths.size());
        }

        double endTrimmedLength = 0.0;
        int endIndex = segmentLengths.size();
        while (endIndex > 0 && endTrimmedLength + segmentLengths.get(endIndex - 1) < negativeOffset) {
            endTrimmedLength += segmentLengths.get(endIndex - 1);
            endIndex--;
        }

        return shape.subList(startIndex, endIndex);
    }


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

    private static double haversineDistance(double[] point1, double[] point2) {
        double R = 6371000;
        double lat1 = Math.toRadians(point1[0]);
        double lon1 = Math.toRadians(point1[1]);
        double lat2 = Math.toRadians(point2[0]);
        double lon2 = Math.toRadians(point2[1]);

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

}
