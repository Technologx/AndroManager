package com.technologx.andromanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.technologx.andromanager.R;
import com.technologx.andromanager.activities.MainActivity;
import com.technologx.andromanager.filesystem.BaseFile;
import com.technologx.andromanager.filesystem.HFile;
import com.technologx.andromanager.fragments.Main;
import com.technologx.andromanager.services.DeleteTask;
import com.technologx.andromanager.utils.DataUtils;
import com.technologx.andromanager.utils.Futils;
import com.technologx.andromanager.utils.OpenMode;

import java.io.File;
import java.util.ArrayList;


/**
 * Created by Arpit on 16-11-2014.
 */
public class HiddenAdapter extends ArrayAdapter<HFile> {
    private Futils utils;

    /*Shortcuts s;*/
    Main context;Context c;
    public ArrayList<HFile> items;
    MaterialDialog materialDialog;
    boolean hide;
    ///	public HashMap<Integer, Boolean> myChecked = new HashMap<Integer, Boolean>();

    public HiddenAdapter(Context c,Main context, Futils utils,  int resourceId, ArrayList<HFile> items,MaterialDialog materialDialog,boolean hide) {
        super(c, resourceId, items);
        this.utils = utils;
        this.c=c;
        this.context = context;
        this.items = items;
        this.hide=hide;
        this.materialDialog=materialDialog;
    /*    s = new Shortcuts(c,"shortcut.xml");
    */}


    private class ViewHolder {
        ImageButton image;
        TextView txtTitle;
        TextView txtDesc;
        LinearLayout row;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final HFile f = items.get(position);
        //final Layoutelements rowItem = getItem(position);

        View view;
        final int p = position;
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) c
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.bookmarkrow, null);
            final ViewHolder vholder = new ViewHolder();
            vholder.txtTitle = (TextView) view.findViewById(R.id.text1);
            vholder.image = (ImageButton) view.findViewById(R.id.delete_button);
            vholder.txtDesc = (TextView) view.findViewById(R.id.text2);
            vholder.row=(LinearLayout)view.findViewById(R.id.bookmarkrow);
            view.setTag(vholder);

        } else {
            view = convertView;

        }
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.txtTitle.setText(f.getName());
        String a=f.getReadablePath(f.getPath());
        holder.txtDesc.setText(a);
        if(hide)
            holder.image.setVisibility(View.GONE);
            holder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if(!f.isSmb() && f.isDirectory())
            {
                ArrayList<BaseFile> a=new ArrayList<BaseFile>();
                BaseFile baseFile=new BaseFile(items.get(p).getPath()+"/.nomedia");
                baseFile.setMode(OpenMode.FILE);
                a.add(baseFile);
                new DeleteTask(context.getActivity().getContentResolver(),c).execute((a));
            }
                DataUtils.removeHiddenFile(items.get(p).getPath());
                items.remove(items.get(p));
                notifyDataSetChanged();
            }
        });
        holder.row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                materialDialog.dismiss();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (f.isDirectory()) {
                            context.getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    context.loadlist(f.getPath(), false, OpenMode.UNKNOWN);
                                }
                            });
                        } else {
                            if(!f.isSmb()){
                                context.getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        utils.openFile(new File(f.getPath()), (MainActivity) context.getActivity());
                                    }
                                });
                            }
                        }
                    }
                }).start();
            }
        });
        return view;
    }
    public void updateDialog(MaterialDialog dialog){
        materialDialog=dialog;
    }


}
