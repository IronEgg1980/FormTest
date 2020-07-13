package com.example.formtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity2 extends AppCompatActivity {

    MatrixTestView matrixTestView;
    TextView infoTV;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        infoTV = findViewById(R.id.showInfoTV);
        matrixTestView = findViewById(R.id.matrixTestView);
        matrixTestView.setOnClick(new MatrixTestView.OnClick() {
            @Override
            public void onClick(MatrixTestView.CellTest cellTest) {
                infoTV.setText(cellTest.value.toString());
            }
        });
    }
}