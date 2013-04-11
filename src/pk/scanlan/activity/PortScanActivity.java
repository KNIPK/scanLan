package pk.scanlan.activity;

import java.net.UnknownHostException;
import java.util.ArrayList;

import pk.scanlan.core.System;
import pk.scanlan.discovery.PortScan;
import pk.scanlan.discovery.R;
import pk.scanlan.discovery.R.id;
import pk.scanlan.discovery.R.layout;
import pk.scanlan.discovery.R.menu;
import pk.scanlan.tools.Receiver;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PortScanActivity extends Activity {

	private ArrayAdapter<String> mPortAdapter = null;
	private  PortReceiver mPortReceiver = null;
	private ListView mScanList = null;
	private PortScan mPortScanner = null;
	private ArrayList<String>     mPortList    	    = null;
	private Button mbtnAnuluj = null;
	private class PortAdapter extends ArrayAdapter<String> 
	{		
		class PortHolder
	    {
			 TextView port;
		        TextView service;
		        Button connectButton;
	    }
		
		public PortAdapter(  ) {		
	        super( PortScanActivity.this, R.layout.port_list_item);	        
	    }

		@Override
		public int getCount(){
			return System.getHosts().size();
		}
		
		@Override
	    public View getView( int position, View convertView, ViewGroup parent ) {		
	        View 		 row    = convertView;
	      PortHolder holder = null;
	        
	        if( row == null )
	        {
	            LayoutInflater inflater = ( LayoutInflater )PortScanActivity.this.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
	            row = inflater.inflate( R.layout.host_list_item , parent, false );
	            
	            holder = new PortHolder();
	            
	            holder.port 	   = ( TextView )row.findViewById( R.id.port );
	            holder.service  	   = ( TextView )row.findViewById( R.id.service );
	            holder.connectButton = ( Button )row.findViewById( R.id.connect );

	            row.setTag(holder);
	        }
	        else
	        {
	            holder = ( PortHolder )row.getTag();
	        }
	        
	  
	        
	      
	        
        
        		       	       	        
	        return row;
	    }
	}

	
	private class PortReceiver implements Receiver
	{
		private static final String TAG = "PortReceiver";
		

		@Override
		public void newItem(final String s) {
			// TODO Auto-generated method stub
			
			PortScanActivity.this.runOnUiThread( new Runnable(){			    			
				@Override
				public void run() 
				{
					
					
				    try
			    	{	    		
				    	if(!mPortList.contains(s))
				    	{
				    	mPortList.add(s);
						mPortAdapter.notifyDataSetChanged();
						
						Log.d(TAG,"nowy port "+s);
				    	}
			    	}
			    	catch( Exception e )
			    	{
			    		Log.e(TAG,"Wyjatek "+e);    		
			    	}									
				}
			});		
			
		}
	}
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.port_scan);
		mScanList = ( ListView )findViewById( R.id.portlist );
		mbtnAnuluj = (Button)findViewById(R.id.btnAnuluj);
		mPortList = new ArrayList<String>();
		mPortReceiver = new PortReceiver();
		mPortAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_list_item_1, mPortList );
		mScanList.setAdapter(mPortAdapter);
	
		 mbtnAnuluj.setOnClickListener(new View.OnClickListener() { 
             
             @Override
             public void onClick(View v) { 
            	 startPortScan(false);
             } 
          }); 
		
	
		
	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.port_scan, menu);
		return true;
	}

	public void startPortScan( boolean silent ) {
		stopPortScan( silent );
		
		
			mPortScanner = new PortScan(System.getCurrentHost().getAddress(), mPortReceiver, 0, 1000);
		
			
	
	mPortScanner.start();
		
		if( silent == false )
			Toast.makeText( this, "Port scan started.", Toast.LENGTH_SHORT ).show();
	}
	public void stopPortScan( boolean silent ) {
		if( mPortScanner != null )
		{
			if( mPortScanner.isRunning() )
			{
				mPortScanner.exit();
				try
				{
					mPortScanner.join();
				}
				catch( Exception e )
				{
					// swallow
				}
				
				if( silent == false )
					Toast.makeText( this, "Port scan stopped.", Toast.LENGTH_SHORT ).show();
			}
			
			mPortScanner = null;
		}
	}

	@Override
	public void onBackPressed() {
	    super.onBackPressed();
	   stopPortScan(true);
	}

}
