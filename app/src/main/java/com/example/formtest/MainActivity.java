package com.example.formtest;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private List<Person> personList;
    private List<Duty> dutyList;
    private BaseAdapter<Person> personAdapter;
    private BaseAdapter<Duty> dutyAdapter;
    private BasePopWindow personListPop, dutyListPop;
    private FormView formView;

    private void initial() {
        formView = findViewById(R.id.formView);

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FormView.Row row = formView.addRow();
                formView.setInsideVisible(row.getCell(0));
            }
        });

        personList = new ArrayList<>();
        for (int i = 1; i < 21; i++) {
            personList.add(new Person("人员" + i, 22 + i));
        }
        personList.add(new Person("重选",999));

        dutyList = new ArrayList<>();
        for (int i = 1; i < 31; i++) {
            Duty duty = new Duty();
            duty.setName("班次班次班次班次班次班次" +
                    "班次班次班次班次班次班次" + i);
            duty.setAdd(i % 9 == 0);
            duty.setPerAddValue(30 * i);
            duty.setOffDay(i % 11 == 0);
            duty.setOffDayValue(1);
            dutyList.add(duty);
        }

        personAdapter = new BaseAdapter<Person>(R.layout.recyclerview_item, personList) {
            @Override
            public void bindData(final BaseViewHolder baseViewHolder, final Person data) {
                Drawable drawable = data.getAge() == 999?getDrawable(R.drawable.ic_close):getDrawable(R.drawable.ic_person3);
                baseViewHolder.setImage(R.id.ico, drawable);
                baseViewHolder.setText(R.id.text, data.getName());
                baseViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onPersonAdapterClick(baseViewHolder.getAbsoluteAdapterPosition());
                    }
                });
            }
        };
        dutyAdapter = new BaseAdapter<Duty>(R.layout.recyclerview_item, dutyList) {
            @Override
            public void bindData(final BaseViewHolder baseViewHolder, final Duty data) {
                baseViewHolder.setImage(R.id.ico, getDrawable(R.drawable.ic_duty));
                baseViewHolder.setText(R.id.text, data.getName());
                baseViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onDutyAdapterClick(baseViewHolder.getAbsoluteAdapterPosition());
                    }
                });
            }
        };

        personListPop = new BasePopWindow(this, personAdapter);
        personListPop.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                formView.cancelFocus();
            }
        });

        dutyListPop = new BasePopWindow(this, dutyAdapter);
        dutyListPop.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                formView.cancelFocus();
            }
        });

        formView.setOnCellClickListener(new FormView.OnCellClickListener() {
            @Override
            public void onClick(final FormView.Cell cell, final FormView.Row row) {
                if (cell.columnIndex == 0) {
                    if (dutyListPop.isShowing())
                        dutyListPop.dismiss();
                    if (personListPop.isShowing()) {
                        personListPop.update(formView);
                    } else {
                        personListPop.show(formView);
                    }
                } else if (cell.columnIndex == 7) {
                    Toast.makeText(MainActivity.this, "暂未完成", Toast.LENGTH_SHORT).show();
                } else {
                    if (personListPop.isShowing())
                        personListPop.dismiss();
                    if (dutyListPop.isShowing()) {
                        dutyListPop.update(formView);
                    } else {
                        dutyListPop.show(formView);
                    }
                }
            }
        });
    }

    private void onPersonAdapterClick(int index) {
        FormView.Cell cell0 = formView.getCurrentCell();
        if(index == personAdapter.getItemCount() - 1){
            if (cell0.getValue() instanceof Person) {
                personList.add(index, (Person) cell0.getValue());
            }
            cell0.setValue("");
            formView.setCurrentCell(cell0);
        }else {
            Person person = personList.remove(index);
            if (cell0.getValue() instanceof Person) {
                personList.add(index, (Person) cell0.getValue());
            }
            cell0.setValue(person);
            if (cell0.rowNumber < formView.getRowCount() - 1) {
                FormView.Cell cell1 = formView.getCell(cell0.rowNumber + 1, 0);
                formView.setCurrentCell(cell1);
                formView.setInsideVisible(cell1);
                personListPop.update(formView);
            } else {
                personListPop.dismiss();
            }
        }
        personAdapter.notifyDataSetChanged();
    }

    private void onDutyAdapterClick(int index) {
        FormView.Cell cell0 = formView.getCurrentCell();
        cell0.setValue(dutyList.get(index));
        FormView.Cell cell1 = null;
        if (cell0.columnIndex < 6) {
            cell1 = formView.getCell(cell0.rowNumber, cell0.columnIndex + 1);
        } else if (cell0.rowNumber < formView.getRowCount() - 1) {
            cell1 = formView.getCell(cell0.rowNumber + 1, 1);
        }

        if (cell1 == null) {
            dutyListPop.dismiss();
        } else {
            formView.setCurrentCell(cell1);
            formView.setInsideVisible(cell1);
            dutyListPop.update(formView);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initial();
    }
}
