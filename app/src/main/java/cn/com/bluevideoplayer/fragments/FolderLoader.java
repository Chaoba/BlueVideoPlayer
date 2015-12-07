package cn.com.bluevideoplayer.fragments;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cn.com.bluevideoplayer.fragments.FolderFragment.Folders;
import cn.com.bluevideoplayer.util.Logger;

public class FolderLoader extends AsyncTaskLoader<List<Folders>> {
    private static final String TAG = "FolderLoader";
    Context mContext;
    List<Folders> mList = new ArrayList<Folders>();
    List<String> mAddedList = new ArrayList<String>();
    Map<String, Integer> map = new HashMap<String, Integer>();

    public FolderLoader(Context context) {
        super(context);
        mContext = context;
        Logger.d(TAG, "FolderLoader");
    }

    @Override
    public List<Folders> loadInBackground() {
        Logger.d(TAG, "loadInBackground");
        map.clear();
        mList.clear();
        Cursor c = mContext.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                FolderFragment.FOLDER_PROJECTIONS, null, null, null);
        int column = c.getColumnIndex(MediaStore.Video.Media.DATA);
        while (c.moveToNext()) {
            String path = c.getString(column);
            path = path.substring(0, path.lastIndexOf(File.separator));
            if (map.containsKey(path)) {
                int count = map.get(path);
                map.put(path, count + 1);
            } else {
                map.put(path, 1);
            }
        }
        c.close();
        Set<Entry<String, Integer>> set = map.entrySet();
        Iterator<Entry<String, Integer>> i = set.iterator();
        while (i.hasNext()) {
            Entry<String, Integer> entry = i.next();
            Folders folders = new Folders();
            folders.path = entry.getKey();
            folders.children = entry.getValue();
            mList.add(folders);
        }
        return mList;
    }

    @Override
    protected void onStartLoading() {
        Logger.d(TAG, "onStartLoading");
        if (mList.size() > 0) {
            deliverResult(mList);
        } else {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(List<Folders> files) {
        Logger.d(TAG, "deliverResult");
        super.deliverResult(files);
    }
}
