package org.foobar.tileCli;

import com.here.olp.util.quad.HereQuad;
import com.here.olp.util.quad.factory.HereQuadFactory;

public class TileCli {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0];

        try {
            switch (command) {
                case "from-wgs84-to-tile-coords":
                    double lat = Double.parseDouble(args[1]);
                    double lng = Double.parseDouble(args[2]);
                    int level = Integer.parseInt(args[3]);
                    var result1 = TileConverter.wgs84ToTileXY(lat, lng, level);
                    System.out.println("{");
                    System.out.printf("  \"quadkey\": \"%s\",\n", result1.quadkey.getLongKey());
                    System.out.printf("  \"tile_row\": %d,\n", result1.tileRow);
                    System.out.printf("  \"tile_column\": %d,\n", result1.tileColumn);
                    System.out.printf("  \"tile_inner_x\": %d,\n", result1.innerX);
                    System.out.printf("  \"tile_inner_y\": %d\n", result1.innerY);
                    System.out.println("}");
                    break;

                case "from-tile-coords-to-wgs84":
                    int tileColumn = Integer.parseInt(args[1]);
                    int tileRow = Integer.parseInt(args[2]);
                    int x = Integer.parseInt(args[3]);
                    int y = Integer.parseInt(args[4]);
                    int level2 = Integer.parseInt(args[5]);
                    double[] wgs84 = TileConverter.tileXYToWGS84(x, y, tileRow, tileColumn, level2);
                    System.out.println("{");
                    System.out.printf("  \"lat\": %.8f,\n", wgs84[0]);
                    System.out.printf("  \"lng\": %.8f\n", wgs84[1]);
                    System.out.println("}");
                    break;

                case "from-quadkey-coords-to-wgs84":
                    String quadkey = args[1];
                    int qx = Integer.parseInt(args[2]);
                    int qy = Integer.parseInt(args[3]);
                    HereQuadFactory factory = HereQuadFactory.INSTANCE;

                    var tileInfo = factory.getMapQuadByLongKey(Integer.parseInt(quadkey));
                    double[] wgs84FromQk = TileConverter.tileXYToWGS84(
                            qx, qy, tileInfo.getY(), tileInfo.getX(), tileInfo.getZoomLevel()
                    );
                    System.out.println("{");
                    System.out.printf("  \"lat\": %.8f,\n", wgs84FromQk[0]);
                    System.out.printf("  \"lng\": %.8f,\n", wgs84FromQk[1]);
                    System.out.printf("  \"tile_level\": %d,\n", tileInfo.getZoomLevel());
                    System.out.printf("  \"tile_row\": %d,\n", tileInfo.getY());
                    System.out.printf("  \"tile_column\": %d\n", tileInfo.getX());
                    System.out.println("}");
                    break;

                case "from-quadkey-to-tile":
                    String qk = args[1];
                    var qkInfo = HereQuadFactory.INSTANCE.getMapQuadByLongKey(Integer.parseInt(qk));
                    System.out.println("{");
                    System.out.printf("  \"tile_row\": %d,\n", qkInfo.getY());
                    System.out.printf("  \"tile_column\": %d,\n", qkInfo.getX());
                    System.out.printf("  \"tile_level\": %d\n", qkInfo.getZoomLevel());
                    System.out.println("}");
                    break;

                case "from-tile-to-quadkey":
                    int tc = Integer.parseInt(args[1]);
                    int tr = Integer.parseInt(args[2]);
                    int lvl = Integer.parseInt(args[3]);
                    long tileQuadkey = HereQuadFactory.INSTANCE.getMapQuadByXYLevel(tc, tr, lvl).getLongKey();
                    System.out.println("{");
                    System.out.printf("  \"quadkey\": \"%s\"\n", tileQuadkey);
                    System.out.println("}");
                    break;

                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("Tile Coordinate Converter CLI (Java)");
        System.out.println("Usage:");
        System.out.println("  from-wgs84-to-tile-coords <lat> <lng> <level>");
        System.out.println("  from-tile-coords-to-wgs84 <tile_column> <tile_row> <x> <y> <level>");
        System.out.println("  from-quadkey-coords-to-wgs84 <quadkey> <x> <y>");
        System.out.println("  from-quadkey-to-tile <quadkey>");
        System.out.println("  from-tile-to-quadkey <tile_column> <tile_row> <level>");
    }
}
