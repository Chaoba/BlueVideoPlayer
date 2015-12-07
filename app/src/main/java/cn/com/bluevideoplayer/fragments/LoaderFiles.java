/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.com.bluevideoplayer.fragments;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.com.bluevideoplayer.MainApplication;
import cn.com.bluevideoplayer.NetPlayActivity;
import cn.com.bluevideoplayer.PlayActivity;
import cn.com.bluevideoplayer.R.drawable;
import cn.com.bluevideoplayer.R.id;
import cn.com.bluevideoplayer.R.layout;
import cn.com.bluevideoplayer.R.string;
import cn.com.bluevideoplayer.util.Logger;
import cn.com.bluevideoplayer.util.ToastManager;
import cn.com.bluevideoplayer.util.Util;
import cn.com.bluevideoplayer.util.Util.ViewHolder;
import edu.mit.mobile.android.imagecache.ImageCache.OnImageLoadListener;

public class LoaderFiles {
    public static final String TAG = "LoaderFiles";
    public static final Comparator<File> FILE_COMPARATOR = new Comparator<File>() {
        @Override
        public int compare(File object1, File object2) {
            return object1.getName().compareTo(object2.getName());
        }
    };
    private static final SparseArray<ImageView> mImageViewsToLoad = new SparseArray<ImageView>();
    public static Context mContext;
    private static String mCurrentPath;
    private static LayoutInflater mInflater;

    public static class FileListLoader extends AsyncTaskLoader<List<File>> {
        String mCurrentPath;
        List<File> mFiles;

        public FileListLoader(Context context, String path) {
            super(context);
            Logger.d(TAG, "FileListLoader");
            mCurrentPath = path;
        }

        @Override
        public List<File> loadInBackground() {
            Logger.d(TAG, "query child files");
            List<File> files = Util.getChildFiles(mCurrentPath);
            // Sort the list.
            Collections.sort(files, FILE_COMPARATOR);
            files.add(0, null);
            // Done!
            return files;
        }

        /**
         * Called when there is new data to deliver to the client. The super
         * class will take care of delivering it; the implementation here just
         * adds a little more logic.
         */
        @Override
        public void deliverResult(List<File> files) {
            Logger.d(TAG, "deliverResult" + files.size());
            if (isReset()) {
                // An async query came in while the loader is stopped. We
                // don't need the result.
                if (files != null) {
                    onReleaseResources(files);
                }
            }
            List<File> oldApps = files;
            mFiles = files;

            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(files);
            }

            // At this point we can release the resources associated with
            // 'oldApps' if needed; now that the new result is delivered we
            // know that it is no longer in use.
            if (oldApps != null) {
                onReleaseResources(oldApps);
            }
        }

        /**
         * Handles a request to start the Loader.
         */
        @Override
        protected void onStartLoading() {
            Logger.d(TAG, "onStartLoading");
            if (mFiles != null) {
                // If we currently have a result available, deliver it
                // immediately.
                deliverResult(mFiles);
            } else {
                forceLoad();
            }

        }

        /**
         * Handles a request to stop the Loader.
         */
        @Override
        protected void onStopLoading() {
            Logger.d(TAG, "onStopLoading");
            // Attempt to cancel the current load task if possible.
            cancelLoad();
        }

        /**
         * Handles a request to cancel a load.
         */
        @Override
        public void onCanceled(List<File> apps) {
            super.onCanceled(apps);

            // At this point we can release the resources associated with 'apps'
            // if needed.
            onReleaseResources(apps);
        }

        /**
         * Handles a request to completely reset the Loader.
         */
        @Override
        protected void onReset() {
            super.onReset();

            // Ensure the loader is stopped
            onStopLoading();

            // At this point we can release the resources associated with 'apps'
            // if needed.
            if (mFiles != null) {
                onReleaseResources(mFiles);
                mFiles = null;
            }

        }

        /**
         * Helper function to take care of releasing resources associated with
         * an actively loaded data set.
         */
        protected void onReleaseResources(List<File> apps) {
            // For a simple List<> there is nothing to do. For something
            // like a Cursor, we would close it here.
        }
    }

    public static class FileListAdapter extends ArrayAdapter<File> {

        private String mPath;

        public FileListAdapter(Context context, String path) {
            super(context, layout.video_item);
            mPath = path;
        }

        public void setData(List<File> data) {
            clear();
            if (data != null) {
                addAll(data);
            }
        }

        /**
         * Populate new items in the list.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                view = mInflater.inflate(layout.video_item, parent, false);
            } else {
                view = convertView;
            }
            ViewHolder holder = (ViewHolder) view.getTag();
            if (holder == null) {
                holder = new ViewHolder();
                holder.durationView = (TextView) view.findViewById(id.time);
                holder.sizeView = (TextView) view.findViewById(id.size);
                holder.titleView = (TextView) view.findViewById(id.title);
                holder.imgView = (ImageView) view.findViewById(id.sumbnail);
            }

            if (position == 0) {
//				view.setBackgroundResource(R.drawable.list_top);
                holder.imgView.setBackgroundResource(drawable.folder);
                holder.titleView.setText("..");
                holder.durationView.setText("上级目录");
                holder.sizeView.setText("");
            } else {
//				if (position == getCount() - 1) {
//					view.setBackgroundResource(R.drawable.list_bottom);
//				} else{
//					view.setBackgroundResource(R.drawable.list_middle);
//				}
                File item = getItem(position);
                holder.titleView.setText(item.getName());
                holder.sizeView.setText("");
                holder.durationView.setText(Util.getDateTime(item.lastModified()));
                if (item.isDirectory()) {
                    holder.imgView.setBackgroundResource(drawable.folder);
                } else {
                    holder.sizeView.setText(Util.getReadableFileSize(item
                            .length()));
                    String mime = Util.getMimeType(item);
                    Logger.d(TAG, "mime:" + mime);
                    if (mime != null && mime.startsWith("video")) {
                        final ImageView img = (ImageView) view
                                .findViewById(id.sumbnail);
                        String path = item.getAbsolutePath();
                        try {
                            Drawable draw = MainApplication.mCache
                                    .loadImage(position,
                                            Uri.parse(path),
                                            120, 120);
                            if (draw != null) {
                                img.setBackgroundDrawable(draw);
                            } else {
                                img.setBackgroundResource(drawable.icon_movie_normal);
                                mImageViewsToLoad.put(position, img);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        holder.imgView.setBackgroundResource(drawable.file);

                    }
                }
            }
            return view;
        }
    }

    public static class FileListFragment extends ListFragment implements
            LoaderManager.LoaderCallbacks<List<File>>, OnImageLoadListener {

        // This is the Adapter being used to display the list's data.
        FileListAdapter mAdapter;

        // If non-null, this is the current filter the user has provided.
        String mCurFilter;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mContext = this.getActivity();
            mInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            SharedPreferences spf = mContext.getSharedPreferences("path", 0);
            mCurrentPath = spf.getString("path", null);
            if (TextUtils.isEmpty(mCurrentPath)) {
                mCurrentPath = Environment.getExternalStorageDirectory()
                        .getAbsolutePath();
            }

        }

        @Override
        public void onPause() {
            super.onPause();
            SharedPreferences spf = mContext.getSharedPreferences("path", 0);
            Editor editor = spf.edit();
            editor.putString("path", mCurrentPath);
            editor.commit();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            // Give some text to display if there is no data. In a real
            // application this would come from a resource.
            setEmptyText("No sdcard");
            MainApplication.mCache.registerOnImageLoadListener(this);
            // We have a menu item to show in action bar.
            setHasOptionsMenu(true);
            // View header = mInflater.inflate(R.layout.video_item, null,
            // false);
            // if (getListView().getAdapter()==null) {
            // getListView().addHeaderView(header);
            // }
            // Create an empty adapter we will use to display the loaded data.
            mAdapter = new FileListAdapter(getActivity(), mCurrentPath);
            setListAdapter(mAdapter);
            getListView().setOnCreateContextMenuListener(this);
            getListView().setDivider(getResources().getDrawable(drawable.divider));
            // Start out with a progress indicator.
            setListShown(false);

            // Prepare the loader. Either re-connect with an existing one,
            // or start a new one.
            Bundle data = new Bundle();
            data.putString("path", mCurrentPath);
            getLoaderManager().initLoader(0, data, this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                                        ContextMenuInfo menuInfo) {
            menu.setHeaderTitle(string.menu_title);
            menu.add(v.getId(), 0, Menu.NONE, "删除");
            menu.add(v.getId(), 1, Menu.NONE, "详细信息");
            super.onCreateContextMenu(menu, v, menuInfo);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
                    .getMenuInfo();
            int position = menuInfo.position;
            final File f = mAdapter.getItem(position);
            if (item.getItemId() == 0) {
                new AlertDialog.Builder(getActivity())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(string.file_delete)
                        .setMessage(
                                getString(string.file_delete_confirm,
                                        f.getName()))
                        .setNegativeButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        if (Util.deleteFile(mContext, f)) {
                                            mAdapter.remove(f);
                                        }
                                    }

                                }).setPositiveButton(android.R.string.no, null)
                        .show();
            } else if (item.getItemId() == 1) {
                String mime = Util.getMimeType(f);
                if (mime != null && mime.startsWith("video")) {
                    String path = f.getAbsolutePath();
                    if (LoaderCursor.mCursor.moveToFirst()) {
                        do {
                            if (path.equals(LoaderCursor.mCursor.getString(LoaderCursor.mCursor
                                    .getColumnIndex(MediaStore.Video.Media.DATA)))) {
                                View v = mInflater.inflate(layout.file_info,
                                        null);
                                ListView list = (ListView) v
                                        .findViewById(id.list);
                                FileInfoAdapter adapter = new FileInfoAdapter(
                                        mContext, LoaderCursor.mCursor);
                                list.setAdapter(adapter);
                                new AlertDialog.Builder(getActivity())
                                        .setIcon(
                                                android.R.drawable.ic_dialog_info)
                                        .setTitle(string.file_info)
                                        .setView(v)
                                        .setPositiveButton(
                                                android.R.string.yes, null)
                                        .show();
                            }
                        } while (LoaderCursor.mCursor.moveToNext());
                    }
                } else {
                    ToastManager.show(mContext, "只能查看视频文件属性");
                }

            }
            return super.onContextItemSelected(item);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            // Place an action bar item for searching.
            // MenuItem item = menu.add("Search");
            // item.setIcon(android.R.drawable.ic_menu_search);
            // item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            // SearchView sv = new SearchView(getActivity());
            // item.setActionView(sv);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Logger.d(TAG, "on item click:" + position);
            if (position == 0) {
                File f = new File(mCurrentPath);
                String parent = f.getParent();
                if (parent != null) {
                    mCurrentPath = parent;
                    Bundle data = new Bundle();
                    mCurrentPath = parent;
                    data.putString("path", mCurrentPath);
                    getLoaderManager().restartLoader(0, data, this);
                }
            } else {
                File f = mAdapter.getItem(position);
                if (f != null) {
                    if (f.isDirectory()) {
                        Bundle data = new Bundle();
                        mCurrentPath = f.getAbsolutePath();
                        data.putString("path", mCurrentPath);
                        getLoaderManager().restartLoader(0, data, this);
                    } else {
                        String mime = Util.getMimeType(f);
                        if (mime != null && mime.startsWith("video")) {
                            String path = f.getAbsolutePath();
                            Intent intent = new Intent(mContext,
                                    NetPlayActivity.class);
                            intent.putExtra(PlayActivity.PATH, path);
                            startActivity(intent);
                        }
                    }
                }
            }
        }

        @Override
        public Loader<List<File>> onCreateLoader(int id, Bundle args) {
            String path = args.getString("path");
            Logger.d(TAG, "create loader:" + path);
            return new FileListLoader(getActivity(), path);
        }

        @Override
        public void onLoadFinished(Loader<List<File>> loader, List<File> data) {
            Logger.d(TAG, "load file finished");
            // Set the new data in the adapter.
            mAdapter.setData(data);

            // The list should now be shown.
            if (isResumed()) {
                setListShown(true);
            } else {
                setListShownNoAnimation(true);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<File>> loader) {
            Logger.d(TAG, "onLoaderReset");
            mAdapter.setData(null);
        }

        @Override
        public void onImageLoaded(int id, Uri arg1, Drawable image) {
            Log.d(TAG, "onImageLoaded:" + id);
            ImageView ivRef = mImageViewsToLoad.get(id);
            if (ivRef == null) {
                Log.d(TAG, "ivRef=null");
                return;
            }
            // final ImageView iv = ivRef.get();
            // if (iv == null) {
            // Log.d(TAG, "ivRef=null");
            // mImageViewsToLoad.remove(id);
            // return;
            // }
            ivRef.setBackgroundDrawable(image);

        }
    }

}
