package pk.scanlan.core;





import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.UnknownHostException;

import pk.scanlan.tools.Host;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class Network
{

	private final String TAG = "Network";
    private static final int BUF = 8 * 1024;
    private static final String CMD_IP = " -f inet addr show %s";
    private static final String PTN_IP1 = "\\s*inet [0-9\\.]+\\/([0-9]+) brd [0-9\\.]+ scope global %s$";
    private static final String PTN_IP2 = "\\s*inet [0-9\\.]+ peer [0-9\\.]+\\/([0-9]+) scope global %s$"; // FIXME: Merge with PTN_IP1
    private static final String PTN_IF = "^%s: ip [0-9\\.]+ mask ([0-9\\.]+) flags.*";
    private static final String NOIF = "0";
    public static final String NOIP = "0.0.0.0";
    public static final String NOMASK = "255.255.255.255";
    public static final String NOMAC = "00:00:00:00:00:00";
    public String mSsid = null;
    public String mBssid = null;
    public String mCarrier = null;

    private String mMacAddress;
	private IP4Address			mGateway			 = null;
	private IP4Address			mNetmask			 = null;
	private IP4Address			mLocal				 = null;
	private IP4Address			mBase				 = null;
	private NetworkInterface    mInterface			 = null;
    DhcpInfo mDHCPinfo;
    WifiInfo mWifiInfo;
    WifiManager mManager;
    private ConnectivityManager mConnectivityManager = null;
    public String intToIp(int i) {

 	   return ( i & 0xFF)  + "." +
 	              
 	               ((i >> 8 ) & 0xFF) + "." +
 	               ((i >> 16 ) & 0xFF) + "." +
 	                  ((i >> 24 ) & 0xFF ) ;
 	}
    public Network(Context context) throws Exception 
    {
    
    	mManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    	mConnectivityManager = ( ConnectivityManager )context.getSystemService( Context.CONNECTIVITY_SERVICE );
    	mDHCPinfo = mManager.getDhcpInfo();
    	mWifiInfo = mManager.getConnectionInfo();
    	mGateway = new IP4Address(mDHCPinfo.gateway);
       	mSsid = mWifiInfo.getSSID();
       	mBssid = mWifiInfo.getBSSID();
       	mCarrier = mWifiInfo.getBSSID();
       	mBase				 = new IP4Address( mDHCPinfo.netmask & mDHCPinfo.gateway );
       	mMacAddress = mWifiInfo.getMacAddress();
       	mNetmask = new IP4Address(mDHCPinfo.netmask);
       	mLocal = new IP4Address(mWifiInfo.getIpAddress());
       	if( isConnected() == false )
			throw new Exception("Not connected to any WiFi access point.");
		
		else
		{
			try
			{
				mInterface = NetworkInterface.getByInetAddress( getLocalAddress() );			
			}
			catch( SocketException e )
			{
				Log.e( TAG, "Wyj 1 "+e );				
				/*
				 * Issue #26: Initialization error in ColdFusionX ROM
				 * 
				 * It seems it's a ROM issue which doesn't correctly populate device descriptors.
				 * This rom maps the default wifi interface to a generic usb device 
				 * ( maybe it's missing the specific interface driver ), which is obviously not, and
				 * it all goes shit, use an alternative method to obtain the interface object.
				 */				
				try {
					mInterface = NetworkInterface.getByName( java.lang.System.getProperty( "wifi.interface", "wlan0" ) );
				} catch (SocketException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();

					Log.e( TAG, "Wyj 2 "+e );				
				}
				
				if( mInterface == null )
					try {
						throw new SocketException();
					} catch (SocketException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();

						Log.e( TAG, "Wyj 3 "+e );				
					}
			}
		}
    	
    }
    public boolean isConnected(){
		return mConnectivityManager.getNetworkInfo( ConnectivityManager.TYPE_WIFI ).isConnected();
	}
	
    public InetAddress getGatewayAddress()
    {
    	
    	return mGateway.toInetAddress();
    }
    public String getLocalAddressAsString( ){
		return mLocal.toString();
	}
	
	public InetAddress getLocalAddress( ){
		return mLocal.toInetAddress();
	}
	
    
    public int getNumberOfAddresses( ) {
		
    	return IP4Address.ntohl( ~mNetmask.toInteger() );
	}
    public IP4Address getStartAddress( ) {
		return mBase;
    }
    public NetworkInterface getInterface(){
		return mInterface;
	}
    public byte[] getGatewayHardware(){
		return Host.parseMacAddress( mWifiInfo.getBSSID() );
	}
    public String getGatewayHardwareAsString(){
		return  mWifiInfo.getBSSID() ;
	}
    public byte[] getLocalHardwere()
    {
    	try {
			return mInterface.getHardwareAddress();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG,"Wyj 154");
		}
    	return null;
    }
    public String getLocalHardwereAsString()
    {
    	byte[] mac=null;
		try {
			mac = mInterface.getHardwareAddress();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG,"Wyj 166");
		}
    	return String.format( "%02X%02X%02X", mac[0], mac[1], mac[2] );
    	
    }

    public String getSSID( ) {
		return mWifiInfo.getSSID();
	}
}