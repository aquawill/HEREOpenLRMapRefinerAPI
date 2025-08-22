package org.foobar.mapRefiner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.here.platform.location.referencing.olr.OlrPrettyPrinter;
import com.here.platform.location.tpeg2.BinaryMarshallers;
import com.here.platform.location.tpeg2.olr.*;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.foobar.mapRefiner.LocationReferenceParser.generateCsvFromDecodedResult;
import static org.foobar.mapRefiner.LocationReferenceParser.parsePrettyPrintString;
import static org.foobar.mapRefiner.RouteMatcher.*;
import static org.foobar.mapRefiner.ShapeProcessor.getTrimmedShape;

public class OpenLRDecoder {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, Object> decodeOpenLR(String base64Data) {
        return decodeOpenLR(base64Data, false, true); // 預設為 API 呼叫模式
    }

    public static Map<String, Object> decodeOpenLR(String base64Data, boolean performMapMatching) {
        return decodeOpenLR(base64Data, performMapMatching, true);
    }

    public static Map<String, Object> decodeOpenLR(String base64Data, boolean performMapMatching, boolean isApiCall) {
        try {
            OpenLRLocationReference reference = decodeBase64(base64Data);
            String parsedJson = parseLocationReference(reference);
            Map<String, Object> jsonMap = objectMapper.readValue(parsedJson, Map.class);

            if (!isApiCall) {
                return jsonMap;
            }

            Map<String, Object> mapMatchedShape = performMapMatching ? performMapMatching(parsedJson, jsonMap) : null;
            return buildResponse(base64Data, jsonMap, mapMatchedShape);
        } catch (UnableToDecodeException e) {
            return Map.of("error", e.getMessage(), "input", base64Data);
        } catch (Exception e) {
            return Map.of("error", "Unexpected error occurred");
        }
    }

    private static OpenLRLocationReference decodeBase64(String base64Data) {
        try {
            byte[] decode = Base64.getDecoder().decode(base64Data);
            OpenLRLocationReference reference = BinaryMarshallers.openLRLocationReference()
                    .unmarshall(new ByteArrayInputStream(decode));

            if (reference.getLocationReference() == null) {
                throw new UnableToDecodeException("Invalid OpenLR data: Decoded reference is null.");
            }
            return reference;
        } catch (IllegalArgumentException e) {
            throw new UnableToDecodeException("Invalid Base64 encoding: " + e.getMessage());
        } catch (AssertionError e) {
            throw new UnableToDecodeException("Invalid OpenLR format: " + e.getMessage());
        } catch (Exception e) {
            throw new UnableToDecodeException("Unexpected error while decoding OpenLR, please input proper OpenLR Base64 string.");
        }
    }

    private static String parseLocationReference(OpenLRLocationReference reference) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        if (reference.getLocationReference().toString().startsWith("LinearLocationReference")) {
            LinearLocationReference reference1 = (LinearLocationReference) reference.getLocationReference();
            return parsePrettyPrintString((OlrPrettyPrinter.prettyPrint(reference1).toString()), mapper);
        } else {
            return LocationReferenceParser.parseToJson(reference.getLocationReference().toString());
        }
    }

    private static Map<String, Object> performMapMatching(String parsedJson, Map<String, Object> jsonMap) throws Exception {
        JsonNode decodedResult = objectMapper.readTree(parsedJson);

        if (!decodedResult.has("type")) {
            throw new UnableToDecodeException("Decoded OpenLR data is missing required fields.");
        }

        String csvData = generateCsvFromDecodedResult(decodedResult);
        JsonNode matchedRouteResponse = fetchMatchedRoute(csvData);
        List<double[]> fullShape = fetchRouteShape(matchedRouteResponse);

        int positiveOffset = (int) jsonMap.getOrDefault("positiveOffset", 0);
        int negativeOffset = decodedResult.has("negativeOffset") ? decodedResult.get("negativeOffset").asInt() : -1;

        List<double[]> trimmedShape = getTrimmedShape(fullShape, positiveOffset, negativeOffset);
        String shapeStyle = trimmedShape.size() == 1 ? "point" : "polyline";
        String flexiblePolyline = PolylineUtil.encodeToFlexiblePolyline(trimmedShape);

        return Map.of(
                "style", shapeStyle,
                "trimmed_shape", trimmedShape,
                "flexible_polyline", flexiblePolyline
        );
    }

    private static Map<String, Object> buildResponse(String base64Data, Map<String, Object> jsonMap, Map<String, Object> mapMatchedShape) {
        if (mapMatchedShape != null) {
            return Map.of(
                    "input", base64Data,
                    "decoded_result", jsonMap,
                    "map_matched_shape", mapMatchedShape
            );
        } else {
            return Map.of(
                    "input", base64Data,
                    "decoded_result", jsonMap
            );
        }
    }
}
