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
import android.content.res.Configuration;
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
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static android.content.Context.CAMERA_SERVICE;//made my life easier....



import java.util.TimeZone;


import static android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF;


public class Camera2BasicFragment extends Fragment  implements View.OnClickListener  {

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
    int preped=0;
    boolean run=false;
    int done=0;
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
    Activity activity=getActivity();
    TextView textImgsDone;
    TextView textStatus;

    CaptureRequest.Builder captureBuilder;
    Handler backgroundHandler;
    CameraCaptureSession.CaptureListener captureListener;
    Surface surface = null;
    boolean surfacegot=false;
    int running =0;
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
    private boolean go=false;
    Runnable runable = new Runnable() {

        boolean take;
        public void setbool(boolean b){
            take=b;
        }
        @Override
        public void run() {

            boolean tr = true;
            while (tr) {
                while (!go||!run) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                while (go && run) {
                    go=false;

                    try {
                        Thread.sleep(100);// need to make this listen for the picture to be taken
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (preped == 0) {//the looper has to be prepared before you can start the looper that is in the takePicture() method
                        Looper.prepare();
                        preped = 1;
                    }
                    /*
                    this is where all of the work is started in the thread.
                     */
                    startPreview();//start the preview here because take picture requires a running preview
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
                        Thread.sleep(10000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.post(new Runnable(){
                        public void run() {
                            textImgsDone.setText(done+"");
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
     * {@link CameraDevice.StateListener} is called when {@link CameraDevice} changes its state.
     */
    private CameraDevice.StateListener mStateListener = new CameraDevice.StateListener() {

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
        return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
    }

    @SuppressLint("Override")
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        textImgsDone = (TextView) getView().findViewById(R.id.textDone);
        textStatus = (TextView ) getView().findViewById(R.id.textStatus);

        view.findViewById(R.id.picture).setOnClickListener(this);

        //view.findViewById(R.id.info).setOnClickListener(this);
    }

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
            // mStateListener is called.
            manager.openCamera(cameraId, mStateListener, null);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
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
                    new CameraCaptureSession.StateListener() {

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
            manager =(CameraManager) activity.getSystemService(CAMERA_SERVICE);

            // Pick the best JPEG size that can be captured with this CameraDevice.
            characteristics =
                    manager.getCameraCharacteristics(mCameraDevice.getId());

            exposure =characteristics.get(SENSOR_INFO_EXPOSURE_TIME_RANGE).getUpper();//gets the upper exposure time suported by the camera object
            //  exposure = exposure - Long.valueOf((long)10000);//reduces the exposure slightly inorder to prevent errors. Will try without. seems to be working without
            // Log.i("tag", characteristics.get(SENSOR_INFO_EXPOSURE_TIME_RANGE) + "");
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
            }

            // We use an ImageReader to get a JPEG from CameraDevice.
            // Here, we create a new ImageReader and prepare its Surface as an output from camera.
            reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 5);
            outputSurfaces = new ArrayList<Surface>(5);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));



            // This is the CaptureRequest.Builder that we use to take a picture.
            captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,CONTROL_AE_MODE_OFF);
            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposure);
            captureBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,CaptureRequest.NOISE_REDUCTION_MODE_OFF);
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, Byte.valueOf(100+""));
            Log.i("tag",exposure.floatValue()+"");

            surface = reader.getSurface();
            if(!surface.isValid()){
                Log.i("tag","invalid surface");
                return;
            }
            captureBuilder.addTarget(surface);

            setUpCaptureRequestBuilder2(captureBuilder);

            // Orientation
            rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));



            // This listener is called when a image is ready in ImageReader
            readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    go=true;

                    try {

                        image = reader.acquireLatestImage();

                        buffer = image.getPlanes()[0].getBuffer();
                        bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);

                        //save(bytes);
                        Log.i("tag","saved image");
                        done++;

                        processor.setImage(bytes);//starts the data processor
                        //processor.process();


                        buffer.clear();//try to fix the buffer abandonding
                        buffer=null;
                        image.close();


                        surface.release();
                        surface=null;
                        Log.i("tag","surface released");
                        reader.close();//added because it seems to be wanting to overload the reader.


                   // } catch (FileNotFoundException e) {
                     //   e.printStackTrace();
                    //} catch (IOException e) {
                      //  e.printStackTrace();
                    } finally {
                        if (image != null) {



                            Log.i("tag",done+"");
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {

                    try {
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
            };

            // We create a Handler since we want to handle the result JPEG in a background thread

            if(thread!=null)
                thread.quit();

            thread = new HandlerThread("CameraPicture");
            thread.start();

            backgroundHandler = new Handler(thread.getLooper());
            reader.setOnImageAvailableListener(readerListener, backgroundHandler);

            // This listener is called when the capture is completed.
            // Note that the JPEG data is not available in this listener, but in the
            // ImageReader.OnImageAvailableListener we created above.
            captureListener =
                    new CameraCaptureSession.CaptureListener() {

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
                    new CameraCaptureSession.StateListener() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            try {
                                session.capture(captureBuilder.build(), captureListener,
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

            manager=null;
            characteristics=null;



        } catch (CameraAccessException e) {
            e.printStackTrace();
        }




    }
    DataProcessor processor = new DataProcessor();

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.picture: {

            }
            if (run == false) {
                if(!go)
                    go=true;

                run = true;

                textStatus.setText("Running");
                textStatus.setTextColor(Color.GREEN);
                Toast.makeText(getActivity(),"Starting Data Collection",Toast.LENGTH_LONG).show();
                if(running==0) {

                    runner.start();
                    startUiUpdateThread();
                    processor.start();

                    running=1;
                }
            }
            else if (run==true){
                run=false;
                Toast.makeText(getActivity(),"Stopped",Toast.LENGTH_LONG).show();
                textStatus.setText("STOPPED");
                textStatus.setTextColor(Color.RED);

            }

            break;

        }

    }

}