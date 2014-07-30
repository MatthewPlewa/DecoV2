package com.matthewplewa.cosmicraydetector.decov2;

/**
 * Created by Matthew on 7/28/2014.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;

public class DataProcessor extends Thread {

    ArrayList<byte[]> paths= new ArrayList<byte[]>();

    public DataProcessor() {

        super();
        Log.i(null, "starting thread");
    }




    private boolean go = false;
    private String path;

    boolean running=false;
    public void setImage(byte[] bytes) {

        paths.add(bytes);
        ready=true;
        if(!running) {
            go = true;
            running = true;
        }


    }
    OutputStream output;
    private void save(byte[] bytes) throws IOException {

        try {
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month= c.get(Calendar.MONTH)+1;
            int day= (c.get(Calendar.DAY_OF_MONTH));
            int hour= c.get(Calendar.HOUR_OF_DAY);
            int min= c.get(Calendar.MINUTE);
            int seconds = c.get(Calendar.SECOND);

            //string convertion
            String Month = month+"";
            String Day = day+"";
            String Hour = hour+"";
            String Min= min+"";
            String Seconds = seconds +"";


            //keeps it in the correct formate
            if(month<10)
                Month = "0"+ month;
            if(day<10)
                Day = "0"+day;
            if(hour <10)
                Hour = "0"+hour;
            if(min<10)
                Min="0"+min;
            if(seconds <10)
                Seconds = "0"+seconds;

            //setting formate for file name
            String pic =""+ year+Month+Day+"_"+Hour+Min+Seconds;

            File file = new File(Environment.getExternalStorageDirectory(),"DECO/"+pic+ ".jpg");
            output = new FileOutputStream(file);
            output.write(bytes);
            output.flush();
            output.close();
            output=null;



        } finally {
            if (null != output) {
                //utput.close();
                //startPreview();

            }
        }
    }
    Bitmap bits;
String tag = "processor";

    public void process() {
        ready=false;
        Log.i(tag,"taking image");
        byte[] bytes = paths.get(0);
        Log.i(tag,"image in");
        paths.remove(0);

        Log.i("tag","Decoding");
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inPreferredConfig= Bitmap.Config.ARGB_8888;
        bits = BitmapFactory.decodeByteArray(bytes,0,bytes.length,bitmapOptions);
        Bitmap bit = bits.copy(Bitmap.Config.ARGB_8888,true);
        bit.reconfigure(408,308, Bitmap.Config.ARGB_8888);
        Log.i("tag","decoded");
        told = false;
        int hight = bit.getHeight();

        int width = bit.getWidth();

        boolean good = false;

        for (int x = 0; x < width; x+=2) {
            for (int y = 0; y < hight; y+=2) {
                int pic;


                String col = String.format("#%08X", bit.getPixel(x, y));

                int r = Integer.parseInt(col.substring(3, 5), 16);
                int b = Integer.parseInt(col.substring(5, 7), 16);
                int g = Integer.parseInt(col.substring(7, 9), 16);
                if (r > 50||b>50||g>50 )

                    try {
                        save(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                if (!told) {
                    System.out.println(r + " "+ g + " "+b);
                    System.out.println(col);
                    told = true;
                }


            }


        }
        Log.i("tag","image done");
        Log.i("tag",good+"");
        if (!good) {//delete if not above cut

            //File file = new File(test);
            //if (file.delete())
                Log.i("tag", "deleted");
        }
         go=true;

        bit.recycle();

    }


    boolean told = false;
    boolean ready=true;

    public void run() {
            while(true) {

                while(go&&ready) {
                    go=false;
                    process();

                }

            }
    }


}
