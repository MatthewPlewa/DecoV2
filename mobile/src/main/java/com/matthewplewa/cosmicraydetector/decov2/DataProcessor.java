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
        Log.i(tag,paths.size()+" in Queue");


    }
    OutputStream output;
    private void save(byte[] bytes) throws IOException {

        try {
            Log.i(tag,"saving");
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
            File file = new File(Environment.getExternalStorageDirectory(),"DECO/EVENTS/");
            if(!file.exists())
                file.mkdirs(); //makes sure that the directory exists! if not then it makes it exist

            file = new File(Environment.getExternalStorageDirectory(),"DECO/"+pic+ ".jpg");
            Log.i(tag,"file made");
            output = new FileOutputStream(file);
            output.write(bytes);
            Log.i(tag,"witten");
            output.flush();
            output.close();
            output=null;
            Camera2BasicFragment.numEvents++;



        } finally {
            if (null != output) {
                //utput.close();
                //startPreview();

            }
        }
    }
    Bitmap bits;
    String tag = "processor";
    double scaleX=11;// if you want to change the scale (which will have to be changed in the calibration protion of the set up (not done) you need to change here TODO
    double scaleY=11;
    int tempBuffer;

    public void process() {
        ready=false;
        Log.i(tag,"taking image");
        byte[] bytes = paths.get(0);
        Log.i(tag,"image in");
        paths.remove(0);

        Log.i(tag,"Decoding");
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inPreferredConfig= Bitmap.Config.ARGB_8888;
        bits = BitmapFactory.decodeByteArray(bytes,0,bytes.length,bitmapOptions);
        Bitmap bit = bits.copy(Bitmap.Config.ARGB_8888,true);
        // we want to scale the image down to something that will be usable and able to be processed alot faster

        bit =  Bitmap.createScaledBitmap(bit,(int) (bit.getWidth()/scaleX),(int)(bit.getHeight()/scaleY),false);
        Log.i(tag,"decoded");
        told = false;
        int hight = bit.getHeight();

        int width = bit.getWidth();
        int rTemp=0;
        int gTemp=0;
        int bTemp=0;
        int numpix=0;
        boolean good = false;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < hight; y++) {
                int pic;


                String col = String.format("#%08X", bit.getPixel(x, y));

                int r = Integer.parseInt(col.substring(3, 5), 16);
                int b = Integer.parseInt(col.substring(5, 7), 16);
                int g = Integer.parseInt(col.substring(7, 9), 16);
                if (r > 40||b>40||g>40 ) {
                    good = true;
                    numpix++;
                }
                if(r>rTemp)
                    rTemp=r;
                if(g>gTemp)
                    gTemp=g;
                if(b>bTemp)
                    bTemp=b;





            }


        }

        Log.i(tag,"Max r g b values"+rTemp+","+gTemp+","+bTemp);
        Log.i(tag,"image done");
        Log.i(tag,good+"");
        if (good&&numpix<200) {//delete if not above cut

            try {
                save(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /*
        this sets up intelegent scaleing so that we can always have the highest resolution possible without having to worry about
        having to large a quoue build up.// for consistancy measures we are not going to do this anymore.... will use for calibration!

         */
        /*if(paths.size()>0) {
            ready = true;
            if(paths.size()>3){
                if(tempBuffer<paths.size()) {
                    scaleX++;
                    scaleY++;
                }
            }
        }
        else if(paths.size()==0) {
            ready = false;
            scaleX--;
            scaleY--;
        }*/
        Camera2BasicFragment.inQuaue=paths.size();
        Log.i(tag,scaleX+","+scaleY+","+bit.getWidth()+","+bit.getHeight());
        go=true;
        bit.recycle();

    }







    public void longProcessor(){
        /*
        this will allow for full resolution proce3ssing of the images.
        should only be used for processing after the initial image filter has been used aka processor()
         */




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
