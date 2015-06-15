/**
 * adapted from http://www.ulduzsoft.com/2012/01/fontpreference-dialog-for-android/
 */

package com.erakk.lnreader.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

import java.util.ArrayList;
import java.util.List;

public class SimpleFontPreference extends DialogPreference implements DialogInterface.OnClickListener
{
	// Keeps the font file paths and names in separate arrays
	private List< String >    m_fontType;

	// Font adaptor responsible for redrawing the item TextView with the appropriate font.
	// We use BaseAdapter since we need both arrays, and the effort is quite small.
	public class FontAdapter extends BaseAdapter
	{
		@Override
		public int getCount()
		{
			return m_fontType.size();
		}

		@Override
		public Object getItem(int position)
		{
			return m_fontType.get( position );
		}

		@Override
		public long getItemId(int position)
		{
			// We use the position as ID
			return position;
		}

		@Override
		public View getView( int position, View convertView, ViewGroup parent )
		{
			View view = convertView;

			// This function may be called in two cases: a new view needs to be created,
			// or an existing view needs to be reused
			if ( view == null )
			{
				// Since we're using the system list for the layout, use the system inflater
				final LayoutInflater inflater = (LayoutInflater)
						getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );

				// And inflate the view android.R.layout.select_dialog_singlechoice
				// Why? See com.android.internal.app.AlertController method createListView()
				view = inflater.inflate( android.R.layout.select_dialog_singlechoice, parent, false);
			}

			if ( view != null )
			{
				// Find the text view from our interface
				CheckedTextView tv = (CheckedTextView) view.findViewById( android.R.id.text1 );
				String face = m_fontType.get( position );
				// Replace the string with the current font name using our typeface
				if(face.equals("sans-serif")) {
					tv.setTypeface( Typeface.SANS_SERIF );
				}
				else if (face.equals("serif")) {
					tv.setTypeface( Typeface.SERIF );
				}
				else {
					tv.setTypeface( Typeface.MONOSPACE );
				}

				tv.setText(face);
			}

			return view;
		}
	}

	public SimpleFontPreference( Context context, AttributeSet attrs )
	{
		super(context, attrs);

	}

	@Override
	protected void onPrepareDialogBuilder( AlertDialog.Builder builder )
	{
		super.onPrepareDialogBuilder(builder);

		// Get the fonts on the device

		m_fontType = new ArrayList< String >();
		m_fontType.add("sans-serif");
		m_fontType.add("serif");
		m_fontType.add("monospace");

		// Get the current value to find the checked item
		String selectedFontface = getSharedPreferences().getString( getKey(), "");
		int idx = 0, checked_item = 0;

		for ( String typeface : m_fontType )
		{
			if ( typeface.equals( selectedFontface ) )
				checked_item = idx;
			idx++;
		}

		// Create out adapter
		// If you're building for API 11 and up, you can pass builder.getContext
		// instead of current context
		FontAdapter adapter = new FontAdapter();

		builder.setSingleChoiceItems( adapter, checked_item, this );

		// The typical interaction for list-based dialogs is to have click-on-an-item dismiss the dialog
		builder.setPositiveButton(null, null);
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if ( which >=0 && which < m_fontType.size() )
		{
			String selectedFontPath = m_fontType.get( which );
			this.callChangeListener(selectedFontPath);
			Editor editor = getSharedPreferences().edit();
			editor.putString( getKey(), selectedFontPath.toString() );
			editor.commit();

			dialog.dismiss();
		}
	}
}