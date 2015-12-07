package cn.com.bluevideoplayer.fragments;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import cn.com.bluevideoplayer.R;
import cn.com.bluevideoplayer.util.Util;

public class FileInfoAdapter extends BaseAdapter {
    private static LayoutInflater mInflater;
    private Context mContext;
    private String[] INFOS;
    private String[] COLUMNS = new String[7];
    private Cursor mCursor;

    public FileInfoAdapter(Context context, Cursor c) {
        mCursor = c;
        mContext = context;

        INFOS = mContext.getResources().getStringArray(
                R.array.file_info_array);
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        COLUMNS[0] = mCursor.getString(mCursor
                .getColumnIndex(MediaStore.Video.Media.TITLE));
        COLUMNS[1] = Util.getReadableFileSize(mCursor.getLong(mCursor
                .getColumnIndex(MediaStore.Video.Media.SIZE)));
        COLUMNS[2] = Util.getDuration(mCursor.getLong(mCursor
                .getColumnIndex(MediaStore.Video.Media.DURATION)));
        COLUMNS[3] = Util.getDateTime(mCursor.getLong(mCursor
                .getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)));
        COLUMNS[4] = mCursor.getString(mCursor
                .getColumnIndex(MediaStore.Video.Media.DATA));
        COLUMNS[5] = mCursor.getString(mCursor
                .getColumnIndex(MediaStore.Video.Media.RESOLUTION));
        COLUMNS[6] = mCursor.getString(mCursor
                .getColumnIndex(MediaStore.Video.Media.MIME_TYPE));
    }

    @Override
    public int getCount() {
        return INFOS.length;
    }

    @Override
    public Object getItem(int position) {
        return INFOS[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = mInflater.inflate(R.layout.file_info_item, null);
        TextView key = (TextView) convertView.findViewById(R.id.key);
        TextView value = (TextView) convertView.findViewById(R.id.value);
        key.setText(INFOS[position]);
        value.setText(COLUMNS[position]);
//		if (position == 0) {
//			convertView.setBackgroundResource(R.drawable.list_top);
//		} else if (position == getCount() - 1) {
//			convertView.setBackgroundResource(R.drawable.list_bottom);
//		}else{
//			convertView.setBackgroundResource(R.drawable.list_middle);
//		}
        return convertView;
    }

}
