package cn.com.bluevideoplayer;


import android.app.Application;
import android.content.ContextWrapper;

import edu.mit.mobile.android.imagecache.ImageCache;

/**
 * app运行的Application
 *
 * @author Administrator
 */
public class MainApplication extends Application {
    public static ContextWrapper mContext;
    public static ImageCache mCache;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mCache = ImageCache.getInstance(mContext);
    }

}
