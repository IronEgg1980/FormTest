package com.example.formtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.time.LocalDate;
import java.util.UUID;

public class MainActivity2 extends AppCompatActivity {

    MyFormView myFormView;
    LocalDate localDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        localDate = LocalDate.now();
        myFormView = findViewById(R.id.matrixTestView);
        myFormView.setDate(localDate);
        myFormView.setOnClick(new MyFormView.OnClick() {
            @Override
            public void onClick(MyFormView.Cell cell) {
                Toast.makeText(MainActivity2.this, cell.value.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.newRow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myFormView.newRow();
                myFormView.scrollToBottom();
            }
        });
        findViewById(R.id.locate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myFormView.locateFocusCell();
            }
        });
        findViewById(R.id.left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                localDate = localDate.plusDays(-7);
                myFormView.setDate(localDate);
            }
        });
        findViewById(R.id.right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                localDate = localDate.plusDays(7);
                myFormView.setDate(localDate);
            }
        });
        findViewById(R.id.dele).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myFormView.getCurrentCell() !=null){
                    myFormView.deleRow(myFormView.getCurrentCell().getRowIndex());
                }
            }
        });
        findViewById(R.id.input).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyFormView.Cell cell = myFormView.getCurrentCell();
                if(cell!=null){
                    cell.value = UUID.randomUUID().toString();
                    if(cell.getColumnIndex() == 0)
                        myFormView.setCurrentCell(cell.getRowIndex() + 1,0);
                    else
                        myFormView.setCurrentCell(cell.getRowIndex(),cell.getColumnIndex() + 1);
                }
            }
        });
    }
}