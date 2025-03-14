package org.foobar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.here.platform.location.referencing.olr.OlrPrettyPrinter;
import com.here.platform.location.tpeg2.BinaryMarshallers;
import com.here.platform.location.tpeg2.olr.*;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.foobar.LocationReferenceParser.generateCsvFromDecodedResult;
import static org.foobar.LocationReferenceParser.parsePrettyPrintString;
import static org.foobar.RouteMatcher.*;

public class OpenLRDecoder {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, Object> decodeOpenLR(String base64Data) {
        return decodeOpenLR(base64Data, false);
    }

    public static Map<String, Object> decodeOpenLR(String base64Data, boolean performMapMatching) {
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
            if (performMapMatching) {
                JsonNode decodedResult = objectMapper.readTree(parsedJson);

                if (!decodedResult.has("type")) {
                    throw new UnableToDecodeException("Decoded OpenLR data is missing required fields.");
                }

                String csvData = generateCsvFromDecodedResult(decodedResult);
                JsonNode matchedRouteResponse = fetchMatchedRoute(csvData);

                List<double[]> fullShape = fetchRouteShape(matchedRouteResponse);

//            List<String> linkIdList = fetchLinkIds(matchedRouteResponse);

//            List<String> segmentRefList = fetchSegmentRefs(matchedRouteResponse);

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
            } else {
                return Map.of(
                        "input", base64Data,
                        "decoded_result", jsonMap
                );
            }


        } catch (UnableToDecodeException e) {
            return Map.of("error", e.getMessage(), "input", base64Data);
        } catch (Exception e) {
            return Map.of("error", "Unexpected error occurred");
        }
    }
}
