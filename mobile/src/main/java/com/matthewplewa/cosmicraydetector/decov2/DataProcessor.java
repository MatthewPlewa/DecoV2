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
    static final String hexaDecimalPattern = "^0x([\\da-fA-F]{1,8})$";




        public void process(){
            Bitmap bit = new BitmapFactory().decodeFile(path);
            int hight=bit.getHeight();
            String test = path;
            int width = bit.getWidth();
            boolean good=false;

            for(int x =0; x < width; x++){
                for(int y =0; y<hight;y++){
                    int pic;



                    String col = String.format("#%08X", bit.getPixel(x, y));

                    int r=Integer.parseInt( col.substring( 3, 5 ), 16 );
                    int b=Integer.parseInt( col.substring( 5, 7 ), 16 );
                    int g=Integer.parseInt( col.substring( 7, 9 ), 16 );
                    if(r>100||g>100||b>100)
                        good=true;
                    if(!told){
                        System.out.println(r+" "+g+" "+b);
                        System.out.println(col);
                        told=true;
                    }



                }
            }

            if(!good){//delete if not above cut

                File file = new File(test);
                if( file.delete())
                    Log.i("tag","deleted");
            }



        }



    boolean told=false;
        public  void run(){
            while(keep){
                if(go)
                    process();
            }


        }

























}
