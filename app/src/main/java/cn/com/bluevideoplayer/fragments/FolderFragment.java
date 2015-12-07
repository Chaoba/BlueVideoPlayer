package cn.com.bluevideoplayer.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import cn.com.bluevideoplayer.NetPlayActivity;
import cn.com.bluevideoplayer.PlayActivity;
import cn.com.bluevideoplayer.R;
import cn.com.bluevideoplayer.fragments.FolderFragment.Folders;
import cn.com.bluevideoplayer.fragments.LoaderCursor.CursorAdapter;
import cn.com.bluevideoplayer.util.LoadingDialog;
import cn.com.bluevideoplayer.util.Logger;
import cn.com.bluevideoplayer.util.Util;
import cn.com.bluevideoplayer.util.Util.ViewHolder;

public class FolderFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<Folders>> {
    static final String[] FOLDER_PROJECTIONS = {MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA};
    private static final String TAG = "FolderFragment";
    private static final SparseArray<ImageView> mImageViewsToLoad = new SparseArray<ImageView>();
    public static Cursor mCursor;
    private static Context mContext;
    private static LayoutInflater mInflater;
    Activity mActivity;
    FolderAdapter mAdapter;
    LoaderCursor.CursorAdapter mCursorAdapter;
    List<Folders> mList;
    String mCurrentPath;

    LoadingDialog mLoadingDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "Oncraete");
        mContext = this.getActivity();
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // // Start out with a progress indicator.
        // setListShown(false);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Logger.d(TAG, "onActivityCreated");
        mActivity = getActivity();
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCurrentPath != null) {
            showFolderVideos();
        }
    }

    @Override
    public Loader<List<Folders>> onCreateLoader(int id, Bundle args) {
        return new FolderLoader(mContext);
    }

    @Override
    public void onLoadFinished(Loader<List<Folders>> loader, List<Folders> data) {
        Logger.d(TAG, "onload finished");
        mCurrentPath = null;
        mList = data;
        mAdapter = new FolderAdapter();
        setListAdapter(mAdapter);
        getListView().setOnCreateContextMenuListener(this);
        getListView()
                .setDivider(getResources().getDrawable(R.drawable.divider));
        // getView().setBackgroundColor(getResources().getColor(R.color.background_color));
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Folders>> loader) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        if (mCurrentPath != null) {
            menu.setHeaderTitle(R.string.menu_title);
            menu.add(v.getId(), 0, Menu.NONE, "删除");
            menu.add(v.getId(), 1, Menu.NONE, "详细信息");
        } else {
            menu.setHeaderTitle(R.string.menu_title);
            menu.add(v.getId(), 0, Menu.NONE, "删除");
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
                .getMenuInfo();
        int position = menuInfo.position;
        if (mCurrentPath != null) {
            Cursor c = (Cursor) mCursorAdapter.getItem(position);
            final File f = new File(c.getString(c
                    .getColumnIndex(MediaStore.Video.Media.DATA)));
            if (item.getItemId() == 0) {
                new AlertDialog.Builder(getActivity())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.file_delete)
                        .setMessage(
                                getString(R.string.file_delete_confirm,
                                        f.getName()))
                        .setNegativeButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        Util.deleteFile(mContext, f);
                                    }

                                }).setPositiveButton(android.R.string.no, null)
                        .show();
            } else if (item.getItemId() == 1) {
                View v = mInflater.inflate(R.layout.file_info, null);
                ListView list = (ListView) v.findViewById(R.id.list);
                FileInfoAdapter adapter = new FileInfoAdapter(mContext, c);
                list.setAdapter(adapter);
                new AlertDialog.Builder(getActivity())
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setTitle(R.string.file_info).setView(v)
                        .setPositiveButton(android.R.string.yes, null).show();
            }
        } else {
            Folders folder = mList.get(position);
            final String path = folder.path;
            new AlertDialog.Builder(getActivity())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.file_delete)
                    .setMessage(
                            getString(R.string.files_delete_confirm,
                                    folder.children))
                    .setNegativeButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    new DeleteFiles().execute(path);
                                }

                            }).setPositiveButton(android.R.string.no, null)
                    .show();
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Logger.d(TAG, "on item click:" + position);
        if (mCurrentPath == null) {
            Folders folder = mList.get(position);
            String path = folder.path;
            mCurrentPath = path;
            showFolderVideos();
        } else {
            Cursor c = (Cursor) l.getItemAtPosition(position);
            String path = c.getString(c
                    .getColumnIndex(MediaStore.Video.Media.DATA));
            Intent intent = new Intent(mContext, NetPlayActivity.class);
            intent.putExtra(PlayActivity.PATH, path);
            startActivity(intent);
        }

    }

    private void showFolderVideos() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        String sort_type = prefs.getString("sort_type", "0");
        String sort_order = prefs.getString("sort_order", "0");

        Cursor c = mContext.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                Util.PROJECTIONS,
                MediaStore.Video.Media.DATA + " like '" + mCurrentPath + "%'",
                null,
                Util.PROJECTIONS[Integer.valueOf(sort_type)]
                        + (sort_order.equals("0") ? " asc" : " desc"));
        mCursorAdapter = new CursorAdapter(getActivity(), R.layout.video_item,
                c, new String[]{MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.SIZE}, new int[]{R.id.time,
                R.id.title, R.id.size});

        getListView().setAdapter(mCursorAdapter);
    }

    public void onBackPressed() {
        Logger.d(TAG, "onback pressed");
        if (mCurrentPath == null) {
            mActivity.finish();
        } else {
            mCurrentPath = null;
            getLoaderManager().initLoader(0, null, this);
        }
    }

    public void refresh() {
        Logger.d(TAG, "refresh");
        mCurrentPath = null;
        getLoaderManager().restartLoader(0, null, this);

    }

    public static class Folders {
        String path;
        int children;
    }

    class FolderAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mList == null ? 0 : mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                view = mInflater.inflate(R.layout.video_item, parent, false);
            } else {
                view = convertView;
            }
            // if (position == 0) {
            // view.setBackgroundResource(R.drawable.list_top);
            // } else if (position == getCount() - 1) {
            // view.setBackgroundResource(R.drawable.list_bottom);
            // }
            ViewHolder holder = (ViewHolder) view.getTag();
            if (holder == null) {
                holder = new ViewHolder();
                holder.durationView = (TextView) view.findViewById(R.id.time);
                holder.sizeView = (TextView) view.findViewById(R.id.size);
                holder.titleView = (TextView) view.findViewById(R.id.title);
                holder.imgView = (ImageView) view.findViewById(R.id.sumbnail);
            }
            // holder.imgView.setVisibility(View.GONE);
            Folders folder = mList.get(position);
            // holder.titleView.setText(Util.getTitle(folder.path));
            holder.titleView.setText(Util.getFolderTitle(folder.path));
            holder.durationView.setText(getString(R.string.video_count,
                    folder.children));
            return view;
        }

    }

    class DeleteFiles extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            Cursor c = mContext.getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    Util.PROJECTIONS,
                    MediaStore.Video.Media.DATA + " like '" + params[0] + "%'",
                    null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String path = c.getString(c
                            .getColumnIndex(MediaStore.Video.Media.DATA));
                    Util.deleteFile(mContext, new File(path));
                }
                c.close();
            }
            File f = new File(params[0]);
            File[] files = f.listFiles();
            if (files == null || files.length == 0) {
                Logger.d(TAG, "empty folder");
                f.delete();
            }

            return null;
        }

        protected void onPreExecute() {
            mLoadingDialog = new LoadingDialog(mContext);
            mLoadingDialog.show();
        }

        protected void onPostExecute(String result) {
            refresh();
            if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
            }
        }

    }
}
