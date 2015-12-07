package cn.com.bluevideoplayer.util;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import cn.com.bluevideoplayer.R;

public class Util {
    public static final String CURSOR = "CURSOR";
    public static final boolean DEBUG = true;
    public static final String[] PROJECTIONS = {MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.MIME_TYPE, MediaStore.Video.Media.DATA,
            MediaStore.Video.Media._ID,};
    static final int BYTES_IN_KILOBYTES = 1024;
    static final DecimalFormat dec = new DecimalFormat("###.#");
    static final String KILOBYTES = " KB";
    static final String MEGABYTES = " MB";
    static final String GIGABYTES = " GB";
    static final SimpleDateFormat FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");
    static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final String TAG = "Util";

    /**
     * Gets the extension of a file name, like ".png" or ".jpg".
     *
     * @param uri
     * @return Extension including the dot("."); "" if there is no extension;
     * null if uri was null.
     */
    public static String getExtension(String uri) {
        if (uri == null) {
            return null;
        }

        int dot = uri.lastIndexOf(".");
        if (dot >= 0) {
            return uri.substring(dot);
        } else {
            // No extension.
            return "";
        }
    }

    /**
     * @return The MIME type for the given file.
     */
    public static String getMimeType(File file) {

        String extension = getExtension(file.getName());

        if (extension.length() > 0)
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    extension.substring(1));

        return "application/octet-stream";
    }

    /**
     * Get the file size in a human-readable string.
     *
     * @param size
     * @return
     * @author paulburke
     */
    public static String getReadableFileSize(long size) {
        float fileSize = 0;
        String suffix = KILOBYTES;

        if (size > BYTES_IN_KILOBYTES) {
            fileSize = size / BYTES_IN_KILOBYTES;
            if (fileSize > BYTES_IN_KILOBYTES) {
                fileSize = fileSize / BYTES_IN_KILOBYTES;
                if (fileSize > BYTES_IN_KILOBYTES) {
                    fileSize = fileSize / BYTES_IN_KILOBYTES;
                    suffix = GIGABYTES;
                } else {
                    suffix = MEGABYTES;
                }
            }
        }
        return String.valueOf(dec.format(fileSize) + suffix);
    }

    public static String getCurrentTime(long millSeconds) {
        Date date = new Date(millSeconds);
        return TIME_FORMAT.format(date);
    }

    public static String getDateTime(long millSeconds) {
        Date date = new Date(millSeconds);
        return FORMAT.format(date);
    }

    /**
     * 转化时长的格式到时：分：秒的格式
     *
     * @param time - 时间，以微妙的格式
     * @return
     */
    public static String getDuration(long time) {
        time /= 1000;
        long minute = time / 60;
        long hour = minute / 60;
        long second = time % 60;
        minute %= 60;
        if (hour == 0) {
            return String.format("%02d:%02d", minute, second);
        } else {
            return String.format("%02d:%02d:%02d", hour, minute, second);
        }
    }

    public static ArrayList<File> getChildFiles(String path) {
        ArrayList<File> list = new ArrayList<File>();
        File f = new File(path);
        File[] child = null;
        if (f.isDirectory()) {
            child = f.listFiles();
        }
        if (child != null) {
            for (File file : child) {
                list.add(file);
            }
        }
        return list;
    }

    public static CharSequence getTitle(String path) {
        int dot = path.lastIndexOf(".");
        int split = path.lastIndexOf(File.separator);
        if (dot >= 0 && split >= 0) {
            return path.substring(split + 1, dot);
        } else {
            return "";
        }
    }

    public static CharSequence getFolderTitle(String path) {
        int split = path.lastIndexOf(File.separator);
        if (split >= 0) {
            return path.substring(split + 1, path.length());
        } else {
            return "";
        }
    }

    public static boolean deleteFile(Context c, File f) {
        if (f.delete()) {
            Logger.d(TAG, "delete succed");
            ContentResolver resolver = c.getContentResolver();
            resolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.DATA + "=?",
                    new String[]{f.getAbsolutePath()});
            SpManager.remove(c, f.getAbsolutePath());
            return true;
        } else {
            ToastManager.show(c, R.string.delete_fail);
            return false;
        }
    }

    public static final class ViewHolder {
        public TextView titleView;
        public TextView durationView;
        public TextView sizeView;
        public ImageView imgView;
    }
}
