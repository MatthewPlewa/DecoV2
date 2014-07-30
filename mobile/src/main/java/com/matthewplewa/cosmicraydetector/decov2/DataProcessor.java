package com.matthewplewa.cosmicraydetector.decov2;

/**
 * Created by Matthew on 7/28/2014.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;

public class DataProcessor extends Thread {







        public DataProcessor() {

            super();
            Log.i(null,"starting thread");
        }


        private boolean go=false;
        private String path;


        public void setImage(String Path){
            path=Path;
            go=true;
            told=false;
        }
    boolean keep = true;



    Bitmap bit;

        public void process(){
            bit = BitmapFactory.decodeFile(path);
            int hight=bit.getHeight();
            int width = bit.getWidth();

            for(int x =0; x < width; x++){
                for(int y =0; y<hight;y++){
                    int pic;
                    pic =bit.getPixel(x,y);
                    if(!told) {
                        System.out.println(pic);
                        told=true;
                    }
                    if(pic<-1000000){//delete if not above cut

                        File file = new File(path);
                        if( file.delete())
                            Log.i("tag","deleted");
                    }


                }
            }



        }



    boolean told=false;
        public  void run(){


        }

























}
