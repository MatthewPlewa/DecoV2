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
        }



        public  void run(){
            boolean keep = true;

            Bitmap bit;
            while(keep) {
                while (!go) {
                    try {
                        wait(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                while(go){

                    bit = BitmapFactory.decodeFile(path);
                    int hight=bit.getHeight();
                    int width = bit.getWidth();

                    for(int x =0; x < width; x++){
                        for(int y =0; y<hight;y++){
                            String pic;
                            pic =""+bit.getPixel(x,y);

                            int r = pic.charAt(2)*10+pic.charAt(3);

                            int g =pic.charAt(4)*10+pic.charAt(5);
                            int b=pic.charAt(6)*10+pic.charAt(7);
                            System.out.println(r);// must remove later
                            if(r<50&&g<50&&b<50){

                                File file = new File(path);
                                boolean deleted = file.delete();

                            }


                        }
                    }

                }
            }
        }

























}
