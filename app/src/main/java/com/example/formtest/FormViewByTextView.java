package com.example.formtest;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FormViewByTextView extends ViewGroup {
    private final String TAG = "殷宗旺";

    private String mTitle = "";
    private TextView mTitleTextView;
    private int mTitleTextSize;
    private int mTitleTextColor;
    private int cellTextSize;
    private int cellTextColor;
    private int mBgColor = Color.parseColor("#cfcfcf");

    private GestureDetector  gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    private CellClickListener cellClickListener;

    private int mPadding = 50;
    private RectF contentBgRectF;
    private Paint contentBgPaint;
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

    public void setOnCellClickListener(CellClickListener cellClickListener) {
        this.cellClickListener = cellClickListener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()){
            case MotionEvent.ACTION_MOVE:
                return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getPointerCount() > 1)
            return scaleGestureDetector.onTouchEvent(event);
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int top = 0, bottom = top;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            Row row = (Row) getChildAt(i);
            MarginLayoutParams marginLayoutParams = (MarginLayoutParams) row.getLayoutParams();
            top = top + marginLayoutParams.topMargin;
            bottom = top + row.getMeasuredHeight();
            row.layout(l + marginLayoutParams.leftMargin, top, row.getMeasuredWidth() + marginLayoutParams.rightMargin, bottom);
            top = bottom;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    private void initial() {
        gestureDetector = new GestureDetector(getContext(),new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                scrollBy((int)distanceX,(int)distanceY);
                invalidate();
                return true;
            }
        });
        scaleGestureDetector = new ScaleGestureDetector(getContext(),new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                setPivotX(detector.getFocusX());
                setPivotY(detector.getFocusY());
                setScaleX(detector.getScaleFactor());
                setScaleY(detector.getScaleFactor());
                invalidate();
                return true;
            }
        });
        rows = new ArrayList<>();
        setBackground(new ColorDrawable(mBgColor));
    }

    public void setData() {
        for (int i = 0; i < 20; i++) {
            Row row = new Row(getContext(), i);
            MarginLayoutParams layoutParams = new MarginLayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int marginStart = mPadding;
            int marginTop = i == 0 ? mPadding : 0;
            int marginEnd = mPadding;
            int marginBottom = i == 19 ? mPadding : 0;
            layoutParams.setMargins(marginStart, marginTop, marginEnd, marginBottom);
            row.setLayoutParams(layoutParams);
            row.setBackground(new ColorDrawable(Color.WHITE));
            List<ICellValue> list = new ArrayList<>();
            for (int j = 0; j < 8; j++) {
                CellValue cellValue = new CellValue("行 " + (i + 1) + " 列 " + (j + 1));
                list.add(cellValue);
            }
            row.setData(list);

            addView(row);
        }
        requestLayout();
    }

    public interface CellClickListener {
        void onCellClicked(Cell cell);
    }

    public interface ICellValue {
        String getText();
    }

    public static class Cell extends androidx.appcompat.widget.AppCompatTextView {
        private int rowIndex, columnIndex;
        private ICellValue value;

        Cell(Context context) {
            super(context);
        }

        Cell(Context context, int rowIndex, int columnIndex) {
            super(context);
            this.rowIndex = rowIndex;
        }

        public void setValue(ICellValue value) {
            this.value = value;
            setText(value.getText());
            invalidate();
        }

        public ICellValue getValue() {
            return this.value;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof Cell) {
                Cell cell = (Cell) obj;
                return cell.columnIndex == this.columnIndex
                        && cell.rowIndex == this.rowIndex;
            }
            return false;
        }


    }

    public static class Row extends LinearLayout {
        private Cell[] cells;
        private int rowIndex;
        private int columnsCount;
        private int defaultCellWidth = 300, defaultCellHeight = 200, padding = 20;
        private SparseIntArray cellWidthArray;
        private CellClickListener cellClickListener;

        public int getRowIndex() {
            return rowIndex;
        }

        public int getColumnsCount() {
            return columnsCount;
        }

        public void setColumnsCount(int columnsCount) {
            this.columnsCount = columnsCount;
        }

        public Cell getCell(int index) {
            if (index < 0 || index >= columnsCount)
                return null;
            return cells[index];
        }

        private Row(Context context) {
            super(context);
        }

        public Row(Context context, int rowIndex) {
            super(context);
            this.rowIndex = rowIndex;
        }

        public void setClick(CellClickListener click){
            cellClickListener = click;
        }

        public void setData(List<ICellValue> list) {
            this.columnsCount = list.size();
            this.cells = new Cell[list.size()];
            for (int i = 0; i < columnsCount; i++) {
                cells[i] = new Cell(getContext(), rowIndex, i);
                ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(defaultCellWidth, defaultCellHeight);
                cells[i].setLayoutParams(layoutParams);
                cells[i].setPadding(padding, padding, padding, padding);
                cells[i].setMaxLines(3);
                cells[i].setEllipsize(TextUtils.TruncateAt.END);
                cells[i].setTextSize(dip2px(getContext(), 4));
                cells[i].setText(list.get(i).getText());
                addView(cells[i]);
            }
            requestLayout();
        }
        private int dip2px(Context context, int dip) {
            float scale = context.getResources().getDisplayMetrics().density;
            return (int) (dip * scale + 0.5f);
        }
    }
}
