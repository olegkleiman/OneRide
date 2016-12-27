package com.labs.okey.oneride.utils.bt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import com.labs.okey.oneride.utils.Globals;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * @author Oleg Kleiman
 * created 23-Aug-16
 */


/**
 * This thread runs attempting to make an outgoing connection
 * with driver's device.
 */
public class BtConnectThread extends Thread {

    private final String          LOG_TAG = getClass().getSimpleName();

    private final BluetoothDevice mDevice;
    private BluetoothSocket       mSocket = null;

    UUID INSECURE_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public BtConnectThread(BluetoothDevice device) {

        mDevice = device;

        try {
            mSocket = device.createInsecureRfcommSocketToServiceRecord(INSECURE_UUID);
        } catch (IOException e) {
            Globals.__logException(e);
        }
    }

    @Override
    public void run() {

            try {
                if (mSocket != null) {
                    mSocket.connect();

                    if (mSocket.isConnected()) {
                        Globals.__log(LOG_TAG, "bluetooth socket connected!");

                        String userName = "Oleg Kleiman";

                        OutputStream outputStream = mSocket.getOutputStream();
                        DataOutputStream out = new DataOutputStream(outputStream);
                        out.writeBytes(userName);
                        //outputStream.write(userName.getBytes("UTF-8"));
                        //outputStream.flush();
                        out.flush();
                        out.close();
                        //outputStream.close();

                        //mSocket.close();
                    }
                }

                connect(mSocket, mDevice);

            } catch (IOException e) {
                Globals.__logException(e);

                try {
                    if (mSocket != null)
                        mSocket.close();
                } catch (IOException e2) {
                    Globals.__log(LOG_TAG, "unable to close()");
                }
            }

    }

    public void cancel() {
        try {
            if( mSocket != null )
                mSocket.close();
        } catch (IOException e) {
            Globals.__logException(e);
        }
    }

    public synchronized void connect(BluetoothSocket socket, BluetoothDevice  device) {


//            Message msg = mHandler.obtainMessage();
//            Bundle bundle = new Bundle();
//            bundle.putString("deviceName", device.getName());
//            msg.setData(bundle);
//            mHandler.sendMessage(msg);
    }



}
