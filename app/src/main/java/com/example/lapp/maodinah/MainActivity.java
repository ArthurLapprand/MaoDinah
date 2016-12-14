package com.example.lapp.maodinah;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;
import android.app.Activity;
import android.graphics.Bitmap;

import java.io.File;

public class MainActivity extends Activity  {

    static boolean initialized;
    static {
        if (!OpenCVLoader.initDebug()) {
            // do something
        } else {
            // quando inicializado
            initialized = true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (initialized) {
            helloworld();
        }
    }

    public void helloworld() {
        // make a mat and draw something
        //File f = new File("/sdcard/Download/");
        //String[] paths = f.list();
        Mat palm = Imgcodecs.imread(Environment.getExternalStorageDirectory().getPath() + "/Download/uJxZT6o.jpg", Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);

        Imgproc.GaussianBlur(palm, palm, new Size(19, 19), 500);
        Imgproc.equalizeHist(palm, palm);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(50, 50));
        //Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_BLACKHAT, kernel, new Point(25, 25), 2);

        Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(4, 4));
        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_GRADIENT, kernel2, new Point(2, 2), 20);
        Imgproc.threshold(palm, palm, 60, 255, Imgproc.THRESH_BINARY);
        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_OPEN, kernel2, new Point(2, 2), 13);
        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_OPEN, kernel2, new Point(2, 2), 13);
        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_OPEN, kernel2, new Point(2, 2), 13);
        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_CLOSE, kernel2, new Point(2, 2), 15);
        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_BLACKHAT, kernel2, new Point(2, 2), 50);
//        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_DILATE, kernel2, new Point(2, 2), 20);
//        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_CLOSE, kernel2, new Point(2, 2), 20);
//        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_DILATE, kernel2, new Point(2, 2), 20);
//        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_CLOSE, kernel2, new Point(2, 2), 20);
//        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_BLACKHAT, kernel2, new Point(2, 2), 10);
        //Imgproc.Laplacian(palm, palm, CvType.CV_8U, 1, 100, 0);

        //CANNY
        Mat edges = new Mat();
//        Imgproc.Canny(palm, edges, 50, 90);
//        Mat dst = palm.clone();
//        dst.setTo(new Scalar(0));
//        palm.copyTo(dst, edges);

        Imgproc.HoughLines(palm, edges, 1, Math.PI / 180, 200);
        for (int i = 0; i < edges.cols(); i++) {
            double[] vec = edges.get(0, i);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);

            Imgproc.line(palm, start, end, new Scalar(255,0,0), 3);
        }

        // convert to bitmap:
        Bitmap bm = Bitmap.createBitmap(palm.cols(), palm.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(palm, bm);

        // find the imageview and draw it!
        ImageView iv = (ImageView) findViewById(R.id.activity_main);
        iv.setImageBitmap(bm);
    }
}