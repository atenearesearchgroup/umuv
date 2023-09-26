package com.uMuv.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ViewPositionActivity extends AppCompatActivity {
    private Button back;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_position);

        back = findViewById(R.id.goBackB);
        textView = findViewById(R.id.positionView);
    }

    public void goBack(View v){
        finish();
    }
}