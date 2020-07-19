package com.example.formtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity2 extends AppCompatActivity {
    class Entity {
        String name;
        String[] duties = new String[7];
        String note;
    }


    MyFormView myFormView;
    LocalDate localDate;
    List<Entity> entities;
    int currentIndex = 1;
    boolean b = false;

    private Entity createEntity() {
        Entity entity = new Entity();
        entity.name = "名称: " + currentIndex;
        String duty = "班次: " + currentIndex;
        for (int j = 1; j <= 7; j++) {
            entity.duties[j - 1] = duty + " - " + j;
        }
        entity.note = "备注: " + currentIndex;
        currentIndex++;
        return entity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        entities = new ArrayList<>();
        for (int i = 1; i < 3; i++) {
            entities.add(createEntity());
        }

        localDate = LocalDate.now();
        myFormView = findViewById(R.id.matrixTestView);
        myFormView.setDataSet(entities, new MyFormView.IBindData() {
            @Override
            public void onBindData(MyFormView.Row row) {
                Entity entity = entities.get(row.rowIndex);
                row.cells[0].value = entity.name;
                for (int i = 1; i <= 7; i++) {
                    row.cells[i].value = entity.duties[i - 1];
                }
                row.cells[8].value = entity.note;
            }
        });
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
                entities.add(createEntity());
                myFormView.notifyDataAdd();
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
//                localDate = localDate.plusDays(7);
//                myFormView.setDate(localDate);
                b = !b;
                myFormView.setColumnVisible(2, b);
            }
        });
        findViewById(R.id.dele).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myFormView.getCurrentCell() != null) {
                    myFormView.notifyDataDele();
                }
            }
        });
        findViewById(R.id.input).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyFormView.Cell cell = myFormView.getCurrentCell();
                if (cell != null) {
                    cell.value = UUID.randomUUID().toString();
                    if (cell.getColumnIndex() == 0) {
                        if (cell.getRowIndex() == myFormView.getRowCount() - 1) {
                            entities.add(createEntity());
                            myFormView.notifyDataAdd();
                            myFormView.scrollToBottom();
                        }
                        myFormView.setCurrentCell(cell.getRowIndex() + 1, 0);
                    } else {
                        if (cell.getColumnIndex() == myFormView.getColumnsCount() - 1 && cell.getRowIndex() < myFormView.getRowCount() - 1) {
                            myFormView.setCurrentCell(cell.getRowIndex() + 1, 1);
                        } else {
                            myFormView.setCurrentCell(cell.getRowIndex(), cell.getColumnIndex() + 1);
                        }
                    }
                }
            }
        });
    }
}