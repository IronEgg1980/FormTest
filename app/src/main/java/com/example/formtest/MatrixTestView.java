package com.example.formtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MatrixTestView extends View {
    private static final String TAG = "殷宗旺";

    //定义 Cell点击接口
    public interface OnClick {
        void onClick(Cell cell);
    }

    // Row类
    public static class Row extends RectF {
        private int rowIndex;
        private int defaultCellWidth = 300;
        private int columnCount = 8;
        private Cell[] cells;

        public int getRowIndex() {
            return rowIndex;
        }

        public Row(int rowIndex, float left, float top, int height, int columnCount, SparseIntArray widthArray) {
            super();
            this.rowIndex = rowIndex;
            this.columnCount = columnCount;
            this.cells = new Cell[columnCount];
            float cellLeft = left;
            float cellRight = left;
            float cellBottom = top + height;
            for (int i = 0; i < columnCount; i++) {
                int cellWidth = widthArray.get(i, defaultCellWidth);
                cellRight = cellLeft + cellWidth;
                cells[i] = new Cell(this.rowIndex, i);
                cells[i].set(cellLeft, top, cellRight, cellBottom);
                cellLeft = cellRight;
            }
            set(left, top, cellRight, cellBottom);
        }

        public Cell getCell(int index) {
            return cells[index];
        }
    }

    // Cell类
    public static class Cell extends RectF {
        private int row, column;
        public Object value = "";

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }

        public Cell(int row, int column) {
            super();
            this.row = row;
            this.column = column;
            this.value = "第 " + (row + 1) + " 行 第 " + (column + 1) + " 列";
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof Cell) {
                Cell other = (Cell) obj;
                return other.row == this.row &&
                        other.column == this.column;
            }
            return false;
        }
    }

    // 惯性滚动 Runnable
    private class Flinger implements Runnable {
        int x = 0, y = 0;
        boolean isFinished = true;

        public void start(float velocityX, float velocityY) {
            autoScroller.abortAnimation();
            if (isFinished) {
                RectF firstRectF = getMatrixRectf(titleRectF);
                x = (int) firstRectF.left;
                y = (int) firstRectF.top;
                int minX = (int) (getWidth() - contentWidth * getMatrixScaleX() - mPadding);
                int maxX = (int) (mPadding);
                int minY = (int) (getHeight() - contentHeight * getMatrixScaleY() - mPadding);
                int maxY = (int) (mPadding);
                overScroller.fling(x, y, (int) velocityX, (int) velocityY, minX, maxX, minY, maxY);
            } else {
                overScroller.abortAnimation();
            }
            post(this);
        }

        @Override
        public void run() {
            if (!overScroller.computeScrollOffset()) {
                isFinished = true;
                return;
            }
            isFinished = false;
            int dx = overScroller.getCurrX() - x;
            int dy = overScroller.getCurrY() - y;
            constrainScoll(dx, dy);
            x = overScroller.getCurrX();
            y = overScroller.getCurrY();
            post(this);
        }
    }

    // 点击后自动滚动至可见位置 Runnable
    private class AutoScroller implements Runnable {
        float x, y;

        public void start(int x, int y, int dx, int dy) {
            autoScroller.abortAnimation();
            overScroller.abortAnimation();
            autoScroller.startScroll(x, y, dx, dy, 100);
            this.x = x;
            this.y = y;
            post(this);
        }

        @Override
        public void run() {
            if (autoScroller.computeScrollOffset()) {
                float _x = autoScroller.getCurrX();
                float _y = autoScroller.getCurrY();
                float dx = _x - x;
                float dy = _y - y;

                matrix.postTranslate(dx, dy);

                x = _x;
                y = _y;

                postInvalidate();
                post(this);
            }
        }
    }

    // 标记
    private boolean isFirstDraw = true;
    private float defaultScaleValue = 1f;
    private float lastClickX, lastClickY;
    private float[] m = new float[9]; // 记录matrix的值
    private Cell focusedCell;// 当前选中的cell

    // 使用到的各种尺寸
    private int mTitleHeight = 500;
    private float mPadding = 100;
    private float textSize = 30;
    private float titleTextSize = 300;
    private int rowHeight = 200;
    private int indicatorWidth = 10;
    private float contentWidth = 0, contentHeight = 0;

    // 自定义的文本或颜色，其他
    private String mTitle = "标题";
    private LocalDate startDay;
    private int weekOfYear;
    private int columnsCount = 9;
    private DateTimeFormatter dateTimeFormatter;

    // 自动滚动工具
    private OverScroller overScroller, autoScroller;
    private Flinger flinger;
    private AutoScroller scroller;

    // 滑动和缩放手势工具
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    // 点击接口
    private OnClick onClick;

    // 绘图相关工具
    private Paint bgPaint;
    private TextPaint textPaint;
    private Matrix matrix;

    // 内容
    private RectF titleRectF;
    private Row header;
    private List<Row> rows = new ArrayList<>();
    private RectF vIndicatorBg, hIndicatorBg, vIndicator, hIndicator; // 滚动条背景和滚动条的矩形框
    private float getTranslateX() {
        matrix.getValues(m);
        return m[2];
    }

    private float getTranslateY() {
        matrix.getValues(m);
        return m[5];
    }

    private float getMatrixScaleY() {
        matrix.getValues(m);
        return m[4];
    }

    private float getMatrixScaleX() {
        matrix.getValues(m);
        return m[Matrix.MSCALE_X];
    }

    private RectF getMatrixRectf(RectF src) {
        RectF rectF = new RectF();
        matrix.mapRect(rectF, src);
        return rectF;
    }

    private float dip2Px(float value) {
        return getResources().getDisplayMetrics().density * value;
    }

    private int getBaseLine(RectF rectF) {
        int centerY = (int) rectF.centerY();
        return (int) ((textPaint.descent() - textPaint.ascent()) / 2 - textPaint.descent()) + centerY;
    }

    public void setOnClick(OnClick onClick) {
        this.onClick = onClick;
    }

    public Row newRow() {
        int index = rows.size() - 1;
        Row lastRow;
        if (index < 0) {
            lastRow = header;
        } else {
            lastRow = rows.get(index);
        }
        Row row = new Row(index + 1, lastRow.left, lastRow.bottom, rowHeight, columnsCount, new SparseIntArray());
        contentHeight += rowHeight;
        rows.add(row);
        invalidate();
        return row;
    }

    public void deleRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex > rows.size() - 1) {
            return;
        }
        rows.remove(rowIndex);
        for(int i = rowIndex;i<rows.size();i++){
            rows.get(i).offset(0,-rowHeight);
        }
        contentHeight -= rowHeight;
        focusedCell = null;
        invalidate();
    }

    public void setCurrentCell(Cell cell) {
        focusedCell = cell;
        autoScroll();
    }

    public Cell getCurrentCell(){
        return this.focusedCell;
    }

    public void setDate(LocalDate localDate) {
        weekOfYear = localDate.get(WeekFields.ISO.weekOfWeekBasedYear());
        startDay = localDate.with(DayOfWeek.MONDAY);
        for (int i = 1; i < 8; i++) {
            header.getCell(i).value = localDate.plusDays(i - 1).format(dateTimeFormatter);
        }
        invalidate();
    }

    public void setCurrentCell(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex > rows.size() - 1 || columnIndex < 0 || columnIndex >= columnsCount)
            return;
        setCurrentCell(rows.get(rowIndex).getCell(columnIndex));
    }

    public int getColumnsCount(){
        return this.columnsCount;
    }

    public int getRowCount(){
        return rows.size();
    }

    // 第一次显示时，缩放到适合的宽度
    private void firstDraw() {
        isFirstDraw = false;
        defaultScaleValue = (getWidth() - mPadding * 2 - indicatorWidth) / contentWidth;
        matrix.setScale(defaultScaleValue, defaultScaleValue);
        matrix.postTranslate(mPadding, 0);
    }

    // 初始化
    private void initial(Context context) {
        overScroller = new OverScroller(getContext(), new DecelerateInterpolator());
        autoScroller = new OverScroller(getContext(), new LinearInterpolator());
        flinger = new Flinger();
        scroller = new AutoScroller();

        dateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 eeee", Locale.CHINA);

        header = new Row(-1, 0, mTitleHeight, rowHeight, columnsCount, new SparseIntArray());
        header.getCell(0).value = "姓名";
        header.getCell(8).value = "备注";
        titleRectF = new RectF(header.left, 0, header.right, mTitleHeight);

        contentHeight = header.height() + titleRectF.height();
        contentWidth = header.width();

        vIndicatorBg = new RectF();
        hIndicatorBg = new RectF();
        vIndicator = new RectF();
        hIndicator = new RectF();

        matrix = new Matrix();

        bgPaint = new Paint();
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(Color.WHITE);

        textPaint = new TextPaint();
        textPaint.setTextSize(textSize);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                lastClickX = e.getX();
                lastClickY = e.getY();
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float dx = e2.getX() - lastClickX;
                float dy = e2.getY() - lastClickY;
                constrainScoll(dx, dy);
                lastClickX = e2.getX();
                lastClickY = e2.getY();
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                flinger.start(velocityX, velocityY);
                return super.onFling(e1, e2, velocityX, velocityY);
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                onSigleTap(e);
                return super.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (getMatrixScaleX() == 1f)
                    matrix.setScale(defaultScaleValue, defaultScaleValue);
                else
                    matrix.postScale(1f / getMatrixScaleX(), 1f / getMatrixScaleX());
                invalidate();
                return super.onDoubleTap(e);
            }
        });
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                matrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                invalidate();
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

            }
        });
    }

    public MatrixTestView(Context context) {
        super(context);
        initial(context);
    }

    public MatrixTestView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initial(context);
    }

    private void autoScroll() {
        if (focusedCell == null) {
            invalidate();
            return;
        }
        RectF rectF = getMatrixRectf(focusedCell);
        int dx = 0, dy = 0;
        if (rectF.left < 0) {
            dx = (int) (0 - rectF.left);
        }
        if (rectF.right > getWidth() - indicatorWidth) {
            dx = (int) (getWidth() - indicatorWidth - rectF.right);
        }
        if (rectF.top < header.height() * getMatrixScaleY()) {
            dy = (int) (header.height() * getMatrixScaleY() - rectF.top);
        }
        if (rectF.bottom > getHeight() - indicatorWidth) {
            dy = (int) (getHeight() - indicatorWidth - rectF.bottom);
        }
        scroller.start((int) rectF.left, (int) rectF.top, dx, dy);
    }

    private boolean isCellVisible(@NonNull RectF rectF) {
        return rectF.right > 0 && rectF.left < getWidth();
    }

    private boolean isRowVisible(@NonNull RectF rectF) {
        return rectF.bottom > 0 && rectF.top < getHeight();
    }

    private void onSigleTap(MotionEvent e) {
        RectF rectF = new RectF();
        for (int i = 0; i < rows.size(); i++) {
            matrix.mapRect(rectF, rows.get(i));
            if (rectF.contains(lastClickX, lastClickY)) {
                for (Cell cell : rows.get(i).cells) {
                    matrix.mapRect(rectF, cell);
                    if (rectF.contains(lastClickX, lastClickY)) {
                        if (focusedCell != cell)
                            focusedCell = cell;
                        else
                            focusedCell = null;
                        onClick.onClick(cell);
                        autoScroll();
                        break;
                    }
                }
                break;
            }
        }
    }

    private float[] computeScrollRange() {
        float[] floats = new float[2];
        floats[0] = contentWidth * getMatrixScaleX() - getWidth();
        if (floats[0] < 0)
            floats[0] = 0;

        floats[1] = contentHeight * getMatrixScaleY() - getHeight();
        if (floats[1] < 0)
            floats[1] = 0;
        return floats;
    }

    private void constrainScoll(float dx, float dy) {
        float leftEdge = mPadding,
                topEdge = mPadding,
                rightEdge = getWidth() - mPadding,
                bottomEdge = getHeight() - mPadding;
        RectF firstRow = getMatrixRectf(titleRectF);
        if (dx > 0 && firstRow.left + dx > leftEdge) {
            dx = leftEdge - firstRow.left;
        } else if (dx < 0) {
            if (firstRow.right < rightEdge) {
                dx = 0;
            } else if (firstRow.right + dx < rightEdge) {
                dx = rightEdge - firstRow.right;
            }
        }
        if (dy > 0 && firstRow.top + dy > topEdge) {
            dy = topEdge - firstRow.top;
        } else if (dy < 0) {
            RectF lastRow = rows.isEmpty() ? getMatrixRectf(header) : getMatrixRectf(rows.get(rows.size() - 1));
            if (lastRow.bottom < bottomEdge) {
                dy = 0;
            } else if (lastRow.bottom + dy < bottomEdge) {
                dy = bottomEdge - lastRow.bottom;
            }
        }
        matrix.postTranslate(dx, dy);
        invalidate();
    }

    private void drawFixedColumn(int index, Canvas canvas) {
        if (rows.isEmpty() || index < 0 || index >= rows.get(0).columnCount)
            return;
        RectF headerRectF = getMatrixRectf(header.getCell(index));
        RectF lastRectF = getMatrixRectf(rows.get(rows.size() - 1));
        float top = Math.max(0, headerRectF.top);
        float left = Math.min(0, Math.abs(headerRectF.left) - headerRectF.width());
        if (headerRectF.left < -mPadding) {
            bgPaint.setColor(getResources().getColor(R.color.colorAccent, null));
            bgPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(left, top, left + headerRectF.width(), lastRectF.bottom, bgPaint);

            textPaint.setTextSize(textSize * getMatrixScaleX());
            textPaint.setStrokeWidth(2f);
            for (Row row : rows) {
                Cell cell1 = row.getCell(index);
                RectF rectF1 = getMatrixRectf(cell1);
                if (rectF1.bottom > top && rectF1.top < getHeight()) {
                    rectF1.offsetTo(left, rectF1.top);
                    canvas.drawLine(rectF1.left, rectF1.bottom, rectF1.right, rectF1.bottom, textPaint);
                    canvas.drawText(cell1.value.toString(), rectF1.centerX(), getBaseLine(rectF1), textPaint);
                }
            }
            bgPaint.setColor(getResources().getColor(R.color.colorHeaderBg, null));
            RectF rectF2 = new RectF(left, top, headerRectF.width() + 2, top + headerRectF.height());
            canvas.drawRect(rectF2, bgPaint);
            canvas.drawText(header.getCell(index).value.toString(), rectF2.centerX(), getBaseLine(rectF2), textPaint);
        }
    }

    private void drawHeader(@NonNull Canvas canvas) {
        RectF rectF = getMatrixRectf(header);
        float top = Math.max(0, rectF.top);
        rectF.set(rectF.left, top, rectF.right, top + rectF.height());
        textPaint.setTextSize(textSize * getMatrixScaleX());
        textPaint.setStrokeWidth(5f);
        bgPaint.setColor(getResources().getColor(R.color.colorHeaderBg, null));
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(rectF, bgPaint);
        for (int i = 0; i < header.columnCount; i++) {
            RectF cellRectF = getMatrixRectf(header.getCell(i));
            cellRectF.set(cellRectF.left, rectF.top, cellRectF.right, rectF.bottom);
            canvas.drawText(header.getCell(i).value.toString(), cellRectF.centerX(), getBaseLine(cellRectF), textPaint);
        }
    }

    private void drawIndicator(Canvas canvas) {
        if (rows.isEmpty())
            return;
        RectF first = getMatrixRectf(rows.get(0));
        float[] ranges = computeScrollRange();
        bgPaint.setStyle(Paint.Style.FILL);
        if (ranges[0] > 0) {
            hIndicatorBg.set(0, getHeight() - indicatorWidth, getWidth() - indicatorWidth, getHeight());
            float scaleX = getWidth() / (contentWidth * getMatrixScaleX());
            float indicatorSize = getWidth() * scaleX;
            float left = (hIndicatorBg.width() - indicatorSize) * Math.abs(first.left > 0 ? 0 : first.left) / ranges[0];
            hIndicator.set(left, getHeight() - indicatorWidth, left + indicatorSize, getHeight());
            bgPaint.setColor(getResources().getColor(R.color.colorIndicatorBg, null));
            canvas.drawRect(hIndicatorBg, bgPaint);
            bgPaint.setColor(getResources().getColor(R.color.colorIndicator, null));
            canvas.drawRect(hIndicator, bgPaint);
        }

        if (ranges[1] > 0) {
            vIndicatorBg.set(getWidth() - indicatorWidth, 0, getWidth(), getHeight());
            float scaleY = getHeight() / (contentHeight * getMatrixScaleX());
            float indicatorSize = getHeight() * scaleY;
            float top = (vIndicatorBg.height() - indicatorSize) * Math.abs(first.top > 0 ? 0 : first.top) / ranges[1];
            vIndicator.set(getWidth() - indicatorWidth, top, getWidth(), top + indicatorSize);
            bgPaint.setColor(getResources().getColor(R.color.colorIndicatorBg, null));
            canvas.drawRect(vIndicatorBg, bgPaint);
            bgPaint.setColor(getResources().getColor(R.color.colorIndicator, null));
            canvas.drawRect(vIndicator, bgPaint);
        }
    }

    private void drawCell(@NonNull Cell cell, @NonNull Canvas canvas) {
        bgPaint.setColor(Color.WHITE);
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(cell, bgPaint);
        textPaint.setTextSize(cell == focusedCell ? textSize * 1.3f : textSize);
        textPaint.setStrokeWidth(cell == focusedCell ? 5f : 2f);
        canvas.drawText(cell.value.toString(), cell.centerX(), getBaseLine(cell), textPaint);
        if(cell == focusedCell){
            bgPaint.setColor(getResources().getColor(R.color.colorAccent,null));
            bgPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect( rows.get(cell.row),bgPaint);
        }
    }

    private void drawLines(@NonNull Canvas canvas) {
        if (rows.isEmpty())
            return;
        Row firstRow = rows.get(0);
        RectF rowRectF = getMatrixRectf(firstRow);
        RectF lastRowRectF = getMatrixRectf(rows.get(rows.size() - 1));
        textPaint.setStrokeWidth(2f);
        float hLineStartX = rowRectF.left;
        float hLineEndX = rowRectF.right;

        float vLineStartY = rowRectF.top;
        float vLineEndY = lastRowRectF.bottom;

        for (Row row : rows) {
            RectF rectF = getMatrixRectf(row);
            if (isRowVisible(rectF)) {
                float startX = Math.max(0, hLineStartX);
                float endX = Math.min(getWidth(), hLineEndX);
                float y = rectF.bottom;
                if (row.rowIndex == 0)
                    canvas.drawLine(startX, rectF.top, endX, rectF.top, textPaint);
                canvas.drawLine(startX, y, endX, y, textPaint);
            }
        }

        for (Cell cell : firstRow.cells) {
            RectF rectF = getMatrixRectf(cell);
            float startY = Math.max(0, vLineStartY);
            float endY = Math.min(getHeight(), vLineEndY);
            if (isCellVisible(rectF)) {
                if (cell.column == 0)
                    canvas.drawLine(rectF.left, vLineStartY, rectF.left, vLineEndY, textPaint);
                canvas.drawLine(rectF.right, startY, rectF.right, endY, textPaint);
            }
        }
    }

    private void drawTitle(@NonNull Canvas canvas) {
        bgPaint.setColor(Color.WHITE);
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(titleRectF, bgPaint);
        textPaint.setTextSize(titleTextSize);
        textPaint.setStrokeWidth(10f);
        canvas.drawText(mTitle, titleRectF.centerX(), getBaseLine(titleRectF), textPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isFirstDraw)
            firstDraw();
        canvas.drawColor(Color.parseColor("#efefef"));
        canvas.save();
        canvas.concat(matrix);
        drawTitle(canvas);
        for (Row row : rows) {
            if (isRowVisible(getMatrixRectf(row))) {
                for (Cell cell : row.cells) {
                    if (isCellVisible(getMatrixRectf(cell))) {
                        drawCell(cell, canvas);
                    }
                }
            }
        }
        canvas.restore();
        drawLines(canvas);
        drawHeader(canvas);
        drawFixedColumn(0, canvas);
        drawIndicator(canvas);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        performClick();
        if (event.getPointerCount() > 1) {
            return scaleGestureDetector.onTouchEvent(event);
        }
        gestureDetector.onTouchEvent(event);
        return true;
    }
}
