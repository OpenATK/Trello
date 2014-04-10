package com.openatk.trello;

import java.util.List;

import com.openatk.trello.internet.TrelloOrganization;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class OrganizationArrayAdapter extends ArrayAdapter<TrelloOrganization> {
	private final Context context;
	private List<TrelloOrganization> organizations = null;
	private int resId;

	public OrganizationArrayAdapter(Context context, int layoutResourceId, List<TrelloOrganization> data) {
		super(context, layoutResourceId, data);
		this.resId = layoutResourceId;
		this.context = context;
		this.organizations = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View row = convertView;
		OrganizationHolder holder = null;
		
		
		if(row == null){
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(resId, parent, false);
			holder = new OrganizationHolder();
			holder.txtTitle = (TextView) row.findViewById(R.id.organizationTitle);
			holder.imgIcon = (ImageView) row.findViewById(R.id.organizationIcon);
			row.setTag(holder);
		} else {
			holder = (OrganizationHolder) row.getTag();
		}
		
		holder.txtTitle.setText(organizations.get(position).getDisplayName());
		if(organizations.get(position).getIcon() != null){
			holder.imgIcon.setImageBitmap(organizations.get(position).getIcon());
		}
				//ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
		// Change the icon for Windows and iPhone
//		String s = values[position];
//		if (s.startsWith("iPhone")) {
//			imageView.setImageResource(R.drawable.no);
//		} else {
//			imageView.setImageResource(R.drawable.ok);
//		}
		return row;
	}
	
	static class OrganizationHolder
    {
        ImageView imgIcon;
        TextView txtTitle;
    }
}
