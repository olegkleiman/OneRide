package com.labs.okey.oneride.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.labs.okey.oneride.R;
import com.labs.okey.oneride.model.Ride;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.IRecyclerClickListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by eli max on 22/06/2015.
 */
public class MyRidesAdapter extends RecyclerView.Adapter<MyRidesAdapter.ViewHolder> {

    private List<Ride>          items;
    IRecyclerClickListener      mClickListener;
    Context                     context;

    public MyRidesAdapter(List<Ride> objects) {
        items = objects;
    }

    public void setOnClickListener(IRecyclerClickListener listener) {
        mClickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();

        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.rides_general_item, parent, false);

        return new ViewHolder(v, mClickListener);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        Ride ride = items.get(position);

//        if(!ride.getDriverId().equals( Globals.userID))
//        {
//            holder.driverName.setText(ride.getDriverName());
//            holder.approvedSign.setVisibility(View.GONE);
//        }
//        else
//        {
//            holder.driverName.setVisibility(View.GONE);

            int approveStatus = ride.getApproved();

            if( approveStatus == Globals.RIDE_STATUS.WAIT .ordinal()) {
                holder.approvedSign.setImageResource(R.drawable.attention_26);
            } else if( approveStatus == Globals.RIDE_STATUS.APPROVED.ordinal()
                    || approveStatus == Globals.RIDE_STATUS.APPROVED_BY_SELFY.ordinal()
                    || approveStatus == Globals.RIDE_STATUS.VALIDATED_MANUALLY.ordinal()){
                holder.approvedSign.setImageResource(R.drawable.v_sing_26);
            } else if( approveStatus == Globals.RIDE_STATUS.DENIED.ordinal() ) {
                holder.approvedSign.setImageResource(R.drawable.ex_sing_26);
            } else if( approveStatus == Globals.RIDE_STATUS.BE_VALIDATED_MANUALLY.ordinal()
                    || approveStatus == Globals.RIDE_STATUS.BE_VALIDATED_MANUALLY_SELFIE.ordinal() ) {
                holder.approvedSign.setImageResource(R.drawable.sand_clock_24);
            }

//            try {
//                User user = User.load(context);
//
//                ImageLoader imageLoader = Globals.volley.getImageLoader();
//                imageLoader.get(user.getPictureURL(), new ImageLoader.ImageListener() {
//                    @Override
//                    public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
//                        Bitmap bitmap = response.getBitmap();
//                        holder.driverImage.setImageBitmap(bitmap);
//                    }
//
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//
//                    }
//                });
//
//            } catch (Exception e) {
//                Log.e(LOG_TAG, e.getMessage());
//            }

//        }

        if( ride.getCreated() != null ) {
            //DateFormat df = DateFormat.getDateInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            sdf.setTimeZone(cal.getTimeZone());
            String localTime = sdf.format(ride.getCreated());
            holder.created.setText(localTime);
        }

        holder.txtRideCode.setText(ride.getRideCode());

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        //CircularImageView driverImage;
        ImageView           approvedSign;
        //TextView            driverName;
        TextView            txtRideCode;
        TextView            carNumber;
        TextView            created;
        View                rowLayout;

        IRecyclerClickListener mClickListener;

        public ViewHolder(View itemView,
                          IRecyclerClickListener clickListener) {
            super(itemView);

            mClickListener = clickListener;
            //driverImage = (CircularImageView) itemView.findViewById(R.id.imageDriver);
            //driverName = (TextView) itemView.findViewById(R.id.txtDriverName);
            txtRideCode = (TextView) itemView.findViewById(R.id. txtRideCode);
            approvedSign = (ImageView) itemView.findViewById(R.id.approvedSign);
            created = (TextView) itemView.findViewById(R.id.txtCreated);
            rowLayout = itemView.findViewById(R.id.mode_row);
            rowLayout.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            v.invalidate();
            int position = this.getLayoutPosition();
            if (mClickListener != null) {
                mClickListener.clicked(v, position);
            }
        }
    }
}
