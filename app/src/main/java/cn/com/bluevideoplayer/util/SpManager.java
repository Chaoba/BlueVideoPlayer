package cn.com.bluevideoplayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Iterator;

public class SpManager {

    private static HashMap<String, Integer> BREAK_POINT_MAP = new HashMap<String, Integer>();
    private static String LAST_PLAY;

    public static void clear(Context c) {
        BREAK_POINT_MAP.clear();
        LAST_PLAY = null;
        SharedPreferences spf = c.getSharedPreferences("last_play", 0);
        Editor editor = spf.edit();
        editor.clear();
        editor.commit();

        spf = c.getSharedPreferences("break_point", 0);
        editor = spf.edit();
        editor.clear();
        editor.commit();
    }

    public static void remove(Context c, String path) {
        if (path.equals(LAST_PLAY)) {
            LAST_PLAY = null;
        }
        BREAK_POINT_MAP.remove(path);
        SharedPreferences spf = c.getSharedPreferences("last_play", 0);
        Editor editor = spf.edit();
        editor.remove(path);
        editor.clear();

        spf = c.getSharedPreferences("break_point", 0);
        editor = spf.edit();
        editor.remove(path);
        editor.clear();
    }

    public static void saveLastPlay(Context c, String path) {
        SharedPreferences spf = c.getSharedPreferences("last_play", 0);
        Editor editor = spf.edit();
        editor.putString("last_play", path);
        editor.commit();
    }

    /**
     * 获得最后播放视频的地址
     *
     * @param c
     * @return
     */
    public static String getLastPlay(Context c) {
        if (TextUtils.isEmpty(LAST_PLAY)) {
            SharedPreferences spf = c.getSharedPreferences("last_play", 0);
            return spf.getString("last_play", null);
        } else {
            return LAST_PLAY;
        }
    }

    /**
     * 将所有的断点信息保存起来
     *
     * @param c
     */
    public static void saveAllBreakPoint(Context c) {
        SharedPreferences spf = c.getSharedPreferences("break_point", 0);
        Editor editor = spf.edit();
        // editor.clear();
        Iterator keys = BREAK_POINT_MAP.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            int value = BREAK_POINT_MAP.get(key);
            editor.putInt(key, value);
        }
        editor.commit();
        saveLastPlay(c, LAST_PLAY);
    }

    /**
     * 获得指定视频的断点
     *
     * @param c
     * @param path
     * @return
     */
    public static int getBreakPoint(Context c, String path) {
        if (BREAK_POINT_MAP.containsKey(path)) {
            return BREAK_POINT_MAP.get(path);
        } else {
            SharedPreferences spf = c.getSharedPreferences("break_point", 0);
            int breakPoint = spf.getInt(path, 0);
            BREAK_POINT_MAP.put(path, breakPoint);
            return breakPoint;
        }
    }

    /**
     * 设置key的断点信息
     *
     * @param key
     * @param value
     */
    public static void setBreakPoint(String key, int value) {
        BREAK_POINT_MAP.put(key, value);
        LAST_PLAY = key;
    }
}
