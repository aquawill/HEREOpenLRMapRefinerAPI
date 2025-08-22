package org.foobar.mapRefiner;

import java.util.ArrayList;
import java.util.List;

public class ShapeProcessor {

    public static List<double[]> getTrimmedShape(List<double[]> shape, int positiveOffset, int negativeOffset) {
        List<Double> segmentLengths = new ArrayList<>();
        double totalLength = 0.0;

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

        if (negativeOffset == -1) {
            if (startIndex == 0) {
                return List.of(shape.get(0));
            }
            if (startIndex >= shape.size()) {
                return List.of(shape.get(shape.size() - 1));
            }

            double remainingDistance = positiveOffset - startTrimmedLength;
            double[] startPoint = shape.get(startIndex - 1);
            double[] endPoint = shape.get(startIndex);

            double segmentDistance = segmentLengths.get(startIndex - 1);
            if (segmentDistance == 0) {
                return List.of(startPoint);
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

    public static double haversineDistance(double[] point1, double[] point2) {
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
