package cn.com.bluevideoplayer.util;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Toast管理类
 *
 * @author wenhaoran
 * @version 4.0
 * @time 2013/4/15
 */
public class ToastManager {

    private static final int SHOW_STRING = 0;
    private static final int SHOW_INT = 1;
    private static Context mContext;
    private static Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_STRING:
                    show(mContext, (String) msg.obj, false);
                    break;
                case SHOW_INT:
                    show(mContext, mContext.getString(msg.arg1), false);
                    break;
            }
            super.handleMessage(msg);
        }

    };

    /**
     * 显示String类型的Toast
     *
     * @param context
     * @param msg
     */
    public static void show(Context context, String msg) {
        mContext = context;
        Message message = new Message();
        message.what = SHOW_STRING;
        message.obj = msg;
        mHandler.sendMessage(message);
    }

    /**
     * 显示int类型的Toast
     *
     * @param context
     * @param id
     */
    public static void show(Context context, int id) {
        mContext = context;
        Message message = new Message();
        message.what = SHOW_INT;
        message.arg1 = id;
        mHandler.sendMessage(message);
    }

    /**
     * 显示toast
     *
     * @param context
     * @param msg
     * @param isLong  时间控制，true为LENGTH_LONG，false为LENGTH_SHORT
     */
    public static void show(Context context, String msg, boolean isLong) {
        mContext = context;
        if (null != msg && !"".equalsIgnoreCase(msg)) {
            if (isLong) {
                Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM, 0, 0);
                toast.show();
            } else {
                Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM, 0, 0);

                toast.show();
            }
        }
    }

}
