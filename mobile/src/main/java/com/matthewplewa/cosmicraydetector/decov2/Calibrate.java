package com.matthewplewa.cosmicraydetector.decov2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Matthew on 8/11/2014.
 * The goal of this class is to create a calibrtion system that will be able to determine the proper
 * x and y scaling that needs to be done inorder to have a quaue of one or less. This will also set up
 * the r g and b values that will trigger the system to run a full image filter rather than a scaled image filter.
 * This should be run for 25 or more photos to get a decent aproximation of the values needed.
 */
public class Calibrate extends Thread{


    ArrayList<byte[]> bitsList= new ArrayList<byte[]>();
    byte[] bit;
    Bitmap bits;
    boolean added;
    boolean inQuaue;
    boolean go=false;
    boolean ready=true;
    boolean running=false;
    int done=0;
    boolean DEBUG=true;
    double scaleX=5;
    double scaleY=5;
    int rTemp=0;
    int gTemp=0;
    int bTemp=0;
    boolean calibrate=true;




    public Calibrate(){
        super();

    }

    public void setImage(byte[] bit){
        if(DEBUG)Log.i(tag,"setting image");
        bitsList.add(bit);
        if(DEBUG)Log.i(tag,"set image");

        go=true;


    }

    public String tag="calibrate";
    int tempBuffer=0;



    public void calibrate(){
        byte[] bytes = bitsList.get(0);
        bitsList.remove(0);
        if(DEBUG)Log.i(tag,"calibrating"+done);
        go=false;
        done++;

        /*
        we first want to find out what the system can handle for processing. Now to do this what we have
        to do is have an inteligent scaler that will find at what point we do not accumulate any more in the quaue.
        Also it has to go then one step lower than that inorder to allow itself to eliminate a build up caused
        by a full resulution filter.
         */


        if(done <35){





            if(DEBUG)Log.i(tag,"Decoding");
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inPreferredConfig= Bitmap.Config.ARGB_8888;
            bits = BitmapFactory.decodeByteArray(bytes,0,bytes.length,bitmapOptions);
            Bitmap bit = bits.copy(Bitmap.Config.ARGB_8888,true);
            // we want to scale the image down to something that will be usable and able to be processed alot faster
            bits.recycle();
            bit =  Bitmap.createScaledBitmap(bit,(int) (bit.getWidth()/scaleX),(int)(bit.getHeight()/scaleY),false);
            int hight = bit.getHeight();

            int width = bit.getWidth();
            int r , g, b;
            int pixel;


            for (int x = 0; x < width; x++) {
                for (int y = 0; y < hight; y++) {
                    int pic;


                   /* String col = String.format("#%08X", bit.getPixel(x, y));

                    int r = Integer.parseInt(col.substring(3, 5), 16);
                    int b = Integer.parseInt(col.substring(5, 7), 16);
                    int g = Integer.parseInt(col.substring(7, 9), 16);
                    */
                    pixel=bit.getPixel(x,y);

                    r= Color.red(pixel);
                    g= Color.green(pixel);
                    b= Color.blue(pixel);

                }


            }



        /*
        this sets up intelegent scaleing so that we can always have the highest resolution possible without having to worry about
        having to large a quoue build up.// for consistancy measures we are not going to do this anymore during processing ... will use for calibration!

         */
            if(bitsList.size()>0) {// when the buffer is accumulating you have to increase scale
                ready = true;
                if(bitsList.size()>2){ // if it is less than 3 we done care
                    if(tempBuffer<bitsList.size()) { //if it is increasing we have to increase the scale
                        scaleX++;
                        scaleY++;

                    }
                }
            }
            else if(bitsList.size()==0) {
                ready = false;
                scaleX--;
                scaleY--;
            }
            tempBuffer=bitsList.size();
            if(DEBUG)Log.i(tag,scaleX+","+scaleY+","+bit.getWidth()+","+bit.getHeight());
            go=true;
            bit.recycle();
        }

        /*
        after we find a scale that works well we have to mess it up to fix the full resolution problem.
        What I mean is that the scaling that has been done is only good for the low res analising.
        by adding one to each scale factor we make it able to compensate.
         */

        if(done==34){
            scaleY+=2;
            scaleX+=2;
        }

        /*
        Now we have to find out what the threshold should be inorder to ajust for the sensors noise level.
         */

        if(done>34&&done<80){


            if(DEBUG) Log.i(tag, "taking image");


            if(DEBUG)Log.i(tag,"Decoding");
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inPreferredConfig= Bitmap.Config.ARGB_8888;
            bits = BitmapFactory.decodeByteArray(bytes,0,bytes.length,bitmapOptions);//makes a non scalable bitmap
            Bitmap bit = bits.copy(Bitmap.Config.ARGB_8888,true);// this makes it so that we can scale it.
            // we want to scale the image down to something that will be usable and able to be processed alot faster
            bits.recycle();
            bit =  Bitmap.createScaledBitmap(bit,(int) (bit.getWidth()/scaleX),(int)(bit.getHeight()/scaleY),false);
            if(DEBUG)Log.i(tag,"decoded");
            int hight = bit.getHeight();

            int width = bit.getWidth();

            int numpix=0;
            boolean good = false;
            int r , g, b;
            int pixel;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < hight; y++) {
                    int pic;


                    /*String col = String.format("#%08X", bit.getPixel(x, y));

                    r = Integer.parseInt(col.substring(3, 5), 16);
                    b = Integer.parseInt(col.substring(5, 7), 16);
                    g = Integer.parseInt(col.substring(7, 9), 16);
                    */

                    pixel=bit.getPixel(x,y);

                    r= Color.red(pixel);
                    g= Color.green(pixel);
                    b= Color.blue(pixel);

                    if(r>rTemp)
                        rTemp=r;
                    if(g>gTemp)
                        gTemp=g;
                    if(b>bTemp)
                        bTemp=b;





                }
            }
            go=true;
            bit.recycle();

        }

        if (done >= 81){
            DataProcessor.scaleY=scaleY;
            DataProcessor.scaleX=scaleX;
            DataProcessor.rThresh=rTemp;
            DataProcessor.gThresh=gTemp;
            DataProcessor.bThresh=bTemp;
            if(DEBUG)Log.i(tag,"setting to false");
            Camera2BasicFragment.calibrating=false;
            calibrate=false;
            done=0;


        }
        if(DEBUG)Log.i(tag,done+" in the calibrator");
        if(DEBUG)Log.i(tag,"Calibration Stats="+scaleX+" "+scaleY+" "+rTemp+" "+gTemp+" "+bTemp);



    }

    public void run(){
        while(calibrate) {

            while(go)  {

                if(bitsList.size()>0)
                    calibrate();
                Camera2BasicFragment.inQuaue=bitsList.size();

            }

        }


    }




}
