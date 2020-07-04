package com.example.formtest;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FormViewByTextView extends ViewGroup {
    private final String TAG = "殷宗旺";
    private List<Row> rows;

    public FormViewByTextView(@NonNull Context context) {
        super(context);
        initial();
    }

    public FormViewByTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initial();
    }

    public FormViewByTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initial();
    }

    public FormViewByTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initial();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    private void initial() {
        rows = new ArrayList<>();
    }


    public interface cellClickListener{
        void onCellClicked(Cell cell);
    }

    public static class Cell extends androidx.appcompat.widget.AppCompatTextView {
        private int rowIndex, columnIndex;
        private Object value;

        Cell(Context context){
            super(context);
        }

        Cell(Context context,int width,int height,int padding,int gravity,int rowIndex,int columnIndex){
            super(context);
            this.rowIndex = rowIndex;
            this.columnIndex =columnIndex;
            setWidth(width);
            setHeight(height);
            setPadding(padding,padding,padding,padding);
            setGravity(gravity);
        }

        public void setValue(Object value){
            this.value = value;
            setText(value.toString());
        }

        public Object getValue(){
            return this.value;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj == null)
                return false;
            if (!(obj instanceof FormViewByTextView.Cell))
                return false;
            Cell cell = (Cell) obj;
            return cell.columnIndex == this.columnIndex
                    && cell.rowIndex == this.rowIndex;
        }
    }

    public static class Row extends LinearLayout {
        private Cell[] cells;
        private int columnsCount;
        public Row(Context context,int columnsCount) {
            super(context);

        }
        private void initial(int columnsCount){
            this.columnsCount = columnsCount;
        }

    }
}
