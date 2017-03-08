package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    int max=0;
    String emID = "";
    ArrayList<String> portList = new ArrayList<String>(Arrays.asList(REMOTE_PORT0,REMOTE_PORT1,REMOTE_PORT2,REMOTE_PORT3,REMOTE_PORT4));
    PriorityQueue<Entry> queue = new PriorityQueue<Entry>();
    HashMap<String,Entry> map  = new HashMap<String, Entry>();
    PriorityQueue<Entry> holdback = new PriorityQueue<Entry>();

    static final int SERVER_PORT = 10000;
    int count =0;

    ContentResolver contentProvider;
    Uri gUri;

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        emID = portStr;
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        TAG = TAG + emID;


        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }





        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        contentProvider = getContentResolver();
        gUri = buildUri("content","edu.buffalo.cse.cse486586.groupmessenger2.provider");

        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                //        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    /*
                     * If the key is pressed (i.e., KeyEvent.ACTION_DOWN) and it is an enter key
                     * (i.e., KeyEvent.KEYCODE_ENTER), then we display the string. Then we create
                     * an AsyncTask that sends the string to the remote AVD.
                     */
                String msg = editText.getText().toString()+"\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg); // This is one way to display a string.
                TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                remoteTextView.append("\n");





                    /*
                     * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                     * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                     * the difference, please take a look at
                     * http://developer.android.com/reference/android/os/AsyncTask.html
                     */
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                //return true;

                //return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, String> {

        @Override
        protected String doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            BufferedReader reader;
            //serverSocket.toString();

            String data="";

            try{

                while(true){
                    Socket clientSocket = serverSocket.accept();


                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()));

                    data = in.readLine();
                   // Log.e(TAG,"Received Data: "+data );
                    DataOutputStream outStream = new DataOutputStream(clientSocket.getOutputStream());





                    if(data!=null) {

                        if(data.startsWith("AgreedID")){
                            //Log.e(TAG, "Checking:"+data);
                            String[] idSplit = data.split("-");

                            double agreedId = Double.parseDouble(idSplit[1]);
                            String finalMsg = idSplit[3];
                            String emId = idSplit[2];

                            Entry x = map.get(finalMsg);
                            holdback.remove(x);

                            Entry e = new Entry(agreedId,finalMsg);
                            e.setDeliverable();
                            holdback.add(e);

                            Entry val = holdback.poll();
                            if(val.getDeliverable()){
                                holdback.remove(val);
                                queue.add(val);
                            }

                            Entry next = queue.poll();
                            ContentValues cv = new ContentValues();
                            cv.put("key",count);
                            cv.put("value",next.getValue());
                            Log.e(TAG,Integer.toString(count) +"-QueueSize:"+queue.size()+"-Seq:"+ Double.toString(agreedId)+"--" + finalMsg);
                            count+=1;
                            contentProvider.insert(gUri,cv);
                            int nxt = (int) Math.ceil(agreedId);

                            max = Math.max(max,nxt);
                            outStream.writeBytes("DONE");
                            publishProgress(next.getValue());

                        }
                       else {
                            //;
                            //Log.e(TAG,"ServerReceived:"+data);
                            double proposedId = max;
                            max = max + 1;
                            String[] splitData = data.split("-");
                            String emId = splitData[0];
                            String m = splitData[1];
                            double add =0;
                            if(emId.equals("5554")){
                                add = 0.2;
                            }
                            else if(emId.equals("5556")){
                                add = 0.3;
                            }
                            else if (emId.equals("5558")){
                                add=0.4;
                            }
                            else if(emId.equals(("5560"))){
                                add=0.5;
                            }
                            else{
                                add = 0.6;
                            }
                            proposedId = proposedId+add;
                            Entry e = new Entry(proposedId,m);
                            map.put(m,e);
                            holdback.add(e);
                            String pId =  Double.toString(proposedId)+"-OK\n";
                            outStream.writeBytes(pId);

                            //Log.e(TAG,pId);
                        }


                        //contentProvider.

                        //count+=1;


                    }
                    clientSocket.close();
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e(TAG,data +" is logged");

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */


            return data;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

            String strReceived = strings[0].trim();


            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String filename = "SimpleMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {


            try {
                String remotePort;
                double agreedId=-1;
                for(int i=0;i<portList.size();i++){
                    //if(msgs[1].equals(portList.get(i)))
                    //    continue;

                    remotePort = portList.get(i);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));


                    String msgToSend = msgs[0];
                    //Log.e(TAG, msgToSend + remotePort+" - Sending Message");
                /*
                 * TODO: Fill in your client code that sends out a message.
                 */

                    //

                    //PrintWriter outStream = new PrintWriter(socket.getOutputStream(), true);
                    DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());

                    //Log.e(TAG, msgToSend + remotePort+" - outStreamFine");

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));

                    //Log.e(TAG, msgToSend + remotePort+" - BufferedReader fine");

                    //if(in.readLine()=="OK")


                    outStream.writeBytes(emID+"-"+msgToSend);

                    //Log.e(TAG, msgToSend + remotePort+" - OutStream write fine");
                    outStream.flush();
                    //outStream.println(msgToSend);
                    String ack = in.readLine();

                    //Log.e(TAG, ack + remotePort+" - checking ACK");
                    if (ack.endsWith("OK")) {
                        double reading = Double.parseDouble(ack.split("-")[0]);
                        if(reading>agreedId)
                            agreedId = reading;
                        //Log.e(TAG,Double.toString(agreedId)+"AgreedSoFar " + msgToSend);

                        socket.close();
                    }
                }

                for(int i=0;i<portList.size();i++){
                    remotePort = portList.get(i);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));

                    String msgToSend = msgs[0];
                    String msg ="AgreedID-"+Double.toString(agreedId)+"-"+emID+"-"+msgToSend;

                    Log.e(TAG,msg);
                    DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());



                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                   // Log.e(TAG, "SendingMessage--"+msg);
                    outStream.writeBytes(msg);
                    outStream.flush();
                    String ack = in.readLine();
                    if(ack.equals("DONE")){
                       // Log.e(TAG,"DONE");
                        socket.close();
                    }
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }catch (Exception e){
                Log.e(TAG,"some Exception");
            }

            return null;
        }
    }


}

class Entry implements Comparable<Entry> {
    private double key;
    private String value;
    private boolean deliverable;
    public Entry(double key, String value) {
        this.key = key;
        this.value = value;
        this.deliverable = false;
    }

    // getters
    public double getKey(){
        return this.key;
    }

    public String getValue(){
        return this.value;
    }
    public void setDeliverable(){
        this.deliverable = true;
    }

    public boolean getDeliverable(){
        return this.deliverable;
    }
    @Override
    public int compareTo(Entry other) {
        int compare  = this.getKey()>other.getKey()?0:1;
        return compare;
    }
}