package org.foobar.mapRefiner;

import com.here.flexpolyline.PolylineEncoderDecoder;
import com.here.flexpolyline.PolylineEncoderDecoder.ThirdDimension;
import java.util.List;
import java.util.ArrayList;

public class PolylineUtil {

 
    public static String encodeToFlexiblePolyline(List<double[]> shape) {
        List<PolylineEncoderDecoder.LatLngZ> coordinates = new ArrayList<>();
        for (double[] point : shape) {
            coordinates.add(new PolylineEncoderDecoder.LatLngZ(point[0], point[1], 0)); // 0 表示沒有高度
        }

        int precision = 5; // 經緯度小數點後 5 位數
        ThirdDimension thirdDimension = ThirdDimension.ABSENT; // 沒有高度資訊
        int thirdDimPrecision = 0; // 不用高度精度

        return PolylineEncoderDecoder.encode(coordinates, precision, thirdDimension, thirdDimPrecision);
    }
}
