package com.huanlezhang.aide;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class AboutMe {

    static public void showDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        String message = "Hello. My name is Huanle Zhang.\n" +
                "For more information, please go to my website:\n" +
                "https://www.huanlezhang.com";
        builder.setMessage(message).setTitle("About");
        builder.setPositiveButton("Return", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
