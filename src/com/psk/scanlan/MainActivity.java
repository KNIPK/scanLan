package com.psk.scanlan;









import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

	private HostsAdapter adapter;
	private Button btn_skanuj;
	private static LayoutInflater mInflater;
	private List<ViewHolder> hosts = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		  btn_skanuj = (Button) findViewById(R.id.btn_skanuj);
	      btn_skanuj.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	               Skanowanie();
	            }
	      });
	      adapter = new HostsAdapter();
	      ListView list = (ListView) findViewById(R.id.output);
	      list.setAdapter(adapter);
	      list.setItemsCanFocus(false);
	     // list.setOnItemClickListener(this);
	      list.setEmptyView(findViewById(R.id.list_empty));
	}
	private void Skanowanie()
	{
		//Tutaj klasy do skanowania
		
		
		btn_skanuj.setText(R.string.btn_anuluj);
       // setButton(btn_discover, R.drawable.cancel, false);
        btn_skanuj.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                anulujSkanowanie();
            }
        });
        
        setProgressBarVisibility(true);
        setProgressBarIndeterminateVisibility(true);
        initList(); //do inicjowania listy
	}
	private void anulujSkanowanie()
	{
		//do anulowania skanowania
	}
	private void initList()
	{
		adapter.clear();
        hosts = new ArrayList<ViewHolder>();
	}
	  static class ViewHolder {
	        TextView host;
	        TextView mac;
	        TextView vendor;
	        ImageView logo;
	    }
	private class HostsAdapter extends ArrayAdapter<Void> {
        public HostsAdapter() {
            super(MainActivity.this,R.layout.lista_host, R.id.list);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.lista_host, null);
                holder = new ViewHolder();
                
                holder.logo.setImageResource(R.drawable.router);
                holder.host.setText("lolol222olol");
                holder.mac.setText("maclol222lolo");
                
                /*
                holder.host = (TextView) convertView.findViewById(R.id.list);
                holder.mac = (TextView) convertView.findViewById(R.id.mac);
                holder.vendor = (TextView) convertView.findViewById(R.id.vendor);
                holder.logo = (ImageView) convertView.findViewById(R.id.logo);
                */
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.logo.setImageResource(R.drawable.router);
            holder.host.setText("lolololol");
            holder.mac.setText("maclolololo");
           /*
            final HostBean host = hosts.get(position);
            if (host.deviceType == HostBean.TYPE_GATEWAY) {
                holder.logo.setImageResource(R.drawable.router);
            } else if (host.isAlive == 1 || !host.hardwareAddress.equals(NetInfo.NOMAC)) {
                holder.logo.setImageResource(R.drawable.computer);
            } else {
                holder.logo.setImageResource(R.drawable.computer_down);
            }
            if (host.hostname != null && !host.hostname.equals(host.ipAddress)) {
                holder.host.setText(host.hostname + " (" + host.ipAddress + ")");
            } else {
                holder.host.setText(host.ipAddress);
            }
            if (!host.hardwareAddress.equals(NetInfo.NOMAC)) {
                holder.mac.setText(host.hardwareAddress);
                if(host.nicVendor != null){
                    holder.vendor.setText(host.nicVendor);
                } else {
                    holder.vendor.setText(R.string.info_unknown);
                }
                holder.mac.setVisibility(View.VISIBLE);
                holder.vendor.setVisibility(View.VISIBLE);
            } else {
                holder.mac.setVisibility(View.GONE);
                holder.vendor.setVisibility(View.GONE);
            }*/
            return convertView;
        }
    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
		
		
	}

}
