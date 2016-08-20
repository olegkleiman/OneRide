package com.labs.okey.oneride.myrides;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.labs.okey.oneride.R;
import com.labs.okey.oneride.adapters.MyRidesAdapter;
import com.labs.okey.oneride.model.Ride;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author eli max
 * created 18/06/2015.
 */
public class GeneralMyRidesFragment extends Fragment {

    List<Ride> mRides = new ArrayList<>();
    private static final String ARG_POSITION = "position";
    private static GeneralMyRidesFragment FragmentInstance;
    MyRidesAdapter adapter;

    public static GeneralMyRidesFragment getInstance() {

        if (FragmentInstance == null ) {
            FragmentInstance = new GeneralMyRidesFragment();
        }
        return FragmentInstance;
    }

    public void setRides(List<Ride> rides) {

        if (rides == null || rides.isEmpty())
            return;

        mRides.clear();
        mRides.addAll(rides);
        sort();
    }

    public void updateRides(List<Ride> rides){

        if (rides == null || rides.isEmpty())
            return;

        RecyclerView recycler = (RecyclerView)getActivity().findViewById(R.id.recyclerMyRides);
        recycler.invalidate();

        mRides.clear();
        mRides.addAll(rides);

        sort();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_myride_general, container, false);

//        if (mRides.isEmpty()) {
//
//            final ProgressBar progress_refresh = (ProgressBar) rootView.findViewById(R.id.progress_refresh);
//            progress_refresh.setVisibility(View.VISIBLE);
//        }

        RecyclerView recycler = (RecyclerView)rootView.findViewById(R.id.recyclerMyRides);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        recycler.addItemDecoration(new GridSpacingItemDecoration(getActivity())); //, 2, dpToPx(10), true));
        recycler.setItemAnimator(new DefaultItemAnimator());

        adapter = new MyRidesAdapter(mRides);
        recycler.setAdapter(adapter);

        return rootView;

    }

    private void sort(){

        Collections.sort(mRides, Collections.reverseOrder(new Comparator<Ride>() {
            public int compare(Ride r1, Ride r2) {
                if( r1.getCreated() == null
                        || r2.getCreated() == null )
                    return 1;

                return r1.getCreated().compareTo(r2.getCreated());
            }}));

    }

    /**
     * RecyclerView item decoration - give equal margin around grid item
     */
    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        private Drawable mDivider;

        public GridSpacingItemDecoration(Context context) { //, int spanCount, int spacing, boolean includeEdge) {
//            this.spanCount = spanCount;
//            this.spacing = spacing;
//            this.includeEdge = includeEdge;

            mDivider = context.getResources().getDrawable(R.drawable.line_divider);

        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }

//        @Override
//        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
//            int position = parent.getChildAdapterPosition(view); // item position
//            int column = position % spanCount; // item column
//
//            if (includeEdge) {
//                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
//                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)
//
//                if (position < spanCount) { // top edge
//                    outRect.top = spacing;
//                }
//                outRect.bottom = spacing; // item bottom
//            } else {
//                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
//                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
//                if (position >= spanCount) {
//                    outRect.top = spacing; // item top
//                }
//            }
//        }
    }

    /**
     * Converting dp to pixel
     */
    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }
}