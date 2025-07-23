package org.foobar.tileCli;

import com.here.olp.util.quad.HereQuad;
import com.here.olp.util.quad.factory.HereQuadFactory;

public class TileConverter {

    private static final int WORLD_COORDINATE_BITS = 25;

    public static double[] tileXYToWGS84(
            int x, int y, int tileRow, int tileColumn, int tileLevel) {

        int shift = WORLD_COORDINATE_BITS - tileLevel;

        int iLatTile = tileRow << shift;
        int iLngTile = tileColumn << shift;

        int iLat = iLatTile + y;
        int iLng = iLngTile + x;

        double lat = (iLat * 360.0) / (1 << WORLD_COORDINATE_BITS) - 90.0;
        double lng = (iLng * 360.0) / (1 << WORLD_COORDINATE_BITS) - 180.0;

        return new double[]{lat, lng};
    }

    public static TileResult wgs84ToTileXY(double lat, double lng, int tileLevel) {
        int iLat = (int) (((lat + 90.0) / 360.0) * (1 << WORLD_COORDINATE_BITS));
        int iLng = (int) (((lng + 180.0) / 360.0) * (1 << WORLD_COORDINATE_BITS));

        int shift = WORLD_COORDINATE_BITS - tileLevel;
        int tileRow = iLat >> shift;
        int tileColumn = iLng >> shift;

        int y = iLat - (tileRow << shift);
        int x = iLng - (tileColumn << shift);

        HereQuadFactory factory = HereQuadFactory.INSTANCE;
        HereQuad tile = factory.getMapQuadByXYLevel(tileColumn, tileRow, tileLevel);

        return new TileResult(tile, tileRow, tileColumn, x, y, tileLevel);
    }

    public static class TileResult {
        public final HereQuad quadkey;
        public final int tileRow;
        public final int tileColumn;
        public final int innerX;
        public final int innerY;
        public final int tileLevel;

        public TileResult(HereQuad quadkey, int tileRow, int tileColumn, int innerX, int innerY, int tileLevel) {
            this.quadkey = quadkey;
            this.tileRow = tileRow;
            this.tileColumn = tileColumn;
            this.innerX = innerX;
            this.innerY = innerY;
            this.tileLevel = tileLevel;
        }
    }
}