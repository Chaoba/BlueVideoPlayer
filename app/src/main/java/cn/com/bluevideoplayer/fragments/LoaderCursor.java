package cn.com.bluevideoplayer.fragments;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import cn.com.bluevideoplayer.MainApplication;
import cn.com.bluevideoplayer.NetPlayActivity;
import cn.com.bluevideoplayer.PlayActivity;
import cn.com.bluevideoplayer.R;
import cn.com.bluevideoplayer.util.Logger;
import cn.com.bluevideoplayer.util.SpManager;
import cn.com.bluevideoplayer.util.Util;
import cn.com.bluevideoplayer.util.Util.ViewHolder;
import edu.mit.mobile.android.imagecache.ImageCache.OnImageLoadListener;

/**
 * Created by Liyanshun on 2015/2/11.
 */
public class LoaderCursor {
    private static final String TAG = "LoaderCursor";
    private static final SparseArray<ImageView> mImageViewsToLoad = new SparseArray<ImageView>();
    public static Cursor mCursor;
    private static Context mContext;
    private static LayoutInflater mInflater;
    private static int mFrist, mLast;

    // private static ImageCache mCache;

    public static class CursorLoaderListFragment extends ListFragment implements
            LoaderManager.LoaderCallbacks<Cursor>, OnImageLoadListener,
            OnScrollListener {

        CursorAdapter mAdapter;
        Bundle mBundle;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mContext = this.getActivity();
            mInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        }

        @Override
        public void onResume() {
            super.onResume();
            getLoaderManager().restartLoader(0, null, this);
            // mAdapter.NotifyContentChanged();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mContext = this.getActivity();
            // mCache = ImageCache.getInstance(mContext);
            MainApplication.mCache.registerOnImageLoadListener(this);
            // Give some text to display if there is no data. In a real
            // application this would come from a resource.
            // setEmptyText("No phone numbers");

            // We have a menu item to show in action bar.
            setHasOptionsMenu(true);

            mAdapter = new CursorAdapter(getActivity(), R.layout.video_item,
                    null, new String[]{MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.TITLE,
                    MediaStore.Video.Media.SIZE}, new int[]{
                    R.id.time, R.id.title, R.id.size});
            setListAdapter(mAdapter);
            getListView().setOnCreateContextMenuListener(this);
            getListView().setOnScrollListener(this);
            getListView().setDivider(
                    getResources().getDrawable(R.drawable.divider));
            // Start out with a progress indicator.
            setListShown(false);
            // getView().setBackgroundColor(getResources().getColor(R.color.background_color));
            // Prepare the loader. Either re-connect with an existing one,
            // or start a new one.
            getLoaderManager().initLoader(0, null, this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                                        ContextMenuInfo menuInfo) {
            menu.setHeaderTitle(R.string.menu_title);
            menu.add(v.getId(), 0, Menu.NONE,
                    mContext.getString(R.string.delete));
            menu.add(v.getId(), 1, Menu.NONE,
                    mContext.getString(R.string.detail));
            super.onCreateContextMenu(menu, v, menuInfo);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
                    .getMenuInfo();
            int position = menuInfo.position;
            Cursor c = (Cursor) mAdapter.getItem(position);
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
            return super.onContextItemSelected(item);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            // Place an action bar item for searching.
            // MenuItem item = menu.add("Search");
            // item.setIcon(android.R.drawable.ic_menu_search);
            // item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            // SearchView sv = new SearchView(getActivity());
            // // sv.setOnQueryTextListener(this);
            // item.setActionView(sv);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Cursor c = (Cursor) l.getItemAtPosition(position);
            String path = c.getString(c
                    .getColumnIndex(MediaStore.Video.Media.DATA));
            Intent intent = new Intent(mContext, NetPlayActivity.class);
            intent.putExtra(PlayActivity.PATH, path);
            startActivity(intent);
        }

        // These are the Contacts rows that we will retrieve.

        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Logger.d(TAG, "onCreateLoader");
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(mContext);
            String sort_type = prefs.getString("sort_type", "0");
            String sort_order = prefs.getString("sort_order", "0");
            return new CursorLoader(mContext,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    Util.PROJECTIONS, null, null,
                    Util.PROJECTIONS[Integer.valueOf(sort_type)]
                            + (sort_order.equals("0") ? " asc" : " desc"));
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mAdapter.swapCursor(data);
            mCursor = data;
            // The list should now be shown.
            if (isResumed()) {
                setListShown(true);
            } else {
                setListShownNoAnimation(true);
            }
        }

        public void onLoaderReset(Loader<Cursor> loader) {
            Logger.d(TAG, "onLoaderReset");
            mAdapter.swapCursor(null);
        }

        @Override
        public void onImageLoaded(int id, Uri uri, Drawable image) {
            if (id < mFrist || id > mLast) {
                return;
            }
            final ImageView ivRef = mImageViewsToLoad.get(id);
            // final ImageView iv = ivRef.get();
            if (ivRef == null) {
                Log.d(TAG, "ivRef=null");
                mImageViewsToLoad.remove(id);
                return;
            }
            Logger.d(TAG, "loaded:" + id);
            ivRef.setBackgroundDrawable(image);
            mImageViewsToLoad.remove(id);
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            mFrist = firstVisibleItem;
            mLast = firstVisibleItem + visibleItemCount;

        }
    }

    public static class CursorAdapter extends SimpleCursorAdapter {
        public CursorAdapter(Context context, int layout, Cursor c,
                             String[] from, int[] to) {
            super(context, layout, c, from, to);
        }

        public void NotifyContentChanged() {
            super.onContentChanged();
        }

        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            final ImageView img = (ImageView) view.findViewById(R.id.sumbnail);
            ViewHolder holder = (ViewHolder) view.getTag();
            if (holder == null) {
                holder = new ViewHolder();
                // List view
                holder.durationView = (TextView) view.findViewById(R.id.time);
                holder.sizeView = (TextView) view.findViewById(R.id.size);
            }
            // int position = cursor.getPosition();
            // if (position == 0) {
            // view.setBackgroundResource(R.drawable.list_top);
            // } else if (position == cursor.getCount() - 1) {
            // view.setBackgroundResource(R.drawable.list_bottom);
            // }else{
            // view.setBackgroundResource(R.drawable.list_middle);
            // }
            int duration = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Video.Media.DURATION));
            int size = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Video.Media.SIZE));
            holder.sizeView.setText(Util.getReadableFileSize(size));
            String path = cursor.getString(cursor
                    .getColumnIndex(MediaStore.Video.Media.DATA));
            String formatedDuration = mContext.getString(R.string.duration,
                    Util.getDuration(SpManager.getBreakPoint(mContext, path)),
                    Util.getDuration(duration));
            holder.durationView.setText(formatedDuration);
//			Bitmap bit=ThumbnailUtils.createVideoThumbnail(path,Images.Thumbnails.MINI_KIND);
//			img.setBackgroundDrawable(new BitmapDrawable(bit));
            try {
                Drawable draw = MainApplication.mCache.loadImage(
                        cursor.getPosition(), Uri.parse(path), 250, 100);
                if (draw != null) {
                    img.setBackgroundDrawable(draw);
                } else {
                    img.setBackgroundResource(R.drawable.icon_movie_normal);
                    mImageViewsToLoad.put(cursor.getPosition(), img);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            view.setTag(holder);
        }
    }
}
