package gov.nasa.jpl.hi.marsimages.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.evernote.edam.type.Note;
import com.powellware.marsimages.R;

import java.util.List;

import gov.nasa.jpl.hi.marsimages.EvernoteMars;
import gov.nasa.jpl.hi.marsimages.MarsImagesApp;

/**
 * Created by mpowell on 5/27/14.
 */
public class ImageListFragment extends ListFragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list, null);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ImageListAdapter adapter = new ImageListAdapter(getActivity());
        for (int i = 0; i < EvernoteMars.EVERNOTE.getNotesCount(); i++) {
            adapter.add(new ImageItem(i));
        }
        setListAdapter(adapter);
    }

    private class ImageItem {
        private int index;
        public ImageItem(int index) {
            this.index = index;
        }
        public Note getNote() { return EvernoteMars.EVERNOTE.getNote(index); }
    }

    public class ImageListAdapter extends ArrayAdapter<ImageItem> {

        public ImageListAdapter(Context context) {
            super(context, 0);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.row, null);
            }
//            ImageView icon = (ImageView) convertView.findViewById(R.id.row_icon);
//            icon.setImageResource(getItem(position).iconRes);

            TextView title = (TextView) convertView.findViewById(R.id.row_title);
            title.setText(getItem(position).getNote().getTitle());

            return convertView;
        }
    }
}
