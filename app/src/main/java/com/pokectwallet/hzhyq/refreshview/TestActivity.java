package com.pokectwallet.hzhyq.refreshview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class TestActivity extends AppCompatActivity {

    private LinearLayout ll;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        ll = (LinearLayout) findViewById(R.id.ll);
    }
    public void addMargin(View view){
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ll.getLayoutParams();
        params.topMargin += 20;
        ll.setLayoutParams(params);
        ((ViewGroup)ll.getParent()).invalidate();
        Toast.makeText(this,"top margin "+((RelativeLayout.LayoutParams) ll.getLayoutParams()).topMargin,Toast.LENGTH_SHORT).show();
    }
}
