package com.labs.okey.oneride.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Cache;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.labs.okey.oneride.BaseActivity;
import com.labs.okey.oneride.MainActivity;
import com.labs.okey.oneride.MyRidesActivity;
import com.labs.okey.oneride.R;
import com.labs.okey.oneride.SettingsActivity;
import com.labs.okey.oneride.TutorialActivity;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.RoundedDrawable;
import com.pkmmte.view.CircularImageView;

/**
 * Created by Oleg Kleiman on 25-Apr-15.
 */
public class DrawerAccountAdapter extends RecyclerView.Adapter<DrawerAccountAdapter.ViewHolder>{

    private static final String LOG_TAG = "FR.DrawerAdapter";

    private static final int TYPE_HEADER = 0;
    // Declaring Variable to Understand which View is being worked on.
    // If the view under inflation and population is header or Item
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
        return new ViewHolder(v,viewType, listener);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        if (holder.holderId == TYPE_HEADER) {

            Drawable d = BaseActivity.scaleImage(mContext,
                    ContextCompat.getDrawable(mContext, R.drawable.rsz_toolbar_bk),
                    0.3f);

            holder.headerLayout.setBackground(d);

            // Retrieves an image through Volley
            if( !mPictureURL.isEmpty() ) {
                RequestQueue requestQueue = Globals.volley.getRequestQueue();
                if (requestQueue == null)
                    return;

                Cache cache = requestQueue.getCache();
                if (cache == null)
                    return;

                Cache.Entry entry = cache.get(mPictureURL);
                if (entry != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(entry.data, 0, entry.data.length);
                    holder.imageProfile.setImageBitmap(bitmap);
                } else {
                    ImageLoader imageLoader = Globals.volley.getImageLoader();

                    if (!mPictureURL.contains("https"))
                        mPictureURL = mPictureURL.replace("http", "https");

                    imageLoader.get(mPictureURL,
                            new ImageLoader.ImageListener() {
                                @Override
                                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                                    Bitmap bitmap = response.getBitmap();
                                    if (bitmap != null)
                                        holder.imageProfile.setImageBitmap(bitmap);
                                }

                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.e(LOG_TAG, error.toString());
                                }
                            });
                }
            }

//            com.android.volley.toolbox.ImageRequest request =
//                    new com.android.volley.toolbox.ImageRequest(mPictureURL,
//                            new Response.Listener<Bitmap>() {
//                                @Override
//                                public void onResponse(Bitmap bitmap) {
//                                    holder.imageProfile.setImageBitmap(bitmap);
//                                }
//                            }, 0, 0, ImageView.ScaleType.CENTER_INSIDE, null,
//                            new Response.ErrorListener(){
//                                public void onErrorResponse(VolleyError error){
//                                    Log.e(LOG_TAG, error.toString());
//                                }
//                            });
//            request.setShouldCache(true);
//            Globals.volley.addToRequestQueue(request);

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
        TextView            txtName;
        TextView            txtEmail;
        CircularImageView   imageProfile;
        RelativeLayout      headerLayout;

        // Row views
        ImageView rowImageView;
        TextView rowTextView;

        public ViewHolder(View itemLayoutView, int viewType, View.OnClickListener listener) {

            super(itemLayoutView);

            if(viewType == TYPE_HEADER) {

                txtName = (TextView) itemLayoutView.findViewById(R.id.name);
                txtEmail = (TextView) itemLayoutView.findViewById(R.id.email);
                imageProfile = (CircularImageView) itemLayoutView.findViewById(R.id.setttingsProfileAvatarView);
                headerLayout = (RelativeLayout) itemLayoutView.findViewById(R.id.drawer_header_layout);

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
