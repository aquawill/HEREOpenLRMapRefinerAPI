package org.foobar;

import io.javalin.Javalin;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.foobar.OpenLRDecoder.decodeOpenLR;

public class ApiServer {
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
