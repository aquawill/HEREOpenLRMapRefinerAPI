package org.foobar.mapRefiner;

import com.here.platform.location.core.geospatial.GeoCoordinate;
import com.here.platform.location.core.geospatial.GreatCircleDistanceCalculator;
import com.here.platform.location.core.geospatial.javadsl.LineStringHolder;
import com.here.platform.location.core.geospatial.javadsl.ProximitySearch;
import com.here.platform.location.core.graph.javadsl.DirectedGraph;
import com.here.platform.location.core.graph.javadsl.PropertyMap;
import com.here.platform.location.core.graph.javadsl.RangeBasedPropertyMap;
import com.here.platform.location.core.mapmatching.MatchResult;
import com.here.platform.location.core.mapmatching.OnRoad;
import com.here.platform.location.core.mapmatching.javadsl.CandidateGenerator;
import com.here.platform.location.core.mapmatching.javadsl.CandidateGenerators;
import com.here.platform.location.core.mapmatching.javadsl.MatchedPath;
import com.here.platform.location.core.mapmatching.javadsl.PathMatcher;
import com.here.platform.location.inmemory.graph.Edge;
import com.here.platform.location.inmemory.graph.Vertex;
import com.here.platform.location.integration.optimizedmap.OptimizedMapLayers;
import com.here.platform.location.integration.optimizedmap.geospatial.javadsl.ProximitySearches;
import com.here.platform.location.integration.optimizedmap.graph.RoadAccess;
import com.here.platform.location.integration.optimizedmap.graph.javadsl.Graphs;
import com.here.platform.location.integration.optimizedmap.graph.javadsl.PropertyMaps;
import com.here.platform.location.integration.optimizedmap.mapmatching.javadsl.EmissionProbabilityStrategies;
import com.here.platform.location.integration.optimizedmap.mapmatching.javadsl.PathMatchers;
import com.here.platform.location.integration.optimizedmap.mapmatching.javadsl.TransitionProbabilityStrategies;

import java.util.ArrayList;
import java.util.List;

public class PathMatchUtil {

    public static PathMatcher<GeoCoordinate, Vertex, List<Vertex>>
    buildCarMatcherWithRadius(OptimizedMapLayers optimizedMap, double radiusMeters) {

        ProximitySearches prox = new ProximitySearches(optimizedMap);
        ProximitySearch<GeoCoordinate, Vertex> vertexSearch = prox.vertices();

        CandidateGenerator<GeoCoordinate, Vertex> cg =
                CandidateGenerators.fromProximitySearch(vertexSearch, radiusMeters);

        Graphs graphs = new Graphs(optimizedMap);
        DirectedGraph<Vertex, Edge> graph = graphs.forward();

        PropertyMaps pm = new PropertyMaps(optimizedMap);
        PropertyMap<Vertex, LineStringHolder<GeoCoordinate>> geometry = pm.geometry();
        PropertyMap<Vertex, Double> length = pm.length();
        RangeBasedPropertyMap<Vertex, Boolean> roadAccess = pm.roadAccess(RoadAccess.Automobile);

        var eps = EmissionProbabilityStrategies.<GeoCoordinate>usingDistance();
        var tps = TransitionProbabilityStrategies.<GeoCoordinate, Vertex, Edge>distanceWithTransitions(
                graph, geometry, length, roadAccess, GreatCircleDistanceCalculator.getInstance());

        return PathMatchers.newHMMPathMatcher(cg, eps, tps);
    }

    public static List<GeoCoordinate> toRouteShape(
            MatchedPath<Vertex, List<Vertex>> matched, OptimizedMapLayers optimizedMap) {

        PropertyMaps pm = new PropertyMaps(optimizedMap);
        PropertyMap<Vertex, LineStringHolder<GeoCoordinate>> geometry = pm.geometry();

        List<GeoCoordinate> projected = new ArrayList<>();
        for (MatchResult<Vertex> r : matched.results()) {
            if (r instanceof OnRoad) {
                @SuppressWarnings("unchecked")
                OnRoad<Vertex> on = (OnRoad<Vertex>) r;
                projected.add(on.elementProjection().getNearest());
            }
        }

        var transitions = matched.transitions(); // List<MatchedPath.Transition<List<Vertex>>>

        List<GeoCoordinate> shape = new ArrayList<>();
        if (!projected.isEmpty()) {
            shape.add(projected.get(0));
            for (int i = 0; i < transitions.size(); i++) {
                for (Vertex v : transitions.get(i).value()) {
                    shape.addAll(geometry.get(v).getPoints());
                }
                shape.add(projected.get(i + 1));
            }
        }
        return dedup(shape);
    }

    private static List<GeoCoordinate> dedup(List<GeoCoordinate> in) {
        if (in.isEmpty()) return in;
        List<GeoCoordinate> out = new ArrayList<>();
        GeoCoordinate prev = null;
        for (GeoCoordinate g : in) {
            if (prev == null || g.getLatitude() != prev.getLatitude() || g.getLongitude() != prev.getLongitude()) {
                out.add(g);
                prev = g;
            }
        }
        return out;
    }
}