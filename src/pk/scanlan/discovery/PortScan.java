package pk.scanlan.discovery;




import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import pk.scanlan.tools.Receiver;


import android.util.Log;


public class PortScan extends Thread {
	private static final String TAG = "PortScan";
	private InetAddress mAddress;
	private Receiver mReceiver;
	private int mStartPort;
	private int mStopPort;
	private boolean   mRunning	 = false;
	private final int mOneThreadsTask =127;
	private ThreadPoolExecutor	   mExecutor   = null;
   private static final int RESOLVER_THREAD_POOL_SIZE = 25;
	
   public final static int OPEN = 0;
   public final static int CLOSED = 1;
   public final static int FILTERED = -1;
   public final static int UNREACHABLE = -2;
   public final static int TIMEOUT = -3;
   private static final int TIMEOUT_SELECT = 300;
   private static long TIMEOUT_CONNECT = 1000 * 1000000; // ns
   private static final long TIMEOUT_RW = 3 * 1000 * 1000000; // ns
   private static final String E_REFUSED = "Connection refused";
   private static final String E_TIMEOUT = "The operation timed out";
   // TODO: Probe system to send other stuff than strings
   private static final String[] PROBES = new String[] { "", "\r\n\r\n", "GET / HTTP/1.0\r\n\r\n" };
   private static final int MAX_READ = 8 * 1024;
   private static final int WRITE_PASS = PROBES.length;
   private static final long WRITE_COOLDOWN = 200 * 1000000; // ns
	
	public PortScan(InetAddress ip, Receiver receiver,int startPort,int stopPort)
	{
		super("PortScan");
		
		//Log.d(TAG, "dla "+ip.getHostName());
		mAddress = ip;
		mReceiver = receiver;
		mStartPort = startPort;
		mStopPort = stopPort;
		mExecutor   = ( ThreadPoolExecutor )Executors.newFixedThreadPool( RESOLVER_THREAD_POOL_SIZE );
		
	}
private class SinglePorts extends Thread
{
	private int start,stop;
	
	public SinglePorts(int start,int stop)
	{
		super("Port");
		this.start=start;
		this.stop = stop;
		 
		
		
	}
	@Override
	public void run() {
		
		 for(int i=start; i <=stop; i++)
		    {
		             
		       try{


		    	   Socket ServerSok = new Socket();
		    	   ServerSok.connect(new InetSocketAddress(mAddress, i),100);
		    	   ServerSok.close();
		         synchronized(mReceiver)
		         {
		         mReceiver.newItem(String.valueOf(i));
		         }
		         
		         }
		        catch(Exception e){
		            //System.out.println("closed");
		        }
		            
		    }
		
	}
}
	
private class SocketScan extends Thread
{
	 private boolean select = true;
	    private Selector selector;
	    private ByteBuffer byteBuffer = ByteBuffer.allocate(MAX_READ);
	    private Charset charset = Charset.forName("UTF-8");
	    
	    private int start,stop;
		
		public SocketScan(int start,int stop)
		{
			super("Port");
			this.start=start;
			this.stop = stop;
			 
			
			
		}
	    private void connectSocket(InetAddress ina, int port) {
	        // Create the socket
	        try {
	            SocketChannel socket = SocketChannel.open();
	            socket.configureBlocking(false);
	            socket.connect(new InetSocketAddress(ina, port));
	            Data data = new Data();
	            data.port = port;
	            data.start = System.nanoTime();
	            socket.register(selector, SelectionKey.OP_CONNECT, data);
	        } catch (IOException e) {
	            Log.e(TAG, e.getMessage());
	        }
	    }
	    private void closeSelector() {
	        try {
	            if (selector.isOpen()) {
	                synchronized (selector.keys()) {
	                    Iterator<SelectionKey> iterator = selector.keys().iterator();
	                    while (iterator.hasNext()) {
	                        finishKey((SelectionKey) iterator.next(), FILTERED);
	                    }
	                    selector.close();
	                }
	            }
	        } catch (IOException e) {
	            Log.e(TAG, e.getMessage());
	        } catch (ClosedSelectorException e) {
	            if (e.getMessage() != null) {
	                Log.e(TAG, e.getMessage());
	            }
	        }
	    }

	    private void finishKey(SelectionKey key, int state) {
	        finishKey(key, state, null);
	    }

	    private void finishKey(SelectionKey key, int state, String banner) {
	        synchronized (key) {
	            if(key == null || !key.isValid()){
	                return;
	            }
	            closeChannel(key.channel());
	            Data data = (Data) key.attachment();
	          mReceiver.newItem(String.valueOf(data.port));//, state, banner);
	            key.attach(null);
	            key.cancel();
	            key = null;
	        }
	    }

	    private void closeChannel(SelectableChannel channel) {
	        if (channel instanceof SocketChannel) {
	            Socket socket = ((SocketChannel) channel).socket();
	            try{
	                if (!socket.isInputShutdown()) socket.shutdownInput();
	            } catch (IOException ex){
	            }
	            try{
	                if (!socket.isOutputShutdown()) socket.shutdownOutput();
	            } catch (IOException ex){
	            }
	            try{
	                socket.close();
	            } catch (IOException ex){
	            }
	        }
	        try{
	            channel.close();
	        } catch (IOException ex){
	        }
	    }

	    // Port private object
	    private  class Data {
	        protected int state = FILTERED;
	        protected int port;
	        protected long start;
	        protected int pass = 0;
	    }
	    @Override
		public void run() 
	    {
	    	
	            int step = 127;
	         
	            if ((start+stop)> step) {
	                
	                for (int i = start; i <= stop - step; i += step + 1) {
	                    if (select) {
	                        start_scan( i, i + ((i + step <=stop- step) ? step : start - i));
	                    }
	                }
	            } else {
	                start_scan( start, stop);
	            }

	        
	    }
	   private void start_scan(int lstart,int lstop)
	   {
	    	 select = true;
	         try {
	             selector = Selector.open();
	             for (int j = lstart; j <= lstop; j++) {
	                 connectSocket(mAddress, j);
	             }
	             while (select && selector.keys().size() > 0) {
	                 if (selector.select(TIMEOUT_SELECT) > 0) {
	                     synchronized (selector.selectedKeys()) {
	                         Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
	                         while (iterator.hasNext()) {
	                             SelectionKey key = (SelectionKey) iterator.next();
	                             try {
	                                 if (!key.isValid()) {
	                                     continue;
	                                 }
	                                 // States
	                                 final Data data = (Data) key.attachment();

	                                 if (key.isConnectable()) {
	                                     if (((SocketChannel) key.channel()).finishConnect()) {
	                                        
	                                             key.interestOps(SelectionKey.OP_READ
	                                                     | SelectionKey.OP_WRITE);
	                                             data.state = OPEN;
	                                             data.start = System.nanoTime();
	                                             mReceiver.newItem(String.valueOf(data.port));//, state, banner);
	                                        
	                                     }

	                                 } else if (key.isReadable()) {
	                                     try {
	                                         byteBuffer.clear();
	                                         final int numRead = ((SocketChannel) key.channel())
	                                                 .read(byteBuffer);
	                                         if (numRead > 0) {
	                                             String banner = new String(byteBuffer.array())
	                                                     .substring(0, numRead).trim();
	                                             // Log.v(TAG, "read " + data.port +
	                                             // " data=" + banner);
	                                             finishKey(key, OPEN, banner);
	                                         } else {
	                                             key.interestOps(SelectionKey.OP_WRITE);
	                                         }
	                                     } catch (IOException e) {
	                                         Log.e(TAG, e.getMessage());
	                                     }
	                                 } else if (key.isWritable()) {
	                                     key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	                                     if (System.nanoTime() - data.start > WRITE_COOLDOWN) {
	                                         if (data.pass < WRITE_PASS) {
	                                             // Log.v(TAG, "write " + data.port);
	                                             // write something (blocking)
	                                             final ByteBuffer bytedata = charset
	                                                     .encode(PROBES[data.pass]);
	                                             final SocketChannel sock = (SocketChannel) key
	                                                     .channel();
	                                             while (bytedata.hasRemaining()) {
	                                                 sock.write(bytedata);
	                                             }
	                                             bytedata.clear();
	                                             data.start = System.nanoTime();
	                                             data.pass++;
	                                         } else {
	                                             finishKey(key, OPEN);
	                                         }
	                                     }
	                                 }

	                             } catch (ConnectException e) {
	                                 if (e.getMessage().equals(E_REFUSED)) {
	                                     finishKey(key, CLOSED);
	                                 } else if (e.getMessage().equals(E_TIMEOUT)) {
	                                     finishKey(key, FILTERED);
	                                 } else {
	                                     Log.e(TAG, e.getMessage());
	                                     e.printStackTrace();
	                                     finishKey(key, FILTERED);
	                                 }
	                             } catch (Exception e) {
	                                 try {
	                                     Log.e(TAG, e.getMessage());
	                                 } catch (java.lang.NullPointerException e1) {
	                                     e1.printStackTrace();
	                                 } finally {
	                                     e.printStackTrace();
	                                     finishKey(key, FILTERED);
	                                 }
	                             } finally {
	                                 iterator.remove();
	                             }
	                         }
	                     }
	                 } else {
	                     // Remove old/non-connected keys
	                     final long now = System.nanoTime();
	                     final Iterator<SelectionKey> iterator = selector.keys().iterator();
	                     while (iterator.hasNext()) {
	                         final SelectionKey key = (SelectionKey) iterator.next();
	                         final Data data = (Data) key.attachment();
	                         if (data.state == OPEN && now - data.start > TIMEOUT_RW) {
	                             Log.e(TAG, "TIMEOUT=" + data.port);
	                             finishKey(key, TIMEOUT);
	                         } else if (data.state != OPEN && now - data.start > TIMEOUT_CONNECT) {
	                             finishKey(key, TIMEOUT);
	                         }
	                     }
	                 }
	             }
	         } catch (IOException e) {
	             Log.e(TAG, e.getMessage());
	         }
	         finally {
	             closeSelector();
	         }
	     }

 

}

	    @Override
		public void run() {
			mRunning = true;
			// TODO Auto-generated method stub
			Log.d(TAG,"start");
			 int portsCount = mStartPort+mStopPort;
			 int threadsCount = portsCount/mOneThreadsTask;
			 Log.d(TAG, "watkow "+threadsCount);
			   
				//   mExecutor.execute(( new SocketScan(mStartPort,mStopPort )));

					 for(int i=mStartPort; i <=mStopPort; i=i+mOneThreadsTask)
					    {
					             
					       try{

			        mExecutor.execute(new SinglePorts(i, i+mOneThreadsTask));
			                       }
					        catch(Exception e){
					            //System.out.println("closed");
					        }
					            
					    }
			   
			   
		}
		
	public boolean isRunning()
	{
		return mRunning;
		
	}
	public synchronized void exit() {
		mRunning = false;
		Log.d(TAG, "stop");
		try
		{
			mExecutor.shutdown();
			mExecutor.awaitTermination( 30, TimeUnit.SECONDS );
			mExecutor.shutdownNow();
		}
		catch( Exception e )
		{
			Log.e(TAG,"w "+e);
		}
   }
}

