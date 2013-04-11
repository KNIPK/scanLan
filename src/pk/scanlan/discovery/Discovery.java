package pk.scanlan.discovery;







import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pk.scanlan.core.IP4Address;
import pk.scanlan.core.Network;
import pk.scanlan.core.System;
import pk.scanlan.tools.Host;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Discovery extends Thread {
      public final String TAG = "Discovery";

  	private static final String  ARP_TABLE_FILE   = "/proc/net/arp";
  	private static final Pattern ARP_TABLE_PARSER = Pattern.compile( "^([\\d]{1,3}\\.[\\d]{1,3}\\.[\\d]{1,3}\\.[\\d]{1,3})\\s+([0-9-a-fx]+)\\s+([0-9-a-fx]+)\\s+([a-f0-9]{2}:[a-f0-9]{2}:[a-f0-9]{2}:[a-f0-9]{2}:[a-f0-9]{2}:[a-f0-9]{2})\\s+([^\\s]+)\\s+(.+)$", Pattern.CASE_INSENSITIVE );
  	private static final short   NETBIOS_UDP_PORT = 137;
  	// NBT UDP PACKET: QUERY; REQUEST; UNICAST
  	public static final String NEW_HOST		 = "Discovery.action.NEW_HOST";
	public static final String HOST_UPDATE	 = "Discovery.action.HOST_UPDATE";
	public static final String HOST_ADDRESS  = "Discovery.data.HOST_ADDRESS";
	public static final String HOST_HARDWARE = "Discovery.data.HOST_HARDWARE";
	public static final String HOST_NAME     = "Discovery.data.HOST_NAME";
  	private static final byte[]  NETBIOS_REQUEST  = 
  	{ 
  		(byte)0x82, (byte)0x28, (byte)0x0,  (byte)0x0,  (byte)0x0, 
  		(byte)0x1,  (byte)0x0,  (byte)0x0,  (byte)0x0,  (byte)0x0, 
  		(byte)0x0,  (byte)0x0,  (byte)0x20, (byte)0x43, (byte)0x4B, 
  		(byte)0x41, (byte)0x41, (byte)0x41, (byte)0x41, (byte)0x41, 
  		(byte)0x41, (byte)0x41, (byte)0x41, (byte)0x41, (byte)0x41, 
  		(byte)0x41, (byte)0x41, (byte)0x41, (byte)0x41, (byte)0x41, 
  		(byte)0x41, (byte)0x41, (byte)0x41, (byte)0x41, (byte)0x41, 
  		(byte)0x41, (byte)0x41, (byte)0x41, (byte)0x41, (byte)0x41, 
  		(byte)0x41, (byte)0x41, (byte)0x41, (byte)0x41, (byte)0x41, 
  		(byte)0x0,  (byte)0x0,  (byte)0x21, (byte)0x0,  (byte)0x1
  	};
	private Network mNetwork;
	private Context   mContext	 = null;
	private UdpProber mProber	 = null;
	private ArpReader mArpReader = null;
	private boolean   mRunning	 = false;
	public Discovery(Context c) throws UnknownHostException
	{
	
		mNetwork = System.getNetwork(); 
		mContext   = c;
        mArpReader = new ArpReader();
		mProber	   = new UdpProber();
		mRunning   = false;
	}
	
	
	private class ArpReader extends Thread
	{
        private static final int RESOLVER_THREAD_POOL_SIZE = 25;
		
		private ThreadPoolExecutor	   mExecutor   = null;
		private boolean 			   mStopped    = true;
		private HashMap<String,String> mNetBiosMap = null;
		
		public ArpReader() {
			super("ArpReader");
			
			mNetBiosMap = new HashMap<String,String>(); 
			mExecutor   = ( ThreadPoolExecutor )Executors.newFixedThreadPool( RESOLVER_THREAD_POOL_SIZE );
		}
		
		public synchronized void addNetBiosName( String address, String name ) {
			synchronized( mNetBiosMap )
			{
				mNetBiosMap.put( address, name );
			}
		}
		
		@Override
		public void run() {
			Log.d( TAG, "ArpReader started ..." );
			
			mNetBiosMap.clear();
			mStopped = false;
			String iface = "";
			
			
				iface = mNetwork.getInterface().getDisplayName();
			
			
			
			while( mStopped == false )
			{
				try
				{
					BufferedReader reader   = new BufferedReader( new FileReader( ARP_TABLE_FILE ) );
					String		   line     = null,
								   name		= null;
					Matcher		   matcher  = null;
					Host       host = null;
					
					
					while( ( line = reader.readLine() ) != null )
					{
						if( ( matcher = ARP_TABLE_PARSER.matcher(line) ) != null && matcher.find() )
						{
							String address = matcher.group( 1 ),
								   // hwtype  = matcher.group( 2 ),
								   flags   = matcher.group( 3 ),
								   hwaddr  = matcher.group( 4 ),
								   // mask	   = matcher.group( 5 ),
								   device  = matcher.group( 6 );
								//	Log.d(TAG,"addr "+address+" fla "+flags+" d "+device+" h "+hwaddr);												
							if( device.equals(iface) && hwaddr.equals("00:00:00:00:00:00") == false && flags.contains("2") )
							{									
								host = new Host( address, hwaddr );
															
								// rescanning the gateway could cause an issue when the gateway itself has multiple interfaces ( LAN, WAN ... )
								if( host.getAddress().equals( mNetwork.getGatewayAddress() ) == false && host.getAddress().equals( mNetwork.getLocalAddress() ) == false )
								{
									synchronized( mNetBiosMap ){ name = mNetBiosMap.get(address); }
									
									if( name == null )
									{
										try
										{
											mExecutor.execute( new NBResolver( address ) );
										}
										catch( RejectedExecutionException e )
										{
											// ignore since this is happening because the executor was shut down.
										}

										if( host.isRouter() == false )
										{
											// attempt DNS resolution
											name = host.getAddress().getHostName();
											
											if( name.equals(address) == false )
											{
												Log.d( "NETBIOS", address + " was DNS resolved to " + name );
												
												synchronized( mNetBiosMap ){ mNetBiosMap.put( address, name ); }
											}
											else
												name = null;
										}
										
									}
	
									if( System.hasHost( host ) == false )				    				   
										sendNewHostNotification( host, name );
				    				
									else if( name != null )
									{
										host = System.getHostByAddress(address);
										if( host != null && host.hasAlias() == false )
										{
											host.setAlias( name );
											sendHostUpdateNotification( );
										}
									}	
								}
							}
						}
					}
					
					reader.close();
					
					Thread.sleep(500);
					
				}
				catch( Exception e )
				{
					Log.e( TAG, "188 w "+e+" "+e.getLocalizedMessage() );
					if(e instanceof NullPointerException)
					{
						Log.e(TAG,e.getLocalizedMessage()+"Null "+e.getMessage());
			
					}
					}
			}
		}		
		
		public synchronized void exit() {
			mStopped = true;
			try
			{
				mExecutor.shutdown();
				mExecutor.awaitTermination( 30, TimeUnit.SECONDS );
				mExecutor.shutdownNow();
			}
			catch( Exception e )
			{
				
			}
		}
		
	}
	private class NBResolver extends Thread
	{
		private static final int MAX_RETRIES = 3;
		
		private InetAddress    mAddress = null;
		private DatagramSocket mSocket  = null;

		public NBResolver( String address ) throws SocketException, UnknownHostException {
			super( "NBResolver" );
			
			mAddress   = InetAddress.getByName( address );
			mSocket    = new DatagramSocket();
			
			mSocket.setSoTimeout( 200 );
		}
		public NBResolver( InetAddress address ) throws SocketException, UnknownHostException {
			super( "NBResolver" );
			
			mAddress   =  address ;
			mSocket    = new DatagramSocket();
			
			mSocket.setSoTimeout( 200 );
		}
		@Override
		public void run() {			
			byte[] 		   buffer  = new byte[128];
			DatagramPacket packet  = new DatagramPacket( buffer, buffer.length, mAddress, NETBIOS_UDP_PORT ),
						   query   = new DatagramPacket( NETBIOS_REQUEST, NETBIOS_REQUEST.length, mAddress, NETBIOS_UDP_PORT );
			String		   name    = null,
						   address = mAddress.getHostAddress();
			Host host = null;
			
			for( int i = 0; i < MAX_RETRIES; i++ )
			{				
				try
				{
					mSocket.send( query );
					mSocket.receive( packet );
					
					byte[] data = packet.getData();
					
					if( data != null && data.length >= 74 )
					{
						String response = new String( data, "ASCII" );

						// i know this is horrible, but i really need only the netbios name
						name = response.substring( 57, 73 ).trim();		
						
						Log.d( "NETBIOS", address + " was resolved to " + name );
						
						// update netbios cache
						mArpReader.addNetBiosName( address, name );
						
						// existing target
						host = System.getHostByAddress( address );
						if( host != null )
						{
							host.setAlias( name );
							sendHostUpdateNotification( );
						}
																								
						break;
					}						
				}				
				catch( SocketTimeoutException ste ) 
				{ 		
					// swallow timeout error
				}
				catch( IOException e )
				{
					Log.e( "NBResolver", "IO" );
				}
				finally
				{
					try
					{
						// send again a query
						mSocket.send( query );
					}
					catch( Exception e )
					{
						// swallow error
					}
				}
			}
			
			mSocket.close();	
		}
	}
	
	private class UdpProber extends Thread
	{
		private static final int PROBER_THREAD_POOL_SIZE = 25;
		
		private class SingleProber extends Thread 
		{
			private InetAddress mAddress = null;
			
			public SingleProber( InetAddress address ) {
				mAddress = address;
			}
			
			@Override
			public void run() {
				try
				{
					DatagramSocket socket  = new DatagramSocket();
					DatagramPacket packet  = new DatagramPacket( NETBIOS_REQUEST, NETBIOS_REQUEST.length, mAddress, NETBIOS_UDP_PORT );
					
					socket.setSoTimeout( 200 );
					socket.send( packet );    	  
	
					socket.close();
				}
				catch( Exception e )
				{
					
				}
			}
		}
		
		private ThreadPoolExecutor mExecutor = null;
		private boolean 		   mStopped  = true;
		

		public UdpProber( ){
			mExecutor = ( ThreadPoolExecutor )Executors.newFixedThreadPool( PROBER_THREAD_POOL_SIZE );
		}
		
		@Override
		public void run() {
			Log.d( TAG, "UdpProber started ..." );
			
			mStopped = false;
									
			int i, nhosts = 0;
			IP4Address current = null;
	
			
			
				nhosts   = System.getNetwork().getNumberOfAddresses();
			Log.d(TAG,"Hostow "+nhosts+" start "+mNetwork.getStartAddress().toString());

			while( mStopped == false && mNetwork != null && nhosts > 0 )
			{										    			    			    
				try
    			{							
					for( i = 1, current = IP4Address.next( mNetwork.getStartAddress() ); current != null && i <= nhosts; current = IP4Address.next( current ), i++ ) 
					{				
						// rescanning the gateway could cause an issue when the gateway itself has multiple interfaces ( LAN, WAN ... )
					//	Log.d(TAG,"Addr "+current.toString()+" i "+i);
						if( current.equals( mNetwork.getGatewayAddress() ) == false && current.equals( mNetwork.getLocalAddress() ) == false )
						{ 
							InetAddress address = current.toInetAddress();

							
							try
							{
								mExecutor.execute( new SingleProber( address ) );
							}
							catch( RejectedExecutionException e )
							{
								// ignore since this is happening because the executor was shut down.
							}
							catch( OutOfMemoryError m )
							{
								// wait until the thread queue gets freed
								break;
							}
							catch( Exception e )
			    			{
			    				Log.e(TAG,"srodek 2"+e);
			    			}	
						}
					}

					Thread.sleep( 1000 );
    			}
    			catch( Exception e )
    			{
    				Log.e(TAG,"389 wyjatek 2"+e);
    			}				
			}
		}
		
		public synchronized void exit() {
			mStopped = true;
			try
			{
				mExecutor.shutdown();
				mExecutor.awaitTermination( 30, TimeUnit.SECONDS );
				mExecutor.shutdownNow();
			}
			catch( Exception e )
			{
				
			}
		}
	}
	


	private void sendNewHostNotification(Host h, String name ) {
		Intent intent = new Intent( NEW_HOST );
		
		intent.putExtra( HOST_ADDRESS,  h.getAddressAsString() );
		intent.putExtra( HOST_HARDWARE, h.getHardwareAsString() );
		intent.putExtra( HOST_NAME,     name == null ? "" : name );
		Log.d(TAG,"nowy host"+h.getAddress()+" "+h.getHardwareAsString()+" ");
        mContext.sendBroadcast(intent);    	
	}
	
	private void sendHostUpdateNotification( ) {
		mContext.sendBroadcast( new Intent( HOST_UPDATE ) );  
	}
	
	public boolean isRunning() {
		return mRunning;
	}

	@Override
	public void run( ) {			
		Log.d( TAG, "Network monitor started ..." );
		
		mRunning = true;
		   				
		try
		{			
			mProber.start();					
			mArpReader.start();
					
			mProber.join();
			mArpReader.join();
			Log.d( TAG, "Network monitor stopped." );
			
			mRunning = false;
		}
		catch( Exception e )
		{
			Log.e( TAG, " 443 Wyjatek" );
		}		
	}
	
	public void exit() {
		try
		{
			mProber.exit();
			mArpReader.exit();
		}
		catch( Exception e )
		{
			Log.e( TAG, "Wyjatek" );
		}				
	}

}
