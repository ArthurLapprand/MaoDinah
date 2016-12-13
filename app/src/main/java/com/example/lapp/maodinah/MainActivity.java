package com.example.lapp.maodinah;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.BackgroundSubtractorMOG2;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Collection;

import static android.R.attr.bitmap;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MAODINAH";
    public static final int CAMERA_REQUEST_CODE = 1;
    static boolean initialized;
    static int height;
    static int width;
    static double limiarSuperior = 90;
    static double limiarInferior = 45;
    static Mat edges;
    static Mat lines;
    static int threshold = 50;
    static double tamanhoMinimoLinha = 20;
    static double lineGap = 20;
    static double[] vec;
    static double x1, x2, y1, y2;
    static Point start, end;
    static Mat matGlobal;

    static {
        if (!OpenCVLoader.initDebug()) {
            // do something
        } else {
            // quando inicializado
            initialized = true;
        }
    }

    JavaCameraView javaCameraView;
    private boolean tocou = false;
    private Point centroid;
    private Mat nonZero = new Mat();
    private Mat MatrizTocada;
    private Scalar minHSV = new Scalar(3);
    private Scalar maxHSV = new Scalar(3);
    private List<Point> dedos;
    private Mat frame;
    private Mat framesegundo;
    private Mat h = new Mat(); // hierarquia findContours
    private List<MatOfPoint> contornos;
    private Scalar lowerBound;
    private Scalar upperBound;
    private float offsetFactX, offsetFactY;
    private float scaleFactX, scaleFactY;
    private MatOfPoint hullPoints;
    private MatOfInt hull;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        // checar permissoes
        checarPerms();

        // seta a view da camera
        javaCameraView = (JavaCameraView) findViewById(R.id.camera_view);

        if (initialized) {
            javaCameraView.setCameraIndex(0); // 0 = camera traseira
            javaCameraView.setCvCameraViewListener(this);
            javaCameraView.enableView();
            javaCameraView.enableFpsMeter();
            height = javaCameraView.getHeight();
            width = javaCameraView.getWidth();
            javaCameraView.setMaxFrameSize(960, 480);
        }
    }

    private void checarPerms() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.CAMERA)) {
                // DEVE MOSTRAR EXPLICACAO CASO PRECISE, DEVE SER ASSINC
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.CAMERA},
                        CAMERA_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {}
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (javaCameraView.isEnabled()) {
            javaCameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        lowerBound = new Scalar(3);
        upperBound = new Scalar(3);
        centroid = new Point(-1, -1);
        setScaleFactors(width, height);
        hullPoints = new MatOfPoint();
        hull = new MatOfInt();
    }

    @Override
    public void onCameraViewStopped() {
        frame.release();

        if (javaCameraView.isEnabled()) {
            javaCameraView.disableView();
        }
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        matGlobal = inputFrame.rgba();
        return paulsMethodForImageTransformation(inputFrame.rgba());
    }

    private void setHSVavg(Mat frame) {

        int x = (int) centroid.x;
        int y = (int) centroid.y;

        int rows = frame.rows();
        int cols = frame.cols();

        Rect retanguloTocado = new Rect();
        int ladoRet = 20;

        if (x > ladoRet) {
            retanguloTocado.x = x - ladoRet;
        } else {
            retanguloTocado.x = 0;
        }

        if (y > ladoRet) {
            retanguloTocado.y = y - ladoRet;
        } else {
            retanguloTocado.y = 0;
        }

        retanguloTocado.width = (x + ladoRet < cols) ? (x + ladoRet - retanguloTocado.x) : (cols - retanguloTocado.x);
        retanguloTocado.height = (y + ladoRet < rows) ? (y + ladoRet - retanguloTocado.y) : (rows - retanguloTocado.y);

        MatrizTocada = frame.submat(retanguloTocado);

        Imgproc.cvtColor(MatrizTocada, MatrizTocada, Imgproc.COLOR_RGB2HSV_FULL);
        Scalar somatorioCores = Core.sumElems(MatrizTocada);
        int total = retanguloTocado.width * (retanguloTocado.height);
        double avgHSV[] = {somatorioCores.val[0] / total, somatorioCores.val[1] / total, somatorioCores.val[2] / total};
        assignHSV(avgHSV);
    }

    private void assignHSV(double avgHSV[]) {
        //B
        minHSV.val[0] = (avgHSV[0] > 10) ? avgHSV[0] - 10 : 0;
        maxHSV.val[0] = (avgHSV[0] < 245) ? avgHSV[0] + 10 : 255;
        //G
        minHSV.val[1] = (avgHSV[1] > 130) ? avgHSV[1] - 100 : 30;
        maxHSV.val[1] = (avgHSV[1] < 155) ? avgHSV[1] + 100 : 255;
        //R
        minHSV.val[2] = (avgHSV[2] > 130) ? avgHSV[2] - 100 : 30;
        maxHSV.val[2] = (avgHSV[2] < 155) ? avgHSV[2] + 100 : 255;
    }


    protected void setScaleFactors(int vidWidth, int vidHeight) {
        float deviceWidth = javaCameraView.getWidth();
        float deviceHeight = javaCameraView.getHeight();
        if (deviceHeight - vidHeight < deviceWidth - vidWidth) {
            float temp = vidWidth * deviceHeight / vidHeight;
            offsetFactY = 0;
            offsetFactX = (deviceWidth - temp) / 2;
            scaleFactY = vidHeight / deviceHeight;
            scaleFactX = vidWidth / temp;
        } else {
            float temp = vidHeight * deviceWidth / vidWidth;
            offsetFactX = 0;
            offsetFactY = (deviceHeight - temp) / 2;
            scaleFactX = vidWidth / deviceWidth;
            scaleFactY = vidHeight / temp;
        }
    }

    public boolean onTouchEvent(MotionEvent event) {

//        if (!tocou) {
//            frame = matGlobal.clone();
//            Imgproc.GaussianBlur(frame, frame, new Size(9, 9), 5);
//
//            /*
//            int x = Math.round((event.getX()));
//            int y = Math.round((event.getY()));
//            */
//            int x = Math.round((event.getX() - offsetFactX) * scaleFactX);
//            int y = Math.round((event.getY() - offsetFactY) * scaleFactY);
//
//            int rows = frame.rows();
//            int cols = frame.cols();
//
//
//            if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;
//
//            centroid.x = x;
//            centroid.y = y;
//
//            setHSVavg(frame);
//
//            tocou = true;
//        }

        return false;
    }

    private ArrayList<MatOfPoint> getAllContornos(Mat frame) {
        framesegundo = frame.clone();
        ArrayList<MatOfPoint> contor = new ArrayList<MatOfPoint>();
        Imgproc.findContours(framesegundo,
                contor,
                h,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

        return contor;
    }

    protected int getPalmContour(List<MatOfPoint> contours) {

        Rect roi;
        int indexOfMaxContour = -1;
        for (int i = 0; i < contours.size(); i++) {
            roi = Imgproc.boundingRect(contours.get(i));
            if (roi.contains(centroid))
                return i;
        }
        return indexOfMaxContour;
    }

    protected Point getDistanceTransformCenter(Mat frame) {

        Imgproc.distanceTransform(frame, frame, Imgproc.CV_DIST_L2, 3);
        frame.convertTo(frame, CvType.CV_8UC1);
        Core.normalize(frame, frame, 0, 255, Core.NORM_MINMAX);
        Imgproc.threshold(frame, frame, 254, 255, Imgproc.THRESH_TOZERO);
        Core.findNonZero(frame, nonZero);

        // have to manually loop through matrix to calculate sums
        int sumx = 0, sumy = 0;
        if (nonZero.rows() > 0) {
            for (int i = 0; i < nonZero.rows(); i++) {
                sumx += nonZero.get(i, 0)[0];
                sumy += nonZero.get(i, 0)[1];
            }
            sumx /= nonZero.rows();
            sumy /= nonZero.rows();
        }

        return new Point(sumx, sumy);
    }

    protected List<Point> getConvexHullPoints(MatOfPoint contour) {
        Imgproc.convexHull(contour, hull);
        List<Point> hullPoints = new ArrayList<>();
        for (int j = 0; j < hull.toList().size(); j++) {
            hullPoints.add(contour.toList().get(hull.toList().get(j)));
        }
        return hullPoints;
    }

    protected double getEuclDistance(Point one, Point two) {
        return Math.sqrt(Math.pow((two.x - one.x), 2)
                + Math.pow((two.y - one.y), 2));
    }

    protected List<Point> getDedos(List<Point> hullPoints, int rows) {
        // group into clusters and find distance between each cluster. distance should approx be same
        double betwFingersThresh = 80;
        double distFromCenterThresh = 80;
        double thresh = 80;
        List<Point> fingerTips = new ArrayList<>();
        for (int i = 0; i < hullPoints.size(); i++) {
            Point point = hullPoints.get(i);
            if (rows - point.y < thresh)
                continue;
            if (fingerTips.size() == 0) {
                fingerTips.add(point);
                continue;
            }
            Point prev = fingerTips.get(fingerTips.size() - 1);
            double euclDist = getEuclDistance(prev, point);

            if (getEuclDistance(prev, point) > thresh / 2 &&
                    getEuclDistance(centroid, point) > thresh)
                fingerTips.add(point);

            if (fingerTips.size() == 5)
                break;
        }
        return fingerTips;
    }


    public Mat paulsMethodForImageTransformation(Mat inputFrame) {
        //if  (tocou){
        frame = matGlobal.clone();
        Imgproc.GaussianBlur(frame, frame, new Size(9, 9), 5);

            /*
            int x = Math.round((event.getX()));
            int y = Math.round((event.getY()));
            */
        int x = Math.round(((javaCameraView.getWidth() / 2) - offsetFactX) * scaleFactX);
        int y = Math.round(((javaCameraView.getHeight() / 2) - offsetFactY) * scaleFactY);

        int rows = frame.rows();
        int cols = frame.cols();

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) {
            Log.d(TAG, "ENTREI ANTES");
            return inputFrame;
        }

        centroid.x = x;
        centroid.y = y;

        setHSVavg(frame);

        frame = inputFrame.clone();
        Imgproc.GaussianBlur(frame, frame, new Size(9, 9), 5);
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2HSV_FULL);
        Core.inRange(frame, minHSV, maxHSV, frame);
        //Imgproc.threshold(frame, frame, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
        contornos = getAllContornos(frame);
        int indiceContorno = getPalmContour(contornos);

        if (indiceContorno == -1) {
            Log.d(TAG, "ENTREI INDEX -1");
            return inputFrame;
        } else {
            Point center = getDistanceTransformCenter(frame);
            Rect handRect = Imgproc.boundingRect(contornos.get(indiceContorno));
            double radius = handRect.height / 3;
            Rect palmRect = new Rect((int) (center.x - radius), (int) (center.y - radius), (int) (radius * 2), (int) (radius * 2));
            //frame = new Mat(matGlobal.clone(), roi);
//            List<Point> hullPoints = getConvexHullPoints(contornos.get(indiceContorno));
//            dedos = getDedos(hullPoints, frame.rows());
//            Collections.reverse(dedos);
//            for (int i = 0; i + 1 < dedos.size(); i++) {
//                Imgproc.line(frame, dedos.get(i), dedos.get(i + 1), new Scalar(0, 255, 0), 2);
//            }
            //Log.d(TAG, "br = " + roi.br() + " tl = " + roi.tl());
            //Imgproc.rectangle(frame, roi.tl(), roi.br(), new Scalar(255), 4);
            //Imgproc.circle(frame, palma, roi.height / 3, new Scalar(255)); // circulo
            //Imgproc.drawContours(frame, contornos, -1, new Scalar(200, 200, 0), 2);
            Rect imgRect = new Rect(0, 0, inputFrame.width(), inputFrame.height());

            // TOP RIGHT
            Point tr = new Point();
            tr.x = palmRect.tl().x + palmRect.width;
            tr.y = palmRect.tl().y;

            // BOTTOM LEFT
            Point bl = new Point();
            bl.x = palmRect.tl().x;
            bl.y = palmRect.br().y;

            if (imgRect.contains(palmRect.br()) && imgRect.contains(palmRect.tl()) && imgRect.contains(tr) && imgRect.contains(bl)) {
                Log.d(TAG, "RETORNANDO SUBMAT");
                Log.d(TAG, "TOP_LEFT = " + palmRect.tl() + "  TOP_RIGHT = " + tr + "  BOTTOM_LEFT = " + bl + "  BOTTOM_RIGHT = " + palmRect.br());
                Mat teste = new Mat(inputFrame.size(), inputFrame.type(), new Scalar(0, 0, 0));
                Imgproc.rectangle(teste, palmRect.tl(), palmRect.br(), new Scalar(255), 4);
                //Mat temp = inputFrame.clone();
                //temp = temp.submat(palmRect);
                //temp.copyTo(teste);
                Log.d(TAG, "SAI");
                return teste;
            }
            else {
                Log.d(TAG, "RETORNANDO NORMAL DEPOIS DE SUBMAT");
                return inputFrame;
            }
        }
        //} else return inputFrame;

    }
}