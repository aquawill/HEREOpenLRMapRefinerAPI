package org.foobar;

import io.javalin.Javalin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.here.platform.location.referencing.olr.OlrPrettyPrinter;
import com.here.platform.location.tpeg2.BinaryMarshallers;
import com.here.platform.location.tpeg2.olr.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


import static org.foobar.LocationReferenceParser.generateCsvFromDecodedResult;
import static org.foobar.LocationReferenceParser.parsePrettyPrintString;

public class OpenLRMapRefiner {
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


    public static Map<String, Object> decodeOpenLR(String base64Data) {
        try {
            OpenLRLocationReference reference;

            try {
                byte[] decode = Base64.getDecoder().decode(base64Data);
                reference = BinaryMarshallers.openLRLocationReference()
                        .unmarshall(new ByteArrayInputStream(decode));

                if (reference.getLocationReference() == null) {
                    throw new UnableToDecodeException("Invalid OpenLR data: Decoded reference is null.");
                }
            } catch (IllegalArgumentException e) {
                throw new UnableToDecodeException("Invalid Base64 encoding: " + e.getMessage());
            } catch (AssertionError e) {
                throw new UnableToDecodeException("Invalid OpenLR format: " + e.getMessage());
            } catch (Exception e) {
                throw new UnableToDecodeException("Unexpected error while decoding OpenLR, please input proper OpenLR Base64 string.");
            }

            System.out.println("getLocationReference: " + reference.getLocationReference().toString());
            ObjectMapper mapper = new ObjectMapper();
            String parsedJson = "";

            if (reference.getLocationReference().toString().startsWith("LinearLocationReference")) {
                LinearLocationReference reference1 = (LinearLocationReference) reference.getLocationReference();
                parsedJson = parsePrettyPrintString((OlrPrettyPrinter.prettyPrint(reference1).toString()), mapper);
            } else {
                parsedJson = LocationReferenceParser.parseToJson(reference.getLocationReference().toString());
            }

            Map<String, Object> jsonMap = objectMapper.readValue(parsedJson, Map.class);
            JsonNode decodedResult = objectMapper.readTree(parsedJson);

            if (!decodedResult.has("type")) {
                throw new UnableToDecodeException("Decoded OpenLR data is missing required fields.");
            }

            String csvData = generateCsvFromDecodedResult(decodedResult);
//            System.out.println(csvData);
            JsonNode matchedRouteResponse = fetchMatchedRoute(csvData);

            List<double[]> fullShape = fetchRouteShape(matchedRouteResponse);
            List<String> linkIdList = fetchLinkIds(matchedRouteResponse);
            List<String> segmentRefList = fetchSegmentRefs(matchedRouteResponse);

            int positiveOffset = (int) jsonMap.getOrDefault("positiveOffset", 0);
            int negativeOffset = decodedResult.has("negativeOffset") ? decodedResult.get("negativeOffset").asInt() : -1;

            List<double[]> trimmedShape = getTrimmedShape(fullShape, positiveOffset, negativeOffset);
            System.out.printf("trimmedShape.size(): %s", trimmedShape.size());

            String shapeStyle = trimmedShape.size() == 1 ? "point" : "polyline";
            String flexiblePolyline = PolylineUtil.encodeToFlexiblePolyline(trimmedShape);

            return Map.of(
                    "input", base64Data,
                    "decoded_result", jsonMap,
                    "map_matched_shape", Map.of(
                            "style", shapeStyle,
                            "trimmed_shape", trimmedShape,
                            "flexible_polyline", flexiblePolyline)
            );

        } catch (UnableToDecodeException e) {
            return Map.of("error", e.getMessage(), "input", base64Data);
        } catch (Exception e) {
            return Map.of("error", "Unexpected error occurred");
        }
    }


    public static void main(String[] args) {
        Javalin app = Javalin.create().start(5000);

        app.post("/decode", ctx -> {

            ctx.contentType("application/json");

            Map<String, Object> requestBody = objectMapper.readValue(ctx.body(), Map.class);
            Object openlrData = requestBody.get("openlr_data");

            if (openlrData instanceof String) {
                ctx.json(decodeOpenLR((String) openlrData));
                return;
            }

            if (openlrData instanceof List<?>) {
                List<?> openlrList = (List<?>) openlrData;

                if (openlrList.size() <= 10) {  // 限制最多 10 筆
                    List<Map<String, Object>> decodedList = openlrList.stream()
                            .map(obj -> decodeOpenLR(obj.toString()))
                            .collect(Collectors.toList());
                    ctx.json(Map.of("decoded_results", decodedList));
                } else {
                    ctx.status(400).json(Map.of(
                            "error", "Too many OpenLR entries. Maximum allowed is 10.",
                            "max_allowed", 10,
                            "received", openlrList.size()
                    ));
                }
                return;
            }
            ctx.status(400).json(Map.of(
                    "error", "Invalid input format",
                    "expected", "JSON with key 'openlr_data' containing a string or an array of strings"
            ));

        });

//        app.exception(JsonProcessingException.class, (e, ctx) -> {
//            ctx.status(400).json(Map.of(
//                    "error", "Invalid input format",
//                    "expected", "JSON with key 'openlr_data' containing a string or an array of strings"
//            ));
//        });
//
//        app.exception(IllegalArgumentException.class, (e, ctx) -> {
//            ctx.status(400).json(Map.of(
//                    "error", "Invalid input format",
//                    "expected", "JSON with key 'openlr_data' containing a string or an array of strings"
//            ));
//        });

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500).json(Map.of(
                    "error", "Internal Server Error",
                    "message", e.getMessage()
            ));
        });

        app.exception(UnableToDecodeException.class, (e, ctx) -> {
            ctx.status(400).json(Map.of(
                    "error", "Unable to decode OpenLR data",
                    "message", e.getMessage()
            ));
        });

        System.out.println("OpenLR Decoder API is running on port 5000");
    }


}
