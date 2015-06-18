package com.erakk.lnreader.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.erakk.lnreader.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class FileListAdapter extends ArrayAdapter<File>  {
	private final ArrayList<File> files;

	public FileListAdapter(Context context, int resourceId, ArrayList<File> items) {
		super(context, resourceId, items);
		this.files = items;
		Collections.sort(this.files, new FileDateComparator());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			// v = vi.inflate(R.layout.item_file, null); // need to pass as null
			v = vi.inflate(R.layout.item_file, parent, false);
		}
		File d = files.get(position);
		if (d != null) {
			TextView name = (TextView) v.findViewById(R.id.filename);
			TextView date = (TextView) v.findViewById(R.id.filedate);

			if (name != null) {
				name.setText(d.getAbsolutePath());
			}

			if(date != null) {
				date.setText(new Date(d.lastModified()).toString());
			}
		}
		return v;
	}

	public class FileDateComparator implements Comparator<File> {
		@Override
		public int compare(File o1, File o2) {
			if(o1.lastModified() == o2.lastModified()) return 0;
			else if(o1.lastModified() < o2.lastModified()) return 1;
			else return -1;
		}
	}

}
