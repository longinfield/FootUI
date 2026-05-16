package cn.edu.tsinghua.footinputdemo.utils;

import android.content.Context;
import android.content.Intent;

import cn.edu.tsinghua.footinputdemo.SideBarService;

/**
 * launch the sidebar service
 * @author majh
 */
public class ServiceGo {

    public static void launchAccessibility(Context context) {
        Intent intent = new Intent(context, SideBarService.class);
        context.startService(intent);
    }
}
