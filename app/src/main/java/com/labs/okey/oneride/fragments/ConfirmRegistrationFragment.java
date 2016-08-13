package com.labs.okey.oneride.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.labs.okey.oneride.R;
import com.labs.okey.oneride.model.User;

/**
 * @author Oleg Kleiman
 * created 12-Apr-15.
 */

public class ConfirmRegistrationFragment extends DialogFragment {

    User user;

    public ConfirmRegistrationFragment setUser(User user){
        this.user = user;
        return this;
    }

    public interface RegistrationDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog, User user);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    RegistrationDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mListener = (RegistrationDialogListener) activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_confirm_registration)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.onDialogPositiveClick(ConfirmRegistrationFragment.this,
                                ConfirmRegistrationFragment.this.user);
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
