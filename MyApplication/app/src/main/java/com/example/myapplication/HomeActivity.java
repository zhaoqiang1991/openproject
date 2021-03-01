package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;

import com.example.buniffer.ButterKnife;
import com.example.butter_annotation.BindView;

public class HomeActivity extends AppCompatActivity {


    @BindView(R.id.home_id)
    Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        button = findViewById(R.id.home_id);
        ButterKnife.bind(this);
        button.setText("homeactivity");
    }
}
