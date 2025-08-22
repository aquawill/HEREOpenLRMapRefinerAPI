/*
 * Copyright (C) 2019 HERE Europe B.V.
 * Licensed under MIT, see full license in LICENSE
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */
package com.here.flexpolyline;

import static com.here.flexpolyline.PolylineEncoderDecoder.decode;
import static com.here.flexpolyline.PolylineEncoderDecoder.encode;
import static com.here.flexpolyline.PolylineEncoderDecoder.getThirdDimension;
import static com.here.flexpolyline.PolylineEncoderDecoder.ThirdDimension.ABSENT;
import static com.here.flexpolyline.PolylineEncoderDecoder.ThirdDimension.ALTITUDE;
import static com.here.flexpolyline.PolylineEncoderDecoder.ThirdDimension.ELEVATION;
import static com.here.flexpolyline.PolylineEncoderDecoder.ThirdDimension.LEVEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.here.flexpolyline.PolylineEncoderDecoder.Converter;
import com.here.flexpolyline.PolylineEncoderDecoder.LatLngZ;
import com.here.flexpolyline.PolylineEncoderDecoder.ThirdDimension;

/**
 * Validate polyline encoding with different input combinations.
 */
public class PolylineEncoderDecoderTest {

    @Test
    public void testInvalidCoordinates() {
        // Null coordinates
        assertThrows(IllegalArgumentException.class,
                     () -> encode(null, 5, ThirdDimension.ABSENT, 0));

        // Empty coordinates list test
        assertThrows(IllegalArgumentException.class,
                     () -> encode(new ArrayList<LatLngZ>(), 5, ThirdDimension.ABSENT, 0));
    }

    @Test
    public void testInvalidThirdDimension() {
        List<LatLngZ> pairs = new ArrayList<>();
        pairs.add(new LatLngZ(50.1022829, 8.6982122));
        ThirdDimension invalid = null;

        // Invalid Third Dimension
        assertThrows(IllegalArgumentException.class,
                     () -> encode(pairs, 5, invalid, 0));
    }

    @Test
    public void testConvertValue() {
        Converter conv = new Converter(5);
        StringBuilder result = new StringBuilder();
        conv.encodeValue(-179.98321, result);
        assertEquals("h_wqiB", result.toString());
    }

    @Test
    public void testSimpleLatLngEncoding() {
        List<LatLngZ> pairs = new ArrayList<>();
        pairs.add(new LatLngZ(50.1022829, 8.6982122));
        pairs.add(new LatLngZ(50.1020076, 8.6956695));
        pairs.add(new LatLngZ(50.1006313, 8.6914960));
        pairs.add(new LatLngZ(50.0987800, 8.6875156));

        String expected = "BFoz5xJ67i1B1B7PzIhaxL7Y";
        String computed = encode(pairs, 5, ThirdDimension.ABSENT, 0);
        assertEquals(expected, computed);
    }

    @Test
    public void testComplexLatLngEncoding() {
        List<LatLngZ> pairs = new ArrayList<>();
        pairs.add(new LatLngZ(52.5199356, 13.3866272));
        pairs.add(new LatLngZ(52.5100899, 13.2816896));
        pairs.add(new LatLngZ(52.4351807, 13.1935196));
        pairs.add(new LatLngZ(52.4107285, 13.1964502));
        pairs.add(new LatLngZ(52.38871,   13.1557798));
        pairs.add(new LatLngZ(52.3727798, 13.1491003));
        pairs.add(new LatLngZ(52.3737488, 13.1154604));
        pairs.add(new LatLngZ(52.3875198, 13.0872202));
        pairs.add(new LatLngZ(52.4029388, 13.0706196));
        pairs.add(new LatLngZ(52.4105797, 13.0755529));

        String expected = "BF05xgKuy2xCx9B7vUl0OhnR54EqSzpEl-HxjD3pBiGnyGi2CvwFsgD3nD4vB6e";
        String computed = encode(pairs, 5, ThirdDimension.ABSENT, 0);
        assertEquals(expected, computed);
    }

    @Test
    public void testLatLngZEncode() {
        List<LatLngZ> tuples = new ArrayList<>();
        tuples.add(new LatLngZ(50.1022829, 8.6982122, 10));
        tuples.add(new LatLngZ(50.1020076, 8.6956695, 20));
        tuples.add(new LatLngZ(50.1006313, 8.6914960, 30));
        tuples.add(new LatLngZ(50.0987800, 8.6875156, 40));

        String expected = "BlBoz5xJ67i1BU1B7PUzIhaUxL7YU";
        String computed = encode(tuples, 5, ThirdDimension.ALTITUDE, 0);
        assertEquals(expected, computed);
    }

    /**********************************************/
    /********** Decoder test starts ***************/
    /**********************************************/

    @Test
    public void testInvalidEncoderInput() {
        // Null coordinates
        assertThrows(IllegalArgumentException.class,
                     () -> decode(null));

        // Empty coordinates list test
        assertThrows(IllegalArgumentException.class,
                     () -> decode(""));
    }

    @Test
    public void testThirdDimension() {
        assertTrue(getThirdDimension("BFoz5xJ67i1BU") == ABSENT);
        assertTrue(getThirdDimension("BVoz5xJ67i1BU") == LEVEL);
        assertTrue(getThirdDimension("BlBoz5xJ67i1BU") == ALTITUDE);
        assertTrue(getThirdDimension("B1Boz5xJ67i1BU") == ELEVATION);
    }

    @Test
    public void testDecodeConvertValue() {
        String encoded = "h_wqiB";
        double expected = -179.98321;
        Converter conv = new Converter(5);
        final double computed = conv.decodeValue(new StringCharacterIterator(encoded));
        assertEquals(expected, computed, 0.000001);
    }

    @Test
    public void testSimpleLatLngDecoding() {
        List<LatLngZ> computed = decode("BFoz5xJ67i1B1B7PzIhaxL7Y");
        List<LatLngZ> expected = new ArrayList<>();
        expected.add(new LatLngZ(50.10228, 8.69821));
        expected.add(new LatLngZ(50.10201, 8.69567));
        expected.add(new LatLngZ(50.10063, 8.69150));
        expected.add(new LatLngZ(50.09878, 8.68752));

        assertEquals(expected.size(), computed.size());
        for (int i = 0; i < computed.size(); ++i) {
            assertEquals(expected.get(i), computed.get(i));
        }
    }

    @Test
    public void testComplexLatLngDecoding() {
        List<LatLngZ> computed = decode("BF05xgKuy2xCx9B7vUl0OhnR54EqSzpEl-HxjD3pBiGnyGi2CvwFsgD3nD4vB6e");

        List<LatLngZ> pairs = new ArrayList<>();
        pairs.add(new LatLngZ(52.51994, 13.38663));
        pairs.add(new LatLngZ(52.51009, 13.28169));
        pairs.add(new LatLngZ(52.43518, 13.19352));
        pairs.add(new LatLngZ(52.41073, 13.19645));
        pairs.add(new LatLngZ(52.38871, 13.15578));
        pairs.add(new LatLngZ(52.37278, 13.14910));
        pairs.add(new LatLngZ(52.37375, 13.11546));
        pairs.add(new LatLngZ(52.38752, 13.08722));
        pairs.add(new LatLngZ(52.40294, 13.07062));
        pairs.add(new LatLngZ(52.41058, 13.07555));

        assertEquals(pairs.size(), computed.size());
        for (int i = 0; i < computed.size(); ++i) {
            assertEquals(pairs.get(i), computed.get(i));
        }
    }

    @Test
    public void testLatLngZDecode() {
        List<LatLngZ> computed = decode("BlBoz5xJ67i1BU1B7PUzIhaUxL7YU");
        List<LatLngZ> tuples = new ArrayList<>();

        tuples.add(new LatLngZ(50.10228, 8.69821, 10));
        tuples.add(new LatLngZ(50.10201, 8.69567, 20));
        tuples.add(new LatLngZ(50.10063, 8.69150, 30));
        tuples.add(new LatLngZ(50.09878, 8.68752, 40));

        assertEquals(tuples.size(), computed.size());
        for (int i = 0; i < computed.size(); ++i) {
            assertEquals(tuples.get(i), computed.get(i));
        }
    }
}

