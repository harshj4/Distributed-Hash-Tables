package edu.buffalo.cse.cse486586.simpledht;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {
    private SharedPreferences serverSP;
    private String nextNode;
    private String nextPort;
    private String previousNode;
    private String previousPort;
    private String myPort;
    private String portStr;
    private static final int SERVER_PORT = 10000;
    private Uri uri;
    private String originator = "0";
    private Boolean isLeader;
    private String lastNode;
    private Boolean countFlag = true;
    Cursor outCursor;
    private Boolean goAhead = false;
    ArrayList<String> peerList = new ArrayList<String>();
    ArrayList<String> nodeList = new ArrayList<String>();
    HashMap<String,String> ring = new HashMap<String, String>();
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        serverSP = getContext().getSharedPreferences("storage", Context.MODE_PRIVATE);
        String ext = serverSP.getString(selection, "-1");
        if(selection.equals("@")){
            SharedPreferences.Editor editor = serverSP.edit();
            editor.clear();
            editor.commit();
        }
        else if(selection.equals("*")){
            SharedPreferences.Editor editor = serverSP.edit();
            editor.clear();
            editor.commit();
            String deleteAll = "DALL";
            send(deleteAll,nextPort);
        }
        else if(ext.equals("-1")){
            Log.d("Forwarding","DELETE request");
            String deleteRequest = "DELETE:"+selection;
            send(deleteRequest,nextPort);
        }
        else{
            SharedPreferences.Editor spEditor = serverSP.edit();
            spEditor.remove(selection);
            spEditor.commit();
        }

        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }



    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        try {
            if(portStr.equals("5554") && countFlag){
                countFlag = false;
                Thread.sleep(2000);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        serverSP = getContext().getSharedPreferences("storage", Context.MODE_PRIVATE);
        String keyToPut = null;
        keyToPut = (String)values.get("key");
        Log.d("keyToPut",keyToPut);
        String valueToPut = (String) values.get("value");
        Log.d("portStr",portStr);
        //Log.d("previousNode",previousNode);
        try {
            if(isLeader==true && peerList.size()==1){
                Log.d("Performing","single node insertion: "+keyToPut);
                SharedPreferences.Editor spEditor = serverSP.edit();
                spEditor.putString(keyToPut,valueToPut);
                spEditor.commit();
                return uri;
            }

            if((genHash(portStr).compareTo(genHash(previousNode)) <0) && ((genHash(keyToPut).compareTo(genHash(portStr)) <= 0) || (genHash(keyToPut).compareTo(genHash(previousNode)) > 0))){
                Log.d("Performing","second if insertion: "+keyToPut);
                SharedPreferences.Editor spEditor = serverSP.edit();
                spEditor.putString(keyToPut,valueToPut);
                spEditor.commit();
                return uri;
            }
            if((genHash(keyToPut).compareTo(genHash(portStr)) <= 0) && (genHash(keyToPut).compareTo(genHash(previousNode)) > 0)){
                Log.d("Inside third if",keyToPut);
                SharedPreferences.Editor spEditor = serverSP.edit();
                spEditor.putString(keyToPut,valueToPut);
                spEditor.commit();
            }
            else{
                Log.d("Forwarding","Insert request");
                new clientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "INSERT:"+keyToPut+":"+valueToPut,nextPort);
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


        return uri;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        Log.d("Welcome","created");
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.d("Current node",portStr);
        isLeader = true;
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            originator = portStr;
            if(!portStr.equals("5554")){
                Log.d("Sending","PRESENT");
                peerList.add(genHash(portStr));
                nodeList.add(portStr);
                nextNode = portStr;
                nextPort = Integer.toString(Integer.parseInt(nextNode) * 2);
                previousNode = portStr;
                previousPort = Integer.toString(Integer.parseInt(previousNode) * 2);
                new clientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "PRESENT:"+portStr,"11108");

            }else{
                peerList.add(genHash("5554"));
                nodeList.add(portStr);
                ring.put(genHash("5554"),"5554");
                nextNode = portStr;
                nextPort = Integer.toString(Integer.parseInt(nextNode)*2);
                previousNode = portStr;
                previousPort = Integer.toString(Integer.parseInt(previousNode)*2);

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        serverSP = getContext().getSharedPreferences("storage",Context.MODE_PRIVATE);
        if(selection.equals("@")){
            outCursor = new MatrixCursor(new String[] {"key","value"});

            Map tempMap = serverSP.getAll();
            for(Object item:tempMap.keySet()){
                ((MatrixCursor) outCursor).addRow(new Object[]{(String)item,tempMap.get(item)});
            }
            return outCursor;
        }
        else if (selection.equals("*")){
            Cursor tempCursor = new MatrixCursor(new String[] {"key","value"});

            Map tempMap = serverSP.getAll();
            for(Object item:tempMap.keySet()){
                ((MatrixCursor) tempCursor).addRow(new Object[]{(String)item,tempMap.get(item)});
            }
            if(nextNode.equals(portStr)){
                return tempCursor;
            }
            try {

                    //if (!nextNode.equals(originator)) {
                        Log.d("Fetching from","other peers");
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(nextPort));
                        Log.d("Sending to", nextPort);
                        //Log.d("ConnectionStatus", Boolean.toString(socket.isConnected()));
                        //socket.setTcpNoDelay(true);
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        //ObjectInputStream dis = new ObjectInputStream(socket.getInputStream());
                        String cursorString = "ALL:"+originator;
                        while(tempCursor.moveToNext()){
                            Log.d("temp key",tempCursor.getString(0));
                            Log.d("temp value",tempCursor.getString(1));
                            cursorString = cursorString + ":" +tempCursor.getString(0)+"#"+tempCursor.getString(1);
                        }
                        Log.d("cursorString",cursorString);
                        dos.writeUTF(cursorString);
                        dos.flush();
                        Thread.sleep(200);

                        dos.close();
                        socket.close();
                        while(true){
                            //Log.d("Entering","infinite while");
                            if(goAhead){
                                //Log.d("Breaking from","infinite while");
                                goAhead = false;
                                break;
                            }
                        }



//                Log.d("Printing each","element in cursor");
//                while (outCursor.moveToNext()) {
//                    Log.d(outCursor.getString(0), outCursor.getString(1));
//                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            originator = portStr;
            Log.d("Rolling back originator",originator);
            return outCursor;
        }
        else {
            String extractedValue = null;
            outCursor = new MatrixCursor(new String[]{"key", "value"});
            extractedValue = serverSP.getString(selection, "-1");
            if(!extractedValue.equals("-1")){
                Log.d("Extracted value for " + selection, extractedValue);
                ((MatrixCursor) outCursor).addRow(new Object[]{selection, extractedValue});

                return outCursor;
            }
            Log.d("Forwarding","QUERY");
            String queryString = "QUERY:"+selection+":"+originator;
            new clientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, queryString,nextPort);
            if(portStr.equals(originator)){
                while(true){
                    //Log.d("Entering","QUERY while");
                    if(goAhead){
                        //Log.d("Breaking out of","query while");

                        break;
                    }
                }
            }
            goAhead = false;
            return outCursor;
        }
    }
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket listeningSocket = null;
            Log.d("Inside","ServerTask");
            String receivedString;
            Cursor responseCursor;
            uri = buildUri("content","edu.buffalo.cse.cse486586.simpledht.provider");
            while(true){
                try {

                    Log.d("Before","Accept");
                    listeningSocket = serverSocket.accept();
                    Log.d("After","Accept");
                    DataInputStream dis = new DataInputStream(listeningSocket.getInputStream());

                    receivedString = (String) dis.readUTF();
                    Log.d("Received string is",receivedString);
                    String[] splits = receivedString.split(":");
                    if(splits[0].equals("ALL")){

                        if(!splits[1].equals(portStr)){
                            responseCursor = query(uri, null,"@",null,null);
                            while(responseCursor.moveToNext()){
                                receivedString = receivedString + ":" + responseCursor.getString(0) + "#" + responseCursor.getString(1);
                            }

                            Log.d("Forwarding","ALL request");
                            Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(nextPort));
                            DataOutputStream d = new DataOutputStream(sock.getOutputStream());
                            d.writeUTF(receivedString);
                            d.flush();
                        }
                        else{
                            for(int k=2; k<splits.length; k++){
                                String entry = splits[k];
                                Log.d("Entry is ",entry);
                                String[] row = entry.split("#");
                                Log.d("Row0 is ",row[0]);
                                String key = row[0];
                                String value = row[1];
                                ((MatrixCursor) outCursor).addRow(new Object[]{(String)key,value});
                            }
                            goAhead = true;

                        }
                    }
                    else if(splits[0].equals("PRESENT")){
                        String incomingNode = splits[1];
                        Log.d("Adding to peerList",splits[1]);
                        peerList.add(genHash(incomingNode));
                        nodeList.add(incomingNode);
                        Collections.sort(peerList);
                        Collections.sort(nodeList);
                        Log.d("PeerList is",peerList.toString());
                        Log.d("NodeList is",nodeList.toString());
                        ring.put(genHash(incomingNode),incomingNode);
                        int idx = peerList.indexOf(genHash(incomingNode));
                        String successor;
                        String predecessor;
                        if(idx == 0){
                            predecessor = ring.get(peerList.get(peerList.size()-1));
                        }
                        else{
                            predecessor = ring.get(peerList.get(idx-1));
                        }
                        //previousPort = Integer.toString(Integer.parseInt(previousNode)*2);
                        if(idx == peerList.size()-1){
                            successor = ring.get(peerList.get(0));
                        }else{
                            successor = ring.get(peerList.get(idx+1));
                        }
                        //nextPort = Integer.toString(Integer.parseInt(nextNode)*2);
                        String adjustNext = "NEXT:"+incomingNode;
                        String adjustPrevious = "PREVIOUS:"+incomingNode;
                        String neighborMsg = "NEIGHBORS:"+successor+":"+predecessor;
                        send(adjustPrevious,Integer.toString(Integer.parseInt(successor)*2));
                        send(adjustNext,Integer.toString(Integer.parseInt(predecessor)*2));
                        send(neighborMsg,Integer.toString(Integer.parseInt(incomingNode)*2));

                    }
                    else if(splits[0].equals("NEIGHBORS")){
                        Log.d("Neighbors","Received");
                        nextNode = splits[1];
                        previousNode = splits[2];
                        //lastNode = splits[3];
                        //Log.d("lastNode is",lastNode);
                        nextPort = Integer.toString(Integer.parseInt(nextNode)*2);
                        previousPort = Integer.toString(Integer.parseInt(previousNode) * 2);
                        if(!portStr.equals("5554")){
                            isLeader = false;
                        }
                        Log.d("Next node",nextNode);
                        Log.d("Previous node",previousNode);
                        //Log.d("isLeader",isLeader.toString());
                    }
                    else if(splits[0].equals("INSERT")){
                        Log.d("INSERT","Request received");
                        String keyToPut = splits[1];
                        String valueToPut = splits[2];
                        ContentValues tempCV = new ContentValues();
                        tempCV.put("key",keyToPut);
                        tempCV.put("value",valueToPut);
                        Log.d("Calling INSERT","from server");
                        insert(uri,tempCV);


                    }
                    else if(splits[0].equals("NEXT")){
                        nextNode = splits[1];
                        nextPort = Integer.toString(Integer.parseInt(nextNode)*2);
                        Log.d("nextNode",nextNode);
                    }
                    else if(splits[0].equals("PREVIOUS")){
                        previousNode = splits[1];
                        previousPort = Integer.toString(Integer.parseInt(previousNode)*2);
                        Log.d("previousNode",previousNode);
                    }else if(splits[0].equals("QUERY")){
                        String sel = splits[1];
                        originator = splits[2];
                        Cursor c = query(uri,null,sel,null,null);
                        if(c.getCount()!=0){
                            c.moveToFirst();
                            String key = c.getString(0);
                            String value = c.getString(1);
                            String qresp = "QRESP:"+key+":"+value;
                            send(qresp,Integer.toString(Integer.parseInt(originator)*2));

                        }
                        originator = portStr;
                    }else if(splits[0].equals("QRESP")){
                        String key = splits[1];
                        String val = splits[2];
                        Log.d("QRESP key",key);
                        Log.d("QRESP value",val);
                        outCursor = new MatrixCursor(new String[] {"key","value"});

                        ((MatrixCursor) outCursor).addRow(new Object[]{(String)key,val});
                        goAhead = true;
                    }else if(splits[0].equals("DALL")){
                        delete(uri,"@",null);
                        send("DALL",nextPort);
                    }else if(splits[0].equals("DELETE")){
                        String sel = splits[1];
                        delete(uri,sel,null);

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }

            //return null;
        }
    }
    public void send(String msg, String dest){
        new clientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg,dest);
    }


    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private class clientTask extends AsyncTask<String,Void,Void>{
        @Override
        protected Void doInBackground(String... strings) {
            String msgToSend = strings[0];
            String destination = strings[1];
            try {
                Log.d("Sending to",destination);
                Log.d("msgToSend",msgToSend);
                Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(destination));
                //sock.setTcpNoDelay(true);
                DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
                dos.writeUTF(msgToSend);
                dos.flush();
                Thread.sleep(500);
                //dos.close();
                //sock.close();
                //Log.d("PRESENT","Sent");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
