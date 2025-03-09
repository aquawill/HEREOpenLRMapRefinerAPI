package org.foobar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.regex.Matcher;

public class CoordinateConverter {
    private static final double SCALE_FACTOR = Math.pow(2, 24) / 360.0;

    /**
     * 绝对坐标转换为 WGS84
     */
    public static String convertToWGS84(int x, int y) {
        return convertIntToDegrees(y) + "," + convertIntToDegrees(x);
    }

    public static String convertToWGS84Relative(int x, int y, int dx, int dy) {
        // 解析 firstReferencePoint 的絕對座標

        // 轉換 firstReferencePoint 為 WGS84
        double baseLat = convertIntToDegrees(y);
        double baseLon = convertIntToDegrees(x);

        // 使用 OpenLR 規範換算 RelativeGeoCoordinate
        double latOffset = dy / 100000.0;  // 1E-5 度的解析度
        double lonOffset = dx / 100000.0;

        double degreesLat = baseLat + latOffset;
        double degreesLon = baseLon + lonOffset;

        return String.format("%s,%s", degreesLat, degreesLon);
    }


    public static String convertToWGS84Relative(Matcher firstMatcher, int dx, int dy) {
        // 解析 firstReferencePoint 的絕對座標
        int baseX = Integer.parseInt(firstMatcher.group(1));
        int baseY = Integer.parseInt(firstMatcher.group(2));

        // 轉換 firstReferencePoint 為 WGS84
        double baseLat = convertIntToDegrees(baseY);
        double baseLon = convertIntToDegrees(baseX);

        // 使用 OpenLR 規範換算 RelativeGeoCoordinate
        double latOffset = dy / 100000.0;  // 1E-5 度的解析度
        double lonOffset = dx / 100000.0;

        double degreesLat = baseLat + latOffset;
        double degreesLon = baseLon + lonOffset;

        return String.format("%s,%s", degreesLat, degreesLon);
    }


//    public static String convertToWGS84Relative(Matcher firstMatcher, int dx, int dy) {
//        int baseX = Integer.parseInt(firstMatcher.group(1));
//        int baseY = Integer.parseInt(firstMatcher.group(2));
//        System.out.println(String.format("baseX %s, baseY %s, dx %s, dy %s", baseX, baseY, dx, dy));
//        return convertIntToDegrees(baseY + dy) + "," + convertIntToDegrees(baseX + dx);
//    }

    private static double convertIntToDegrees(int intAngle) {
        double degrees = intAngle * 360.0 / Math.pow(2, 24);
        System.out.printf("intAngle %s, degrees %s%n", intAngle, degrees);
        return degrees;
    }

    private static int convertDegreesToInt(double degrees) {
        return (int) Math.round(degrees * SCALE_FACTOR);
    }

    public static void main(String[] args) {
        System.out.println(convertIntToDegrees(-174122));
        System.out.println(convertIntToDegrees(-174122 -54));
        System.out.println(convertIntToDegrees(-1794889));
        System.out.println(convertIntToDegrees(-1794889 -19));
    }
}
