package pk.scanlan.tools;

import android.app.Activity;
import android.content.SharedPreferences;

public class SavingData {
	private static final String PREFS_NAME = null;
	private String pref_name = "";
	public SavingData(String name,Activity main)
	{
		  // Restore preferences
	       SharedPreferences settings = main.getSharedPreferences(PREFS_NAME, 0);
	       boolean silent = settings.getBoolean("silentMode", false);
	      
		
	}
	

}