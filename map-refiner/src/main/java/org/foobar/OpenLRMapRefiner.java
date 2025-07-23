package org.foobar;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

public class OpenLRMapRefiner {

    public static Map<String, Object> decodeOpenLR(String base64Data) {
        return OpenLRDecoder.decodeOpenLR(base64Data);
    }

    public static JsonNode fetchMatchedRoute(String inputJson) throws Exception {
        return RouteMatcher.fetchMatchedRoute(inputJson);
    }

    public static List<double[]> getTrimmedShape(List<double[]> shape, int positiveOffset, int negativeOffset) {
        return ShapeProcessor.getTrimmedShape(shape, positiveOffset, negativeOffset);
    }
}
