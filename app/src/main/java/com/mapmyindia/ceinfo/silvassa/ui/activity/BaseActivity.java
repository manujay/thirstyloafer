package com.mapmyindia.ceinfo.silvassa.ui.activity;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

/**
 * Created by ceinfo on 27-02-2017.
 */

public abstract class BaseActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    public static void showToast(Context context, String mesg) {
        Toast.makeText(context, mesg, Toast.LENGTH_SHORT).show();
    }

    public static void showSnackBar(View view, String mesg) {
        Snackbar.make(view, mesg, Snackbar.LENGTH_SHORT).show();
    }


    public Toolbar getToolbar() {
        return mToolbar;
    }

    public void setToolbar(Toolbar mToolbar) {
        this.mToolbar = mToolbar;
    }

    public abstract void setTitle(String mTitle);
}