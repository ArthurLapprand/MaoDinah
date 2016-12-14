package com.example.lapp.maodinah;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

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

    public boolean onTouchEvent(MotionEvent event){
        int temp = (int)(Math.random() * 10);
        TextView view = new TextView(this);
        view.setTextSize(50);
        if(temp == 1){
            view.setText( "Você vai morrer de velhice" );
        } else if(temp == 2){
            view.setText("Você ganhará na loteria mas a tristeza ainda lhe perseguirá");
        }else if(temp == 3){
            view.setText("Sua vida estará repleta de felicidades e tristezas");
        }else if(temp == 4){
            view.setText("Você só pensa no ontem.. a partir de 2020 começará a pensar no amanhã!");
        }else if(temp == 5){
            view.setText("Você encontrará alguem muito especial em sua vida que lhe ajudará nos momentos mais dificeis");
        }else if(temp == 6){
            view.setText("Você ira desistir de seus principais objetivos por causa de problemas pessoais ");
        }else if(temp == 7){
            view.setText("Sua linha da vida afirma que viverá bastante!");
        }else view.setText("Sua linha da vida diz que você ira viver pouco");


        setContentView(view);

        return false;
    }

    public void helloworld() {
        // make a mat and draw something
        //File f = new File("/sdcard/Download/");
        //String[] paths = f.list();
        Mat original_image = Imgcodecs.imread(Environment.getExternalStorageDirectory().getPath() + "/Download/palma2.jpg", Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
        Mat palm = original_image.clone();
      //  Imgproc.cvtColor(palm, palm, Imgproc.COLOR_RGBA2GRAY);
//        Mat binary =new Mat();
//        Imgproc.threshold(palm, binary, 0,255, Imgproc.THRESH_BINARY);
//        Mat mask = new Mat();
//        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20, 20));
//        Imgproc.morphologyEx(binary, mask, Imgproc.MORPH_OPEN, kernel);
//        Imgproc.threshold(mask, mask, 128,255, Imgproc.THRESH_BINARY_INV );
//        Mat blurred = new Mat();
//        Imgproc.GaussianBlur(palm, blurred, new Size(9, 9), 0);
//        Imgproc.threshold(blurred, binary,0, 255, Imgproc.THRESH_OTSU );
//        Mat result = new Mat();
//        binary.copyTo(result,mask);
//        Imgproc.equalizeHist(palm, palm);
//        Imgproc.GaussianBlur(palm, palm, new Size(9, 9), 5);
//        Imgproc.equalizeHist(palm, palm);
////
//        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(50, 50));
//        //Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_BLACKHAT, kernel, new Point(25, 25), 2);
//
       Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(4, 4));
        Imgproc.cvtColor(original_image,original_image, Imgproc.COLOR_BGRA2GRAY);
        //Imgproc.GaussianBlur(original_image,original_image, new Size(5,5),3);
        //Imgproc.threshold(original_image,original_image,1,255,Imgproc.THRESH_BINARY);
        Imgproc.morphologyEx(original_image, original_image, Imgproc.MORPH_GRADIENT, kernel2, new Point(2, 2), 10);
       Imgproc.threshold(original_image, original_image, 20, 255,Imgproc.THRESH_BINARY);
//        Imgproc.threshold(palm ,palm,120,255,Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY_INV);
//        Imgproc.equalizeHist(palm, palm);
//        Imgproc.morphologyEx(result, result, Imgproc.MORPH_OPEN, kernel2, new Point(2, 2), 13);
//        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_OPEN, kernel2, new Point(2, 2), 13);
//        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_OPEN, kernel2, new Point(2, 2), 13);
//        Imgproc.morphologyEx(result, result, Imgproc.MORPH_CLOSE, kernel2, new Point(2, 2), 15);
//        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_BLACKHAT, kernel2, new Point(2, 2), 50);
//        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_DILATE, kernel2, new Point(2, 2), 20);
//      Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_CLOSE, kernel2, new Point(2, 2), 20);
//        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_DILATE, kernel2, new Point(2, 2), 20);
//        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_CLOSE, kernel2, new Point(2, 2), 20);
//        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_BLACKHAT, kernel2, new Point(2, 2), 10);
        //Imgproc.Laplacian(palm, palm, CvType.CV_8U, 1, 100, 0);

//CANNY
        Mat edges = new Mat();
  //      Imgproc.Canny(original_image, original_image, 30, 60);
        //Mat palma = original_image.clone();
        //Imgproc.cvtColor(palma, palma, Imgproc.COLOR_RGB2GRAY);
        //dst.setTo(new Scalar(0));
        //palm.copyTo(dst, edges);
//
//        //HoughLinesP
        Imgproc.HoughLinesP(original_image, edges, 1, Math.PI / 180, 20, 100, 100);
        for (int i = 0; i < edges.rows(); i++) {
            double[] vec = edges.get(i, 0);
            double  x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);
            //if (Math.sqrt((x1-x2) * (x1-x2) + (y1-y2) * (y1-y2)) > 400)
            if (y1 < (palm.height()*0.7) && y2 < (palm.height()*0.7) &&
                y1 > (palm.height()*0.15) && y2 > (palm.height()*0.15)   )
                Imgproc.line(palm, start, end, new Scalar(255,0,0), 3);

        }
        //Imgproc.cvtColor(original_image,original_image,Imgproc.COLOR_RGB2GRAY);
        //Imgproc.cvtColor(palm,palm,Imgproc.COLOR_RGB2GRAY);
        // convert to bitmap:
        Bitmap bm = Bitmap.createBitmap(palm.cols(), palm.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(palm, bm);

        // find the imageview and draw it!
        ImageView iv = (ImageView) findViewById(R.id.activity_main);
        iv.setImageBitmap(bm);
    }
}