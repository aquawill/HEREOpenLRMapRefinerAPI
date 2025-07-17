package org.foobar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LocationReferenceParser {
    public static String parseToJson(String input) throws JsonProcessingException{
        try {
            ObjectMapper mapper = new ObjectMapper();
            return parseFlatString(input, mapper);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }




    public static String parseFlatString(String input, ObjectMapper mapper) throws JsonProcessingException {
        StringBuilder result = new StringBuilder();

        Pattern pattern = Pattern.compile("^(.*?)(?=FirstLocationReferencePoint)");
        Matcher matcher = pattern.matcher(input);

        String objectType = "";

        if (matcher.find()) {
            String extracted = matcher.group(1);

            long openCount = extracted.chars().filter(ch -> ch == '(').count();
            long closeCount = extracted.chars().filter(ch -> ch == ')').count();

            if (openCount == 1 && closeCount == 0) {
                objectType = extracted.substring(0, extracted.indexOf('('));
            } else if (openCount == 2 && closeCount == 0) {
                int secondOpen = extracted.indexOf('(', extracted.indexOf('(') + 1);
                objectType = extracted.substring(0, secondOpen) + ")";
            }
        }

        result.append("type: OLR " + objectType + "\n");

        Pattern offsetPattern = Pattern.compile("DistanceMetresMax15000\\((\\d+)\\)");
        Matcher offsetMatcher = offsetPattern.matcher(input);
        int[] distances = new int[3];
        int i = 0;
        while (offsetMatcher.find() && i < 3) {
            distances[i++] = Integer.parseInt(offsetMatcher.group(1));
        }
        if (i > 1) {
            result.append("positiveOffset: DistanceMetresMax15000(" + distances[1] + ")\n");
        }
        if (i > 2) {
            result.append("negativeOffset: DistanceMetresMax15000(" + distances[2] + ")\n");
        }

        Pattern firstPointPattern = Pattern.compile(
                "FirstLocationReferencePoint\\(AbsoluteGeoCoordinate\\((-?\\d+),(-?\\d+),None\\)," +
                        "LineProperties\\(FunctionalRoadClass\\((\\d+)\\),FormOfWay\\((\\d+)\\),Bearing\\((\\d+)\\)" +
                        ",None,None\\),PathProperties\\(FunctionalRoadClass\\((\\d+)\\),DistanceMetresMax15000\\((\\d+)\\),false\\)"
        );
        Matcher firstMatcher = firstPointPattern.matcher(input);
        if (firstMatcher.find()) {
            result.append(formatReferencePoint("firstReferencePoint", firstMatcher, mapper));
        }

        Pattern intermediatePattern = Pattern.compile(
                "IntermediateLocationReferencePoint\\(RelativeGeoCoordinate\\((-?\\d+),(-?\\d+),None\\)," +
                        "LineProperties\\(FunctionalRoadClass\\((\\d+)\\),FormOfWay\\((\\d+)\\),Bearing\\((\\d+)\\)" +
                        ",None,None\\),PathProperties\\(FunctionalRoadClass\\((\\d+)\\),DistanceMetresMax15000\\((\\d+)\\),false\\)"
        );
        Matcher intermediateMatcher = intermediatePattern.matcher(input);
        if (intermediateMatcher.find()) {
            result.append("intermediateReferencePoints:\n");
            do {
                result.append(formatRelativeReferencePoint("  -", firstMatcher, intermediateMatcher, mapper));
            } while (intermediateMatcher.find());
        }

        Pattern lastPointPattern = Pattern.compile(
                "LastLocationReferencePoint\\(RelativeGeoCoordinate\\((-?\\d+),(-?\\d+),None\\)," +
                        "LineProperties\\(FunctionalRoadClass\\((\\d+)\\),FormOfWay\\((\\d+)\\),Bearing\\((\\d+)\\)"
        );
        Matcher lastMatcher = lastPointPattern.matcher(input);
        if (lastMatcher.find()) {
            Matcher referenceMatcher = firstMatcher;
            if (intermediateMatcher.find()) {
                referenceMatcher = intermediateMatcher;
            }
            result.append(formatRelativeReferencePoint("lastReferencePoint", referenceMatcher, lastMatcher, mapper));
        }

        return parsePrettyPrintString(result.toString(), mapper);
    }


    private static String formatReferencePoint(String label, Matcher matcher, ObjectMapper mapper) {
        StringBuilder point = new StringBuilder();
        int x = Integer.parseInt(matcher.group(1));
        int y = Integer.parseInt(matcher.group(2));
        int frc = Integer.parseInt(matcher.group(3));
        int fow = Integer.parseInt(matcher.group(4));
        int bearing = Integer.parseInt(matcher.group(5));
        int pathFrc = Integer.parseInt(matcher.group(6));
        int dnp = Integer.parseInt(matcher.group(7));

        point.append(label + ":\n");
        point.append("    coordinate: " + x + "," + y + " => " + CoordinateConverter.convertToWGS84(x, y) + "\n");
        point.append("    lineProperties:\n");
        point.append("        bearing: " + bearing + " => " + convertBearingToAzimuth(bearing) + "°\n");
        point.append("        frc: " + frc + " (0-7, with 0 most important)\n");
        point.append("        fow: " + fow + " => " + getFowDescription(fow) + "\n");
        point.append("    pathProperties:\n");
        point.append("        lfrcnp: " + pathFrc + " (0-7, with 0 most important)\n");
        point.append("        dnp: " + dnp + " m\n");
        point.append("        againstDrivingDirection: false\n");

        return point.toString();
    }

    private static String formatRelativeReferencePoint(String label, Matcher firstMatcher, Matcher lastMatcher, ObjectMapper mapper) {
        StringBuilder point = new StringBuilder();
        int dx = Integer.parseInt(lastMatcher.group(1));
        int dy = Integer.parseInt(lastMatcher.group(2));
        int frc = Integer.parseInt(lastMatcher.group(3));
        int fow = Integer.parseInt(lastMatcher.group(4));
        int bearing = Integer.parseInt(lastMatcher.group(5));

        point.append(label + ":\n");
        point.append("    coordinate: " + dx + "," + dy + " => " + CoordinateConverter.convertToWGS84RelativeString(firstMatcher, dx, dy) + "\n");
        point.append("    lineProperties:\n");
        point.append("        bearing: " + bearing + " => " + convertBearingToAzimuth(bearing) + "°\n");
        point.append("        frc: " + frc + " [0-7], with 0 most important\n");
        point.append("        fow: " + fow + " => " + getFowDescription(fow) + "\n");

        return point.toString();
    }

    public static double convertBearingToAzimuth(int bearingValue) {
        return Math.round(bearingValue * 360.0 / 256 * 10.0) / 10.0;
    }

    public static int convertAzimuthToBearing(double azimuth) {
    return (int) Math.round(azimuth * 256.0 / 360.0);
}


    static String parsePrettyPrintString(String input, ObjectMapper mapper) throws JsonProcessingException {
        ObjectNode rootNode = mapper.createObjectNode();

        Pattern typePattern = Pattern.compile("type: (.+)");
        Matcher typeMatcher = typePattern.matcher(input);
        if (typeMatcher.find()) {
            rootNode.put("type", typeMatcher.group(1).trim());
        }

        Pattern offsetPattern = Pattern.compile("(positiveOffset|negativeOffset): DistanceMetresMax15000\\((\\d+)\\)");
        Matcher offsetMatcher = offsetPattern.matcher(input);
        while (offsetMatcher.find()) {
            String key = offsetMatcher.group(1);
            int value = Integer.parseInt(offsetMatcher.group(2));
            rootNode.put(key, value);
        }

        ObjectNode firstPoint = parseReferencePoint(input, "firstReferencePoint", mapper);
        if (firstPoint != null) {
            rootNode.set("firstReferencePoint", firstPoint);
        }

        List<ObjectNode> intermediates = parseIntermediatePoints(input, mapper);
        if (!intermediates.isEmpty()) {
            ArrayNode intermediateArray = mapper.createArrayNode();
            intermediates.forEach(intermediateArray::add);
            rootNode.set("intermediateReferencePoints", intermediateArray);
        }

        ObjectNode lastPoint = parseReferencePoint(input, "lastReferencePoint", mapper);
        if (lastPoint != null) {
            rootNode.set("lastReferencePoint", lastPoint);
        }

        String result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);

//        System.out.println(result);
    
        return result;
    }


    private static ObjectNode parseReferencePoint(String input, String section, ObjectMapper mapper) {
        ObjectNode pointNode = mapper.createObjectNode();
        String sectionPattern = section + ":[\\s\\S]+?(?=intermediateReferencePoints|lastReferencePoint|$)"
                + (section.equals("lastReferencePoint") ? "" : "(?=\\n\\s*\\w+:|$)");
        Pattern pattern = Pattern.compile(sectionPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String sectionContent = matcher.group(0);


            Pattern coordPattern = Pattern.compile("coordinate: ([-]?\\d+),([-]?\\d+) => ([-]?\\d+\\.\\d+),([-]?\\d+\\.\\d+)");
            Matcher coordMatcher = coordPattern.matcher(sectionContent);
            if (coordMatcher.find()) {
                ObjectNode coordNode = mapper.createObjectNode();
                coordNode.put("x", Integer.parseInt(coordMatcher.group(1)));
                coordNode.put("y", Integer.parseInt(coordMatcher.group(2)));
                coordNode.put("lat", Double.parseDouble(coordMatcher.group(3)));
                coordNode.put("lon", Double.parseDouble(coordMatcher.group(4)));
                pointNode.set("coordinate", coordNode);
            }


            ObjectNode lineProps = mapper.createObjectNode();
            Pattern bearingPattern = Pattern.compile("bearing: (\\d+) => ([-]?\\d+\\.\\d+)°");
            Matcher bearingMatcher = bearingPattern.matcher(sectionContent);
            if (bearingMatcher.find()) {
                lineProps.put("bearing", Integer.parseInt(bearingMatcher.group(1)));
                lineProps.put("bearingDegrees", Double.parseDouble(bearingMatcher.group(2)));
            }

            Pattern frcPattern = Pattern.compile("frc: (\\d+)");
            Matcher frcMatcher = frcPattern.matcher(sectionContent);
            if (frcMatcher.find()) {
                lineProps.put("frc", Integer.parseInt(frcMatcher.group(1)));
            }

            Pattern fowPattern = Pattern.compile("fow: (\\d+) => (.+)");
            Matcher fowMatcher = fowPattern.matcher(sectionContent);
            if (fowMatcher.find()) {
                lineProps.put("fow", Integer.parseInt(fowMatcher.group(1)));
                lineProps.put("fowDescription", fowMatcher.group(2).trim());
            }
            if (lineProps.size() > 0) {
                pointNode.set("lineProperties", lineProps);
            }


            if (section.equals("firstReferencePoint")) {
                ObjectNode pathProps = mapper.createObjectNode();
                Pattern lfrcnpPattern = Pattern.compile("lfrcnp: (\\d+)");
                Matcher lfrcnpMatcher = lfrcnpPattern.matcher(sectionContent);
                if (lfrcnpMatcher.find()) {
                    pathProps.put("lfrcnp", Integer.parseInt(lfrcnpMatcher.group(1)));
                }

                Pattern dnpPattern = Pattern.compile("dnp: (\\d+) m");
                Matcher dnpMatcher = dnpPattern.matcher(sectionContent);
                if (dnpMatcher.find()) {
                    pathProps.put("dnp", Integer.parseInt(dnpMatcher.group(1)));
                }

                Pattern againstPattern = Pattern.compile("againstDrivingDirection: (true|false)");
                Matcher againstMatcher = againstPattern.matcher(sectionContent);
                if (againstMatcher.find()) {
                    pathProps.put("againstDrivingDirection", Boolean.parseBoolean(againstMatcher.group(1)));
                }
                if (pathProps.size() > 0) {
                    pointNode.set("pathProperties", pathProps);
                }
            }
        }
        return pointNode.size() > 0 ? pointNode : null;
    }

    private static List<ObjectNode> parseIntermediatePoints(String input, ObjectMapper mapper) {
        List<ObjectNode> intermediates = new ArrayList<>();
        Pattern interPattern = Pattern.compile("intermediateReferencePoints:[\\s\\S]+?(?=lastReferencePoint|$)", Pattern.DOTALL);
        Matcher interMatcher = interPattern.matcher(input);

        if (interMatcher.find()) {
            String interContent = interMatcher.group(0);

            Pattern pointPattern = Pattern.compile("-\\s+coordinate: ([-]?\\d+),([-]?\\d+) => ([-]?\\d+\\.\\d+),([-]?\\d+\\.\\d+)[\\s\\S]+?(?=-\\scoordinate:|$)", Pattern.DOTALL);
            Matcher pointMatcher = pointPattern.matcher(interContent);

            while (pointMatcher.find()) {
                ObjectNode interNode = mapper.createObjectNode();


                ObjectNode coordNode = mapper.createObjectNode();
                coordNode.put("x", Integer.parseInt(pointMatcher.group(1)));
                coordNode.put("y", Integer.parseInt(pointMatcher.group(2)));
                coordNode.put("lat", Double.parseDouble(pointMatcher.group(3)));
                coordNode.put("lon", Double.parseDouble(pointMatcher.group(4)));
                interNode.set("coordinate", coordNode);


                ObjectNode lineProps = new ObjectNode(mapper.getNodeFactory());
                Pattern bearingPattern = Pattern.compile("bearing: (\\d+) => ([-]?\\d+\\.\\d+)°");
                Matcher bearingMatcher = bearingPattern.matcher(pointMatcher.group(0));
                if (bearingMatcher.find()) {
                    lineProps.put("bearing", Integer.parseInt(bearingMatcher.group(1)));
                    lineProps.put("bearingDegrees", Double.parseDouble(bearingMatcher.group(2)));
                }

                Pattern frcPattern = Pattern.compile("frc: (\\d+)");
                Matcher frcMatcher = frcPattern.matcher(pointMatcher.group(0));
                if (frcMatcher.find()) {
                    lineProps.put("frc", Integer.parseInt(frcMatcher.group(1)));
                }

                Pattern fowPattern = Pattern.compile("fow: (\\d+) => (.+)");
                Matcher fowMatcher = fowPattern.matcher(pointMatcher.group(0));
                if (fowMatcher.find()) {
                    lineProps.put("fow", Integer.parseInt(fowMatcher.group(1)));
                    lineProps.put("fowDescription", fowMatcher.group(2).trim());
                }
                interNode.set("lineProperties", lineProps);


                ObjectNode pathProps = new ObjectNode(mapper.getNodeFactory());
                Pattern lfrcnpPattern = Pattern.compile("lfrcnp: (\\d+)");
                Matcher lfrcnpMatcher = lfrcnpPattern.matcher(pointMatcher.group(0));
                if (lfrcnpMatcher.find()) {
                    pathProps.put("lfrcnp", Integer.parseInt(lfrcnpMatcher.group(1)));
                }

                Pattern dnpPattern = Pattern.compile("dnp: (\\d+) m");
                Matcher dnpMatcher = dnpPattern.matcher(pointMatcher.group(0));
                if (dnpMatcher.find()) {
                    pathProps.put("dnp", Integer.parseInt(dnpMatcher.group(1)));
                }

                Pattern againstPattern = Pattern.compile("againstDrivingDirection: (true|false)");
                Matcher againstMatcher = againstPattern.matcher(pointMatcher.group(0));
                if (againstMatcher.find()) {
                    pathProps.put("againstDrivingDirection", Boolean.parseBoolean(againstMatcher.group(1)));
                }
                interNode.set("pathProperties", pathProps);

                intermediates.add(interNode);
            }
        }
        return intermediates;
    }

    public static String generateCsvFromDecodedResult(JsonNode decodedResult) {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("ID,LATITUDE,LONGITUDE\n");


        JsonNode firstPoint = decodedResult.path("firstReferencePoint").path("coordinate");
        csvBuilder.append("F,")
                .append(firstPoint.get("lat").asDouble()).append(",")
                .append(firstPoint.get("lon").asDouble()).append("\n");


        JsonNode intermediatePoints = decodedResult.path("intermediateReferencePoints");
        for (int i = 0; i < intermediatePoints.size(); i++) {
            JsonNode point = intermediatePoints.get(i).path("coordinate");
            csvBuilder.append("I").append(i).append(",")
                    .append(point.get("lat").asDouble()).append(",")
                    .append(point.get("lon").asDouble()).append("\n");
        }


        JsonNode lastPoint = decodedResult.path("lastReferencePoint").path("coordinate");
        csvBuilder.append("L,")
                .append(lastPoint.get("lat").asDouble()).append(",")
                .append(lastPoint.get("lon").asDouble()).append("\n");

        return csvBuilder.toString();
    }

    private static String getFowDescription(int fow) {
        switch (fow) {
            case 0:
                return "undefined";
            case 1:
                return "motorway";
            case 2:
                return "multiple carriageway";
            case 3:
                return "single carriageway";
            case 4:
                return "roundabout";
            case 5:
                return "trafficsquare";
            case 6:
                return "sliproad";
            case 7:
                return "other";
            case 8:
                return "bike_path";
            case 9:
                return "footpath";
            default:
                return "unknown";
        }
    }


//    private static String generateRoutingUrl(ObjectNode first, List<ObjectNode> intermediates, ObjectNode last) {
//        if (first == null || last == null) return "";
//
//        ObjectNode firstCoord = (ObjectNode) first.get("coordinate");
//        ObjectNode lastCoord = (ObjectNode) last.get("coordinate");
//        if (firstCoord == null || lastCoord == null) return "";
//
//        String apiKey = "DEFAULT_API_KEY";
//
//        StringBuilder csvBuilder = new StringBuilder();
//        csvBuilder.append("ID,LATITUDE,LONGITUDE\n");
//
//        csvBuilder.append("F,")
//                .append(firstCoord.get("lat").asText()).append(",")
//                .append(firstCoord.get("lon").asText()).append("\n");
//
//        for (int i = 0; i < intermediates.size(); i++) {
//            ObjectNode interCoord = (ObjectNode) intermediates.get(i).get("coordinate");
//            if (interCoord != null) {
//                csvBuilder.append("I").append(i).append(",")
//                        .append(interCoord.get("lat").asText()).append(",")
//                        .append(interCoord.get("lon").asText()).append("\n");
//            }
//        }
//        csvBuilder.append("L,")
//                .append(lastCoord.get("lat").asText()).append(",")
//                .append(lastCoord.get("lon").asText()).append("\n");
//
//        String postData = "{\"InputTrace\":\"" + csvBuilder.toString() + "\"}";
//
//        String encodedPostData = postData.replace("\"", "%22");
//
//        return "https://demo.routing.ext.here.com/#url=https://routematching.hereapi.com/v8/match/routelinks?"
//                + "attributes=LINK_FCn(*),LINK_TMC_FCn(*),ROAD_GEOM_FCn(*)"
//                + "&mode=car"
//                + "&routeMatch=1"
//                + "&apiKey=" + apiKey
//                + "&postData=" + encodedPostData;
//    }


}