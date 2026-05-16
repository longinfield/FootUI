package cn.edu.tsinghua.footinputdemo.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ryze on 2016-5-26.
 */
public class FileUtil {

  //系统保存截图的路径
  public static final String SCREENCAPTURE_PATH = "ScreenCapture" + File.separator + "Screenshots" + File.separator;
//  public static final String SCREENCAPTURE_PATH = "ZAKER" + File.separator + "Screenshots" + File.separator;

  public static final String SCREENSHOT_NAME = "Screenshot";

  public static String getAppPath(Context context) {

    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {


      return Environment.getExternalStorageDirectory().toString();

    } else {

      return context.getFilesDir().toString();
    }

  }


  public static StringBuffer getScreenShots(Context context) {
    String root=Environment.getExternalStorageDirectory().toString();
    StringBuffer stringBuffer = new StringBuffer(root);
    stringBuffer.append(File.separator);
    stringBuffer.append("Pictures");
    stringBuffer.append(File.separator);
    stringBuffer.append("Screenshots");
    stringBuffer.append(File.separator);

    //stringBuffer.append(SCREENCAPTURE_PATH);

    //File file = new File(stringBuffer.toString());

    //if (!file.exists()) {
     // file.mkdirs();
    //}

    return stringBuffer;

  }

  public static String getScreenShotsName(Context context) {

    @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");

    String date = simpleDateFormat.format(new Date());

    StringBuffer stringBuffer = getScreenShots(context);
    stringBuffer.append(SCREENSHOT_NAME);
    stringBuffer.append("_");
    stringBuffer.append(date);
    stringBuffer.append(".png");

    //Log.d("stringbuffer to string",stringBuffer.toString());
    return stringBuffer.toString();

  }


}
