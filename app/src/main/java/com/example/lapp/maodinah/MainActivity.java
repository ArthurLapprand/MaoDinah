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
    public static final int CAMERA_REQUEST_CODE = 1; // USADO PARA PERMISSÕES
    static boolean initialized; // TESTA SE INICIALIZOU O OPENCV, A INICIALIZAÇÃO É ESTÁTICA
    static int height;
    static int width;

    // USADOS EM TRANSFORMAÇÕES E PROCESSAMENTO DE IMAGENS
    // EX. CANNY E HOUGHLINESP
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

    // INICIALIZAÇÃO ESTÁTICA
    static {
        if (!OpenCVLoader.initDebug()) {
            // do something
        } else {
            // quando inicializado
            initialized = true;
        }
    }

    /**
     * USADOS PARA CÁLCULOS DE CORES HSV, CENTRO DA PALMA DA MÃO,
     * PONTAS DOS DEDOS E OUTROS EXPERIMENTOS REALIZADOS
      */
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
        // checar permissoes
        checarPerms();
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

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
                    // CASO TENHA SIDO ACEITO, FAZER ALGO SE NECESSÁRIO
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
        setScaleFactors(width, height); // usado para compatibilidade em outras resoluções
        hullPoints = new MatOfPoint(); // usados para achar as concavidades entre os dedos
        hull = new MatOfInt();
    }

    @Override
    public void onCameraViewStopped() {
        if (frame != null) frame.release();

        if (javaCameraView.isEnabled()) {
            javaCameraView.disableView();
        }
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        matGlobal = inputFrame.rgba(); // frame global usado em contas de HSV
        return paulsMethodForImageTransformation(inputFrame.rgba()); // frame processado mostrado na tela
    }

    /**
     * CALCULA A MÉDIA HSV DO TOM DE PELE
     * A MÉDIA É RECALCULADA BASEADA NA COR HSV EM UM QUADRADO
     * NO CENTRO DA TELA, PORTANTO É NECESSÁRIO MANTER A PALMA
     * DA MÃO NO CENTRO
      */
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

        // SOMATÓRIO DAS CORES DO QUADRADO
        Imgproc.cvtColor(MatrizTocada, MatrizTocada, Imgproc.COLOR_RGB2HSV_FULL);
        Scalar somatorioCores = Core.sumElems(MatrizTocada);
        int total = retanguloTocado.width * (retanguloTocado.height);
        double avgHSV[] = {somatorioCores.val[0] / total, somatorioCores.val[1] / total, somatorioCores.val[2] / total};
        assignHSV(avgHSV);
    }

    /**
     * AJUSTA VALORES MEDIANOS HSV
     */
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

    /**
     * AJUSTA OS TAMANHOS DE RESOLUÇÃO INICIAIS
     * PARA VALORES CONDIZENTES COM O DISPOSITIVO
     */
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

    /**
     * ESTE MÉTODO ERA USADO PARA COMEÇAR A CALCULAR A MÉDIA
     * HSV QUANDO A TELA FOSSE TOCADA
     */
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

    /**
     * ENCONTRA OS CONTORNOS DE UM FRAME
     * RETORNA UM ARRAYLIST DE MATRIZ DE PONTOS
     */
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

    /**
     * RETORNA A POSIÇÃO DO CONTORNO QUE
     * CONTÉM O CENTRO DA MÃO
     */
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

    /**
     * CALCULA A DISTANCIA APROXIMADA DE CADA PIXEL PARA
     * O PIXEL ZERO MAIS PRÓXIMO AFIM DE ENCONTRAR
     * UM NOVO PONTO DE DESLOCAMENTO, QUE VAI RETORNAR O CENTRO
     * DA PALMA
     */
    protected Point getDistanceTransformCenter(Mat frame) {

        Imgproc.distanceTransform(frame, frame, Imgproc.CV_DIST_L2, 3);
        frame.convertTo(frame, CvType.CV_8UC1);
        Core.normalize(frame, frame, 0, 255, Core.NORM_MINMAX);
        Imgproc.threshold(frame, frame, 254, 255, Imgproc.THRESH_TOZERO);
        Core.findNonZero(frame, nonZero);

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

    /**
     * ESTE MÉTODO ERA USADO PARA ENCONTRAR
     * OS PONTOS EM CONCAVIDADES NO FRAME
     */
    protected List<Point> getConvexHullPoints(MatOfPoint contour) {
        Imgproc.convexHull(contour, hull);
        List<Point> hullPoints = new ArrayList<>();
        for (int j = 0; j < hull.toList().size(); j++) {
            hullPoints.add(contour.toList().get(hull.toList().get(j)));
        }
        return hullPoints;
    }

    // DISTÂNCIA EUCLIDIANA
    protected double getEuclDistance(Point one, Point two) {
        return Math.sqrt(Math.pow((two.x - one.x), 2)
                + Math.pow((two.y - one.y), 2));
    }

    /**
     * MÉTODO ERA USADO PARA ENCONTRAR OS PONTOS
     * QUE INDICAM AS PONTAS DOS DEDOS
     */
    protected List<Point> getDedos(List<Point> hullPoints, int rows) {
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

    /**
     * MÉTODO QUE TRABALHA SOBRE O FRAME DE ENTRADA
     * PARA RETORNAR O FRAME PROCESSADO
     */
    public Mat paulsMethodForImageTransformation(Mat inputFrame) {
        //if  (tocou){
        frame = matGlobal.clone();
        Imgproc.GaussianBlur(frame, frame, new Size(9, 9), 5);

        int x = Math.round(((javaCameraView.getWidth() / 2) - offsetFactX) * scaleFactX);
        int y = Math.round(((javaCameraView.getHeight() / 2) - offsetFactY) * scaleFactY);

        int rows = frame.rows();
        int cols = frame.cols();

        // se as mudanças na escala não batem com o real, retorna o frame normal
        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) {
            Log.d(TAG, "ENTREI ANTES");
            return inputFrame;
        }

        centroid.x = x;
        centroid.y = y;

        setHSVavg(frame); // média HSV

        // procura mão por tom de pele
        frame = inputFrame.clone();
        Imgproc.GaussianBlur(frame, frame, new Size(9, 9), 5);
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2HSV_FULL);
        Core.inRange(frame, minHSV, maxHSV, frame);
        //Imgproc.threshold(frame, frame, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
        contornos = getAllContornos(frame);
        int indiceContorno = getPalmContour(contornos);

        // caso não haja contorno com o centro da mão, retorna frame normal
        if (indiceContorno == -1) {
            Log.d(TAG, "ENTREI INDEX -1");
            return inputFrame;
        } else {
            // caso haja, calcula o retângulo da mão e o da palma da mão
            Point center = getDistanceTransformCenter(frame);
            Rect handRect = Imgproc.boundingRect(contornos.get(indiceContorno));
            double radius = handRect.height / 2.66;
            Rect palmRect = new Rect((int) (center.x - radius), (int) (center.y - radius), (int) (radius * 2), (int) (radius * 2));

            // PARTE NÃO USADA, POIS NÃO PRECISAMOS DAS PONTAS DOS DEDOS
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


            // retângulo do tamanho da imagem original
            Rect imgRect = new Rect(0, 0, inputFrame.width(), inputFrame.height());

            // ENCONTRA COORDENADAS RESTANTES DO RETÂNGULO
            // DA PALMA DA MÃO

            // TOP RIGHT
            Point tr = new Point();
            tr.x = palmRect.tl().x + palmRect.width;
            tr.y = palmRect.tl().y;

            // BOTTOM LEFT
            Point bl = new Point();
            bl.x = palmRect.tl().x;
            bl.y = palmRect.br().y;

            // CASO O RETÂNGULO DA PALMA ESTEJA COMPLETO NA TELA
            if (imgRect.contains(palmRect.br()) && imgRect.contains(palmRect.tl()) && imgRect.contains(tr) && imgRect.contains(bl)) {
                Mat original_image = inputFrame.clone();
                Log.d(TAG, "RETORNANDO SUBMAT");
                Log.d(TAG, "TOP_LEFT = " + palmRect.tl() + "  TOP_RIGHT = " + tr + "  BOTTOM_LEFT = " + bl + "  BOTTOM_RIGHT = " + palmRect.br());

                Mat palm = new Mat(original_image.size(), original_image.type(), new Scalar(0, 0, 0)); // cria frame preto
                //Imgproc.rectangle(palm, palmRect.tl(), palmRect.br(), new Scalar(255), 4); // desenha retângulo da palma

                Mat temp = original_image.submat(palmRect); // recorta a palma da imagem original
                Mat mask = temp.clone();
                temp.copyTo(palm, mask);
                Imgproc.resize(palm, palm, original_image.size()); // estica recorte para a tela toda

                // converte imagem para escala de cinza
                Imgproc.cvtColor(palm,palm, Imgproc.COLOR_BGRA2GRAY);

                // matriz usada para aplidar o gradient
                Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
                Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_GRADIENT, kernel2, new Point(2, 2), 4);

                // equaliza o histograma para realçar linhas e detalhes
                Imgproc.equalizeHist(palm, palm);

                // filtra regiões mais escuras (ex. linhas)
                // este processamento não está ótimo pois a partir daqui
                // o projeto foi mudado para um de imagem estática
                Imgproc.threshold(palm, palm, 240, 255,Imgproc.THRESH_BINARY);

                // REGIÃO DE TESTES
//                Mat inMat = inputFrame.submat(palmRect);
//                Mat mask = new Mat(palmRect.size(), CvType.CV_8UC4);
//                Mat out = new Mat(inputFrame.size(), inputFrame.type(), new Scalar(0, 0, 0));
//                inMat.copyTo(out, mask);
                //Mat teste = new Mat(inputFrame.size(), inputFrame.type(), new Scalar(0, 0, 0));
                //Mat cropped = new Mat(inputFrame, palmRect);
                //Mat mask = new Mat(cropped.rows(), cropped.cols(), inputFrame.type());
                //cropped.copyTo(teste, mask);
                Log.d(TAG, "SAI");
                return palm;
            }
            else {
                // CASO NÃO ESTEJA, RETORNA FRAME NORMAL
                Log.d(TAG, "RETORNANDO NORMAL DEPOIS DE SUBMAT");
                return inputFrame;
            }
        }
        //} else return inputFrame;

    }
}