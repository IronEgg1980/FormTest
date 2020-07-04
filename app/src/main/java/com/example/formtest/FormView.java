package com.example.formtest;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.OvershootInterpolator;
import android.widget.Scroller;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FormView extends View {
    public interface OnCellClickListener {
        void onClick(Cell cell, Row row);
    }

    public static class Cell {
        public int rowNumber;
        public int columnIndex;
        private Object value;
        public RectF rect;
        public boolean isFocused = false;
        public boolean isHide = false;

        public Cell(int rowNumber, int columnIndex, RectF rect) {
            this.rowNumber = rowNumber;
            this.columnIndex = columnIndex;
            this.value = "";
            this.rect = rect;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            if (value == null) {
                this.value = "";
            } else {
                this.value = value;
            }
        }

        public boolean isClicked(int x, int y) {
            return this.rect.contains(x, y);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof Cell) {
                Cell cell = (Cell) obj;
                return cell.rowNumber == this.rowNumber && cell.columnIndex == this.columnIndex;
            }
            return false;
        }
    }

    public static class Row {
        public int rowNumber;
        public int columnCount;
        public RectF rect;
        private Cell[] cells;
        private int defaultColumnWidth = 240;
        public boolean isSelected = false;

        public Row(int index, int columnCount, int x, int y, int rowHeight, int cellMargin, SparseIntArray columnWidth) {
            this.rowNumber = index;
            this.columnCount = columnCount;
            this.cells = new Cell[columnCount];
            generateCells(x, y, rowHeight, cellMargin, columnWidth);
            generateRectF(x, y, rowHeight, cellMargin);
        }

        public Cell getCell(int column) {
            if (column < 0 || column > 7)
                throw new IndexOutOfBoundsException("Call getCell() error! The Column index is out of range !");
            return cells[column];
        }

        public boolean isClicked(int x, int y) {
            return this.rect.contains(x, y);
        }

        private void generateRectF(int x, int y, int rowHeight, int cellMargin) {
            this.rect = new RectF();
            rect.left = x;
            rect.top = y;
            rect.bottom = rowHeight + y;
            int width = 0;
            for (int i = 0; i < cells.length; i++) {
                width += cells[i].rect.width();
                if (i < cells.length - 1) {
                    width += cellMargin;
                }
            }
            rect.right = x + width;
        }

        private void generateCells(int x, int y, int rowHeight, int cellMargin, SparseIntArray columnWidth) {
            int left = x;
            int width;
            for (int i = 0; i < columnCount; i++) {
                width = columnWidth.get(i, defaultColumnWidth);

                RectF rectF = new RectF();
                rectF.left = left;
                rectF.top = y;
                rectF.bottom =y + rowHeight;
                rectF.right = left + width;
                cells[i] = new Cell(rowNumber, i, rectF);

                left += (width + cellMargin);
            }
        }
    }

    private class FlingRunnable implements Runnable {
        int mInitX, mInitY, mVelocityX, mVelocityY, mMaxX, mMaxY;

        void start(int initX, int initY,
                   int velocityX,
                   int velocityY,
                   int maxX,
                   int maxY) {
            this.mInitX = initX;
            this.mInitY = initY;
            this.mVelocityX = velocityX;
            this.mVelocityY = velocityY;
            this.mMaxX = maxX;
            this.mMaxY = maxY;

            // 先停止上一次的滚动
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }

            // 开始 fling
            mScroller.fling(initX, initY, velocityX,
                    velocityY, 0, maxX, 0, maxY);
            post(this);
        }

        void stop() {
            if (!mScroller.isFinished())
                mScroller.abortAnimation();
        }

        @Override
        public void run() {
            // 如果已经结束，就不再进行
            if (!mScroller.computeScrollOffset()) {
                return;
            }

            // 计算偏移量
            int currX = mScroller.getCurrX();
            int diffX = mInitX - currX;

            int currY = mScroller.getCurrY();
            int diffY = mInitY - currY;

            // 用于记录是否超出边界，如果已经超出边界，则不再进行回调，即使滚动还没有完成
            boolean isEnd = false;

            if (diffX != 0) {
                // 超出右边界，进行修正
                if (getScrollX() >= totalWidth - getWidth()) {
                    diffX = totalWidth - getWidth() - getScrollX();
                    isEnd = true;
                }

                // 超出左边界，进行修正
                if (getScrollX() <= 0) {
                    diffX = -getScrollX();
                    isEnd = true;
                }
            }

            if (diffY != 0) {
                // 超出右边界，进行修正
                if (getScrollY() >= totalHeight - getHeight()) {
                    diffY = totalHeight - getHeight() - getScrollY();
                    isEnd = true;
                }

                // 超出左边界，进行修正
                if (getScrollY() <= 0) {
                    diffX = -getScrollY();
                    isEnd = true;
                }
            }


            if (!mScroller.isFinished()) {
                scrollBy(diffX, diffY);
            }
            mInitX = currX;
            mInitY = currY;

            if (!isEnd) {
                post(this);
            }

        }
    }

    private String TAG = "殷宗旺";
    private Row header;
    private List<Row> rows;
    private int columnCount = 8;
    private int totalWidth, totalHeight;
    private int rowHeight = 240;
    private int rowMargin = 10;
    private int cellMargin = 10;
    private int rectRadius = 10;
    private int lastX, lastY;
    private ValueAnimator focusInAnim;
    private float focusScaleValue = 1.2f;
    private float textSize = 40, focusedTextSize = textSize * focusScaleValue;
    private float fontBaseLine;
    private Cell firstClickedCell = null, currentCell = null;
    private long firstClickTime;
    private boolean isMoved, needShowFocus = false;
    private int normalTextColor = Color.BLACK, focusedTextColor = Color.WHITE;
    private int normalBgColor = Color.parseColor("#dddddd"), focusedBgColor = Color.parseColor("#6200EE");

    private OnCellClickListener onCellClickListener;

    private Paint rectPaint;
    private TextPaint textPaint;
    private Paint rowPaint;

    private Scroller mScroller;
    private VelocityTracker velocityTracker;
    private FlingRunnable flingRunnable;
    private int mMaximumVelocity, mMinimumVelocity;


    public void setOnCellClickListener(OnCellClickListener onCellClickListener) {
        this.onCellClickListener = onCellClickListener;
    }

    public int getRowCount() {
        return rows.size();
    }

    public void setRowMargin(int rowMargin) {
        this.rowMargin = rowMargin;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    public void setCellMargin(int cellMargin) {
        this.cellMargin = cellMargin;
    }

    public Row getRow(int rowIndex) {
        if (rowIndex >= rows.size()) {
            throw new IndexOutOfBoundsException("Call getRow() error! The rowIndex is out of range !");
        }
        return rows.get(rowIndex);
    }

    public Cell getCell(int rowIndex, int column) {
        if (rowIndex >= rows.size() || column >= 8) {
            return null;
        }
        return getRow(rowIndex).getCell(column);
    }

    public Cell getCurrentCell() {
        return this.currentCell;
    }

    public void setCurrentCell(int rowIndex, int column) {
        setCurrentCell(getCell(rowIndex, column));
    }

    public void setCurrentCell(Cell cell) {
        if (cell == null) {
            return;
        }
        cancelFocus();
        currentCell = cell;
        rows.get(currentCell.rowNumber).isSelected = true;
        needShowFocus = true;
        focusInAnim.start();
    }

    public void cancelFocus() {
        if (currentCell == null)
            return;
        currentCell.isFocused = false;
        rows.get(currentCell.rowNumber).isSelected = false;
        currentCell = null;
        invalidate();
    }

    public boolean isVisible(Cell cell) {
        Rect rect = new Rect();
        getGlobalVisibleRect(rect);

        RectF rectF = cell.rect;
        int left = (int) (rectF.left + 0.5);
        int top = (int) (rectF.top + 0.5);
        int right = (int) (rectF.right + 0.5);
        int bottom = (int) (rectF.bottom + 0.5);

        return (left >= getScrollX() &&
                right <= getScrollX() + rect.width() &&
                top >= getScrollY() &&
                bottom <= getScrollY() + rect.height());
    }

    public void setInsideVisible(Cell cell) {
        if (!isVisible(cell)) {
            int xOffset = (int) (cell.rect.left - cell.rect.width()) - getScrollX() - rowMargin;
            int yOffset = (int) (cell.rect.top - cell.rect.height()) - getScrollY() - rowMargin;
            constrainMove(xOffset, yOffset);
        }
    }

    public FormView(Context context) {
        super(context);
        initial(context);
    }

    public FormView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initial(context);
    }

    public FormView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initial(context);
    }

    private void initial(Context context) {
        this.rows = new ArrayList<>();

        this.rectPaint = new Paint();
        this.rectPaint.setAntiAlias(true);
        this.rectPaint.setStyle(Paint.Style.FILL);
        this.rectPaint.setStrokeWidth(1f);

        this.rowPaint = new Paint();
        this.rowPaint.setAntiAlias(true);
        this.rowPaint.setStyle(Paint.Style.STROKE);
        this.rowPaint.setColor(Color.RED);
        this.rowPaint.setStrokeWidth(8f);

        this.textPaint = new TextPaint();
        this.textPaint.setAntiAlias(true);
        this.textPaint.setTextSize(textSize);
        this.textPaint.setTextAlign(Paint.Align.CENTER);

//        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
//        fontBaseLine = (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent;
        fontBaseLine = (textPaint.descent() - textPaint.ascent()) / 2 - textPaint.descent();
        Log.d(TAG, "baseLine: "+fontBaseLine+"  textPaint.baselineShift:"+textPaint.baselineShift);
        this.focusInAnim = ObjectAnimator.ofFloat(1f, focusScaleValue);
        this.focusInAnim.setDuration(100);
        this.focusInAnim.setInterpolator(new OvershootInterpolator());
        this.focusInAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                focusScaleValue = (float) animation.getAnimatedValue();
                focusedTextSize = textSize * focusScaleValue;
                invalidate();
            }
        });
        this.focusInAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                needShowFocus = false;
            }
        });

        this.mScroller = new Scroller(context);
        this.flingRunnable = new FlingRunnable();
        this.mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        this.mMinimumVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
    }

    private Row createNewRow() {
        SparseIntArray sparseIntArray = new SparseIntArray();
        sparseIntArray.put(0, 500);
        sparseIntArray.put(7, 500);

        int index = this.rows.size();
        int x = getPaddingLeft();
        int y = totalHeight == 0 ? getPaddingTop() : totalHeight - getPaddingBottom();
        return new Row(index, columnCount, x, y, rowHeight, cellMargin, sparseIntArray);
    }

    public Row addRow() {
        Row row = createNewRow();
        this.rows.add(row);
        totalHeight = getRowCount() * (rowMargin + rowHeight) + getPaddingTop() + getPaddingBottom();
        totalWidth = rows.size() == 0 ? 600 : (int) (rows.get(0).rect.width())+ getPaddingLeft() + getPaddingRight();
        invalidate();
        return row;
}

    private RectF getFocusedCellRectF(RectF rect) {
        float sideValue = rect.width() * (focusScaleValue - 1) / 2;
        RectF rect1 = new RectF();
        rect1.left = rect.left - sideValue;
        rect1.top = rect.top - sideValue;
        rect1.right = rect.right + sideValue;
        rect1.bottom = rect.bottom + sideValue;
        return rect1;
    }

    private void drawRows(Canvas canvas) {
        int side = rowMargin / 2;
        for (Row row : rows) {
            if (canvas.quickReject(row.rect, Canvas.EdgeType.BW))
                continue;
            if (row.isSelected) {
                RectF rect = new RectF(side, row.rect.top - side, row.rect.right + side, row.rect.bottom + side);
                canvas.drawRect(rect, rowPaint);
            }
            for (int i = 0; i < 8; i++) {
                Cell cell = row.getCell(i);
                if (canvas.quickReject(cell.rect, Canvas.EdgeType.BW) || cell.isFocused)
                    continue;
                RectF rect = cell.rect;
                rectPaint.setColor(normalBgColor);
                textPaint.setColor(normalTextColor);
                textPaint.setTextSize(textSize);
                textPaint.setStrokeWidth(2);
                canvas.drawRoundRect(rect, rectRadius, rectRadius, rectPaint);

                int textpadding = 10;
                int width = (int)cell.rect.width() - textpadding * 2;
//                StaticLayout staticLayout = new StaticLayout(cell.getValue().toString(),
//                        textPaint,
//                        (int)cell.rect.width() - textpadding * 2,
//                        Layout.Alignment.ALIGN_NORMAL,
//                        1f,
//                        0,
//                        false);
                StaticLayout.Builder builder = StaticLayout.Builder.obtain(cell.getValue().toString(),
                        0,
                        cell.getValue().toString().length(),
                        textPaint,
                        width);
                builder.setEllipsize(TextUtils.TruncateAt.END)
                        .setMaxLines(3)
                        .setLineSpacing(0f,1.2f);
                StaticLayout staticLayout = builder.build();

                int x = (int) cell.rect.centerX();
                int y =(int) cell.rect.top + ((int) cell.rect.height() - staticLayout.getHeight()) / 2;

                canvas.save();
                canvas.translate(x,y);
                staticLayout.draw(canvas);
                canvas.restore();
//                canvas.drawText(cell.getValue().toString(), rect.centerX(), fontBaseLine + rect.centerY(), textPaint);
            }
        }
        if (currentCell != null) {
            rectPaint.setColor(focusedBgColor);
            textPaint.setColor(focusedTextColor);
            textPaint.setTextSize(focusedTextSize);
            textPaint.setStrokeWidth(5);
            canvas.drawRoundRect(getFocusedCellRectF(currentCell.rect), rectRadius, rectRadius, rectPaint);
            canvas.drawText(currentCell.getValue().toString(), currentCell.rect.centerX(), currentCell.rect.centerY() + fontBaseLine, textPaint);
        }
    }

    private void drawCell(Canvas canvas) {
        if (currentCell == null)
            return;
        rectPaint.setColor(focusedBgColor);
        textPaint.setColor(focusedTextColor);
        textPaint.setTextSize(focusedTextSize);
        textPaint.setStrokeWidth(5);

        RectF rectF = currentCell.rect;

        canvas.saveLayer(rectF, rectPaint);
        canvas.scale(focusScaleValue, focusScaleValue, rectF.centerX(), rectF.centerY());
        canvas.drawRoundRect(rectF, rectRadius, rectRadius, rectPaint);
        canvas.restore();
        canvas.drawText(currentCell.getValue().toString(), rectF.centerX(), fontBaseLine + rectF.centerY(), textPaint);
    }

    private Cell getClickedCell(int x, int y) {
        for (Row row : rows) {
            if (row.isClicked(x, y)) {
                for (int i = 0; i < 8; i++) {
                    Cell cell = row.getCell(i);
                    if (cell.isClicked(x, y)) {
                        return cell;
                    }
                }
                break;
            }
        }
        return null;
    }

    private void onClick(int x, int y) {
        long timeDiff = System.currentTimeMillis() - firstClickTime;
        if (timeDiff < 500) {
            if (focusInAnim.isRunning())
                focusInAnim.cancel();
            Cell cell = getClickedCell(x, y);
            if (cell != null && cell.equals(firstClickedCell) && onCellClickListener != null) {
                if (currentCell != null && !cell.equals(currentCell)) {
                    currentCell.isFocused = false;
                    rows.get(currentCell.rowNumber).isSelected = false;
                }
                cell.isFocused = !cell.isFocused;
                Row row = rows.get(cell.rowNumber);
                row.isSelected = !row.isSelected;
                if (cell.isFocused) {
                    setCurrentCell(cell);
                    onCellClickListener.onClick(cell, row);
                } else {
                    currentCell = null;
                    invalidate();
                }

            } else {
                cancelFocus();
            }
        }
    }

    public void constrainMove(int xOffset, int yOffset) {
        Rect rect = new Rect();
        getGlobalVisibleRect(rect);
        int rightEdge = totalWidth - rect.width();
        int bottomEdge = totalHeight - rect.height();

        if (Math.abs(xOffset) > 10 || Math.abs(yOffset) > 10) {
            isMoved = true;
            if (totalWidth <= rect.width()) {
                xOffset = 0;
            } else if (getScrollX() + xOffset <= 0) {
                xOffset = -getScrollX();
            } else if (getScrollX() + xOffset > rightEdge) {
                xOffset = rightEdge - getScrollX();
            }


            if (totalHeight <= rect.height()) {
                yOffset = 0;
            } else if (getScrollY() + yOffset <= 0) {
                yOffset = -getScrollY();
            } else if (getScrollY() + yOffset > bottomEdge) {
                yOffset = bottomEdge - getScrollY();
            }

            scrollBy(xOffset, yOffset);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        totalHeight = getRowCount() * (rowMargin + rowHeight) + getPaddingTop() + getPaddingBottom();
        totalWidth = rows.size() == 0 ? 600 : (int) (rows.get(0).rect.width() )+ getPaddingLeft() + getPaddingRight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawRows(canvas);
        if (needShowFocus) {
            drawCell(canvas);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = Math.round(event.getRawX() + 0.5f);
        int y = Math.round(event.getRawY() + 0.5f);

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                performClick();
                isMoved = false;
                firstClickTime = System.currentTimeMillis();
                firstClickedCell = getClickedCell(getScrollX() + Math.round(event.getX() + 0.5f), getScrollY() + Math.round(event.getY() + 0.5f));
                lastX = x;
                lastY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                int xOffset = lastX - x;
                int yOffset = lastY - y;
                constrainMove(xOffset, yOffset);
                lastX = x;
                lastY = y;
                break;
            case MotionEvent.ACTION_UP:
                if (isMoved) {
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    // 获取横向速度
                    int velocityX = (int) velocityTracker.getXVelocity();
                    int velccityY = (int) velocityTracker.getYVelocity();
                    // 速度要大于最小的速度值，才开始滑动
                    if (Math.abs(velocityX) > mMinimumVelocity || Math.abs(velccityY) > mMinimumVelocity) {
                        int initX = getScrollX();
                        int initY = getScrollY();
                        int maxX = totalWidth - getWidth();
                        int maxY = totalHeight > getHeight() ? totalHeight - getHeight() : 0;
                        if (maxX > 0 || maxY > 0) {
                            flingRunnable.start(initX, initY, velocityX, velccityY, maxX, maxY);
                        }
                    }

                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                        velocityTracker = null;
                    }
                } else {
                    onClick(getScrollX() + Math.round(event.getX() + 0.5f), getScrollY() + Math.round(event.getY() + 0.5f));
                }
                break;
        }
        return true;
    }
}

