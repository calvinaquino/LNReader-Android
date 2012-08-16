package com.erakk.lnreader.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import android.widget.ExpandableListView;

import com.erakk.lnreader.R;
import com.erakk.lnreader.classes.*;
import com.erakk.lnreader.adapter.ExpandListAdapter;
 
public class ExpandListAdapter extends BaseExpandableListAdapter {
 
    private Context context;
    private ArrayList<ExpandListGroup> groups;
    public ExpandListAdapter(Context context, ArrayList<ExpandListGroup> groups) {
        this.context = context;
        this.groups = groups;
    }
     
    public void addItem(ExpandListChild item, ExpandListGroup group) {
        if (!groups.contains(group)) {
            groups.add(group);
        }
        int index = groups.indexOf(group);
        ArrayList<ExpandListChild> ch = groups.get(index).getItems();
        ch.add(item);
        groups.get(index).setItems(ch);
    }
    public Object getChild(int groupPosition, int childPosition) {
        // TODO Auto-generated method stub
        ArrayList<ExpandListChild> chList = groups.get(groupPosition).getItems();
        return chList.get(childPosition);
    }
 
    public long getChildId(int groupPosition, int childPosition) {
        // TODO Auto-generated method stub
        return childPosition;
    }
 
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view,
            ViewGroup parent) {
        ExpandListChild child = (ExpandListChild) getChild(groupPosition, childPosition);
        if (view == null) {
            LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
            view = infalInflater.inflate(R.layout.expandchapter_list_item, null);

        }
        TextView tv = (TextView) view.findViewById(R.id.novel_chapter);
        tv.setText(child.getName().toString());
        tv.setTag(child.getTag());
        // TODO Auto-generated method stub
        return view;
    }
 
    public int getChildrenCount(int groupPosition) {
        // TODO Auto-generated method stub
        ArrayList<ExpandListChild> chList = groups.get(groupPosition).getItems();
 
        return chList.size();
 
    }
 
    public Object getGroup(int groupPosition) {
        // TODO Auto-generated method stub
        return groups.get(groupPosition);
    }
 
    public int getGroupCount() {
        // TODO Auto-generated method stub
        return groups.size();
    }
 
    public long getGroupId(int groupPosition) {
        // TODO Auto-generated method stub
        return groupPosition;
    }
 
    public View getGroupView(int groupPosition, boolean isLastChild, View view,
            ViewGroup parent) {
        ExpandListGroup group = (ExpandListGroup) getGroup(groupPosition);
        if (view == null) {
            LayoutInflater inf = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
            view = inf.inflate(R.layout.expandvolume_list_item, null);
        }
        TextView tv = (TextView) view.findViewById(R.id.novel_volume);
        tv.setText(group.getName());
        // TODO Auto-generated method stub
        return view;
    }
 
    public boolean hasStableIds() {
        // TODO Auto-generated method stub
        return true;
    }
 
    public boolean isChildSelectable(int arg0, int arg1) {
        // TODO Auto-generated method stub
    return true;
    }
 
}
