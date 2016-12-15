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
    static boolean boo = false;

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
        if(!boo) { // Para caso tocar na tela uma vez, mensagem aleatória é retornada
            int temp = (int) (Math.random() * 10);
            TextView view = new TextView(this);
            view.setTextSize(50);
            if (temp == 1) {
                view.setText("Você vai morrer de velhice");
            } else if (temp == 2) {
                view.setText("Você ganhará na loteria mas a tristeza ainda lhe perseguirá");
            } else if (temp == 3) {
                view.setText("Sua vida estará repleta de felicidades e tristezas");
            } else if (temp == 4) {
                view.setText("Você só pensa no ontem.. a partir de 2020 começará a pensar no amanhã!");
            } else if (temp == 5) {
                view.setText("Você encontrará alguem muito especial em sua vida que lhe ajudará nos momentos mais dificeis");
            } else if (temp == 6) {
                view.setText("Você ira desistir de seus principais objetivos por causa de problemas pessoais ");
            } else if (temp == 7) {
                view.setText("Sua linha da vida mandou avisar que você irá viver bastante!");
            } else view.setText("Sua linha da vida diz que você ira viver pouco");


            setContentView(view);
            boo = true; // Para que só seja possivel ler as linhas da imagem apenas 1x
        }
        return false;
    }
    //Metodo para calcular distancia euclidiana
    private double distEuclidiana(Point p1, Point p2){

        return Math.sqrt(Math.abs(((p1.x - p2.x) * (p1.x - p2.x)) + ((p1.y - p2.y) * (p1.y - p2.y))));
    }
//Metodo principal do projeto
    public void helloworld() {
        //Recebendo imagem original e guardando em original_image
        Mat original_image = Imgcodecs.imread(Environment.getExternalStorageDirectory().getPath() + "/Download/palma2.jpg", Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
        Mat palm = original_image.clone(); // Clonando original_image
        //Kernel sendo criado para utilização do morphologyEx
       Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(4, 4));
        // transformando a imagem para preto e branco/ cinza
        Imgproc.cvtColor(palm,palm, Imgproc.COLOR_BGRA2GRAY);
        //Aplicando o gradient e o threshold na copia da imagem original
        Imgproc.morphologyEx(palm, palm, Imgproc.MORPH_GRADIENT, kernel2, new Point(2, 2), 10);
       Imgproc.threshold(palm, palm, 20, 255,Imgproc.THRESH_BINARY);

        Mat edges = new Mat();
        //HoughLinesP
        // Utilização do metodo da HoughLines probabilistica para detectar as linhas
        Imgproc.HoughLinesP(palm, edges, 1, Math.PI /180 , 100, 100, 20);
        //pontos e arrays de pontos sendo declarados para seperação das principais linhas (Linha da Vida, Linha do Amor, Linha da cabeça
        Point[] pi = new Point[edges.rows()];
        Point[] pf = new Point[edges.rows()];
        Point[] res = new Point[edges.rows()];
        Point[] pm = new Point[edges.rows()];
        Point pontobaixo = new Point((palm.width()*0.17),(palm.height()*0.6));
        Point pontomeio = new Point((palm.width()*0.5),(palm.height()*0.5));
        Point pontocima = new Point((palm.width()*0.7),(palm.height())*0.2);
        Point[] resultanteai = new Point[edges.rows()];
        Point[] resultantebi = new Point[edges.rows()];
        Point[] resultanteci = new Point[edges.rows()];
        Point[] resultanteaf = new Point[edges.rows()];
        Point[] resultantebf = new Point[edges.rows()];
        Point[] resultantecf = new Point[edges.rows()];
        // Auxiliares para armazenar a quantidade de pontos em diferentes regiões
        int aux = 0;
        int aux1 = 0;
        int aux2 = 0;
        int aux3 = 0;
        //Variaveis sendo declaradas para saber o tamanho aproximadamente das 3 principais linhas.
        double min1x = original_image.width();
        double max1x = 0;
        double min2x = original_image.width();
        double max2x = 0;
        double min3x = original_image.width();
        double max3x = 0;
        double min1y = original_image.height();
        double max1y = 0;
        double min2y = original_image.height();
        double max2y = 0;
        double min3y = original_image.height();
        double max3y = 0;
        //Pegando os pontos da tela
        for (int i = 0; i < edges.rows(); i++) {
            double[] vec = edges.get(i, 0);
            double  x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);
            Point medio = new Point((x1+x2)/2,(y1+y2)/2);
            // Algumas condições para limitar a região que as linhas aparecem
            if (y1 < (palm.height()*0.7) && y2 < (palm.height()*0.7) &&
                y1 > (palm.height()*0.15) && y2 > (palm.height()*0.15)) {
                //Desenhando as linhas
                Imgproc.line(original_image, start, end, new Scalar(255, 0, 0), 3);
                pm[aux] = medio;
                //Calculando a distancia dos pontos que satisfazem as condições para os pontos fixos para detecção de cada linha
                double a,b,c;
                a = distEuclidiana(pm[aux],pontobaixo);
                b = distEuclidiana(pm[aux],pontomeio);
                c = distEuclidiana(pm[aux],pontocima);
                aux = aux+1;
                //Calculando os valores min e max de cada uma das 3 principais linhas
                //BAIXO - Linha da vida
                if((a < b) &&( a < c)){
                    resultanteai[aux1] = start;
                    resultanteaf[aux1] = end;
                    if(start.x < min1x) min1x = start.x;
                    if(end.x < min1x) min1x = end.x;
                    if(start.x > max1x) max1x = start.x;
                    if(end.x > max1x) max1x = end.x;

                    if(start.y < min1y) min1y = start.y;
                    if(end.y < min1y) min1y = end.y;
                    if(start.y > max1y) max1y = start.y;
                    if(end.y > max1y) max1y = end.y;
                    aux1 = aux1+1;
                } //CIMA - Linha do coração
                 else if((c < b) &&( c < a)){
                    resultantebi[aux2] = start;
                    resultantebf[aux2] = end;
                    if(start.x < min2x) min2x = start.x;
                    if(end.x < min2x) min2x = end.x;
                    if(start.x > max2x) max2x = start.x;
                    if(end.x > max2x) max2x = end.x;

                    if(start.y < min2y) min2y = start.y;
                    if(end.y < min2y) min2y = end.y;
                    if(start.y > max2y) max2y = start.y;
                    if(end.y > max2y) max2y = end.y;
                    aux2 = aux2+1;
                } // MEIO - Linha da cabeça
                else {
                    resultanteci[aux3] = start;
                    resultantecf[aux3] = end;
                    if(start.x < min3x) min3x = start.x;
                    if(end.x < min3x) min3x = end.x;
                    if(start.x > max3x) max3x = start.x;
                    if(end.x > max3x) max3x = end.x;

                    if(start.y < min3y) min3y = start.y;
                    if(end.y < min3y) min3y = end.y;
                    if(start.y > max3y) max3y = start.y;
                    if(end.y > max3y) max3y = end.y;
                    aux3 = aux3+1;
                }


            }
        }
//criando os pontos que os valores max e min das principais linhas
        Point p1 = new Point(min1x,min1y);
        Point p2 = new Point(min2x,min2y);
        Point p3 = new Point(min3x,min3y);
        Point p4 = new Point(max1x,max1y);
        Point p5 = new Point(max2x,max2y);
        Point p6 = new Point(max3x,max3y);
        //Se quiser imprimir linhas que tem o tamanho das 3 principais linhas da mão.
//        Imgproc.line(original_image,p1, p4, new Scalar(255,0,0), 3);
//        Imgproc.line(original_image,p2, p5, new Scalar(255,0,0), 3);
//        Imgproc.line(original_image,p3, p6, new Scalar(255,0,0), 3);

        // Comentado... mudar o resultante se quiser aparecer só 1 linha( e comentar a linha 138)
//        for(int i = 0; i < aux3; i++){
//       Imgproc.line(original_image,resultanteci[i], resultantecf[i], new Scalar(255,0,0), 3);
//        }


        // convert to bitmap:
        Bitmap bm = Bitmap.createBitmap(palm.cols(), palm.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(original_image, bm);

        // find the imageview and draw it!
        ImageView iv = (ImageView) findViewById(R.id.activity_main);
        iv.setImageBitmap(bm);
    }
}