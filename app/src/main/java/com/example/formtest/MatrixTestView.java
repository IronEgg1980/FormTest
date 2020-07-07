package com.example.formtest;

import android.animation.ObjectAnimator;
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
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

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
            if(obj instanceof CellTest){
                CellTest other = (CellTest) obj;
                return other.row == this.row &&
                        other.column == this.column;
            }
            return false;
        }
    }

    private static final String TAG = "殷宗旺";
    private GestureDetector gestureDetector;
    private OnClick onClick;

    private List<RowTest> rows = new ArrayList<>();
    private Paint cellBgPaint;
    private TextPaint textPaint;
    private float textSize;
    private int rowMargin = 10;
    private RectF viewRectF, contentRectF;
    private Rect drawRect;
    private Matrix matrix;
    private CellTest focusedCell;
    private ValueAnimator valueAnimator;

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

    private void initialValueAnimator(){
        valueAnimator = new ValueAnimator();
        valueAnimator.setDuration(500);
        valueAnimator.setObjectValues(new PointF());
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                PointF pointF = (PointF) animation.getAnimatedValue();
                Log.d(TAG, "onAnimationUpdate: dx = "+pointF.x+" dy = "+pointF.y);
                matrix.postTranslate(pointF.x,pointF.y);
                invalidate();
            }
        });
    }

    private void atuoScroll(final float velocityX, final float velocityY){
        final float distanceX = (float) Math.sqrt(Math.sqrt(Math.abs(velocityX)));
        final float distanceY = (float) Math.sqrt(Math.sqrt(Math.abs(velocityY)));
        valueAnimator.setEvaluator(new TypeEvaluator<PointF>() {
            @Override
            public PointF evaluate(float fraction, PointF startValue, PointF endValue) {
                Log.d(TAG, "evaluate: fraction = "+fraction);
                PointF pointF = new PointF();
                float dx =  distanceX - distanceX * fraction;
                float dy = distanceY - distanceY * fraction;
                pointF.x = dx * velocityX / Math.abs(velocityX);
                pointF.y = dy * velocityY / Math.abs(velocityY);
                return pointF;
            }
        });
        valueAnimator.start();
    }

    private void initial(Context context) {
        initialValueAnimator();

        final int rowHeight = 100;
        int top = rowMargin, left = rowMargin;
        for (int i = 1; i < 11; i++) {
            RowTest rowTest = new RowTest(i - 1, top, left, rowHeight, 10, 10, new SparseIntArray());
            top += rowHeight + rowMargin;
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

        textPaint = new TextPaint();
        textPaint.setTextSize(textSize);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                piovX = e.getX();
                piovY = e.getY();
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float dx = e2.getX() - piovX;
                float dy = e2.getY() - piovY;
                matrix.postTranslate(dx,dy);
                invalidate();
                piovX = e2.getX();
                piovY = e2.getY();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Log.d(TAG, "onFling: velocityX = "+velocityX);
                if(Math.abs(velocityX) > 1000 || Math.abs(velocityY) > 1000)
                    atuoScroll(velocityX,velocityY);
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                RectF rectF = new RectF();
                for (int i = 0; i < rows.size(); i++) {
                    matrix.mapRect(rectF,rows.get(i).getRectF());
                    if (rectF.contains(piovX,piovY)) {
                        for (CellTest cellTest : rows.get(i).cells) {
                            matrix.mapRect(rectF,cellTest.getRectF());
                            if (rectF.contains(piovX,piovY)) {
                                if(focusedCell != cellTest)
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
                return true;
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


    private int dXProgress = 0, dYProgress = 0, scaleProgress = 50;
    private float piovX = 0, piovY = 0;

    public void transX(int progress) {
        float dx = (getWidth() - 200) * (progress - dXProgress) / 100f;
        dXProgress = progress;
        matrix.postTranslate(dx, 0);
        invalidate();
    }

    public void transY(int progress) {
        float dy = (getHeight() - 200) * (progress - dYProgress) / 100f;
        dYProgress = progress;
        matrix.postTranslate(0, dy);
        invalidate();
    }

    public void scale(int progress) {
        float scale = (progress + 1f) / scaleProgress;
        scaleProgress = progress + 1;
        matrix.postScale(scale, scale, piovX, piovY);
        invalidate();
    }

    private void drawFocusCell(Canvas canvas){
        int offset = 20;
        if(focusedCell!=null){
            RectF rowRectF = rows.get(focusedCell.row).getRectF();
            cellBgPaint.setColor(Color.RED);
            canvas.drawRect(rowRectF,cellBgPaint);
            RectF cellRectF = new RectF(focusedCell.getRectF().left - offset,focusedCell.getRectF().top - offset,
                    focusedCell.getRectF().right + offset,focusedCell.getRectF().bottom + offset);
            cellBgPaint.setColor(Color.TRANSPARENT);
            canvas.drawRect(cellRectF,cellBgPaint);
            textPaint.setTextSize(textSize * 1.5f);
            canvas.drawText(focusedCell.value.toString(),cellRectF.centerX(),getBaseLine(cellRectF),textPaint);
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.concat(matrix);
        for (RowTest rowTest : rows) {
            for (CellTest cellTest : rowTest.cells) {
                if(cellTest == focusedCell)
                    continue;
                cellBgPaint.setColor(Color.BLUE);
                canvas.drawRect(cellTest.rectF, cellBgPaint);
                textPaint.setTextSize(textSize);
                canvas.drawText(cellTest.value.toString(), cellTest.rectF.centerX(), getBaseLine(cellTest.rectF), textPaint);
            }
        }
        drawFocusCell(canvas);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        performClick();
        return gestureDetector.onTouchEvent(event);
    }
}
