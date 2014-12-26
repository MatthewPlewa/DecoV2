package com.matthewplewa.cosmicraydetector.decov2;

/**
 * Created by Matthew on 12/23/2014.
 */
public class DataExport {

    //TODO Take in data
    //TODO Take in Device specific info
    //TODO put into a readable file that the data logger handles
    //TODO set up initiliaiser

    //Variables

    Byte[] img;


    public DataExport(){
        //TODO null out all data feild in here for giggles
    }

    public boolean addImage(Byte[] imgIn){
        img= imgIn;
        if(img!=null)
            return true;
        else
            return false;
    }

 //   public boolean addObsData(){ //TODO

   // }



}
