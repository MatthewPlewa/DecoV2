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
    final boolean DEBUG=true;

    public DataProcessor() {

        super();
        if(DEBUG)Log.i(null, "starting thread");
    }




    private boolean go = false;
    private String path;

    boolean running=false;
    public void setImage(byte[] bytes) {

        paths.add(bytes);
        go=true;
        if(!running) {
            go = true;
            running = true;
        }
        if(DEBUG)Log.i(tag,paths.size()+" in Queue. is long running? "+longProcess );
        Camera2BasicFragment.inQuaue=paths.size();//tells Camera2BasicFragment number in q so that
                                                   // uiupdater can update properly.



    }
    OutputStream output;
    private void save(byte[] bytes) throws IOException {

        try {
            if(DEBUG)Log.i(tag,"saving");
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

            file = new File(Environment.getExternalStorageDirectory(),"DECO/EVENTS/"+pic+ ".jpg");
            if(DEBUG)Log.i(tag,"file made");
            output = new FileOutputStream(file);
            output.write(bytes);
            if(DEBUG)Log.i(tag,"witten");
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
    public static double scaleX=11;// if you want to change the scale (which will have to be changed in the calibration protion of the set up (not done) <done
    public static double scaleY=11;
    public static int rThresh=100;
    public static int gThresh=100;
    public static int bThresh=100;
    public static boolean longProcess=false;

    public void process() {
        ready=false;
        if(DEBUG)Log.i(tag,"taking image");
        byte[] bytes = paths.get(0);
        if(DEBUG)Log.i(tag,"image in");
        paths.remove(0);

        if(DEBUG)Log.i(tag,"Decoding");
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inPreferredConfig= Bitmap.Config.ARGB_8888;
        bits = BitmapFactory.decodeByteArray(bytes,0,bytes.length,bitmapOptions);
        Bitmap bit = bits.copy(Bitmap.Config.ARGB_8888,true);

        // we want to scale the image down to something that will be usable and able to be processed alot faster

        bit =  Bitmap.createScaledBitmap(bit,(int) (bit.getWidth()/scaleX),(int)(bit.getHeight()/scaleY),false);
        if(DEBUG)Log.i(tag,"decoded");
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
                if (r > rThresh||b>bThresh||g>gThresh ) {
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


        if(DEBUG)Log.i(tag,"Max r g b values"+rTemp+","+gTemp+","+bTemp);
        if(DEBUG)Log.i(tag,"image done");
        if(DEBUG)Log.i(tag,good+"");
        if (good&&numpix<200) {//delete if not above cut
            longProcessor(bits,bytes);
        }


        if(DEBUG)Log.i(tag,scaleX+","+scaleY+","+bit.getWidth()+","+bit.getHeight());

        bit.recycle();
        bits.recycle();
        ready=true;

    }







    public void longProcessor(Bitmap map,byte[] bits){
        /*
        this will allow for full resolution proce3ssing of the images.
        should only be used for processing after the initial image filter has been used aka processor()
         */
        longProcess=true;
        int hight = map.getHeight();

        int width = map.getWidth();
        int rTemp=0;
        int gTemp=0;
        int bTemp=0;
        int fullNumPix=0;
        boolean fineGood = false;

        for (int x = 0; x < width; x+=2) {
            for (int y = 0; y < hight; y+=2) {
                int pic;


                String col = String.format("#%08X", map.getPixel(x, y));

                int r = Integer.parseInt(col.substring(3, 5), 16);
                int b = Integer.parseInt(col.substring(5, 7), 16);
                int g = Integer.parseInt(col.substring(7, 9), 16);
                if (r > rThresh||b>bThresh||g>gThresh ) {
                    fineGood = true;
                    fullNumPix++;
                    if(fullNumPix>3){
                        try {
                            save(bits);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        longProcess=false;
                        return;//this should short circut the mthod to prevent needless processing
                    }
                }
                if(r>rTemp)
                    rTemp=r;
                if(g>gTemp)
                    gTemp=g;
                if(b>bTemp)
                    bTemp=b;

            }


        }

        if(fineGood&&fullNumPix>3){//Im paranoid this is comnpletely unneeded but it makes me feel better
                                    //it will catch any events that arnt caught cusing the return to break us.
            try {
                save(bits);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        longProcess=false;



    }























    boolean told = false;
    boolean ready=true;

    public void run() {
            while(true) {

                while(go&&ready) {

                    process();
                    if(paths.size()==0){
                        go=false;
                    }
                    if(paths.size()>30){
                        paths.clear();
                        go=false;
                    }

                }

            }
    }



}
