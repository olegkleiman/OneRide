package com.labs.okey.oneride.adapters;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.labs.okey.oneride.R;
import com.labs.okey.oneride.SettingsActivity;
import com.labs.okey.oneride.databinding.CarItemRowBinding;
import com.labs.okey.oneride.model.RegisteredCar;

import java.util.List;

/**
 * @author Oleg Kleiman
 * created 05-Jun-15,
 * updated to use DataBinding 11-Apr-17
 */
public class CarsAdapter extends RecyclerView.Adapter<CarsAdapter.ViewHolder>
                         {

    private List<RegisteredCar> mItems;
    private SettingsActivity.ActionListener actionListener;

    public CarsAdapter(Context context,
                       List<RegisteredCar> cars,
                       SettingsActivity.ActionListener listener){
        mItems = cars;
        this.actionListener = listener;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    private RegisteredCar getItem(int position) {
        return mItems.get(position);
    }

    public void add(RegisteredCar car) {
        mItems.add(car);
    }

    public void remove(RegisteredCar car) {
        mItems.remove(car);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        if( parent instanceof RecyclerView ) {
            LayoutInflater layoutInflater = LayoutInflater.
                    from(parent.getContext());
            CarItemRowBinding binging = CarItemRowBinding.inflate(layoutInflater, parent, false);

            return new ViewHolder(binging, actionListener);
        } else {
            throw new RuntimeException("Something is wrong with Cars Recycler");
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder,
                                 int position) {
        RegisteredCar car = this.getItem(position);
        holder.bind(car);
        holder.binding.executePendingBindings();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
                                   implements View.OnClickListener {

        private final CarItemRowBinding binding;
        private SettingsActivity.ActionListener actionListener;

        public ViewHolder(CarItemRowBinding binding,
                          SettingsActivity.ActionListener actionListener) {

            super(binding.getRoot());

            this.actionListener = actionListener;
            binding.getRoot().setOnClickListener(this);

            this.binding = binding;
        }

        public void bind(RegisteredCar car) {
            binding.setCar(car);
            binding.executePendingBindings();
        }

        @Override
        public void onClick(View v) {
            RegisteredCar car = binding.getCar();
            if( actionListener != null )
                actionListener.showChangeCarDialog(car);
        }
    }
}
