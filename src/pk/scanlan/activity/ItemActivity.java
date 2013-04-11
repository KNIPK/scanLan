package pk.scanlan.activity; 
  
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import pk.scanlan.core.System;
import pk.scanlan.discovery.R; 
import pk.scanlan.discovery.R.layout; 
import pk.scanlan.discovery.R.menu; 
import android.os.Bundle; 
import android.app.Activity; 
import android.content.Intent; 
import android.text.Editable;
import android.util.Log;
import android.view.Menu; 
import android.view.View; 
import android.widget.Button; 
import android.widget.TextView; 
import android.widget.Toast;
  
public class ItemActivity extends Activity { 
  
    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.activity_item); 
          
        //Creating Button variable 
        Button buttonTrace = (Button) findViewById(R.id.buttonT);       
        Button buttonPing = (Button) findViewById(R.id.buttonP);  
        Button buttonPortScan = (Button) findViewById(R.id.buttonPS);  
       //Adding Listener to button 
         buttonTrace.setOnClickListener(new View.OnClickListener() { 
             
            @Override
            public void onClick(View v) { 
                // TODO Auto-generated method stub 
                 
                //Creating TextView Variable 
                TextView text = (TextView) findViewById(R.id.textViewItem); 
                 
                //Sets the new text to TextView (runtime click event) 
                text.setText("You Have click the buttonTrace"); 
            } 
         }); 
           
         buttonPing.setOnClickListener(new View.OnClickListener() { 
               
             @Override
             public void onClick(View v) { 
                 // TODO Auto-generated method stub 
                  
                 //Creating TextView Variable 
                 TextView text = (TextView) findViewById(R.id.textViewItem); 
                 
                             			        
             			        try {
             			            String pingCmd = "ping -c 2 " + System.getCurrentHost().getAddressAsString();
             			            String pingResult = "";
             			            Runtime r = Runtime.getRuntime();
             			            Process p = r.exec(pingCmd);
             			            BufferedReader in = new BufferedReader(new
             			            InputStreamReader(p.getInputStream()));
             			            String inputLine;
             			           double time = 0;
             			           int linesCount = 0;
             			            while ((inputLine = in.readLine()) != null) {
             			            
             			            	//text.setText(inputLine + "\n\n");
             			            	pingResult += inputLine;
             			            	if(linesCount == 1 || linesCount == 2)
             			            	{
             			            	int start= inputLine.indexOf("time=");
             			            int end =inputLine.indexOf(" ms");
             			            	time = time + Double.parseDouble((inputLine.substring(start+5, end)));
             			            	}
             			          linesCount++;
             			            }
             			            in.close();
             			           text.setText("Czas ping: " +time/2);

             			            }//try
             			            catch (IOException e) {
             			           Log.e("Ping", "IO wyjatek");
             			            
             			            }
             			      
                     		}
             			    
             			
                 
                 
                 
                 
                 
            
          }); 
           
        buttonPortScan.setOnClickListener(new View.OnClickListener() { 
               
             @Override
             public void onClick(View v) { 
                 // TODO Auto-generated method stub 
            	 
         		
         		Toast.makeText( ItemActivity.this, "Selected " + System.getCurrentHost().getAlias(), Toast.LENGTH_SHORT ).show();	     
                  startActivity 
                  (  
                    new Intent 
                    (  
                      ItemActivity.this,  
                      PortScanActivity.class
                    )  
                  ); 
             } 
          }); 
    } 
  
    @Override
    public boolean onCreateOptionsMenu(Menu menu) { 
        // Inflate the menu; this adds items to the action bar if it is present. 
        getMenuInflater().inflate(R.menu.activity_item, menu); 
        return true; 
    } 
    public void onBackPressed() {
	    super.onBackPressed();
	   
	}
      
} 