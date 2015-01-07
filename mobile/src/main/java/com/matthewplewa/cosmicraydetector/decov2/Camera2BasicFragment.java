package com.matthewplewa.cosmicraydetector.decov2;

/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;

import static android.content.Context.CAMERA_SERVICE;
import static android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE;
import static android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE;
import static android.hardware.camera2.CameraCharacteristics.SENSOR_MAX_ANALOG_SENSITIVITY;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF;


public class Camera2BasicFragment extends Fragment implements View.OnClickListener {

    final boolean DEBUG = true;
    final static boolean RESTARTED = false;
    public static boolean restarted = false;

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    SurfaceTexture texture;

    /*
       I cannot seem to find where these ojects are being created saying open that is causing the "too many files open" error, so I am just going
       to move all of the file declorations to outside the take picture method.
        */
    int preped = 0;
    boolean run = false;
    int done = 0;
    byte[] bytes;
    ByteBuffer buffer;
    Calendar c;
    OutputStream output;
    Image image;
    CameraManager manager;
    HandlerThread thread;
    File file;
    ImageReader.OnImageAvailableListener readerListener;
    int rotation;
    int offset;
    int offsetHrs;
    int offsetMins;
    CameraCharacteristics characteristics;
    Long exposure;
    int width;
    int height;
    ImageReader reader;
    List<Surface> outputSurfaces;
    TimeZone z;
    Activity activity = getActivity();
    TextView textImgsDone;
    TextView textStatus;
    TextView textEventsFound;
    TextView textQueue;
    ImageView CroppedImage;
    Button buttonStart;
    Button ButtonConfig;
    ToggleButton AutoCal;
    EditText Editr1, Editr2, Editb1, Editb2, Editg1, Editg2, Editscale;

    CaptureRequest.Builder captureBuilder;
    Handler backgroundHandler;
    CameraCaptureSession.CaptureCallback CaptureCallback;
    Surface surface = null;
    boolean surfacegot = false;
    boolean changed = false;
    int running = 0;
    public static boolean calibrating = true;
    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;
    /**
     * A {@link android.hardware.camera2.CaptureRequest.Builder} for camera preview.
     */
    private CaptureRequest.Builder mPreviewBuilder;
    /**
     * A {@link android.hardware.camera2.CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mPreviewSession;
    /**
     * A reference to the opened {@link android.hardware.camera2.CameraDevice}.
     */
    private CameraDevice mCameraDevice;
    private boolean go = false;
    public static int numEvents = 0;
    public static int inQuaue = 0;
    public static Bitmap croped = null;
    public static boolean newCropped = false;
    Runnable runable = new Runnable() {

        boolean take;

        public void setbool(boolean b) {

            take = b;
        }

        @Override
        public void run() {

            boolean tr = true;
            while (tr) {
                if (restarted) {
                    try {
                        Thread.sleep(5000);//this will fix all the problems with it not being ready when it starts;
                        restarted = false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();

                    }
                }

                while (!go || !run) {
                    try {
                        Thread.sleep(100);

                    } catch (InterruptedException e) {
                        e.printStackTrace();

                    }
                }
                //if(done-initial>=1000){//this will open a new activity that will then reopen this activitiy.
                //    Intent workaround = new Intent(getActivity(),WorkAround.class);
                //    startActivity(workaround);
                //    getActivity().finish();
                //    go=false;
                //}


                while (go && run) {


                    go = false;
                    if (preped == 0) {//the looper has to be prepared before you can start the looper that is in the takePicture() method
                        Looper.prepare();
                        preped = 1;
                    }
                    startPreview();//start the preview here because take picture requires a running preview

                    try {
                        Thread.sleep(100);// need to make this listen for the picture to be taken
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    /*
                    this is where all of the work is started in the thread.
                     */

                    takePicture();//starts the image on the main thread NOT this thread.


                }
            }
        }
    };
    Thread runner = new Thread(runable);

    private void startUiUpdateThread() {
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            private long startTime = System.currentTimeMillis();

            public void run() {
                boolean keep = true;
                while (keep) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.post(new Runnable() {
                        public void run() {
                            textImgsDone.setText(" " + done + "");
                            textEventsFound.setText(numEvents + " ");
                            textQueue.setText(inQuaue + " In Queue ");

                            if (!runner.isAlive() && running == 1) {
                                runner.start();
                            }

                            if (newCropped = true)
                                CroppedImage.setImageBitmap(croped);

                            if (!calibrating) {
                                textStatus.setText(" Running");
                            }
                            BufferedWriter writer = null;
                            if (changed) {
                                changed = false;
                                try {


                                    File file = new File(Environment.getExternalStorageDirectory(), "DECO/status");
                                    if (!file.exists()) {
                                        file.mkdirs();
                                    }
                                    if (DEBUG) Log.i("write", "making file");
                                    file = new File(Environment.getExternalStorageDirectory(), "DECO/status/current.txt");

                                    writer = new BufferedWriter(new FileWriter(file));
                                    // data is output to a text file

                                    writer.write(done + "");
                                    writer.newLine();
                                    writer.write(numEvents + "");
                                    writer.newLine();
                                    if (run)//this will tell this activity if it should start again.
                                        writer.write(1 + "");
                                    if (!run)
                                        writer.write(0 + "");
                                    writer.close();

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }


    /**
     * {@link android.view.TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link android.view.TextureView}.
     */
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            configureTransform(width, height);
            startPreview();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
            configureTransform(width, height);
            startPreview();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };
    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;
    /**
     * True if the app is currently trying to open camera
     */
    private boolean mOpeningCamera;
    /**
     * {@link android.hardware.camera2.CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraDevice = cameraDevice;
            startPreview();
            mOpeningCamera = false;

        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
            mOpeningCamera = false;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {

            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
            mOpeningCamera = false;
        }

    };

    public static Camera2BasicFragment newInstance() {
        Camera2BasicFragment fragment = new Camera2BasicFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @SuppressLint("Override")
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//screen wasnt staying on so i added this to keep it on!
        return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
    }

    @SuppressLint("Override")
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        textImgsDone = (TextView) getView().findViewById(R.id.textDone);
        textEventsFound = (TextView) getView().findViewById((R.id.textFound));
        textStatus = (TextView) getView().findViewById(R.id.textStatus);
        textQueue = (TextView) getView().findViewById(R.id.textQueue);
        CroppedImage = (ImageView) getView().findViewById(R.id.imageCropped);
        AutoCal = (ToggleButton) getView().findViewById(R.id.toggleAutoOn);
        ButtonConfig = (Button) getView().findViewById(R.id.buttonConfig);
        Editb1 = (EditText) getView().findViewById(R.id.editb1);
        Editr1 = (EditText) getView().findViewById(R.id.editr1);
        Editg1 = (EditText) getView().findViewById(R.id.editg1);
        Editb2 = (EditText) getView().findViewById(R.id.editb2);
        Editr2 = (EditText) getView().findViewById(R.id.editr2);
        Editg2 = (EditText) getView().findViewById(R.id.editg2);
        Editscale = (EditText) getView().findViewById(R.id.editScale);


        buttonStart = (Button) getActivity().findViewById(R.id.buttonPicture);
        if (buttonStart != null) {
            buttonStart.setOnClickListener(this);
            AutoCal.setOnClickListener(this);
            ButtonConfig.setOnClickListener(this);
            Log.i("tag", "onclick set");
        }
        ButtonConfig.setVisibility(View.INVISIBLE);
        Editscale.setVisibility(View.INVISIBLE);
        Editr1.setVisibility(View.INVISIBLE);
        Editr2.setVisibility(View.INVISIBLE);
        Editg1.setVisibility(View.INVISIBLE);
        Editg2.setVisibility(View.INVISIBLE);
        Editb1.setVisibility(View.INVISIBLE);
        Editb2.setVisibility(View.INVISIBLE);
        Log.i("tag", buttonStart + " this");
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        int[] tall = new int[3];
        Scanner scanner;
        File thefile = new File(Environment.getExternalStorageDirectory(), "DECO/status/current.txt");


        if (thefile.exists()) {
            try {
                scanner = new Scanner(new File(Environment.getExternalStorageDirectory(), "DECO/status/current.txt"));


                int i = 0;
                while (scanner.hasNextInt()) {
                    tall[i++] = scanner.nextInt();
                }
                scanner.close();
                scanner = null;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            textImgsDone.setText(tall[0] + "");
            textEventsFound.setText(tall[1] + "");
            done = tall[0];
            initial = tall[0];

            numEvents = tall[1];
            if (tall[2] == 1)
                restarted = true;

            //view.findViewById(R.id.info).setOnClickListener(this);
        }
    }

    int initial = 0;

    @SuppressLint("Override")
    @Override
    public void onResume() {
        super.onResume();
        openCamera();
    }

    @SuppressLint("Override")
    @Override
    public void onPause() {
        super.onPause();
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    /**
     * Opens a {@link CameraDevice}. The result is listened by `mStateListener`.
     */
    private void openCamera() {
        Activity activity = getActivity();
        if (null == activity || activity.isFinishing() || mOpeningCamera) {
            return;
        }
        mOpeningCamera = true;
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];

            // To get a list of available sizes of camera preview, we retrieve an instance of
            // StreamConfigurationMap from CameraCharacteristics
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }


            // We are opening the camera with a listener. When it is ready, onOpened of
            // mStateCallbackis called.
            manager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        }
        if (restarted) {
            startStop();

        }

    }

    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(surface);


            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            // When the session is ready, we start displaying the preview.
                            mPreviewSession = cameraCaptureSession;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Activity activity = getActivity();
                            if (null != activity) {
                                Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            // The camera preview can be run in a background thread. This is a Handler for camera
            // preview.
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            Handler backgroundHandler = new Handler(thread.getLooper());

            // Finally, we start displaying the camera preview.
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fills in parameters of the {@link CaptureRequest.Builder}.
     *
     * @param builder The builder for a {@link CaptureRequest}
     */
    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        // In this sample, we just let the camera device pick the automatic settings.
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        //builder.set(CaptureRequest.)
    }

    private void setUpCaptureRequestBuilder2(CaptureRequest.Builder builder) {
        // In this sample, we just let the camera device pick the automatic settings.
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        //builder.set(CaptureRequest.)
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in openCamera and
     * also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    public void takePicture() {


        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            manager = (CameraManager) activity.getSystemService(CAMERA_SERVICE);

            // Pick the best JPEG size that can be captured with this CameraDevice.
            characteristics =
                    manager.getCameraCharacteristics(mCameraDevice.getId());

            exposure = characteristics.get(SENSOR_INFO_EXPOSURE_TIME_RANGE).getUpper();//gets the upper exposure time suported by the camera object TODO
            //exposure = exposure - Long.valueOf((long)10000);//reduces the exposure slightly inorder to prevent errors. Will try without. seems to be working without
            if (DEBUG) Log.i("tag", characteristics.get(SENSOR_INFO_EXPOSURE_TIME_RANGE) + "");
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics
                        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);
            }
            width = 640;
            height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
                if (DEBUG) Log.i("tag", width + "," + height);
            }

            // We use an ImageReader to get a JPEG from CameraDevice.
            // Here, we create a new ImageReader and prepare its Surface as an output from camera.
            reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 10);
            outputSurfaces = new ArrayList<Surface>(5);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));


//TODO in here i want to make sure that it will work for legacy extentions!!!!!!!!!!!!


            // This is the CaptureRequest.Builder that we use to take a picture.
            captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF);
            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposure); //TODO reenable this
            captureBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
            captureBuilder.set(CaptureRequest.BLACK_LEVEL_LOCK, false);// without it unlocked it might cause issues
            Range<Integer> sensitivity = characteristics.get(SENSOR_INFO_SENSITIVITY_RANGE);
            captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, characteristics.get(SENSOR_MAX_ANALOG_SENSITIVITY));// TODO this should also be done in the claibrator but saved forever. This just looked nice for the nexus 5.... this must be less than SENSOR_MAX_ANALOG_SENSITIVITY
            if (DEBUG)
                Log.i("max analog sensitivity", "" + characteristics.get(SENSOR_MAX_ANALOG_SENSITIVITY));
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, Byte.valueOf(100 + ""));
            if (DEBUG) Log.i("tag", (1 / (exposure.doubleValue() * .000000001)) + " <= exposure");

            surface = reader.getSurface();
            if (!surface.isValid()) {
                if (DEBUG) Log.i("tag", "invalid surface");
                return;
            }
            captureBuilder.addTarget(reader.getSurface());

            setUpCaptureRequestBuilder2(captureBuilder);

            // Orientation
            rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));


            // This listener is called when a image is ready in ImageReader
            readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    go = true;// this will tell our thread that we are ready to take a nother photo. This
                    // is not the most efficiant way to handle this because the camera takes a photo
                    // and it takes a lil bit before this is called. OnImageCapture or somthing similar should be used!

                    try {

                        image = reader.acquireLatestImage();//Pulls your image from the image reader

                        buffer = image.getPlanes()[0].getBuffer();//Each image has diffrent plains you have to pull from the first plain aka index of 0
                        bytes = new byte[buffer.capacity()];

                        buffer.get(bytes);
                        //we do not want to save here anymore because it just causes efficancy issues
                        //save(bytes);

                        done++;//this is a gobal counter that will allow us to find out how many frames we have captured
                        changed = true;

                        if (DEBUG) Log.i("tag", "Passed Image");
                        /////////////////////////////////////////////////////////////////////
                        try {//  THIS IS ONLY FOR TESTING OF THE IMAGE FILTER THIS MUST BE REMOVED BEFORE DISTROBUTION TODO
                            save(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                        ////////////////////////////////////////////////////////////////////
                        //calibrating=false;
                        if (calibrating) {

                            calibrater.setImage(bytes);

                        }
                        if (DEBUG) Log.i("whats up", calibrating + "");
                        if (!calibrating)
                            processor.setImage(bytes);//starts the data processor/ sends it the bytes


                        //after capture clean up to prevent memory leaks
                        buffer.clear();//try to fix the buffer abandonding
                        buffer = null;
                        image.close();
                        bytes = null;

                        if (surface != null)
                            surface.release();
                        surface = null;
                        if (DEBUG) Log.i("tag", "surface released");
                        reader.close();//added because it seems to be wanting to overload the reader.


                    } finally {
                        if (image != null) {

                            // just to make sure!
                            image = null;

                            if (DEBUG) Log.i("tag", "done on this run=" + (done - initial));
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    // this is to make the name for the file when we save it also does all the formating
                    try {
                        if (DEBUG) Log.i("tag", "saving in all images");
                        Calendar c = Calendar.getInstance();
                        int year = c.get(Calendar.YEAR);
                        int month = c.get(Calendar.MONTH) + 1;
                        int day = (c.get(Calendar.DAY_OF_MONTH));
                        int hour = c.get(Calendar.HOUR_OF_DAY);
                        int min = c.get(Calendar.MINUTE);
                        int seconds = c.get(Calendar.SECOND);

                        //string convertion
                        String Month = month + "";
                        String Day = day + "";
                        String Hour = hour + "";
                        String Min = min + "";
                        String Seconds = seconds + "";


                        //keeps it in the correct formate
                        if (month < 10)
                            Month = "0" + month;
                        if (day < 10)
                            Day = "0" + day;
                        if (hour < 10)
                            Hour = "0" + hour;
                        if (min < 10)
                            Min = "0" + min;
                        if (seconds < 10)
                            Seconds = "0" + seconds;

                        //setting formate for file name
                        String pic = "" + year + Month + Day + "_" + Hour + Min + Seconds;
                        // bellow starts the file path creation

                        File file = new File(Environment.getExternalStorageDirectory(), "DECO/AllSamples");
                        //^^ sets up for new directory check
                        if (!file.exists()) {
                            file.mkdirs();// if the directory doesnt exist this will make it.
                        }
                        // creates the file to be output
                        file = new File(Environment.getExternalStorageDirectory(), "DECO/AllSamples/" + pic + ".jpg");
                        output = new FileOutputStream(file);
                        output.write(bytes);
                        output.flush();
                        output.close();
                        output = null;


                    } finally {
                        if (null != output) {


                        }
                    }
                }
            };

            // We create a Handler since we want to handle the result JPEG in a background thread

            if (thread != null)
                thread.quit();

            thread = new HandlerThread("CameraPicture");
            thread.start();

            backgroundHandler = new Handler(thread.getLooper());
            reader.setOnImageAvailableListener(readerListener, backgroundHandler);

            // This listener is called when the capture is completed.
            // Note that the JPEG data is not available in this listener, but in the
            // ImageReader.OnImageAvailableListener we created above.
            CaptureCallback =
                    new CameraCaptureSession.CaptureCallback() {

                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session,
                                                       CaptureRequest request,
                                                       TotalCaptureResult result) {
                            //  Toast.makeText(activity, "Saved: " + file, Toast.LENGTH_SHORT).show();
                            // We restart the preview when the capture is completed

                            //startPreview();
                            /*
                            when starting the preview there i rna into major issues with it trying to take a photo and trying to start the preview at the same time.
                            It is unknown why this occures and shouldnt happen with the new api so it is assumed to be a bug in the os. Preview wont mean much to us but its kinda annoying!
                             */
                        }

                    };

            // Finally, we can start a new CameraCaptureSession to take a picture.
            mCameraDevice.createCaptureSession(outputSurfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            try {
                                session.capture(captureBuilder.build(), CaptureCallback,
                                        backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                        }
                    }, backgroundHandler

            );

            manager = null;
            characteristics = null;


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    DataProcessor processor = new DataProcessor();
    Calibrate calibrater = new Calibrate();


    public void startStop() {

        if (run == false) {
            if (!go)
                go = true;

            run = true;
            buttonStart.setText("STOP");


            textStatus.setText(" Calibrating");
            textStatus.setTextColor(Color.GREEN);
            Toast.makeText(getActivity(), "Starting Data Collection", Toast.LENGTH_LONG).show();
            if (running == 0) {

                runner.start();
                startUiUpdateThread();
                processor.start();
                calibrater.start();

                running = 1;
            }
            if (!runner.isAlive()) {
                runner.start();
            }

        } else if (run == true) {
            buttonStart.setText("START");
            run = false;
            Toast.makeText(getActivity(), "Stopped", Toast.LENGTH_LONG).show();
            textStatus.setText(" STOPPED");
            textStatus.setTextColor(Color.RED);

        }

    }

    @Override
    public void onClick(View view) {// will have the button itself trigger the onclick

        switch (view.getId()) {
            case R.id.buttonPicture: {
                startStop();
            }

            case R.id.toggleAutoOn: {
                if (AutoCal.isChecked()) {
                    calibrating = false;
                    ButtonConfig.setVisibility(View.VISIBLE);
                    Editscale.setVisibility(View.VISIBLE);
                    Editr1.setVisibility(View.VISIBLE);
                    Editr2.setVisibility(View.VISIBLE);
                    Editg1.setVisibility(View.VISIBLE);
                    Editg2.setVisibility(View.VISIBLE);
                    Editb1.setVisibility(View.VISIBLE);
                    Editb2.setVisibility(View.VISIBLE);

                }
                if (!AutoCal.isChecked()) {
                    calibrating = true;
                    ButtonConfig.setVisibility(View.INVISIBLE);
                    Editscale.setVisibility(View.INVISIBLE);
                    Editr1.setVisibility(View.INVISIBLE);
                    Editr2.setVisibility(View.INVISIBLE);
                    Editg1.setVisibility(View.INVISIBLE);
                    Editg2.setVisibility(View.INVISIBLE);
                    Editb1.setVisibility(View.INVISIBLE);
                    Editb2.setVisibility(View.INVISIBLE);

                }

            }

            case R.id.buttonConfig: {
                DataProcessor.scaleX = Integer.parseInt(Editscale.getText().toString());

                DataProcessor.rThresh = Integer.parseInt(Editr1.getText().toString());
                DataProcessor.gThresh = Integer.parseInt(Editg1.getText().toString());
                DataProcessor.bThresh = Integer.parseInt(Editb1.getText().toString());
                DataProcessor.r2Thresh = Integer.parseInt(Editr2.getText().toString());
                DataProcessor.g2Thresh = Integer.parseInt(Editg2.getText().toString());
                DataProcessor.b2Thresh = Integer.parseInt(Editb2.getText().toString());


            }

            break;

        }

    }

}