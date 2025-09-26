package org.foobar.mapRefiner;

import com.here.platform.location.tpeg2.olr.AbsoluteGeoCoordinate;

import java.util.Optional;
import java.util.regex.Matcher;

public class CoordinateConverter {
    private static final double SCALE_FACTOR = Math.pow(2, 24) / 360.0;


    public static String convertToWGS84(int x, int y) {
        return convertIntToDegrees(y) + "," + convertIntToDegrees(x);
    }

    public static int[] calculateDeltaXY(int originX, int originY, double targetDegreesLat, double targetDegreesLon) {
        // 轉換基準點為 WGS84
        double baseLat = convertIntToDegrees(originY);
        double baseLon = convertIntToDegrees(originX);

        // 計算 dx, dy（使用 1E-5 度解析度）
        int dy = (int) Math.round((targetDegreesLat - baseLat) * 100000);
        int dx = (int) Math.round((targetDegreesLon - baseLon) * 100000);

        return new int[]{dx, dy};
    }

    public static String convertToWGS84RelativeString(int x, int y, int dx, int dy) {
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


    public static String convertToWGS84RelativeString(Matcher firstMatcher, int dx, int dy) {
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

    public static int convertDegreestoIntAngle(double degrees) {
        return (int) (0.5 * Math.signum(degrees) + degrees * Math.pow(2, 24) / 360.0);
    }

    public static double convertIntToDegrees(int intAngle) {
        double degrees = intAngle * 360.0 / Math.pow(2, 24);
//        System.out.printf("intAngle %s, degrees %s%n", intAngle, degrees);
        return degrees;
    }

    static int convertDegreesToInt(double degrees) {
        return (int) Math.round(degrees * SCALE_FACTOR);
    }

    public static AbsoluteGeoCoordinate fromWgs84DegreesToAbsGeoCoord(double latitude, double longitude) {
        int latInt = convertDegreestoIntAngle(latitude);
        int lonInt = convertDegreestoIntAngle(longitude);
        return new AbsoluteGeoCoordinate(latInt, lonInt, Optional.empty());
    }


    public static void main(String[] args) {
//        int intDegreesLongitude = -4622669;
//        int intDegreesLatitude = 903352;
//        int intDegreesLongitudeOffset = 47;
//        int intDegreesLatitudeOffset = 90;
//
//        double wgs84Latitude = 19.39819;
//        double wgs84Longitude = -99.15080;

//        System.out.println(LocationReferenceParser.convertBearingToAzimuth(230));

//        System.out.println(String.format("1st wgs84 point: %s, %s", convertIntToDegrees(intDegreesLatitude), convertIntToDegrees(intDegreesLongitude)));
//        System.out.println(String.format("last wgs84 point: %s, %s", convertIntToDegrees(intDegreesLatitude + intDegreesLatitudeOffset), convertIntToDegrees(intDegreesLongitude + intDegreesLongitudeOffset)));
        double oriLng = 121.541898;
        double oriLat = 25.036104;

        int oriLngInt = convertDegreesToInt(oriLng);
        int oriLatInt = convertDegreesToInt(oriLat);

//        System.out.println(String.format(String.format("%s,%s", convertDegreesToInt(oriLng), convertDegreesToInt(oriLat))));
//        System.out.println(String.format(String.format("%s,%s", convertIntToDegrees(convertDegreesToInt(oriLng)), convertIntToDegrees(convertDegreesToInt(oriLat)))));

        double destLng = 121.548608;
        double destLat = 25.035984;

        int destLngOffset =  convertDegreesToInt(destLng) - convertDegreesToInt(oriLng);
        int destLatOffset =  convertDegreesToInt(destLat) - convertDegreesToInt(oriLat);

        double destLngDegree =  convertIntToDegrees(oriLngInt + destLngOffset);
        double destLatDegree = convertIntToDegrees(oriLatInt + destLatOffset);

//        System.out.println(String.format("offset: %s,%s", destLngOffset, destLatOffset));
//        System.out.println(String.format(String.format("offsetY: %s", convertDegreesToInt(destLat) - convertDegreesToInt(oriLat))));

        System.out.println(String.format(String.format("%s,%s", destLngDegree, destLatDegree)));

//        System.out.println(String.format("OLR Longitude: %s, Latitude: %s", convertDegreesToInt(wgs84Longitude), convertDegreesToInt(wgs84Latitude)));

//        System.out.println(String.format("%s, %s", convertIntToDegrees(-4620782), convertIntToDegrees(903759)));
//        System.out.println(String.format("%s, %s", convertIntToDegrees(-4620782 + 226), convertIntToDegrees(903759 + 1860)));
        System.out.println(String.format("%s", convertIntToDegrees(29165184)));
        System.out.println(String.format("%s", convertIntToDegrees(29703296)));
        System.out.println(String.format("%s", convertToWGS84(29703296,584299904)));


    }
}
