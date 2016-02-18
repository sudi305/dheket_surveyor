package com.bgs.dheket.surveyor;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.esri.android.map.LocationDisplayManager;
import com.facebook.login.LoginManager;

/**
 * Created by SND on 12/02/2016.
 */
public class FormAddNewLocationActivity extends AppCompatActivity {

    android.support.v7.app.ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_add_new_location);

        actionBar = getSupportActionBar();

        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        //actionBar.setHomeAsUpIndicator(R.drawable.logo);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("Dheket for Surveyor");
        actionBar.setSubtitle("New Location");
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#5991ff")));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                back_to_previous_screen();
                return super.onOptionsItemSelected(item);

            case R.id.action_logout:
                logout_user();
                return super.onOptionsItemSelected(item);

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    public void logout_user(){
        String logout = getResources().getString(com.facebook.R.string.com_facebook_loginview_log_out_action);
        String cancel = getResources().getString(com.facebook.R.string.com_facebook_loginview_cancel_action);
        String message= "Are you sure?";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setCancelable(true)
                .setPositiveButton(logout, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        LoginManager.getInstance().logOut();
                        Intent logout_user_fb = new Intent(FormAddNewLocationActivity.this, FormLoginActivity.class);
                        startActivity(logout_user_fb);
                        finish();
                    }
                })
                .setNegativeButton(cancel, null);
        builder.create().show();
    }

    @Override
    public void onBackPressed() {
        back_to_previous_screen();
    }

    public void back_to_previous_screen(){
        Intent intent = new Intent(FormAddNewLocationActivity.this,MainActivity.class);
        startActivity(intent);
        finish();
    }
}
