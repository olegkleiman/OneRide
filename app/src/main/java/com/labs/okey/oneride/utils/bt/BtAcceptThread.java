package com.labs.okey.oneride.utils.bt;

/**
 * @author Oleg Kleiman
 * created 23-Aug-16
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.common.io.ByteStreams;
import com.labs.okey.oneride.utils.Globals;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This thread runs while listening for incoming connections. It behaves
 * like a server-side client.
 */

public class BtAcceptThread extends Thread {

    private final String          LOG_TAG = getClass().getSimpleName();

    private BluetoothAdapter        mAdapter;
    private Handler                 mHandler;

    private int                     mTotalConnections = 0;
    private List<BluetoothSocket>   mClientSockets;
    public List<BluetoothSocket>    getConnectedSockets() {
        return mClientSockets;
    }
    public void removeSocket(BluetoothSocket socket){
        if( mClientSockets.contains(socket) )
            mClientSockets.remove(socket);
    }

    private BluetoothServerSocket   mServerSocket = null;
    private static final String     NAME_INSECURE = "OneRideInsecure";
    UUID                            INSECURE_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public BtAcceptThread(BluetoothAdapter adapter, Handler handler) {
        mAdapter = adapter;
        mClientSockets = new ArrayList<>();
   }

    @Override
    public void run() {

        try {

            while (mTotalConnections < Globals.REQUIRED_PASSENGERS_NUMBER) {

                BluetoothSocket socket = null;

                try {

                    mServerSocket = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE,
                            INSECURE_UUID);

                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mServerSocket.accept();
                    if( socket != null ) {

                        InputStream inputStream = socket.getInputStream();
                        byte[] buffer = new byte[1024];
                        int bytes = inputStream.read(buffer);
//                        String strInput = new String(ByteStreams.toByteArray(inputStream));
//                        Log.d(LOG_TAG, strInput);
                        inputStream.close();
                    }

                    mServerSocket.close();

                } catch (IOException e) {
                    Globals.__logException(e);
                }

                // If a connection was accepted
                if (socket != null) {
                    BluetoothDevice device = socket.getRemoteDevice();
                    Globals.__log(LOG_TAG, "Socket accepted from: " + device.getName());

                    mClientSockets.add(socket);

                }
            }

        } catch(Exception e) { // InterruptedException e) {
            Globals.__logException(e);
        }
    }
}
