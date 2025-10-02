package org.foobar.mapRefiner;

import com.here.platform.location.core.geospatial.GeoCoordinate;
import com.here.platform.location.core.graph.PropertyMap;
import com.here.platform.location.core.mapmatching.MatchResult;
import com.here.platform.location.core.mapmatching.MatchedPath.Transition;
import com.here.platform.location.core.mapmatching.OnRoad;
import com.here.platform.location.core.mapmatching.javadsl.MatchedPath;
import com.here.platform.location.core.mapmatching.javadsl.PathMatcher;
import com.here.platform.location.inmemory.geospatial.PackedGeoCoordinate;
import com.here.platform.location.inmemory.geospatial.PackedLineString;
import com.here.platform.location.inmemory.graph.Vertex;
import com.here.platform.location.integration.optimizedmap.graph.PropertyMaps;
import scala.collection.IndexedSeq;

import java.util.ArrayList;
import java.util.List;

import static org.foobar.mapRefiner.ApiServer.optimizedMap;
import static org.foobar.mapRefiner.ApiServer.pathMatchers;


public class SdkPathMatcher {

    public static List<double[]> fetchRouteShapeCustomRadius(List<GeoCoordinate> geoCoordinateList, double radiusMeters) {


        // 可調半徑的 PathMatcher（含 transitions）
        PathMatcher<GeoCoordinate, Vertex, List<Vertex>> matcher =
                PathMatchUtil.buildCarMatcherWithRadius(optimizedMap, radiusMeters);

        MatchedPath<Vertex, List<Vertex>> matched = matcher.matchPath(geoCoordinateList);

        // 轉成折線
        List<GeoCoordinate> coords = PathMatchUtil.toRouteShape(matched, optimizedMap);

        // 若你要 double[] 輸出
        List<double[]> out = new ArrayList<>(coords.size());
        for (GeoCoordinate gc : coords) {
            out.add(new double[]{gc.getLatitude(), gc.getLongitude()});
        }
        return out;
    }

    public static List<double[]> fetchRouteShape(List<GeoCoordinate> geoCoordinateList) {

        PathMatcher<GeoCoordinate, Vertex, List<Vertex>> pathMatcher = pathMatchers.unrestrictedPathMatcherWithTransitions();
        MatchedPath<Vertex, List<Vertex>> matchedPath;
        matchedPath = pathMatcher.matchPath(geoCoordinateList);
        if (matchedPath != null) {

            List<double[]> routed = new ArrayList<>();
            List<Transition<List<Vertex>>> transitions = matchedPath.transitions();

            PropertyMaps propertyMaps = new PropertyMaps(optimizedMap);
            PropertyMap<Vertex, PackedLineString> geometryMap = propertyMaps.geometry();

//            System.out.println("matchedPath.results().size(): " + matchedPath.results().size());
//            System.out.println("matchedPath.transitions().size(): " + matchedPath.transitions().size());

            if (!matchedPath.transitions().isEmpty()) {
                for (int matchedResultIndex = 0; matchedResultIndex < matchedPath.results().size(); matchedResultIndex++) {
                    MatchResult<Vertex> r = matchedPath.results().get(matchedResultIndex);
                    if (r instanceof OnRoad<?> on) {
                        GeoCoordinate gc = on.elementProjection().getNearest();
//                        System.out.printf("%s,%s\n", gc.getLatitude(), gc.getLongitude());
                        routed.add(new double[]{gc.getLatitude(), gc.getLongitude()});
                        if (transitions.size() > matchedResultIndex) {
                            List<Vertex> seg = transitions.get(matchedResultIndex).value();
                            for (Vertex v : seg) {
                                PackedLineString segGeom = geometryMap.apply(v);
                                IndexedSeq<PackedGeoCoordinate> indexedSeq = segGeom.toIndexedSeq();
                                scala.collection.immutable.List<PackedGeoCoordinate> packedGeoCoordinateList = indexedSeq.toList();
                                for (int geoCoordinateIndex = 0; geoCoordinateIndex < packedGeoCoordinateList.size(); geoCoordinateIndex++) {
                                    routed.add(new double[]{packedGeoCoordinateList.apply(geoCoordinateIndex).latitude(), packedGeoCoordinateList.apply(geoCoordinateIndex).longitude()});
                                }
                            }
                        }
                    }
                }
                return routed;
            } else {
                for (int vertexMatchResultIndex = 0; vertexMatchResultIndex < matchedPath.results().size(); vertexMatchResultIndex++) {
                    MatchResult<Vertex> vertexMatchResult = matchedPath.results().get(vertexMatchResultIndex);
                    if (!(vertexMatchResult instanceof OnRoad<?>)) {
                        throw new UnableToMatchRouteException(String.format("Unable to match route on reference point %s (%s,%s)", vertexMatchResultIndex, geoCoordinateList.get(vertexMatchResultIndex).getLatitude(), geoCoordinateList.get(vertexMatchResultIndex).getLongitude()));
                    }
                }
                return new ArrayList<>();
            }
        } else {
            return new ArrayList<>();
        }
    }
}
