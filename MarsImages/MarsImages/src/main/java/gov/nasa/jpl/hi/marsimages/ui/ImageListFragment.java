package gov.nasa.jpl.hi.marsimages.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.powellware.marsimages.R;

import gov.nasa.jpl.hi.marsimages.EvernoteMars;
import gov.nasa.jpl.hi.marsimages.MarsImagesApp;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import static gov.nasa.jpl.hi.marsimages.EvernoteMars.EVERNOTE;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;

/**
 * Created by mpowell on 5/27/14.
 */
public class ImageListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener {

    private static final int THUMBNAIL_IMAGE_WIDTH = 50;
    private StickyListHeadersListView mStickyList;
    private ImageListAdapter mAdapter;

    public ImageListFragment() {} //empty ctor as per Fragment docs

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.image_list_fragment, container, false);
        mStickyList = (StickyListHeadersListView) view.findViewById(R.id.image_list_view);
        mStickyList.setOnItemClickListener(this);
        mAdapter = new ImageListAdapter(this.getActivity());
        mStickyList.setAdapter(mAdapter);
        mStickyList.setDrawingListUnderStickyHeader(false);
        SwipeRefreshLayout refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        refreshLayout.setOnRefreshListener(this);

        IntentFilter filter = new IntentFilter(MarsImagesApp.MISSION_CHANGED);
        filter.addAction(EvernoteMars.END_NOTE_LOADING);
        filter.addAction(MarsImagesApp.IMAGE_SELECTED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);

        return view;
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MarsImagesApp.MISSION_CHANGED)) {
                mAdapter.setCount(0);
                mAdapter.notifyDataSetChanged();
            }
            else if (intent.getAction().equals(EvernoteMars.END_NOTE_LOADING)) {
                mAdapter.setCount(EVERNOTE.getNotesCount());
                mAdapter.notifyDataSetChanged();
            }
            else if (intent.getAction().equals(MarsImagesApp.IMAGE_SELECTED)) {
                String selectionSource = intent.getStringExtra(MarsImagesApp.SELECTION_SOURCE);
                if (!selectionSource.equals(MarsImagesApp.LIST_SOURCE)) {
                    int i = intent.getIntExtra(MarsImagesApp.IMAGE_INDEX, 0);
                    mAdapter.setSelectedPosition(i);
                    makeSureWeCanSeeItem(i);
                }
            }
        }
    };

    private void makeSureWeCanSeeItem(int position) {
        int firstVisiblePosition = mStickyList.getFirstVisiblePosition();
        int lastVisiblePosition = mStickyList.getLastVisiblePosition();
        if (position < firstVisiblePosition || position > lastVisiblePosition) {
            mStickyList.smoothScrollToPosition(position);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public void onRefresh() {
        Log.d("refresh", "List refreshed, number of items:"+mAdapter.getCount());
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long id) {
        mAdapter.setSelectedPosition(i);
        Intent intent = new Intent(MarsImagesApp.IMAGE_SELECTED);
        intent.putExtra(MarsImagesApp.IMAGE_INDEX, i);
        intent.putExtra(MarsImagesApp.SELECTION_SOURCE, MarsImagesApp.LIST_SOURCE);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    private class ImageListAdapter extends BaseAdapter implements StickyListHeadersAdapter {

        private final LayoutInflater inflater;
        private int mItemCount;
        private int selectedPosition = 0;

        public ImageListAdapter(Context context) {
            inflater = LayoutInflater.from(context);
            mItemCount = EVERNOTE.getNotesCount();
        }

        public void setSelectedPosition(int selectedPosition) {
            this.selectedPosition = selectedPosition;
            notifyDataSetChanged();
        }

        @Override
        public View getHeaderView(int i, View view, ViewGroup viewGroup) {
            HeaderViewHolder holder;
            if (view == null) {
                holder = new HeaderViewHolder();
                view = inflater.inflate(R.layout.list_header, viewGroup, false);
                holder.text = (TextView) view.findViewById(R.id.header_title);
                view.setTag(holder);
            } else {
                holder = (HeaderViewHolder) view.getTag();
            }
            //set header text as first char in name
            String headerText = MARS_IMAGES.getMission().getSectionText(EVERNOTE.getNote(i));
            holder.text.setText(headerText);
            return view;
        }

        @Override
        public long getHeaderId(int i) {
            return MARS_IMAGES.getMission().getSol(EVERNOTE.getNote(i));
        }

        @Override
        public int getCount() {
            return mItemCount;
        }

        @Override
        public Object getItem(int i) {
            return EVERNOTE.getNote(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View recycledView, ViewGroup viewGroup) {
            ViewHolder holder;
            View view = null;
            if (recycledView == null) {
                holder = new ViewHolder();
                view = inflater.inflate(R.layout.row, viewGroup, false);
                holder.row = view.findViewById(R.id.list_row);
                holder.text = (TextView) view.findViewById(R.id.row_title);
                holder.detail = (TextView) view.findViewById(R.id.row_detail);
                holder.imageView = (ImageView)view.findViewById(R.id.row_icon);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) recycledView.getTag();
                holder.imageView.setImageDrawable(null);
                view = recycledView;
            }

            if (selectedPosition == i) {
                holder.row.setBackgroundColor(Color.CYAN);
            } else {
                holder.row.setBackgroundColor(Color.WHITE);
            }

            holder.text.setText(MARS_IMAGES.getMission().getLabelText(EVERNOTE.getNote(i)));
            holder.detail.setText(MARS_IMAGES.getMission().getDetailText(EVERNOTE.getNote(i)));
            String thumbnailURL = EVERNOTE.getThumbnailURL(i, THUMBNAIL_IMAGE_WIDTH);
            if (thumbnailURL != null) {
                ImageLoader.getInstance().displayImage(thumbnailURL, holder.imageView);
            }

            if (i == EVERNOTE.getNotesCount()-1) {
                EVERNOTE.loadMoreNotes(getActivity());
            }

            return view;
        }

        public void setCount(int count) {
            mItemCount = count;
        }
    }

    class HeaderViewHolder {
        TextView text;
    }

    class ViewHolder {
        View row;
        TextView text;
        TextView detail;
        ImageView imageView;
    }
}
