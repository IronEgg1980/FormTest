package com.example.formtest;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
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

import java.util.ArrayList;
import java.util.List;

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
        private int cellMargin = 10;
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
            this.cellMargin = cellMargin;
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
            rectF = new RectF(left, top, width, top + height);
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
        float lastX = 0, lastY = 0;
        private boolean isFirstRun = true;

        @Override
        public void run() {
            if (overScroller.computeScrollOffset()) {
                if (isFirstRun) {
                    isFirstRun = false;
                    lastX = overScroller.getCurrX();
                    lastY = overScroller.getCurrY();
                }
                float dx = overScroller.getCurrX() - lastX;
                float dy = overScroller.getCurrY() - lastY;
                lastX = overScroller.getCurrX();
                lastY = overScroller.getCurrY();
                matrix.postTranslate(dx, dy);
                postInvalidate();
                post(this);
                Log.d(TAG, "run: currentX = " + overScroller.getCurrX());
            } else {
                isFirstRun = true;
            }
        }

    }

    private static final String TAG = "殷宗旺";
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private OverScroller overScroller;
    private Flinger flinger;
    private OnClick onClick;
    private boolean isFirstDraw = true;

    private List<RowTest> rows = new ArrayList<>();
    private Paint cellBgPaint, numberBgPaint;
    private TextPaint textPaint;
    private float textSize;
    private int rowMargin = 10, rowHeight = 100;
    private List<RectF> numbers = new ArrayList<>();

    private RectF viewRectF, contentRectF;
    private Rect drawRect;
    private Matrix matrix;
    private CellTest focusedCell;
    private ValueAnimator valueAnimator;
    private float lastClickX = 0, lastClickY = 0;

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
        float totalWidth = rowMargin * 2 + rows.get(0).width;
        float scale = getWidth() / totalWidth;
        matrix.setScale(scale, scale);
    }


    private void initial(Context context) {
        overScroller = new OverScroller(getContext(), new DecelerateInterpolator());
        flinger = new Flinger();

        int top = rowMargin, left = rowMargin;
        for (int i = 1; i < 11; i++) {
            RectF rectF = new RectF(0,top,rowMargin * 2,top+rowHeight);
            numbers.add(rectF);

            RowTest rowTest = new RowTest(i - 1, top, left, rowHeight, 10, 10, new SparseIntArray());
            top += (rowHeight + rowMargin);
            rows.add(rowTest);
        }

        textSize = 30f;
        rowMargin = 20;

        contentRectF = new RectF(400, 100, 600, 300);
        viewRectF = new RectF(100, 100, 300, 300);
        drawRect = new Rect();

        matrix = new Matrix();

        cellBgPaint = new Paint();
        cellBgPaint.setStyle(Paint.Style.STROKE);
        cellBgPaint.setColor(Color.BLUE);
        cellBgPaint.setAntiAlias(true);
        cellBgPaint.setStrokeWidth(4f);

        numberBgPaint = new Paint();
        numberBgPaint.setStyle(Paint.Style.FILL);
        numberBgPaint.setColor(Color.parseColor("#cfcfcf"));
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
                return super.onDown(e);
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float dx = e2.getX() - lastClickX;
                float dy = e2.getY() - lastClickY;
                matrix.postTranslate(dx, dy);
                invalidate();
                lastClickX = e2.getX();
                lastClickY = e2.getY();
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityX) > 1000 || Math.abs(velocityY) > 1000) {
                    int contentHeight = rowMargin * (rows.size() + 1) + rowHeight * rows.size();
                    int minX = (int) (getWidth() - rows.get(0).width * getScaleX());
                    int maxX = (int) (rows.get(0).width * getScaleX());
                    int minY = getHeight() - contentHeight;
                    int maxY = contentHeight;
                    Log.d(TAG, "onFling: maxX = " + maxX);
                    overScroller.fling((int) e1.getX(), (int) e1.getY(), (int) velocityX, (int) velocityY, minX, maxX, minY, maxY);
                    new Thread(flinger).start();
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
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
                return super.onSingleTapUp(e);
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                matrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(), detector.getCurrentSpanX(), detector.getCurrentSpanY());
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

    private void drawFocusCell(Canvas canvas) {
        int offset = 20;
        if (focusedCell != null) {
            RectF rowRectF = rows.get(focusedCell.row).getRectF();
            cellBgPaint.setColor(Color.RED);
            canvas.drawRect(rowRectF, cellBgPaint);
            RectF cellRectF = new RectF(focusedCell.getRectF().left - offset, focusedCell.getRectF().top - offset,
                    focusedCell.getRectF().right + offset, focusedCell.getRectF().bottom + offset);
            cellBgPaint.setColor(Color.TRANSPARENT);
            canvas.drawRect(cellRectF, cellBgPaint);
            textPaint.setTextSize(textSize * 1.5f);
            canvas.drawText(focusedCell.value.toString(), cellRectF.centerX(), getBaseLine(cellRectF), textPaint);
        }
    }

    private void drawHeader(Canvas canvas){
        RowTest rowTest = rows.get(0);
        RectF rectF = getMatrixRectf(rowTest.getRectF());
        rectF.set(rectF.left,0,rectF.right,50 * getMatrixScaleX());
        canvas.drawRect(rectF,numberBgPaint);
        for(int i = 1;i<= rowTest.columnCount;i++){
            textPaint.setTextSize(textSize * getMatrixScaleX());
            String s = "第 "+i +" 列";
            RectF cellRectF = getMatrixRectf(rowTest.getCell(i - 1).rectF);
            cellRectF.set(cellRectF.left,0,cellRectF.right,50 * getMatrixScaleX());
            canvas.drawText(s,cellRectF.centerX(),getBaseLine(cellRectF),textPaint);
        }
    }

    private void drawRowNumber(Canvas canvas) {
        int i = 1;
        for (RectF rectF1:numbers) {
            RectF rectF = getMatrixRectf(rectF1);
            rectF.set(0,rectF.top,rowMargin *1.5f* getMatrixScaleX(),rectF.bottom);

            canvas.drawRect(rectF, numberBgPaint);
            textPaint.setTextSize(textSize * getMatrixScaleX());
            canvas.drawText(String.valueOf(i++), rectF.centerX(), getBaseLine(rectF), textPaint);
        }
//        float top = 0;
//        float bottom = 0;
//        float left = 0;
//        float right =left + rowMargin * 2 * getScaleX();
//        for(int j = 1;j<=rows.size();j++){
//            RectF rowRectF =getMatrixRectf(rows.get(j - 1).getRectF());
//            top = rowRectF.top;
//            bottom = rowRectF.bottom;
//
//            RectF rectF = new RectF(left,top,right,bottom);
//            canvas.drawRect(rectF, numberBgPaint);
//
//            textPaint.setTextSize(textSize * getScaleX());
//            canvas.drawText(String.valueOf(j), rectF.centerX(), getBaseLine(rectF), textPaint);
//        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isFirstDraw)
            firstDraw();
        canvas.save();
        canvas.concat(matrix);
        for (RowTest rowTest : rows) {
            for (CellTest cellTest : rowTest.cells) {
                if (cellTest == focusedCell)
                    continue;
                cellBgPaint.setColor(Color.BLUE);
                canvas.drawRect(cellTest.rectF, cellBgPaint);
                textPaint.setTextSize(textSize);
                canvas.drawText(cellTest.value.toString(), cellTest.rectF.centerX(), getBaseLine(cellTest.rectF), textPaint);
            }
        }
        drawFocusCell(canvas);
        canvas.restore();
        drawRowNumber(canvas);
        drawHeader(canvas);
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
