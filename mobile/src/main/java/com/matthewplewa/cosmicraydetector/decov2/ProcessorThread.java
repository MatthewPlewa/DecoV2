package com.matthewplewa.cosmicraydetector.decov2;

import android.util.Log;

/**
 * Created by Matthew on 7/22/2014.
 */
public class ProcessorThread extends Thread {



    public ProcessorThread() {

        super();
        Log.i(null, "starting thread");
    }


    private static boolean go=false;


    public static void setbool(boolean a){
        go=a;
        Log.i(null,"bool set");
    }



    public  void run()  {
        Log.i("dev","thread started");
        while(go==false){
            try {
                sleep(100);
                Log.i("dev","thread slept");
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        while (go==true){
            Camera2BasicFragment runner = new Camera2BasicFragment();
            //runner.takePicture();

            try {
                sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
