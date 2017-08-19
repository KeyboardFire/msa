package com.keyboardfire.msa;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class LoginDialog extends DialogFragment
    implements DialogInterface.OnClickListener {

    // https://possiblemobile.com/2013/05/layout-inflation-as-intended/
    @android.annotation.SuppressLint("InflateParams")
    @Override public Dialog onCreateDialog(Bundle state) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater()
            .inflate(R.layout.login_dialog, null);
        builder.setView(view).setPositiveButton("LOGIN", this);
        return builder.create();
    }

    @Override public void onClick(DialogInterface dialog, int underscore) {
        ((MainActivity) getActivity()).setCredentials(
            "\"Username\":\"" +
            ((TextView) getDialog().findViewById(R.id.username)).getText() +
            "\",\"Password\":\"" +
            ((TextView) getDialog().findViewById(R.id.password)).getText() +
            "\"");
    }

}
