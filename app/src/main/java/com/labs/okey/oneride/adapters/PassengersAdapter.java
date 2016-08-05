package com.labs.okey.oneride.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.facebook.FacebookSdk;
import com.labs.okey.oneride.R;
import com.labs.okey.oneride.model.User;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.IRecyclerClickListener;
import com.pkmmte.view.CircularImageView;

import java.util.List;

/**
 * Created by Oleg Kleiman on 26-May-15.
 */
public class PassengersAdapter extends RecyclerView.Adapter<PassengersAdapter.ViewHolder>{

    private final String LOG_TAG = getClass().getSimpleName();

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<User> items;
    private Context mContext;
    private Globals.LayoutManagerType mManagerType;
    private int mHeaderLayoutId;
    private int mRowLayoutId;

    public PassengersAdapter(Context context,
                             Globals.LayoutManagerType managerType,
                             int headerLayoutId,
                             int rowLayoutId,
                             List<User> objects){
        mContext        = context;
        mManagerType    = managerType;
        mHeaderLayoutId = headerLayoutId;
        mRowLayoutId    = rowLayoutId;
        items           = objects;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;

        if( mManagerType == Globals.LayoutManagerType.LINEAR_LAYOUT_MANAGER ) {

            if (viewType == TYPE_HEADER) { // Inflate header layout
                v = LayoutInflater.
                        from(parent.getContext()).
                        inflate(mHeaderLayoutId, parent, false);
            } else { // Inflate row layout
                v = LayoutInflater.
                        from(parent.getContext()).
                        inflate(mRowLayoutId, parent, false);
            }
        } else {
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
                v, viewType);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        if (holder.holderId == TYPE_ITEM) {

            int nPosition = ( mManagerType == Globals.LayoutManagerType.LINEAR_LAYOUT_MANAGER) ?
                                position - 1 :
                                position;

            User passenger = items.get(nPosition);

            holder.txtDriverName.setText(passenger.getFullName());
            if( !passenger.wasSelfPictured() ) {
                holder.imageStatus.setVisibility(View.INVISIBLE);
            } else {
                holder.imageStatus.setVisibility(View.VISIBLE);
            }

            String userId = passenger.getRegistrationId();
            String pictureURL = passenger.getPictureURL(); // getUserPictureURL(userId);
            if( pictureURL == null || pictureURL.isEmpty() )
                return;

            if( !pictureURL.contains("https") )
                pictureURL = pictureURL.replace("http", "https");

            if( Globals.volley == null )
                Globals.initializeVolley(mContext);

            ImageLoader imageLoader = Globals.volley.getImageLoader();
            imageLoader.get(pictureURL,
                    new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                            Bitmap bitmap = response.getBitmap();
                            if (bitmap != null )
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
        return ( mManagerType == Globals.LayoutManagerType.LINEAR_LAYOUT_MANAGER) ?
                items.size() + 1 :  // +1 for header view
                items.size();
    }

    // With the following method we check what type of view is being passed
    // In case of drawer adapter, each item corresponds to different type,
    // According to this item type (i.e. item position) different click listeners
    // will be attached to view items
    @Override
    public int getItemViewType(int position) {

        if( mManagerType == Globals.LayoutManagerType.LINEAR_LAYOUT_MANAGER )
            return( isPositionHeader(position) ) ? TYPE_HEADER : TYPE_ITEM;
        else
            return TYPE_ITEM;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    public void add(User passenger){
        if( !items.contains(passenger) )
            items.add(passenger);
    }

    private String getUserPictureURL(String userId){
        if( userId == null || userId.isEmpty() )
            return "";

        String[] tokens = userId.split(":");
        if( tokens.length < 2 )
            return "";

        if( Globals.FB_PROVIDER.equals(tokens[0]) ) {
            if( !FacebookSdk.isInitialized() )
                FacebookSdk.sdkInitialize(mContext);

            return "http://graph.facebook.com/" + tokens[1] + "/picture?type=large";
        } else if( Globals.GOOGLE_PROVIDER.equals(tokens[0])) {
                return "";
        } else
            return "";

    }
    public static class ViewHolder extends RecyclerView.ViewHolder {

        int holderId;

        // Row views
        TextView            txtDriverName;
        CircularImageView   userPicture;
        RelativeLayout      rowLayout;
        ImageView           imageStatus;

        Drawable drawableAvailable;
        Drawable drawableConnected;

        public ViewHolder(Context context,
                          IRecyclerClickListener clickListener,
                          View itemLayoutView,
                          int viewType) {
            super(itemLayoutView);

            if(viewType == TYPE_ITEM){

                holderId = viewType;

                txtDriverName   = (TextView) itemLayoutView.findViewById(R.id.txt_peer_name);
                userPicture     = (CircularImageView) itemLayoutView.findViewById(R.id.userPicture);
                rowLayout       = (RelativeLayout)itemLayoutView.findViewById(R.id.device_row);
                imageStatus     = (ImageView) itemLayoutView.findViewById(R.id.imgStatus);

                drawableAvailable = context.getResources().getDrawable(R.drawable.ic_action_disconnected);
                drawableConnected = context.getResources().getDrawable(R.drawable.accept_24);

            }

        }
    }
}
