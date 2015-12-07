package cn.com.bluevideoplayer.util;

import android.content.Context;
import android.graphics.LightingColorFilter;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Button;

public class DarkButton extends Button {

    public DarkButton(Context context) {
        super(context);
    }

    public DarkButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // darken background
                getBackground().setColorFilter(
                        new LightingColorFilter(0xff888888, 0x000000));
                break;

            case MotionEvent.ACTION_UP:
                // clear color filter
                getBackground().setColorFilter(null);
                break;
        }
        return super.onTouchEvent(event);
    }

}