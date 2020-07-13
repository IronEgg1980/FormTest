package com.example.formtest;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
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
import android.widget.OverScroller;

import androidx.annotation.Nullable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MatrixTestView extends View {
    public interface OnClick {
        void onClick(CellTest cellTest);
    }

    public static class RowTest {
        private int row;
        private int height, width;
        private int defaultCellWidth = 300;
        private int columnCount;
        private CellTest[] cells;
        private RectF rectF;

        public RectF getRectF() {
            return rectF;
        }

        public int getRow() {
            return row;
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }

        public RowTest(int row, int top, int left, int height, int columnCount, int cellMargin, SparseIntArray widthArray) {
            this.row = row;
            this.height = height;
            this.columnCount = columnCount;
            this.cells = new CellTest[columnCount];
            width = cellMargin * (columnCount + 1);
            int cellLeft = cellMargin + left;
            for (int i = 0; i < columnCount; i++) {
                int cellWidth = widthArray.get(i, defaultCellWidth);
                RectF rectF = new RectF(cellLeft, top, cellLeft + cellWidth, top + height);
                cells[i] = new CellTest(rectF, row, i);
                width += cellWidth;
                cellLeft += cellWidth + cellMargin;
            }
            rectF = new RectF(left, top, left + width, top + height);
        }

        public CellTest getCell(int index) {
            return cells[index];
        }
    }

    public static class CellTest {
        private int row, column;
        private RectF rectF;
        public Object value = "";

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }

        public RectF getRectF() {
            return rectF;
        }

        public CellTest(RectF rectF, int row, int column) {
            this.rectF = rectF;
            this.row = row;
            this.column = column;
            this.value = "第 " + (row + 1) + " 行 第 " + (column + 1) + " 列";
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof CellTest) {
                CellTest other = (CellTest) obj;
                return other.row == this.row &&
                        other.column == this.column;
            }
            return false;
        }
    }

    private class Flinger implements Runnable {
        int x = 0, y = 0;
        boolean isFinished = true;

        public void start(MotionEvent e1, float velocityX, float velocityY) {
            if (isFinished) {
                RectF firstRectF = getMatrixRectf(rows.get(0).rectF);
                x = (int) firstRectF.left;
                y = (int) firstRectF.top;
                int minX = (int) (getWidth() - contentWidth * getMatrixScaleX());
                int maxX = (int) (rowMargin * getMatrixScaleX());
                int minY = (int) (getHeight() - contentHeight * getMatrixScaleY());
                int maxY = (int) (header.height * getMatrixScaleY());
                overScroller.fling(x, y, (int) velocityX, (int) velocityY, minX, maxX, minY, maxY, 20, 20);
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
            matrix.postTranslate(dx, dy);
            x = overScroller.getCurrX();
            y = overScroller.getCurrY();
            postInvalidate();
            post(this);
        }
    }

    private static final String TAG = "殷宗旺";
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private OverScroller overScroller;
    private OnClick onClick;
    private boolean isFirstDraw = true;

    private List<RowTest> rows = new ArrayList<>();
    private RowTest header;
    private Paint cellBgPaint, numberBgPaint;
    private TextPaint textPaint;
    private float textSize;
    private int rowMargin = 20, rowHeight = 200, indicatorWidth = 10, cellMargin = 10;
    private List<RectF> numbers = new ArrayList<>();

    private RectF vIndicatorBg, hIndicatorBg, vIndicator, hIndicator;
    private Matrix matrix;
    private CellTest focusedCell;
    private Flinger flinger;
    private float contentWidth = 0, contentHeight = 0, defaultScaleValue = 1f;
    private float lastClickX, lastClickY;

    private float[] m = new float[9];

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

    private void firstDraw() {
        isFirstDraw = false;
        float totalWidth = rows.get(0).width + rowMargin * 2;
        defaultScaleValue = getWidth() / totalWidth;
        matrix.setScale(defaultScaleValue, defaultScaleValue);
    }


    private void initial(Context context) {
        overScroller = new OverScroller(getContext(), new DecelerateInterpolator());
        flinger = new Flinger();
        header = new RowTest(-1, 0, rowMargin, rowHeight, 8, 10, new SparseIntArray());
        LocalDate localDate = LocalDate.now().with(DayOfWeek.MONDAY);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 eeee", Locale.CHINA);
        for (int i = 1; i < 8; i++) {
            header.getCell(i).value = localDate.plusDays(i - 1).format(dateTimeFormatter);
        }
        header.getCell(0).value = "姓名";

        int top = rowMargin + header.height, left = rowMargin;
        for (int i = 1; i < 100; i++) {
            RectF rectF = new RectF(0, top, rowMargin * 2, top + rowHeight);
            numbers.add(rectF);

            RowTest rowTest = new RowTest(i - 1, top, left, rowHeight, 8, cellMargin, new SparseIntArray());
            top += (rowHeight + rowMargin);
            rows.add(rowTest);
        }

        contentHeight = rowMargin * (rows.size() + 1) + rows.size() * rowHeight;
        contentWidth = rowMargin * 2 + rows.get(0).width;

        textSize = 30f;
        rowMargin = 20;

        vIndicatorBg = new RectF();
        hIndicatorBg = new RectF();
        vIndicator = new RectF();
        hIndicator = new RectF();

        matrix = new Matrix();

        cellBgPaint = new Paint();
        cellBgPaint.setStyle(Paint.Style.FILL);
        cellBgPaint.setColor(Color.WHITE);

        numberBgPaint = new Paint();
        numberBgPaint.setStyle(Paint.Style.FILL);
        numberBgPaint.setColor(Color.parseColor("#cfcfcfcf"));
        numberBgPaint.setAntiAlias(true);

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
                contraintScoll(dx, dy);
                lastClickX = e2.getX();
                lastClickY = e2.getY();
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                flinger.start(e1, velocityX, velocityY);
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

    private boolean isCellVisible(RectF rectF) {
        return rectF.right > 0 && rectF.left < getWidth();
    }

    private boolean isRowVisible(RectF rectF) {
        return rectF.bottom > 0 && rectF.top < getHeight();
    }

    private void onSigleTap(MotionEvent e) {
        RectF rectF = new RectF();
        for (int i = 0; i < rows.size(); i++) {
            matrix.mapRect(rectF, rows.get(i).getRectF());
            if (rectF.contains(lastClickX, lastClickY)) {
                for (CellTest cellTest : rows.get(i).cells) {
                    matrix.mapRect(rectF, cellTest.getRectF());
                    if (rectF.contains(lastClickX, lastClickY)) {
                        if (focusedCell != cellTest)
                            focusedCell = cellTest;
                        else
                            focusedCell = null;
                        onClick.onClick(cellTest);
                        invalidate();
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

    private void contraintScoll(float dx, float dy) {
        if (rows.isEmpty())
            return;
        float leftEdge = rowMargin * getMatrixScaleX(),
                topEdge = header.getHeight() * getMatrixScaleY(),
                rightEdge = getWidth() - rowMargin * getMatrixScaleX(),
                bottomEdge = getHeight() - rowMargin * getMatrixScaleY();
        RectF firstRow = getMatrixRectf(rows.get(0).rectF);
        RectF lastRow = getMatrixRectf(rows.get(rows.size() - 1).rectF);

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
            if (lastRow.bottom < bottomEdge) {
                dy = 0;
            } else if (lastRow.bottom + dy < bottomEdge) {
                dy = bottomEdge - lastRow.bottom;
            }
        }

        matrix.postTranslate(dx, dy);
        invalidate();
    }

//    private void drawFocusCell(Canvas canvas) {
//        if (focusedCell != null) {
//            RectF rectF = focusedCell.getRectF();
//            canvas.drawRect(rectF, cellBgPaint);
//            textPaint.setTextSize(textSize * 1.5f);
//            textPaint.setStrokeWidth(5f);
//            canvas.drawText(focusedCell.value.toString(), rectF.centerX(), getBaseLine(rectF), textPaint);
//        }
//    }

    private void drawFixedColumn(int index, Canvas canvas) {
        if (rows.isEmpty() || index < 0 || index >= rows.get(0).columnCount)
            return;
        RectF rectF = getMatrixRectf(rows.get(0).getCell(index).rectF);
        float top = header.height * getMatrixScaleY();
        if (rectF.left < - 10) {
            numberBgPaint.setColor(getResources().getColor(R.color.colorAccent, null));
            canvas.drawRect(0, top, rectF.width(), getHeight() - indicatorWidth, numberBgPaint);

            textPaint.setTextSize(textSize * getMatrixScaleX());
            textPaint.setStrokeWidth(2f);
            for (RowTest rowTest : rows) {
                CellTest cellTest1 = rowTest.getCell(index);
                RectF rectF1 = getMatrixRectf(cellTest1.getRectF());
                if (rectF1.bottom > top && rectF1.top < getHeight() - indicatorWidth) {
                    rectF1.offsetTo(0, rectF1.top);
                    canvas.drawLine(rectF1.left, rectF1.bottom + rowMargin * getMatrixScaleY() / 2, rectF1.right, rectF1.bottom + rowMargin * getMatrixScaleY() / 2, textPaint);
                    canvas.drawText(cellTest1.value.toString(), rectF1.centerX(), getBaseLine(rectF1), textPaint);
                }
            }
            numberBgPaint.setColor(getResources().getColor(R.color.colorHeaderBg, null));
            RectF rectF2 = new RectF(0, 0, rectF.width() + 2, top);
            canvas.drawRect(rectF2, numberBgPaint);
            canvas.drawText(header.getCell(index).value.toString(), rectF2.centerX(), getBaseLine(rectF2), textPaint);
        }
    }

    private void drawHeader(Canvas canvas) {
        RectF rectF = getMatrixRectf(header.getRectF());
        rectF.set(rectF.left, 0, rectF.right, rectF.height());
        textPaint.setTextSize(textSize * getMatrixScaleX());
        textPaint.setStrokeWidth(5f);
        numberBgPaint.setColor(getResources().getColor(R.color.colorHeaderBg, null));
        canvas.drawRect(rectF, numberBgPaint);
        for (int i = 0; i < header.columnCount; i++) {
            RectF cellRectF = getMatrixRectf(header.getCell(i).rectF);
            cellRectF.set(cellRectF.left, rectF.top, cellRectF.right, rectF.bottom);
            canvas.drawText(header.getCell(i).value.toString(), cellRectF.centerX(), getBaseLine(cellRectF), textPaint);
        }
    }

    private void drawIndicator(Canvas canvas) {
        if (rows.isEmpty())
            return;
        numberBgPaint.setColor(getResources().getColor(R.color.colorIndicatorBg, null));
        cellBgPaint.setColor(getResources().getColor(R.color.colorIndicator, null));
        cellBgPaint.setStyle(Paint.Style.FILL);
        RectF first = getMatrixRectf(rows.get(0).rectF);

        float[] ranges = computeScrollRange();

        if (ranges[0] > 0) {
            hIndicatorBg.set(0, getHeight() - indicatorWidth, getWidth() - indicatorWidth, getHeight());
            float scaleX = getWidth() / (contentWidth * getMatrixScaleX());
            float indicatorSize = getWidth() * scaleX;
            float left = (hIndicatorBg.width() - indicatorSize) * Math.abs(first.left > 0 ? 0 : first.left) / ranges[0];
            hIndicator.set(left, getHeight() - indicatorWidth, left + indicatorSize, getHeight());
            canvas.drawRect(hIndicatorBg, numberBgPaint);
            canvas.drawRect(hIndicator, cellBgPaint);
        }

        if (ranges[1] > 0) {
            vIndicatorBg.set(getWidth() - indicatorWidth, 0, getWidth(), getHeight());
            float scaleY = getHeight() / (contentHeight * getMatrixScaleX());
            float indicatorSize = getHeight() * scaleY;
            float top = (vIndicatorBg.height() - indicatorSize) * Math.abs(first.top > 0 ? 0 : first.top) / ranges[1];
            vIndicator.set(getWidth() - indicatorWidth, top, getWidth(), top + indicatorSize);
            canvas.drawRect(vIndicatorBg, numberBgPaint);
            canvas.drawRect(vIndicator, cellBgPaint);
        }
    }

    private void drawCell(CellTest cellTest, Canvas canvas) {
        cellBgPaint.setColor(Color.WHITE);
        canvas.drawRect(cellTest.rectF, cellBgPaint);
        textPaint.setTextSize(cellTest == focusedCell ? textSize * 1.3f : textSize);
        textPaint.setStrokeWidth(cellTest == focusedCell ? 5f : 2f);
        canvas.drawText(cellTest.value.toString(), cellTest.rectF.centerX(), getBaseLine(cellTest.rectF), textPaint);
    }

    private void drawLines(Canvas canvas) {
        if (rows.isEmpty())
            return;
        RowTest firstRow = rows.get(0);
        RectF rowRectF = getMatrixRectf(firstRow.getRectF());
        RectF lastRowRectF = getMatrixRectf(rows.get(rows.size() - 1).getRectF());
        textPaint.setStrokeWidth(2f);
        float rowOffset = rowMargin * getMatrixScaleX() / 2f;
        float hLineStartX = rowRectF.left;
        float hLineEndX = rowRectF.right;

        float cellOffset = cellMargin * getMatrixScaleX() / 2f;
        float vLineStartY = rowRectF.top - rowOffset;
        float vLineEndY = lastRowRectF.bottom + rowOffset;

        for (RowTest rowTest : rows) {
            RectF rectF = getMatrixRectf(rowTest.getRectF());
            if (isRowVisible(rectF)) {
                float startX = Math.max(0, hLineStartX);
                float endX = Math.min(getWidth(), hLineEndX);
                float y = rectF.bottom + rowOffset;
                if (rowTest.row == 0)
                    canvas.drawLine(startX, vLineStartY, endX, vLineStartY, textPaint);
                canvas.drawLine(startX, y, endX, y, textPaint);
            }
        }

        for (CellTest cellTest : firstRow.cells) {
            RectF rectF = getMatrixRectf(cellTest.getRectF());
            float x = cellTest.column == (firstRow.columnCount - 1) ? hLineEndX : rectF.right + cellOffset;
            float startY = Math.max(0, vLineStartY);
            float endY = Math.min(getHeight(), vLineEndY);
            if (isCellVisible(rectF)) {
                if (cellTest.column == 0)
                    canvas.drawLine(hLineStartX, vLineStartY, hLineStartX, vLineEndY, textPaint);
                canvas.drawLine(x, startY, x, endY, textPaint);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isFirstDraw)
            firstDraw();
        canvas.save();
        canvas.concat(matrix);
        for (RowTest rowTest : rows) {
            if (isRowVisible(getMatrixRectf(rowTest.getRectF()))) {
                for (CellTest cellTest : rowTest.cells) {
                    if (isCellVisible(getMatrixRectf(cellTest.getRectF()))) {
                        drawCell(cellTest, canvas);
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
