package com.labs.okey.oneride.utils.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.labs.okey.oneride.model.User;
import com.labs.okey.oneride.utils.Globals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;

/**
* @author Oleg Kleiman
* created 24-Dec-16
*/

public class BtChatService {

    private final String                LOG_TAG = getClass().getSimpleName();

    private static final String     NAME_INSECURE = "OneRideInsecure";
    private UUID INSECURE_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private final BluetoothAdapter mAdapter;

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private ArrayList<String> mDeviceAddresses;
    private ArrayList<String> mDeviceNames;
    private ArrayList<ConnectedThread> mConnThreads;
    private ArrayList<BluetoothSocket> mSockets;

    /**
     * A bluetooth piconet can support up to 7 connections. This array holds 7
     * unique UUIDs. When attempting to make a connection, the UUID on the
     * client must match one that the server is listening for. When accepting
     * incoming connections server listens for all 7 UUIDs. When trying to form
     * an outgoing connection, the client tries each UUID one at a time.
     */
    private ArrayList<UUID> mUuids;

    private int mState;
    private Handler mHandler;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_LISTEN = 1; // now listening for incoming
                                              // connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing
                                                  // connection
    public static final int STATE_CONNECTED = 3; // now connected to a remote
                                                 // device

    public BtChatService(Context context,
                         BluetoothAdapter btAdapter,
                         Handler handler) {
        mAdapter = btAdapter;
        mState = STATE_NONE;
        mHandler = handler;

        initializeArrayLists();

        // 7 randomly-generated UUIDs. These must match on both server and
        // client.
//         mUuids.add(UUID.fromString("b7746a40-c758-4868-aa19-7ac6b3475dfc"));
//         mUuids.add(UUID.fromString("2d64189d-5a2c-4511-a074-77f199fd0834"));
//         mUuids.add(UUID.fromString("e442e09a-51f3-4a7b-91cb-f638491d1412"));
//         mUuids.add(UUID.fromString("a81d6504-4536-49ee-a475-7d96d09439e4"));
//         mUuids.add(UUID.fromString("aa91eab1-d8ad-448e-abdb-95ebba4a9b55"));
//         mUuids.add(UUID.fromString("4d34da73-d0a4-4f40-ac38-917e0a9dee97"));
//         mUuids.add(UUID.fromString("5e14d4df-9c8a-4db7-81e4-c937564c86e0"));
    }

    private void initializeArrayLists() {
        mDeviceAddresses = new ArrayList<String>(5);
        mDeviceNames = new ArrayList<String>(5);
        mConnThreads = new ArrayList<ConnectedThread>(5);
        mSockets = new ArrayList<BluetoothSocket>(5);
        mUuids = new ArrayList<UUID>(5);

        for (int i = 0; i < 5; i++) {
            mDeviceAddresses.add(null);
            mDeviceNames.add(null);
            mConnThreads.add(null);
            mSockets.add(null);
            mUuids.add(null);
        }
    }

    public synchronized void start() {

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }

        setState(STATE_LISTEN);
    }

    public synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        for (int i = 0; i < 5; i++) {
            mDeviceNames.set(i, null);
            mDeviceAddresses.set(i, null);
            mSockets.set(i, null);
            if (mConnThreads.get(i) != null) {
                mConnThreads.get(i).cancel();
                mConnThreads.set(i, null);
            }
        }

        setState(STATE_NONE);
    }

    private synchronized void setState(int state) {
        mState = state;
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void connect(BluetoothDevice device,
                                     int selectedPosition) {
        if (getPositionIndexOfDevice(device) == -1) {
            if (mState == STATE_CONNECTING) {
                if (mConnectThread != null) {
                    mConnectThread.cancel();
                    mConnectThread = null;
                }
            }

            // Cancel any thread currently running a connection
            if (mConnThreads.get(selectedPosition) != null) {
                mConnThreads.get(selectedPosition).cancel();
                // mConnectedThread = null;
                mConnThreads.set(selectedPosition, null);
            }

            // Create a new thread and attempt to connect to each UUID
            // one-by-one.
            try {
                // String
                // s="00001101-0000-1000-8000"+device.getAddress().split(":");
                ConnectThread mConnectThread = new ConnectThread(device,
                                                                selectedPosition);

                mConnectThread.start();
                setState(STATE_CONNECTING);
            } catch(Exception e) {
                Globals.__logException(e);
            }
        }
    }

    public synchronized void connected(BluetoothSocket socket,
                                       BluetoothDevice device,
                                       int selectedPosition) {

        ConnectedThread mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Add each connected thread to an array
        mConnThreads.set(selectedPosition, mConnectedThread);

        setState(STATE_CONNECTED);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);
   }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost(BluetoothDevice device) {
        int positionIndex = getPositionIndexOfDevice(device);
        if (positionIndex != -1) {
            mDeviceAddresses.set(positionIndex, null);
            mDeviceNames.set(positionIndex, null);
            mConnThreads.set(positionIndex, null);
        }
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out
     *            The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // When writing, try to write out to all connected threads
        for (int i = 0; i < mConnThreads.size(); i++) {
            try {
                // Create temporary object
                ConnectedThread r;
                // Synchronize a copy of the ConnectedThread
                synchronized (this) {
                    if (mState != STATE_CONNECTED)
                        return;
                    r = mConnThreads.get(i);
                }
                // Perform the write unsynchronized
                r.write(out);
            } catch (Exception e) {
            }
        }
    }

    public void write(String message) {

        try {
            write(message.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Globals.__logException(e);
        }
    }

    private class AcceptThread extends Thread {

        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {

            BluetoothServerSocket tmp = null;

            try {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE,
                                                                    INSECURE_UUID);
//                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_INSECURE,
//                                                                  INSECURE_UUID);
            } catch(IOException e) {
                Globals.__logException(e);
            }

            mmServerSocket = tmp;

        }

        public void run() {
            BluetoothSocket socket = null;

            while( mState != BtChatService.STATE_CONNECTED) {
                try {
                    socket = mmServerSocket.accept();
                } catch(IOException e) {
                    Globals.__logException(e);
                    break;
                }

                if( socket != null ) {
                    synchronized (BtChatService.this) {
                        switch( mState ) {
                            case BtChatService.STATE_LISTEN:
                            case BtChatService.STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice(),
                                        getAvailablePositionIndexForNewConnection(socket.getRemoteDevice()));
                                break;

                            case BtChatService.STATE_NONE:
                            case BtChatService.STATE_CONNECTED:
                                // Either not ready or already connected. Terminate
                                // new socket.
                                try {
                                    socket.close();
                                } catch(IOException e) {
                                    Globals.__logException(e);
                                }
                                break;

                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Globals.__logException(e);
            }
        }
    }

    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private int selectedPosition;

        public ConnectThread(BluetoothDevice device,
                             int selectedPosition) {
            mmDevice = device;
            this.selectedPosition = selectedPosition;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp =  device.createInsecureRfcommSocketToServiceRecord(INSECURE_UUID);
                //tmp = device.createRfcommSocketToServiceRecord(uuidToTry);
            } catch (IOException e) {
                Globals.__logException(e);
            }

            mmSocket = tmp;
        }

        public void run() {

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {

                connectionFailed();

                Globals.__logException(e);

                BtChatService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BtChatService.this) {
                mConnectThread = null;
            }

            mDeviceAddresses.set(selectedPosition, mmDevice.getAddress());
            mDeviceNames.set(selectedPosition, mmDevice.getName());

            // Start the connected thread
            connected(mmSocket, mmDevice, selectedPosition);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Globals.__logException(e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device. It handles all
     * incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Globals.__logException(e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while( true ) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    User passenger = (User)ois.readObject();

//                    String message = new String(buffer, 0, bytes); //"UTF-8");
//                    Globals.__log(LOG_TAG, message);

                    // Send the bundle from obtained User object back to UI Activity
                    Message msg = mHandler.obtainMessage(Globals.MESSAGE_READ);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("passenger", passenger);
                    msg.setData(bundle);
                    mHandler.dispatchMessage(msg);
                } catch (Exception e) {
                    Globals.__logException(e);
                    connectionLost(mmSocket.getRemoteDevice());
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch(IOException e) {
                Globals.__logException(e);
            }
        }

        /**
         *
         * @param buffer
         *            The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Globals.__logException(e);
            }
        }
    }

    public int getPositionIndexOfDevice(BluetoothDevice device) {
        for (int i = 0; i < mDeviceAddresses.size(); i++) {
            if (mDeviceAddresses.get(i) != null
                    && mDeviceAddresses.get(i).equalsIgnoreCase(
                    device.getAddress()))
                return i;
        }
        return -1;
    }

    public int getAvailablePositionIndexForNewConnection(BluetoothDevice device) {
        if (getPositionIndexOfDevice(device) == -1) {
            for (int i = 0; i < mDeviceAddresses.size(); i++) {
                if (mDeviceAddresses.get(i) == null) {
                    return i;
                }
            }
        }
        return -1;
    }
}

