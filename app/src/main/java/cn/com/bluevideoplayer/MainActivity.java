package cn.com.bluevideoplayer;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import cn.com.bluevideoplayer.fragments.FolderFragment;
import cn.com.bluevideoplayer.fragments.LoaderCursor.CursorLoaderListFragment;
import cn.com.bluevideoplayer.util.Logger;
import cn.com.bluevideoplayer.util.SpManager;
import cn.com.bluevideoplayer.util.ToastManager;

public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";
    ActionBar bar;
    MenuItem mRefresh, mRefreshing;
    boolean mIsRefreshing = false;
    private Activity mActivity;
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            mRefresh.setVisible(true);
            mIsRefreshing = false;
            FolderFragment mFragment = (FolderFragment) mActivity
                    .getFragmentManager().findFragmentByTag("folder");
            if (mFragment != null) {
                mFragment.refresh();
            }
            ToastManager.show(mActivity, R.string.end_scan);
            setProgressBarIndeterminateVisibility(false);
        }
    };
    private ScanSdFilesReceiver scanReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        mActivity = this;
        bar = getActionBar();
        bar.setBackgroundDrawable(getResources()
                .getDrawable(R.drawable.divider));
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
                | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_USE_LOGO);

        Tab tab1 = bar.newTab();
        tab1.setText(R.string.video_list);
        tab1.setTabListener(new TabListener<CursorLoaderListFragment>(
                "contentprovider", CursorLoaderListFragment.class));
        bar.addTab(tab1);
        // Tab tab2 = bar.newTab();
        // tab2.setText("目录");
        // tab2.setTabListener(new TabListener<FileListFragment>("file",
        // FileListFragment.class));
        // bar.addTab(tab2);
        Tab tab3 = bar.newTab();
        tab3.setText(R.string.folder_list);
        tab3.setTabListener(new TabListener<FolderFragment>("folder",
                FolderFragment.class));
        bar.addTab(tab3);

        // Tab tab4 = bar.newTab();
        // tab4.setText("设置");
        // tab4.setTabListener(new TabListener<SettingFragment>("setting",
        // SettingFragment.class));
        // bar.addTab(tab4);

        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    @Override
    protected void onDestroy() {
        SpManager.saveAllBreakPoint(mActivity);
        super.onDestroy();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mRefresh = menu.findItem(R.id.menu_refresh);
        if (mIsRefreshing) {
            mRefresh.setVisible(false);
            setProgressBarIndeterminateVisibility(true);
        } else {
            setProgressBarIndeterminateVisibility(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_continue:
                String path = SpManager.getLastPlay(mActivity);
                if (TextUtils.isEmpty(path)) {
                    ToastManager.show(this, R.string.no_played_video);
                } else {
                    Intent intent = new Intent(this, NetPlayActivity.class);
                    intent.putExtra(PlayActivity.PATH, path);
                    startActivity(intent);
                }
                return true;
            case R.id.menu_refresh:
                mRefresh.setVisible(false);
                setProgressBarIndeterminateVisibility(true);
                if (VERSION.SDK_INT <= 18) {
                    registerReceiver();
                    Intent i = new Intent(Intent.ACTION_MEDIA_MOUNTED,
                            Uri.parse("file://"
                                    + Environment.getExternalStorageDirectory()));
                    sendBroadcast(i);
                    ToastManager.show(mActivity, R.string.start_scan);
                    mIsRefreshing = true;
                } else {
                    MediaScannerConnection.scanFile(this,
                            new String[]{Environment
                                    .getExternalStorageDirectory().toString()},
                            null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    Logger.i("ExternalStorage", "Scanned " + path
                                            + ":");
                                    Logger.i("ExternalStorage", "-> uri=" + uri);
                                    mHandler.sendEmptyMessage(0);
                                }
                            });
                }

                return true;
            case R.id.menu_setting:
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_left_in,
                        R.anim.slide_left_out);
                // SpManager.clear(this);
                // ToastManager.show(mActivity, "清除完成");
                // break;
                // case R.id.menu_about:
                // new AlertDialog.Builder(mActivity)
                // .setIcon(android.R.drawable.ic_dialog_info)
                // .setTitle(R.string.about)
                // .setMessage(getString(R.string.about_content))
                // .setPositiveButton(android.R.string.yes, null).show();
                break;
        }
        return false;
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        scanReceiver = new ScanSdFilesReceiver();
        registerReceiver(scanReceiver, intentFilter);
    }

    @Override
    public void onBackPressed() {
        if (bar.getSelectedTab().getText()
                .equals(mActivity.getString(R.string.folder_list))) {
            FolderFragment f = (FolderFragment) mActivity.getFragmentManager()
                    .findFragmentByTag("folder");
            f.onBackPressed();
            return;
        } else
            super.onBackPressed();
    }

    public class TabListener<T extends Fragment> implements
            ActionBar.TabListener {
        private final String mTag;
        private final Class<T> mClass;
        private final Bundle mArgs;
        private Fragment mFragment;

        public TabListener(String tag, Class<T> clz) {
            this(tag, clz, null);
        }

        public TabListener(String tag, Class<T> clz, Bundle args) {
            mTag = tag;
            mClass = clz;
            mArgs = args;

            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state. If so, deactivate it, because our
            // initial state is that a tab isn't shown.
            mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
            if (mFragment != null && !mFragment.isDetached()) {
                FragmentTransaction ft = mActivity.getFragmentManager()
                        .beginTransaction();
                ft.detach(mFragment);
                ft.commit();
            }
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                mFragment = Fragment.instantiate(mActivity, mClass.getName(),
                        mArgs);
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                ft.attach(mFragment);
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            Toast.makeText(mActivity, "Reselected!", Toast.LENGTH_SHORT).show();
        }
    }

    private class ScanSdFilesReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Logger.d(TAG, action);
            if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
                Logger.d(TAG, "scan finished");
                mRefresh.setVisible(true);
                mIsRefreshing = false;
                FolderFragment mFragment = (FolderFragment) mActivity
                        .getFragmentManager().findFragmentByTag("folder");
                if (mFragment != null) {
                    mFragment.refresh();
                }
                ToastManager.show(mActivity, R.string.end_scan);
                setProgressBarIndeterminateVisibility(false);
                unregisterReceiver(scanReceiver);
            }
        }
    }
}
