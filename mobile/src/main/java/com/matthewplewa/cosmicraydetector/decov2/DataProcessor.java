package com.matthewplewa.cosmicraydetector.decov2;

/**
 * Created by Matthew on 7/28/2014.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

public class DataProcessor extends Thread {

    ArrayList<String> paths= new ArrayList<String>();

    public DataProcessor() {

        super();
        Log.i(null, "starting thread");
    }




    private boolean go = false;
    private String path;

    boolean running=false;
    public void setImage(String Path) {
        paths.add(Path);
        if(!running)
            go=true;


    }

    boolean keep = true;
    static final String hexaDecimalPattern = "^0x([\\da-fA-F]{1,8})$";


    public void process() {
        String test = paths.get(0);
        paths.remove(0);
        Bitmap bits;
        bits = BitmapFactory.decodeFile(test);// should pass a buffer and then copy
        Bitmap bit= bits.copy(Bitmap.Config.ARGB_4444,true);
        bit.reconfigure(2000,2000, Bitmap.Config.ARGB_4444);
        told = false;
        int hight = bit.getHeight();

        int width = bit.getWidth();

        //bit.reconfigure(2000,2000, Bitmap.Config.ARGB_4444);
        boolean good = false;

        for (int x = 0; x < width; x+=2) {
            for (int y = 0; y < hight; y+=2) {
                int pic;


                String col = String.format("#%08X", bit.getPixel(x, y));

                int r = Integer.parseInt(col.substring(3, 5), 16);
               // int b = Integer.parseInt(col.substring(5, 7), 16);
               // int g = Integer.parseInt(col.substring(7, 9), 16);
                if (r > 200 )
                    good = true;
                if (!told) {
                    System.out.println(r + " " );
                    System.out.println(col);
                    told = true;
                }


            }


        }
        Log.i("tag","image done");
        Log.i("tag",good+"");
        if (!good) {//delete if not above cut

            File file = new File(test);
            if (file.delete())
                Log.i("tag", "deleted");
        }
         go=true;

    }


    boolean told = false;

    public void run() {
            while(true) {

                while(go) {
                    go=false;
                    process();

                }

            }
    }


}
