package pk.scanlan.activity;

import 	android.text.TextUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import pk.scanlan.core.System;
import pk.scanlan.discovery.Discovery;
import pk.scanlan.discovery.R;
import pk.scanlan.discovery.R.id;
import pk.scanlan.discovery.R.layout;
import pk.scanlan.discovery.R.menu;
import pk.scanlan.tools.Host;
import pk.scanlan.tools.ManagedReceiver;
import pk.scanlan.tools.WakeOnLan;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;

public class MainActivity  extends ListActivity {
	protected static final String TAG = null;
	TextView text;
	private HostAdapter  	 mTargetAdapter   	     = null;
	private Discovery mNetworkDiscovery		 = null;
	private HostReceiver mHostReceiver 		 = null;
	private HostAdapter mHostAdapter = null;
	private Toast 			 mToast 			 	 = null;
	private long  			 mLastBackPressTime 	 = 0;
	private String PREF_HOST = "Hosts"; 

	private class HostAdapter extends ArrayAdapter<Host> 
	{		
		class HostHolder
	    {
	        ImageView  itemImage;
	        TextView   itemTitle;
	        TextView   itemDescription;
	    }
		
		public HostAdapter(  ) {		
	        super( MainActivity.this, R.layout.host_list_item);	        
	    }

		@Override
		public int getCount(){
			if (System.getHosts() != null)
						  return System.getHosts().size();
						  return 0;
		}
		
		@Override
	    public View getView( int position, View convertView, ViewGroup parent ) {		
	        View 		 row    = convertView;
	        HostHolder holder = null;
	        
	        if( row == null )
	        {
	            LayoutInflater inflater = ( LayoutInflater )MainActivity.this.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
	            row = inflater.inflate( R.layout.host_list_item , parent, false );
	            
	            holder = new HostHolder();
	            
	            holder.itemImage  	   = ( ImageView )row.findViewById( R.id.itemIcon );
	            holder.itemTitle  	   = ( TextView )row.findViewById( R.id.itemTitle );
	            holder.itemDescription = ( TextView )row.findViewById( R.id.itemDescription );

	            row.setTag(holder);
	        }
	        else
	        {
	            holder = ( HostHolder )row.getTag();
	        }
	        
	        Host host = System.getHost( position );
	        
	        if( host.hasAlias() == true )
	        {
	        	holder.itemTitle.setText
	        	(
	    			Html.fromHtml
		        	(
		        	  "<b>" + host.getAlias() + "</b> <small>( " + host.getAddress() +" )</small>"
		  			)	
	        	);
	        }
	        else
	        	holder.itemTitle.setText( host.getAlias() );
	        
        	holder.itemTitle.setTypeface( null, Typeface.NORMAL );
      //TODO  	holder.itemImage.setImageResource( host.getDrawableResourceId() );
        	holder.itemDescription.setText( host.getDescription() );
        		       	       	        
	        return row;
	    }
	}

	
	private class HostReceiver extends ManagedReceiver
	{
		private static final String TAG = "HostReceiver";
		private IntentFilter mFilter = null;
		
		public HostReceiver() {
			mFilter = new IntentFilter();
			
			mFilter.addAction( Discovery.NEW_HOST );
			mFilter.addAction( Discovery.HOST_UPDATE );
		}
		
		public IntentFilter getFilter( ) {
			return mFilter;
		}
		
		@Override
		public void onReceive( Context context, Intent intent ) 
		{
			if( intent.getAction().equals( Discovery.NEW_HOST ) )
			{
				String address  = ( String )intent.getExtras().get( Discovery.HOST_ADDRESS ),
					   hardware = ( String )intent.getExtras().get( Discovery.HOST_HARDWARE ),
					   name		= ( String )intent.getExtras().get( Discovery.HOST_NAME );
				final  Host host = new Host( address );
				Log.d(TAG,"odebran "+address+" "+hardware+" "+name );
				
				if( host != null)
				{
					if( name != null && name.isEmpty() == false )
						host.setAlias( name );
					
					host.setHardwerAddres( hardware );
																												
					// refresh the target listview
	            	MainActivity.this.runOnUiThread(new Runnable() {
	                    @Override
	                    public void run() {
	                    	if( System.addOrderedHost( host ) == true )
							{
	                    		mHostAdapter.notifyDataSetChanged();
							}
	                    }
	                });		
				}
			}	
			else if( intent.getAction().equals(Discovery.HOST_UPDATE ) )
			{
				// refresh the target listview
            	MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    	mHostAdapter.notifyDataSetChanged();
                    }
                });		
			}					
		}
	}
	

	public void createOnlineLayout( ) {
		mHostAdapter = new HostAdapter( );
		
		setListAdapter( mTargetAdapter );	
	
		getListView().setOnItemLongClickListener( new OnItemLongClickListener() 
		{
			@Override
			public boolean onItemLongClick( AdapterView<?> parent, View view, int position, long id ) {									
				
														
				return false;
			}
		});

		if( mHostReceiver == null )		
			mHostReceiver = new HostReceiver();
		
		
	
	    mHostReceiver.unregister();
	   
	    
	    mHostReceiver.register( MainActivity.this );		
 
        mHostAdapter = new HostAdapter();
		setListAdapter(mHostAdapter);
startNetworkDiscovery(false);
	
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		

		 ConnectivityManager manager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		    NetworkInfo netInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		    if(!netInfo.isConnected())
		    {
		    	
		    	
		    	final WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		        final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
		        if (connectionInfo == null || TextUtils.isEmpty(connectionInfo.getSSID())) {
		         
		    	Log.d("MainActivity", "No wifi connection");
		    	 AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);                 
		         alert.setTitle("No Data Connection!");  
		         alert.setMessage("You have no data connection.");   
		             alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

		                 public void onClick(DialogInterface dialog, int whichButton) {
		                     // TODO Auto-generated method stub
		                     final Intent intent = new Intent(Intent.ACTION_MAIN, null);
		                     intent.addCategory(Intent.CATEGORY_LAUNCHER);
		                     final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.wifi.WifiSettings");
		                     intent.setComponent(cn);
		                     intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		                     startActivity( intent);
		                 }

		             });
		             alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

		                 public void onClick(DialogInterface dialog, int which) {
		                     // TODO Auto-generated method stub
		                     return;   
		                 }
		             });
		             alert.show();
		        }
		    	
		    }
		
		// we are online, and the system was already initialized
    	if( mHostAdapter != null )
    		createOnlineLayout( );
    	
    	// initialize the ui for the first time
    	else if( mHostAdapter == null )
        {	
    		
    		MainActivity.this.runOnUiThread( new Runnable(){			    			
				@Override
				public void run() 
				{
					
					
				    try
			    	{	    							        	
				    	createOnlineLayout( );						    							    	
			    	}
			    	catch( Exception e )
			    	{
			    		Log.e(TAG,"Wyjatek "+e);    		
			    	}									
				}
			});		
				
			
        }
		
    	  // Restore preferences
      
       
		
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	 menu.add(1, 1, 0, "Ping");
         menu.add(1, 2, 1, "Tracer");
         menu.add(1, 3, 2, "Wake");
     return true;
    }
	 static final int CUSTOM_DIALOG_ID = 0;
	    static final int CUSTOM_DIALOG_ID1 = 1;
	    static final int CUSTOM_DIALOG_ID2 = 2;
	    EditText customDialog_EditText;
	    Button customDialog_Update, customDialog_Dismiss;
	    TextView customDialog_TextView;
	    EditText customDialog_EditText1;
	    Button customDialog_Update1;
	    TextView customDialog_TextView1;
	    EditText customDialog_EditText2;
	    Button customDialog_Update2;
	    TextView customDialog_TextView2;
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch(item.getItemId())
        {
        case 1:
            showDialog(CUSTOM_DIALOG_ID);
             
         return true;
        case 2:
            showDialog(CUSTOM_DIALOG_ID1);
           
         return true;
        case 3:
            showDialog(CUSTOM_DIALOG_ID2);
           
         return true;

        }
        return super.onOptionsItemSelected(item);  

    }
    private Button.OnClickListener customDialog_UpdateOnClickListener
    = new Button.OnClickListener(){
   
            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
                 
                customDialog_TextView.setText(customDialog_EditText.getText().toString());
                dismissDialog(CUSTOM_DIALOG_ID);
                 
            }
      
    };
    private Button.OnClickListener customDialog_UpdateOnClickListener1
    = new Button.OnClickListener(){
   
            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
                 
            	  customDialog_TextView1.setText("Send");
                  WakeOnLan wake = new WakeOnLan(customDialog_EditText1.getText().toString());
          		Toast.makeText( MainActivity.this, "Send to "+customDialog_EditText1.getText().toString(), Toast.LENGTH_SHORT ).show();	                	

                   wake.start();;
                 
            }
      
    };
    private Button.OnClickListener customDialog_UpdateOnClickListener2
    = new Button.OnClickListener(){
   
            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
                 
             
                
                 
            }
      
    };
    private Button.OnClickListener customDialog_DismissOnClickListener
    = new Button.OnClickListener(){
   
  @Override
  public void onClick(View arg0) {
   // TODO Auto-generated method stub
   dismissDialog(CUSTOM_DIALOG_ID);
   dismissDialog(CUSTOM_DIALOG_ID2);
   dismissDialog(CUSTOM_DIALOG_ID1);
  }
      
    };   
    @Override
    protected Dialog onCreateDialog(int id) {
     // TODO Auto-generated method stub
     Dialog dialog = null;;
        switch(id) {
        case CUSTOM_DIALOG_ID:
         dialog = new Dialog(MainActivity.this);
      
         dialog.setContentView(R.layout.preferences);
          
          
         customDialog_EditText = (EditText)dialog.findViewById(R.id.dialogedittext);
         customDialog_Update = (Button)dialog.findViewById(R.id.dialogupdate);
         customDialog_Dismiss = (Button)dialog.findViewById(R.id.dialogdismiss);
         customDialog_TextView = (TextView)dialog.findViewById(R.id.dialogtextview);
         customDialog_Update.setOnClickListener(customDialog_UpdateOnClickListener);
         customDialog_Dismiss.setOnClickListener(customDialog_DismissOnClickListener);
                  
          
            break;
        case CUSTOM_DIALOG_ID1:
            dialog = new Dialog(MainActivity.this);
         
            dialog.setContentView(R.layout.preferences);
             
             
            customDialog_EditText1 = (EditText)dialog.findViewById(R.id.dialogedittext);
            customDialog_Update1 = (Button)dialog.findViewById(R.id.dialogupdate);
            customDialog_Dismiss = (Button)dialog.findViewById(R.id.dialogdismiss);
            customDialog_TextView1 = (TextView)dialog.findViewById(R.id.dialogtextview);
            customDialog_Update1.setOnClickListener(customDialog_UpdateOnClickListener1);
            customDialog_Dismiss.setOnClickListener(customDialog_DismissOnClickListener);
                     
             
               break;
        case CUSTOM_DIALOG_ID2:
            dialog = new Dialog(MainActivity.this);
         
            dialog.setContentView(R.layout.preferences);
             
             
            customDialog_EditText2 = (EditText)dialog.findViewById(R.id.dialogedittext);
            customDialog_Update2 = (Button)dialog.findViewById(R.id.dialogupdate);
            customDialog_Dismiss = (Button)dialog.findViewById(R.id.dialogdismiss);
            customDialog_TextView2 = (TextView)dialog.findViewById(R.id.dialogtextview);
            customDialog_Update2.setOnClickListener(customDialog_UpdateOnClickListener2);
            customDialog_Dismiss.setOnClickListener(customDialog_DismissOnClickListener);
                     
             
               break;
        }
        return dialog;
    }
	
	public void startNetworkDiscovery( boolean silent ) {
		stopNetworkDiscovery( silent );
		
		try {
			mNetworkDiscovery = new Discovery( this );
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	mNetworkDiscovery.start();
	
		
		if( silent == false )
			Toast.makeText( this, "Network discovery started.", Toast.LENGTH_SHORT ).show();
	}
	public void stopNetworkDiscovery( boolean silent ) {
		if( mNetworkDiscovery != null )
		{
			if( mNetworkDiscovery.isRunning() )
			{
				mNetworkDiscovery.exit();
				try
				{
					mNetworkDiscovery.join();
				}
				catch( Exception e )
				{
					// swallow
				}
				
				if( silent == false )
					Toast.makeText( this, "Network discovery stopped.", Toast.LENGTH_SHORT ).show();
			}
			
			mNetworkDiscovery = null;
		}
	}
	

	
	@Override
	public void onBackPressed() {
		if( mLastBackPressTime < java.lang.System.currentTimeMillis() - 4000 ) 
		{
			mToast = Toast.makeText( this, "Press back again to close this app.", Toast.LENGTH_SHORT );
			mToast.show();
			mLastBackPressTime = java.lang.System.currentTimeMillis();
		} 
		else
		{
			if( mToast != null ) 
				mToast.cancel();
		

					MainActivity.this.finish();
			
			mLastBackPressTime = 0;
		}
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		stopNetworkDiscovery( true );
					
		if( mHostReceiver != null )
			mHostReceiver.unregister();
		
		
				
		// make sure no zombie process is running before destroying the activity
		System.clean(  );		
						
				
		
		// remove the application from the cache
		java.lang.System.exit( 0 );
	}
	@Override
	protected void onListItemClick( ListView l, View v, int position, long id ){
		super.onListItemClick( l, v, position, id);

		stopNetworkDiscovery( true );		
		System.setCurrentHost( position );
		
		Toast.makeText( MainActivity.this, "Selected " + System.getCurrentHost().getAlias(), Toast.LENGTH_SHORT ).show();	                	
        startActivity
        ( 
          new Intent
          ( 
            MainActivity.this, 
            ItemActivity.class
          ) 
        );
        //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);                
	}
	
}
