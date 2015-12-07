package cn.com.bluevideoplayer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import cn.com.bluevideoplayer.fragments.LoaderCursor;
import cn.com.bluevideoplayer.util.Logger;
import cn.com.bluevideoplayer.util.SpManager;
import cn.com.bluevideoplayer.util.ToastManager;
import cn.com.bluevideoplayer.util.Util;
import io.vov.vitamio.LibsChecker;

public class PlayActivity extends Activity implements OnClickListener,
        OnSeekBarChangeListener, Callback, OnBufferingUpdateListener,
        OnCompletionListener, OnPreparedListener, OnVideoSizeChangedListener, OnErrorListener {
    public static final String PATH = "path";
    private static final String TAG = "PlayActivity";
    private static final int SEEK_BAR_UPDATE_DELAY = 200;
    private static final int CONTROLER_GONE_DELAY = 3000;
    private Button mLastBtn, mFastBackBtn, mPlayBtn, mFastForwardBtn, mNextBtn;
    private View mControler;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private TextView mCurrentTime, mTotalTime, mTitle, mTime;
    private SeekBar mProgressBar;
    private String mPath;
    private MediaPlayer mMediaPlayer;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mSurfaceViewWidth;
    private int mSurfaceViewHeight;
    private Bundle extras;
    private Context mContext = null;
    private boolean mIsVideoSizeKnown = false;
    private boolean mIsVideoReadyToBePlayed = false;
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg1) {
            switch (msg1.what) {
                case 0:
                    if (mMediaPlayer != null) {
                        long position = mMediaPlayer.getCurrentPosition();
                        mCurrentTime.setText(Util.getDuration(position));
                        mProgressBar.setProgress((int) position);
                        mHandler.sendEmptyMessageDelayed(0, SEEK_BAR_UPDATE_DELAY);
                    }
                    break;
                case 1:
                    mControler.setVisibility(View.GONE);
                    break;
            }

            super.handleMessage(msg1);
        }

    };

//	@Override
//	protected void onResume() {
//		super.onResume();
//		Logger.d(TAG, "onResume");
//		start();
//	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!LibsChecker.checkVitamioLibs(this))
            return;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.play);
        mContext = this;
        // if (getRequestedOrientation() !=
        // ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // }
        mControler = findViewById(R.id.controler);
        mSurfaceView = (SurfaceView) this.findViewById(R.id.surface);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setKeepScreenOn(true);

        mLastBtn = (Button) findViewById(R.id.last_button);
        mFastBackBtn = (Button) findViewById(R.id.fast_back_button);
        mPlayBtn = (Button) findViewById(R.id.play_button);
        mFastForwardBtn = (Button) findViewById(R.id.fast_forward_button);
        mNextBtn = (Button) findViewById(R.id.next_button);
        mCurrentTime = (TextView) mControler.findViewById(R.id.current_time);
        mTotalTime = (TextView) mControler.findViewById(R.id.total_time);
        mTitle = (TextView) findViewById(R.id.title);
        mTime = (TextView) findViewById(R.id.time);
        mProgressBar = (SeekBar) findViewById(R.id.progress);

        mProgressBar.setOnSeekBarChangeListener(this);
        mControler.setOnClickListener(this);
        mFastBackBtn.setOnClickListener(this);
        mLastBtn.setOnClickListener(this);
        mPlayBtn.setOnClickListener(this);
        mFastForwardBtn.setOnClickListener(this);
        mNextBtn.setOnClickListener(this);
        mSurfaceView.setOnClickListener(this);

        extras = getIntent().getExtras();
        if (extras != null) {
            mPath = extras.getString(PATH);
        }
        Uri uri = getIntent().getData();
        if (uri != null) {
            mPath = uri.toString();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.d(TAG, "onPause");
        pause();
        releaseMediaPlayer();
        doCleanUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, "onDestroy");
        releaseMediaPlayer();
        doCleanUp();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // if (this.getResources().getConfiguration().orientation ==
        // Configuration.ORIENTATION_LANDSCAPE) {
        // Toast.makeText(getApplicationContext(), "切换为横屏", Toast.LENGTH_SHORT)
        // .show();
        // } else if (this.getResources().getConfiguration().orientation ==
        // Configuration.ORIENTATION_PORTRAIT) {
        // Toast.makeText(getApplicationContext(), "切换为竖屏", Toast.LENGTH_SHORT)
        // .show();
        // }
    }

    private void setControlerGone() {
        mTime.setText(Util.getCurrentTime(System.currentTimeMillis()));
        mHandler.removeMessages(1);
        mHandler.sendEmptyMessageDelayed(1, CONTROLER_GONE_DELAY);
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void doCleanUp() {
        mVideoWidth = 0;
        mVideoHeight = 0;
        mIsVideoReadyToBePlayed = false;
        mIsVideoSizeKnown = false;
    }

    private void startVideoPlayback() {
        Logger.d(TAG, "startVideoPlayback");
        mProgressBar.setMax(mMediaPlayer.getDuration());
        mHolder.setFixedSize(mVideoWidth, mVideoHeight);
        int position = SpManager.getBreakPoint(mContext, mPath);
        mCurrentTime.setText(Util.getDuration(position));
        mProgressBar.setProgress(position);
        if (position > 0) {
            mMediaPlayer.seekTo(position);
        }
        start();

        mTotalTime.setText(Util.getDuration(mMediaPlayer.getDuration()));
    }

    private void playVideo(String path) {
        Logger.d(TAG, "play:" + path);
        if (!TextUtils.isEmpty(mPath)) {
            try {
                if (mMediaPlayer == null) {
                    mMediaPlayer = new MediaPlayer();
                    mMediaPlayer.setDisplay(mHolder);
                    mMediaPlayer.setOnBufferingUpdateListener(this);
                    mMediaPlayer.setOnCompletionListener(this);
                    mMediaPlayer.setOnPreparedListener(this);
                    mMediaPlayer.setOnVideoSizeChangedListener(this);
                    mMediaPlayer.setOnErrorListener(this);
                    setVolumeControlStream(AudioManager.STREAM_MUSIC);
//					mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                } else {
                    mMediaPlayer.reset();
                }
                mMediaPlayer.setDataSource(mPath);
                mMediaPlayer.prepare();
                mTitle.setText(Util.getTitle(path));
            } catch (Exception e) {
                Logger.e(TAG, e);
                ToastManager.show(this, "无法播放该视频");
                finish();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.surface:
                mControler.setVisibility(View.VISIBLE);
                break;
            case R.id.controler:
                mControler.setVisibility(View.GONE);
                break;
            case R.id.last_button:
                playPrevious();
                break;
            case R.id.fast_back_button:
                long position = mMediaPlayer.getCurrentPosition();
                long target = position - 10000;
                if (target > 0) {
                    mMediaPlayer.seekTo((int) target);
                    mProgressBar.setProgress((int) position);
                }
                break;
            case R.id.play_button:
                if (mMediaPlayer.isPlaying()) {
                    pause();
                } else {
                    start();
                }
                break;
            case R.id.fast_forward_button:
                position = mMediaPlayer.getCurrentPosition();
                target = position + 10000;
                if (target < mMediaPlayer.getDuration()) {
                    mMediaPlayer.seekTo((int) target);
                    mProgressBar.setProgress((int) position);
                }
                break;
            case R.id.next_button:
                playNext();
                break;
        }
        setControlerGone();
    }

    private void start() {
        Logger.d(TAG, "start");
        mHandler.removeMessages(0);
        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.start();
            } catch (Exception e) {
                Logger.e(TAG, e);
            }
            mPlayBtn.setBackgroundResource(R.drawable.pause);
            mHandler.sendEmptyMessageDelayed(0, SEEK_BAR_UPDATE_DELAY);
        }
        setControlerGone();
    }

    private void pause() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            mPlayBtn.setBackgroundResource(R.drawable.play);
            mHandler.removeMessages(0);
            SharedPreferences spf = getSharedPreferences("break_point", 0);
            Editor editor = spf.edit();
            editor.putString("path", mPath);
            editor.putInt("position", mMediaPlayer.getCurrentPosition());
            editor.commit();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        if (fromUser) {
            mMediaPlayer.seekTo(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        playVideo(mPath);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        if (width == 0 || height == 0) {
            Log.e(TAG, "invalid video width(" + width + ") or height(" + height
                    + ")");
            return;
        }
        Logger.d(TAG, "onVideoSizeChanged width:" + width + " height:" + height);
        mIsVideoSizeKnown = true;
        mVideoHeight = height;
        mVideoWidth = width;
        int wid = mMediaPlayer.getVideoWidth();
        int hig = mMediaPlayer.getVideoHeight();
        // 根据视频的属性调整其显示的模式
        if (wid > hig) {
            if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        } else {
            if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mSurfaceViewWidth = dm.widthPixels;
        mSurfaceViewHeight = dm.heightPixels;
        if (width > height) {
            // 竖屏录制的视频，调节其上下的空余
            int w = mSurfaceViewHeight * width / height;
            int margin = (mSurfaceViewWidth - w) / 2;
            Logger.d(TAG, "margin:" + margin);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            lp.setMargins(margin, 0, margin, 0);
            mSurfaceView.setLayoutParams(lp);
        } else {
            // 横屏录制的视频，调节其左右的空余
            int h = mSurfaceViewWidth * height / width;
            int margin = (mSurfaceViewHeight - h) / 2;
            Logger.d(TAG, "margin:" + margin);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            lp.setMargins(0, margin, 0, margin);
            mSurfaceView.setLayoutParams(lp);
        }
        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
            startVideoPlayback();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Logger.d(TAG, "onPrepared");
        mIsVideoReadyToBePlayed = true;
        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
            startVideoPlayback();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mHandler.removeMessages(0);
        SharedPreferences spf = getSharedPreferences("break_point", 0);
        Editor editor = spf.edit();
        editor.putString("path", "");
        editor.putInt("position", 0);
        editor.commit();

        playNext();
    }

    private void playNext() {
        if (LoaderCursor.mCursor.moveToFirst()) {
            do {
                String path = LoaderCursor.mCursor
                        .getString(LoaderCursor.mCursor
                                .getColumnIndex(MediaStore.Video.Media.DATA));
                if (path.equals(mPath)) {
                    if (LoaderCursor.mCursor.moveToNext()) {
                        mPath = LoaderCursor.mCursor
                                .getString(LoaderCursor.mCursor
                                        .getColumnIndex(MediaStore.Video.Media.DATA));
                        playVideo(mPath);
                        break;
                    } else {
                        break;
                    }
                }
            } while (LoaderCursor.mCursor.moveToNext());
        }
    }

    private void playPrevious() {
        if (LoaderCursor.mCursor.moveToFirst()) {
            do {
                String path = LoaderCursor.mCursor
                        .getString(LoaderCursor.mCursor
                                .getColumnIndex(MediaStore.Video.Media.DATA));
                if (path.equals(mPath)) {
                    if (LoaderCursor.mCursor.moveToPrevious()) {
                        mPath = LoaderCursor.mCursor
                                .getString(LoaderCursor.mCursor
                                        .getColumnIndex(MediaStore.Video.Media.DATA));
                        playVideo(mPath);
                        break;
                    } else {
                        break;
                    }
                }
            } while (LoaderCursor.mCursor.moveToNext());
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Logger.d(TAG, "error:" + extra);
        ToastManager.show(this, "无法播放该视频");
        finish();
        return true;
    }
}
