package com.example.formtest;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseIntArray;
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

    public void setData(List<ICellValue> list){

    }

    public interface CellClickListener {
        void onCellClicked(Cell cell);
    }

    public interface ICellValue {
        String getText();
        Object getObject();
    }

    public static class Cell extends androidx.appcompat.widget.AppCompatTextView {
        private int rowIndex, columnIndex;
        private ICellValue value;

        Cell(Context context) {
            super(context);
        }

        Cell(Context context, int width, int height, int padding, int gravity, int rowIndex, int columnIndex) {
            super(context);
            this.rowIndex = rowIndex;
            this.columnIndex = columnIndex;
            setWidth(width);
            setHeight(height);
            setPadding(padding, padding, padding, padding);
            setGravity(gravity);
            setMaxLines(3);
            setEllipsize(TextUtils.TruncateAt.END);
            setTextSize(dip2px(getContext(),16));
        }

        public void setValue(ICellValue value) {
            this.value = value;
            setText(value.getText());
        }

        public Object getObject() {
            return this.value.getObject();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
           if(obj instanceof Cell) {
               Cell cell = (Cell) obj;
               return cell.columnIndex == this.columnIndex
                       && cell.rowIndex == this.rowIndex;
           }
           return false;
        }

        private int dip2px(Context context, int dip) {
            float scale = context.getResources().getDisplayMetrics().density;
            return (int) (dip * scale + 0.5f);
        }
    }

    public static class Row extends LinearLayout {
        private Cell[] cells;
        private int rowIndex;
        private int columnsCount;
        private int defaultCellWidth = 150,padding = 20;
        private SparseIntArray cellWidthArray;

        public int getRowIndex() {
            return rowIndex;
        }

        public int getColumnsCount() {
            return columnsCount;
        }

        public void setColumnsCount(int columnsCount) {
            this.columnsCount = columnsCount;
        }

        public Cell getCell(int index){
            if(index < 0 || index >= columnsCount)
                return null;
            return cells[index];
        }
        
        private Row(Context context){
            super(context);
        }

        public Row(Context context,int rowIndex,int columnsCount,int rowHeight,SparseIntArray widthArry) {
            super(context);
            initial(rowIndex,columnsCount,rowHeight);
            this.rowIndex = rowIndex;
            this.cellWidthArray = widthArry;
        }

        private void initial(int rowIndex, int columnsCount,int rowHeight) {
            this.columnsCount = columnsCount;
            this.cells = new Cell[columnsCount];
            for(int i = 0;i<columnsCount;i++){
                cells[i] = new Cell(getContext(),cellWidthArray.get(i,defaultCellWidth),rowHeight,padding,Gravity.START,rowIndex,i);
                addView(cells[i]);
            }
        }
    }
}
