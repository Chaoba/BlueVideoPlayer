package cn.com.bluevideoplayer.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ProgressBar;

import cn.com.bluevideoplayer.R;

public class LoadingDialog extends ProgressDialog {
    // 要指定样式，调用此构造方法
    public LoadingDialog(Context context, int theme) {
        super(context, theme);
    }

    // 默认使用R.style.my_dialog_style
    public LoadingDialog(Context context) {
        super(context, R.style.my_dialog_style);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setScreenBrightness();
        setContentView(R.layout.myalert);

        ProgressBar image = (ProgressBar) LoadingDialog.this
                .findViewById(R.id.loading_img);
    }

}
