package com.labs.okey.oneride.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.labs.okey.oneride.R;
import com.labs.okey.oneride.model.BtDeviceUser;
import com.labs.okey.oneride.model.PropertyHolder;
import com.labs.okey.oneride.model.sc.SCModule;
import com.labs.okey.oneride.model.sc.SCUser;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.IRecyclerClickListener;
import com.labs.okey.oneride.utils.IRefreshable;

import java.util.List;
import java.util.Locale;

/**
 * @author Oleg Kleiman
 * created 09-Jul-16.
 */
public class BtPeersAdapter extends RecyclerView.Adapter<BtPeersAdapter.ViewHolder>{

    private final String        LOG_TAG = getClass().getSimpleName();

    private static final int    TYPE_HEADER = 0;
    private static final int    TYPE_ITEM = 1;

    private Context             mContext;
    private int                 mHeaderLayoutId;
    private int                 mRowLayoutId;
    private List<BtDeviceUser>  mItems;

    public BtPeersAdapter(Context context,
                             int headerLayoutId,
                             int rowLayoutId,
                             List<BtDeviceUser> objects){
        mContext = context;
        mHeaderLayoutId = headerLayoutId;
        mRowLayoutId = rowLayoutId;
        mItems = objects;
    }

    View.OnClickListener[] mListeners = new View.OnClickListener[] {
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    IRefreshable refreshable =
                            (mContext instanceof IRefreshable) ?
                                    (IRefreshable)mContext :
                                    null;
                    if( refreshable != null )
                        refreshable.refresh();
                }
            }
    };

    @Override
    public BtPeersAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        View.OnClickListener listener = null;

        if (viewType == TYPE_HEADER) { // Inflate header layout
            v = LayoutInflater.
                    from(parent.getContext()).
                    inflate(mHeaderLayoutId, parent, false);
            listener = mListeners[TYPE_HEADER];
        } else { // Inflate row layout
            v = LayoutInflater.
                    from(parent.getContext()).
                    inflate(mRowLayoutId, parent, false);
        }

        IRecyclerClickListener recyclerClickListener =
                ( mContext instanceof IRecyclerClickListener ) ?
                        (IRecyclerClickListener)mContext :
                        null;

        return new ViewHolder(mContext,
                recyclerClickListener,
                v, viewType, listener);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        if (holder.holderId == TYPE_ITEM) {

            final BtDeviceUser device = mItems.get(position - 1);

            holder.txtDriverName.setText(device.get_UserName());
            if( holder.txtRideCode != null )
                holder.txtRideCode.setText(device.get_RideCode());
            //holder.deviceStatus.setText(getDeviceStatus(device.status));
            if( holder.txtRssi != null ) {
                String txt = String.format(Locale.US, " %d dBm", device.get_Rssi());
                holder.txtRssi.setText(txt);
            }
            holder.setImageStatus(device.getStatus());

            SCModule scModule = new SCModule();
            SCUser scUser = scModule.getUser(device.get_authProvider(), device.get_UserId());
            String pictureURL = scUser.get_PictureURL();
            PropertyHolder<String> callback = new PropertyHolder<String>() {
                @Override
                public Void call() throws Exception {
                    if( this.property != null ) {
                        holder.txtDriverName.setText(this.property);
                    }
                    if( device.get_UserName() == null ||
                            device.get_UserName().isEmpty() )
                        device.set_UserName(this.property);
                    return null;
                }
            };
            scUser.get_FullName(callback);

            ImageLoader imageLoader = Globals.volley.getImageLoader();
            imageLoader.get(pictureURL,
                    new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                            Bitmap bitmap = response.getBitmap();
                            if (bitmap != null)
                                holder.userPicture.setImageBitmap(bitmap);
                        }

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(LOG_TAG, error.toString());
                        }
                    });
        }

    }

    @Override
    public int getItemCount() {
        return mItems.size() + 1;  // +1 for header view
    }

    // With the following method we check what type of view is being passed
    // In case of drawer adapter, each item corresponds to different type,
    // According to this item type (i.e. item position) different click listeners
    // will be attached to view items
    @Override
    public int getItemViewType(int position) {

        return( isPositionHeader(position) ) ? TYPE_HEADER : TYPE_ITEM;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    public void add(BtDeviceUser device){
        if( !mItems.contains(device) )
            mItems.add(device);
    }

    public void replaceItem(BtDeviceUser device) {
        mItems.remove(device);
        mItems.add(device);
    }

    public void updateItem(BtDeviceUser device){
        int index = mItems.indexOf(device);
        if( index != -1 )
            mItems.set(index, device);
        else
            mItems.add(device);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{

        int holderId;
        IRecyclerClickListener mClickListener;

        // Header views
        ImageButton             btnRefresh;

        // Row views
        TextView                txtDriverName;
        TextView                txtRideCode;
        TextView                txtRssi;
        TextView                deviceStatus;
        ImageView               userPicture;
        RelativeLayout          rowLayout;
        ImageView               imageStatus;

        Drawable                drawableAvailable;
        Drawable                drawableConnected;


        public ViewHolder(Context context,
                          IRecyclerClickListener clickListener,
                          View itemLayoutView,
                          int viewType,
                          View.OnClickListener listener) {
            super(itemLayoutView);
            mClickListener = clickListener;

            if(viewType == TYPE_HEADER) {

                btnRefresh = (ImageButton)itemLayoutView.findViewById(R.id.btnRefresh);
                btnRefresh.setOnClickListener(listener);

            } else if(viewType == TYPE_ITEM){

                holderId = viewType;

                txtDriverName = (TextView) itemLayoutView.findViewById(R.id.txt_peer_name);
                txtRideCode = (TextView) itemLayoutView.findViewById(R.id.txt_ride_code);
                txtRssi = (TextView) itemLayoutView.findViewById(R.id.txt_rssi);
                userPicture = (ImageView) itemLayoutView.findViewById(R.id.userPicture);
                rowLayout = (RelativeLayout)itemLayoutView.findViewById(R.id.device_row);
                imageStatus = (ImageView) itemLayoutView.findViewById(R.id.imgStatus);
                rowLayout.setOnClickListener(this);

                drawableAvailable = context.getResources().getDrawable(R.drawable.ic_action_disconnected);
                drawableConnected = context.getResources().getDrawable(R.drawable.accept_24);

            }

        }

        @Override
        public void onClick(View v) {
            int position = this.getLayoutPosition();
            --position; // header starts at position 0
            if( mClickListener != null ) {
                mClickListener.clicked(v, position);
            }
        }

        public void setImageStatus(int deviceStatus) {

            Drawable drawable;

            switch (deviceStatus) {
                case BluetoothDevice.BOND_BONDED:
                case BluetoothDevice.BOND_BONDING:
                    drawable = drawableAvailable;
                    break;
                case BluetoothDevice.BOND_NONE:
                    drawable = drawableConnected;
                    break;

                default:
                    drawable = null;
            }

            if( drawable != null
                    && imageStatus != null ) {
                imageStatus.setImageDrawable(drawable);
            }
        }
    }
}
