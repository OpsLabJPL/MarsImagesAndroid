package gov.nasa.jpl.hi.marsimages.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
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
public class ImageListFragment extends Fragment implements AdapterView.OnItemClickListener {

    public static final int THUMBNAIL_IMAGE_WIDTH = 50;
    private StickyListHeadersListView mStickyList;
    private ImageListAdapter mAdapter;

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    public ImageListFragment() {
    } //empty ctor as per Fragment docs

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.image_list_fragment, container, false);
        mStickyList = (StickyListHeadersListView) view.findViewById(R.id.image_list_view);
        mStickyList.setOnItemClickListener(this);
        mAdapter = new ImageListAdapter(this.getActivity());
        mStickyList.setAdapter(mAdapter);
        mStickyList.setDrawingListUnderStickyHeader(false);

        IntentFilter filter = new IntentFilter(EvernoteMars.END_NOTE_LOADING);
        filter.addAction(MarsImagesApp.MISSION_CHANGED);
        filter.addAction(MarsImagesApp.IMAGE_SELECTED);
        filter.addAction(MarsImagesApp.NOTES_CLEARED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);

        return view;
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MarsImagesApp.MISSION_CHANGED) ||
                    intent.getAction().equals(MarsImagesApp.NOTES_CLEARED)) {
                mAdapter.setCount(0);
                mAdapter.notifyDataSetChanged();
            } else if (intent.getAction().equals(EvernoteMars.END_NOTE_LOADING)) {
                mAdapter.setCount(EVERNOTE.getNotesCount());
                mAdapter.notifyDataSetChanged();
            } else if (intent.getAction().equals(MarsImagesApp.IMAGE_SELECTED)) {
                String selectionSource = intent.getStringExtra(MarsImagesApp.SELECTION_SOURCE);
                if (!selectionSource.equals(MarsImagesApp.LIST_SOURCE)) {
                    int i = intent.getIntExtra(MarsImagesApp.IMAGE_INDEX, 0);
                    mAdapter.setSelectedPosition(i);
                    scrollToShowItem(i);
                }
            }
        }
    };

    private void scrollToShowItem(int position) {
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long id) {
        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mAdapter.setSelectedPosition(i);
        mCallbacks.onItemSelected(i);
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
            View view;
            if (recycledView == null) {
                holder = new ViewHolder();
                view = inflater.inflate(R.layout.row, viewGroup, false);
                holder.row = view.findViewById(R.id.list_row);
                holder.text = (TextView) view.findViewById(R.id.row_title);
                holder.detail = (TextView) view.findViewById(R.id.row_detail);
                holder.imageView = (ImageView) view.findViewById(R.id.row_icon);
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
            String thumbnailURL = EVERNOTE.getThumbnailURL(i);
            if (thumbnailURL != null) {
                ImageLoader.getInstance().displayImage(thumbnailURL, holder.imageView);
            }

            if (i == EVERNOTE.getNotesCount() - 1)
                EVERNOTE.loadMoreImages(getActivity());

            return view;
        }

        public void setCount(int count) {
            mItemCount = count;
            if (count == 0) {
                setSelectedPosition(0);
            }
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

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(int imageIndex);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(int imageIndex) {
        }
    };

}
