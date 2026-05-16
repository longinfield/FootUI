package cn.edu.tsinghua.footinputdemo.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import cn.edu.tsinghua.footinputdemo.ScreenShot;
import cn.edu.tsinghua.footinputdemo.SideBarService;

public class ScreenShotTool {

    private ImageReader mImageReader;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private static Intent mResultData = null;
    private Context mContext;
    private SideBarService mSideBarService;



    public ScreenShotTool(int screenWidth,int screenHeight,int screenDensity,Context context,SideBarService sideBarService){
        mScreenDensity=screenDensity;
        mScreenHeight=screenHeight;
        mScreenWidth=screenWidth;
        mContext=context;
        mSideBarService=sideBarService;
        mResultData=SideBarService.mResultData;
        Log.d("screenshotcreate","screenshot");
    }


    public void startScreenShot() {
        if(mSideBarService!=null){
            Handler handler=new Handler(mSideBarService.getMainLooper());

            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    Log.d("startvirtual","virtual");
                    startVirtual();
                }
            },5);

            Log.d("startscreenshot","screenshot");
            handler.postDelayed(new Runnable() {
                public void run() {
                    //capture the screen
                    Log.d("startcapture","cap");
                    startCapture();
                }
            }, 30);


        }

        //Handler handler1 = new Handler();
        /*handler1.postDelayed(new Runnable() {
            public void run() {
                //start virtual
                Log.d("startvirtual","virtual");
                startVirtual();
            }
        }, 5);
        Log.d("startscreenshot","screenshot");
        handler1.postDelayed(new Runnable() {
            public void run() {
                //capture the screen
                Log.d("startcapture","cap");
                startCapture();
            }
        }, 30);*/
    }


    public void createImageReader() {

        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1);
        Log.d("create image reader",mImageReader.toString());
    }

    private void startVirtual() {
        if (mMediaProjection != null) {
            Log.d("mMediaProjection",mMediaProjection.toString());
            virtualDisplay();
        } else {
            setUpMediaProjection();
            Log.d("mMediaProjection",mMediaProjection.toString());
            virtualDisplay();
        }
    }

    private void setUpMediaProjection() {
        if (mResultData == null) {
            Intent intent = new Intent(mContext,ScreenShot.class);
            //intent.addCategory(Intent.CATEGORY_LAUNCHER);
            mSideBarService.startActivity(intent);
            mResultData= SideBarService.mResultData;
            //Log.d("mResultData",mResultData.toString());
        } else {
            mMediaProjection = getMediaProjectionManager().getMediaProjection(Activity.RESULT_OK, mResultData);
            //Log.d("mMediaProjection",mMediaProjection.toString());
        }
    }

    private MediaProjectionManager getMediaProjectionManager() {

        return (MediaProjectionManager) mSideBarService.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    private void virtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
    }

    private void startCapture() {
        Log.d("startcapture","startcapture");
        Image image = mImageReader.acquireLatestImage();

        if (image == null) {
            startScreenShot();
        } else {

            SaveTask mSaveTask = new SaveTask();
            AsyncTaskCompat.executeParallel(mSaveTask, image);
        }
    }


    public class SaveTask extends AsyncTask<Image, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Image... params) {

            if (params == null || params.length < 1 || params[0] == null) {

                return null;
            }

            Image image = params[0];

            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            //每个像素的间距
            int pixelStride = planes[0].getPixelStride();
            //总的间距
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            image.close();

            stopVirtual();
            tearDownMediaProjection();

            File fileImage = null;
            if (bitmap != null) {
                try {
                    fileImage = new File(FileUtil.getScreenShotsName(mSideBarService.getApplicationContext()));
                    //Log.d("fileImage",fileImage.toString());
                    if (!fileImage.exists()) {
                        fileImage.createNewFile();
                        //Log.d("fileImage",fileImage.toString());
                    }
                    FileOutputStream out = new FileOutputStream(fileImage);
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.flush();
                        out.close();
                        Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri contentUri = Uri.fromFile(fileImage);
                        media.setData(contentUri);
                        mSideBarService.sendBroadcast(media);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    fileImage = null;
                } catch (IOException e) {
                    e.printStackTrace();
                    fileImage = null;
                }
            }

            if (fileImage != null) {
                return bitmap;
            }
            return null;
        }

    }

    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    private void stopVirtual() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }

}
