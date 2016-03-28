package com.labs.okey.oneride.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.labs.okey.oneride.MainActivity;
import com.labs.okey.oneride.MyRidesActivity;
import com.labs.okey.oneride.R;
import com.labs.okey.oneride.SettingsActivity;
import com.labs.okey.oneride.TutorialActivity;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.RoundedDrawable;

/**
 * Created by Oleg Kleiman on 25-Apr-15.
 */
public class DrawerAccountAdapter extends RecyclerView.Adapter<DrawerAccountAdapter.ViewHolder>{

    private static final String LOG_TAG = "FR.DrawerAdapter";

    private static final int TYPE_HEADER = 0;  // Declaring Variable to Understand which View is being worked on
    // IF the view under inflation and population is header or Item
    private static final int TYPE_ITEM = 1;

    private Context         mContext;
    private String          mNavTitles[];
    private int             mIcons[];
    private String          mName;
    private String          mEmail;
    private String          mPictureURL;

    public DrawerAccountAdapter(Context context,
                                String[] titles,
                                int[] icons,
                                String name,
                                String email,
                                String pictureURL){
        mContext = context;

        mNavTitles = titles;
        mIcons = icons;
        mName = name;
        mEmail = email;
        mPictureURL = pictureURL;
    }

    View.OnClickListener[] mListeners = new View.OnClickListener[] {

            new View.OnClickListener() {
                @Override
                public void onClick(View view){
                    Intent intent = new Intent(mContext, SettingsActivity.class);
                    mContext.startActivity(intent);
                }
            },
            new View.OnClickListener() {
                @Override
                public void onClick(View view){
                    Intent intent = new Intent(mContext, MainActivity.class);
                    //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mContext.startActivity(intent);
                }
            },
            new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mContext, MyRidesActivity.class);
                    mContext.startActivity(intent);
                }
            },
            new View.OnClickListener(){

                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mContext, SettingsActivity.class);
                    mContext.startActivity(intent);
                }
            },
            new View.OnClickListener(){

                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mContext, TutorialActivity.class);
                    mContext.startActivity(intent);

                }
            }
    };

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v;

        if (viewType == TYPE_HEADER) {
            // Inflate header layout
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_header,parent,false);
        } else {
            // Inflate row layout
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_item_raw,parent,false);
        }

        View.OnClickListener listener = mListeners[viewType];
        ViewHolder vhItem = new ViewHolder(v,viewType, listener);
        return vhItem;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        if (holder.holderId == TYPE_HEADER) {

            Drawable drawable = null;
//            try {
//                drawable = (Globals.drawMan.userDrawable(mContext,
//                        "1",
//                        mPictureURL)).get();
//                if( drawable != null ) {
//                    drawable = RoundedDrawable.fromDrawable(drawable);
//                    ((RoundedDrawable) drawable)
//                            .setCornerRadius(Globals.PICTURE_CORNER_RADIUS)
//                            .setBorderColor(Color.LTGRAY)
//                            .setBorderWidth(Globals.PICTURE_BORDER_WIDTH)
//                            .setOval(true);
//
//                    holder.imageProfile.setImageDrawable(drawable);
//                }
//            } catch (InterruptedException | ExecutionException e) {
//                Log.e(LOG_TAG, e.getMessage());
//            }

            // Retrieves an image thru Volley
            com.android.volley.toolbox.ImageRequest request =
                    new com.android.volley.toolbox.ImageRequest(mPictureURL,
                            new Response.Listener<Bitmap>() {
                                @Override
                                public void onResponse(Bitmap bitmap) {
                                    Drawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);

                                    drawable = RoundedDrawable.fromDrawable(drawable);
                                    ((RoundedDrawable) drawable)
                                            .setCornerRadius(Globals.PICTURE_CORNER_RADIUS)
                                            .setBorderColor(Color.WHITE)
                                            .setBorderWidth(Globals.PICTURE_BORDER_WIDTH)
                                            .setOval(true);

                                    holder.imageProfile.setImageDrawable(drawable);
                                }
                            }, 0, 0, null,
                            new Response.ErrorListener(){
                                public void onErrorResponse(VolleyError error){
                                    Log.e(LOG_TAG, error.toString());
                                }
                            });
            request.setShouldCache(true);
            Globals.volley.addToRequestQueue(request);

            holder.txtName.setText(mName);
            holder.txtEmail.setText(mEmail);

        } else{
            holder.rowTextView.setText(mNavTitles[position - 1]);
            holder.rowImageView.setImageResource(mIcons[position -1]);
        }


    }

    @Override
    public int getItemCount() {
        return mNavTitles.length+1;
    }

    // With the following method we check what type of view is being passed
    // In case of drawer adapter, each item corresponds to different type,
    // According to this item type (i.e. item position) different click listeners
    // will be attached to view items
    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position))
            return TYPE_HEADER;

        return TYPE_ITEM + position - 1; // = position;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        int holderId;

        // Header views
        TextView txtName;
        TextView txtEmail;
        ImageView imageProfile;

        // Row views
        ImageView rowImageView;
        TextView rowTextView;

        public ViewHolder(View itemLayoutView, int viewType, View.OnClickListener listener) {

            super(itemLayoutView);

            if(viewType == TYPE_HEADER) {

                txtName = (TextView) itemLayoutView.findViewById(R.id.name);
                txtEmail = (TextView) itemLayoutView.findViewById(R.id.email);
                imageProfile = (ImageView) itemLayoutView.findViewById(R.id.circleView);

                if( listener != null ) {
                    itemLayoutView.setOnClickListener(listener);
                }

                holderId = TYPE_HEADER;
            } else {

                rowTextView = (TextView) itemLayoutView.findViewById(R.id.rowText);
                rowImageView = (ImageView) itemLayoutView.findViewById(R.id.rowIcon);

                if( listener != null ) {
                    itemLayoutView.setOnClickListener(listener);
                }

                holderId = viewType;
            }
        }

    }
}
