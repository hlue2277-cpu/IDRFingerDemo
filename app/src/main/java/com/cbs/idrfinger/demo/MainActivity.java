package com.cbs.idrfinger.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public Context getContext() {
        return this.getApplicationContext();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void OnBnUsb(View view) {
        startActivity(new Intent(this,IDUMainActivity.class));
//        startActivity(new Intent(this, TestMainActivity.class));
    }

    public void OnBnSerial(View view) {
        startActivity(new Intent(this, IDSMainActivity.class));
//        startActivity(new Intent(this, SerialMainActivity.class));
    }


}
