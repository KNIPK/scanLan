package pk.scanlan.discovery;




import pk.scanlan.core.System;
import android.app.Application;
import android.content.Context;
import android.util.Log;


public class ScanLanApplication extends Application 
{
	
	
	
	@Override
	public void onCreate() {		
		super.onCreate();
		
		
		
		try
		{
			
			System.init( this );
			Log.d("s","iniy");
		}
		catch( Exception e )
		{
			Log.e( "SCANT", "Poczatek "+e );
		}		
		

        		
	}
}
