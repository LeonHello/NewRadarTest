package com.example.newradartest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;

public class WelcomeActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent toNext = new Intent(WelcomeActivity.this, StartActivity.class);
                startActivity(toNext);
                WelcomeActivity.this.finish();
                finish();
            }
        }, 2000);

    }
}
