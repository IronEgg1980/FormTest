package com.example.formtest;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyFormView extends View {
    private static final String TAG = "殷宗旺";

    // 标记
    private boolean isFirstDraw = true;
    private boolean showFixedColumn = false;
    private float defaultScaleValue = 1f;
    private float lastClickX, lastClickY;
    private float[] m = new float[9]; // 记录matrix的值
    private Cell focusedCell;// 当前选中的cell

    // 使用到的各种尺寸
    private int mTitleHeight = 500;
    private float mPadding = 100;
    private float textSize = 30;
    private float titleTextSize = 150;
    private int rowHeight = 200;
    private int indicatorWidth = 10;

    // 自定义的文本或颜色，其他
    private String mTitle = "标题";
    private LocalDate startDay;
    private int weekOfYear;
    private int columnsCount = 9;
    private int hearBgColor = getResources().getColor(R.color.colorHeaderBg, null);
    private int lineColor = Color.GRAY;
    private int cellBgColor = Color.WHITE, cellFocusedColor = getResources().getColor(R.color.colorAccent, null);
    private DateTimeFormatter dateTimeFormatter;
    private Layout.Alignment cellAlignment = Layout.Alignment.ALIGN_LEFT;

    // 自动滚动工具
    private OverScroller overScroller, autoScroller;
    private Flinger flinger;
    private AutoScroller scroller;

    // 滑动和缩放手势工具
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    // 添加Row动画
    private ValueAnimator addRowAnimator;

    // 移除Row动画
    private ValueAnimator removeRowAnimator;

    // 点击接口
    private OnClick onClick;

    // 绘图相关工具
    private Paint bgPaint;
    private Paint linePaint;
    private TextPaint textPaint;
    private Matrix matrix;
    private RectF cellRectF;// 存储cell位置和大小信息的矩形，用于绘制cell

    // 内容
    private Row title;
    private Row header;
    private List<Row> rows = new ArrayList<>();
    private RectF vIndicatorBg, hIndicatorBg, vIndicator, hIndicator; // 滚动条背景和滚动条的矩形框

    // 初始化
    private void initial(Context context) {
        overScroller = new OverScroller(getContext(), new DecelerateInterpolator());
        autoScroller = new OverScroller(getContext(), new LinearInterpolator());

        flinger = new Flinger();
        scroller = new AutoScroller();

        dateTimeFormatter = DateTimeFormatter.ofPattern("M月d日\neeee", Locale.CHINA);
        header = new Row(-1, rowHeight, columnsCount, new SparseIntArray());
        header.getCell(0).value = "姓名";
        header.getCell(8).value = "备注";
        title = new Row(-2, mTitleHeight, 1, new SparseIntArray());
        title.width = header.width;

        addRowAnimator = ValueAnimator.ofInt(0, rowHeight);
        addRowAnimator.setDuration(200);
        addRowAnimator.setInterpolator(new OvershootInterpolator());

        removeRowAnimator = ValueAnimator.ofInt(rowHeight, 0);
        removeRowAnimator.setDuration(300);
        removeRowAnimator.setInterpolator(new AccelerateInterpolator());

        vIndicatorBg = new RectF();
        hIndicatorBg = new RectF();
        vIndicator = new RectF();
        hIndicator = new RectF();
        cellRectF = new RectF();

        matrix = new Matrix();

        bgPaint = new Paint();
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(cellBgColor);

        textPaint = new TextPaint();
        textPaint.setTextSize(textSize);
        textPaint.setColor(Color.BLACK);
        textPaint.setAntiAlias(true);

        linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(lineColor);
        linePaint.setStrokeCap(Paint.Cap.SQUARE);

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

    private int getContentWidth() {
        return header == null ? 0 : header.width;
    }

    private int getContentHeight() {
        return title.height + header.height + rows.size() * rowHeight;
    }

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
        return m[Matrix.MSCALE_Y];
    }

    private float getMatrixScaleX() {
        matrix.getValues(m);
        return m[Matrix.MSCALE_X];
    }

    private float dip2Px(float value) {
        return getResources().getDisplayMetrics().density * value;
    }

    private int getBaseLine(float centerY) {
        return (int) (((textPaint.descent() - textPaint.ascent()) / 2 - textPaint.descent()) + centerY);
    }

    public void setOnClick(OnClick onClick) {
        this.onClick = onClick;
    }

    public void showFixedColumn(boolean show) {
        this.showFixedColumn = show;
    }

    public Row newRow() {
        final Row row = new Row(rows.size(), rowHeight, columnsCount, new SparseIntArray());
        rows.add(row);
        if (addRowAnimator.isRunning()) {
            addRowAnimator.cancel();
        }
        addRowAnimator.removeAllUpdateListeners();
        addRowAnimator.removeAllListeners();
        addRowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                row.height = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        addRowAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                row.height = rowHeight;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                row.height = rowHeight;
            }
        });
        addRowAnimator.start();
        return row;
    }

    public void deleRow(final int rowIndex) {
        if (rowIndex < 0 || rowIndex > rows.size() - 1) {
            return;
        }
        final Row row = rows.get(rowIndex);
        if (removeRowAnimator.isRunning()) {
            removeRowAnimator.cancel();
        }
        removeRowAnimator.removeAllListeners();
        removeRowAnimator.removeAllUpdateListeners();
        removeRowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                row.height = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        removeRowAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationCancel(Animator animation) {
                removeRow(rowIndex);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                removeRow(rowIndex);
            }
        });
        removeRowAnimator.start();
    }

    private void removeRow(int rowIndex) {
        rows.remove(rowIndex);
        for (int i = rowIndex; i < rows.size(); i++) {
            rows.get(i).rowIndex = i;
        }
        focusedCell = null;
    }

    public void setCurrentCell(Cell cell) {
        focusedCell = cell;
        scrollToCell(cell);
    }

    public Cell getCurrentCell() {
        return this.focusedCell;
    }

    public void setDate(LocalDate localDate) {
        weekOfYear = localDate.get(WeekFields.ISO.weekOfWeekBasedYear());
        startDay = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int i = 1; i < 8; i++) {
            header.getCell(i).value = startDay.plusDays(i - 1).format(dateTimeFormatter);
        }
        invalidate();
    }

    public void setCurrentCell(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex > rows.size() - 1 || columnIndex < 0 || columnIndex >= columnsCount) {
            invalidate();
            return;
        }
        setCurrentCell(rows.get(rowIndex).getCell(columnIndex));
    }

    public int getColumnsCount() {
        return this.columnsCount;
    }

    public int getRowCount() {
        return rows.size();
    }

    // 第一次显示时，缩放到适合的宽度
    private void firstDraw() {
        isFirstDraw = false;
        int contentWidth = getContentWidth();
        defaultScaleValue = contentWidth == 0 ? 1 : (getWidth() - mPadding * 2 - indicatorWidth) / contentWidth;
        matrix.setScale(defaultScaleValue, defaultScaleValue);
        matrix.postTranslate(mPadding, 0);
    }

    private RectF computeEdges(@NonNull Cell cell) {
        RectF rectF = computeEdges(rows.get(cell.getRowIndex()));
        float cellLeft = rectF.left;
        float cellRight = 0;
        float cellTop = rectF.top;
        float cellBottom = rectF.bottom;

        for (Cell c : rows.get(cell.getRowIndex()).cells) {
            cellRight = cellLeft + c.width * getMatrixScaleX();
            if (c == cell)
                break;
            cellLeft = cellRight;
        }
        rectF.set(cellLeft, cellTop, cellRight, cellBottom);
        return rectF;
    }

    private RectF computeEdges(@NonNull Row row) {
        float left = getTranslateX();
        float right = left + row.width * getMatrixScaleX();
        float top = getTranslateY();
        if (row.rowIndex > -2) {
            top += title.height * getMatrixScaleY();
        }
        if (row.rowIndex > -1) {
            top += (row.rowIndex * rowHeight + header.height) * getMatrixScaleY();
        }
        float bottom = top + Math.max(rowHeight, row.height) * getMatrixScaleY();
        return new RectF(left, top, right, bottom);
    }

    private void scrollToCell(Cell cell) {
        if (cell == null) {
            invalidate();
            return;
        }
        int dx = 0, dy = 0;

        RectF tempRectF = computeEdges(cell);

        if (tempRectF.left < 0) {
            dx = (int) (0 - tempRectF.left);
        }
        if (tempRectF.right > getWidth() - indicatorWidth) {
            dx = (int) (getWidth() - indicatorWidth - tempRectF.right);
        }
        if (tempRectF.top < header.height * getMatrixScaleY()) {
            dy = (int) (header.height * getMatrixScaleY() - tempRectF.top);
        }
        if (tempRectF.bottom > getHeight() - indicatorWidth) {
            dy = (int) (getHeight() - indicatorWidth - tempRectF.bottom);
        }
        scroller.start((int) tempRectF.left, (int) tempRectF.top, dx, dy);
    }

    public void scrollToBottom() {
        scrollToCell(rows.size() - 1, 0);
    }

    public void scrollToTop() {
        scrollToCell(0, 0);
    }

    public void scrollToCell(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || columnIndex < 0 || rowIndex >= rows.size() || columnIndex >= columnsCount)
            return;
        scrollToCell(rows.get(rowIndex).getCell(columnIndex));
    }

    public void locateFocusCell() {
        scrollToCell(focusedCell);
    }

    private void onSigleTap(MotionEvent e) {
        float distansY = (e.getY() - getTranslateY()) / getMatrixScaleY() - title.height - header.height;
        int index = (int) (distansY / rowHeight);
        if (distansY < 0 || index < 0 || index >= rows.size())
            return;
        float width = e.getX() - getTranslateX();
        for (Cell cell : rows.get(index).cells) {
            width -= cell.width * getMatrixScaleX();
            if (width <= 0) {
                if (focusedCell != cell)
                    focusedCell = cell;
                else
                    focusedCell = null;
                onClick.onClick(cell);
                scrollToCell(cell);
                break;
            }
        }
    }

    private float[] computeScrollRange() {
        float[] floats = new float[2];
        floats[0] = getContentWidth() * getMatrixScaleX() - getWidth();
        if (floats[0] < 0)
            floats[0] = 0;

        floats[1] = getContentHeight() * getMatrixScaleY() - getHeight();
        if (floats[1] < 0)
            floats[1] = 0;
        return floats;
    }

    private void constrainScoll(float dx, float dy) {
        float leftEdge = mPadding,
                topEdge = mPadding,
                rightEdge = getWidth() - mPadding,
                bottomEdge = getHeight() - mPadding;
        RectF tempRectF = computeEdges(title);
        if (dx > 0 && tempRectF.left + dx > leftEdge) {
            dx = leftEdge - tempRectF.left;
        } else if (dx < 0) {
            if (tempRectF.right < rightEdge) {
                dx = 0;
            } else if (tempRectF.right + dx < rightEdge) {
                dx = rightEdge - tempRectF.right;
            }
        }
        if (dy > 0 && tempRectF.top + dy > topEdge) {
            dy = topEdge - tempRectF.top;
        } else if (dy < 0) {
            float bottom = tempRectF.bottom + header.height * getMatrixScaleY();
            bottom = rows.isEmpty() ? bottom : bottom + rows.size() * rowHeight * getMatrixScaleY();
            if (bottom < bottomEdge) {
                dy = 0;
            } else if (bottom + dy < bottomEdge) {
                dy = bottomEdge - bottom;
            }
        }
        matrix.postTranslate(dx, dy);
        invalidate();
    }

    private void drawFixedColumn(int index, Canvas canvas) {
        if (rows.isEmpty() || index < 0 || index >= rows.get(0).columnCount)
            return;
        RectF titleRectF = computeEdges(title);
        RectF firstRowRectF = computeEdges(rows.get(0).getCell(index));
        RectF lastRowRectF = computeEdges(rows.get(rows.size() - 1));
        float top = Math.max(0, titleRectF.bottom);
        float left = Math.min(0, Math.abs(firstRowRectF.left) - firstRowRectF.width());
        float bottom = Math.min(getBottom() - indicatorWidth, lastRowRectF.bottom);
        float right = left + firstRowRectF.width();
        if (firstRowRectF.left < -mPadding) {
            bgPaint.setColor(getResources().getColor(R.color.colorAccent, null));

            canvas.drawRect(left, top, right, bottom, bgPaint);
            textPaint.setTextSize(textSize * getMatrixScaleX());
            textPaint.setStrokeWidth(2f);
            for (Row row : rows) {
                Cell cell = row.getCell(index);
                RectF rectF = computeEdges(cell);
                if (rectF.bottom > top && rectF.top < getHeight()) {
                    rectF.offsetTo(left, rectF.top);
                    canvas.drawLine(rectF.left, rectF.bottom, rectF.right, rectF.bottom, textPaint);
                    drawText(canvas, rectF, cell.value.toString());
                }
            }
            bgPaint.setColor(getResources().getColor(R.color.colorHeaderBg, null));
            RectF rectF2 = new RectF(left, top, right + 2, top + header.height * getMatrixScaleY());
            canvas.drawRect(rectF2, bgPaint);
            canvas.drawText(header.getCell(index).value.toString(), rectF2.centerX(), getBaseLine(rectF2.centerY()), textPaint);
        }
    }

    private void drawHeader(@NonNull Canvas canvas) {
        RectF tempRectF = computeEdges(header);
        if (tempRectF.bottom > 0 && tempRectF.top < getHeight()) {
            float top = Math.max(0, tempRectF.top);
            float left = tempRectF.left;
            float right = tempRectF.right;
            float bottom = top + tempRectF.height();
            tempRectF.set(left, top, right, bottom);
            textPaint.setTextSize(textSize * getMatrixScaleX());
            textPaint.setStrokeWidth(6f * getMatrixScaleX());
            bgPaint.setColor(hearBgColor);
            canvas.drawRect(tempRectF, bgPaint);

            linePaint.setStrokeWidth(6f * getMatrixScaleX());
            canvas.drawLine(left, top, right, top, linePaint);
            canvas.drawLine(left, top, left, bottom, linePaint);
            canvas.drawLine(right, top, right, bottom, linePaint);
            linePaint.setStrokeWidth(rows.isEmpty() ? 6f * getMatrixScaleX() : 3f * getMatrixScaleX());
            canvas.drawLine(left, bottom, right, bottom, linePaint);
            linePaint.setStrokeWidth(3f * getMatrixScaleX());
            float cellLeft = left;
            float cellRight;
            for (int i = 0; i < header.columnCount; i++) {
                Cell cell = header.getCell(i);
                cellRight = cellLeft + cell.width * getMatrixScaleX();
                tempRectF.set(cellLeft, top, cellRight, bottom);
                drawText(canvas, tempRectF, header.getCell(i).value.toString());
                if (i < header.columnCount - 1)
                    canvas.drawLine(tempRectF.right, tempRectF.top, tempRectF.right, tempRectF.bottom, linePaint);
                cellLeft = cellRight;
            }

        }
    }

    private void drawIndicator(Canvas canvas) {
        float[] ranges = computeScrollRange();

        if (ranges[0] > 0) {
            hIndicatorBg.set(0, getHeight() - indicatorWidth, getWidth() - indicatorWidth, getHeight());
            float scaleX = hIndicatorBg.width() / (getContentWidth() * getMatrixScaleX());
            float indicatorSize = hIndicatorBg.width() * scaleX;
            float left = Math.max(0, -getTranslateX() * scaleX);
            hIndicator.set(left, hIndicatorBg.top, left + indicatorSize, hIndicatorBg.bottom);
            bgPaint.setColor(getResources().getColor(R.color.colorIndicatorBg, null));
            canvas.drawRect(hIndicatorBg, bgPaint);
            bgPaint.setColor(getResources().getColor(R.color.colorIndicator, null));
            canvas.drawRect(hIndicator, bgPaint);
        }

        if (ranges[1] > 0) {
            vIndicatorBg.set(getWidth() - indicatorWidth, 0, getWidth(), getHeight());
            float scaleY = getHeight() / (getContentHeight() * getMatrixScaleX());
            float indicatorSize = getHeight() * scaleY;
            float top = Math.max(0, -getTranslateY() * scaleY);
            vIndicator.set(vIndicatorBg.left, top, vIndicatorBg.right, top + indicatorSize);
            bgPaint.setColor(getResources().getColor(R.color.colorIndicatorBg, null));
            canvas.drawRect(vIndicatorBg, bgPaint);
            bgPaint.setColor(getResources().getColor(R.color.colorIndicator, null));
            canvas.drawRect(vIndicator, bgPaint);
        }
    }

    private void drawCells(@NonNull Canvas canvas) {
        float top = getTranslateY() + (title.height + header.height) * getMatrixScaleY();
        float bottom;
        bgPaint.setColor(cellBgColor);
        for (Row row : rows) {
            float left = getTranslateX();
            float right = left + row.width * getMatrixScaleX();
            bottom = top + row.height * getMatrixScaleY();
            if (bottom > 0 && top < getHeight()) {
                float cellLeft = left;
                float cellRight;
                for (Cell cell : row.cells) {
                    cellRight = cellLeft + cell.width * getMatrixScaleX();
                    cellRectF.set(cellLeft, top, cellRight, bottom);
                    if (cellRight > 0 && cellLeft < getWidth()) {
                        bgPaint.setColor(cellBgColor);
                        textPaint.setTextSize(cell == focusedCell ? textSize * 1.3f * getMatrixScaleX() : textSize * getMatrixScaleX());
                        textPaint.setStrokeWidth(cell == focusedCell ? 6f * getMatrixScaleX() : 3f * getMatrixScaleX());
                        canvas.drawRect(cellRectF, bgPaint);
                        if (cell == focusedCell) {
                            float offset = 6f * getMatrixScaleX();
                            cellRectF.inset(offset, offset);
                            float radius = cellRectF.height() / 4;
                            bgPaint.setColor(cellFocusedColor);
                            canvas.drawRoundRect(cellRectF, radius, radius, bgPaint);
                        }
                        if (cell.getColumnIndex() < row.columnCount - 1) {
                            canvas.drawLine(cellRight, top, cellRight, bottom, linePaint);
                        }
                        drawText(canvas, cellRectF, cell.value.toString());
                    }
                    cellLeft = cellRight;
                }
                linePaint.setStrokeWidth(6f * getMatrixScaleX());
                canvas.drawLine(left, top, left, bottom, linePaint);
                canvas.drawLine(right, top, right, bottom, linePaint);
                linePaint.setStrokeWidth(row.rowIndex == rows.size() - 1 ? 6f * getMatrixScaleX() : 3f * getMatrixScaleX());
                canvas.drawLine(left, bottom, right, bottom, linePaint);
            }
            top = bottom;
        }
    }

    private void drawText(Canvas canvas, RectF rectF, CharSequence text) {
        float cellPadding = 25 * getMatrixScaleX();
        int width = (int) (rectF.width() - cellPadding * 2);
        StaticLayout.Builder builder = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, width);
        StaticLayout staticLayout = builder.setMaxLines(3)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setLineSpacing(0, 1.3f)
                .setAlignment(cellAlignment)
                .build();
        float dx = rectF.left + cellPadding;
        float dy = rectF.top + (rectF.height() - staticLayout.getHeight()) / 2;
        if (rectF.height() > staticLayout.getHeight()) {
            canvas.save();
            canvas.translate(dx,dy);
            staticLayout.draw(canvas);
            canvas.restore();
        }
    }

//    private void drawLines(@NonNull Canvas canvas) {
//        RectF rectF = computeEdges(header);
//        float startX = rectF.left;
//        float endX = rectF.right;
//        float startY = rectF.top;
//        float endY = rows.isEmpty() ? rectF.bottom : computeEdges(rows.get(rows.size() - 1)).bottom;
//
//        float x1 = Math.max(0, startX);
//        float x2 = Math.min(getWidth(), endX);
//        float y1 = Math.max(0, startY);
//        float y2 = Math.min(getHeight(), endY);
//
//        bgPaint.setColor(Color.GRAY);
//        bgPaint.setStrokeWidth(6f * getMatrixScaleX());
//        if (startY >= 0) {
//            canvas.drawLine(x1, startY, x2, startY, bgPaint);
//        }
//        if (endY < getHeight()) {
//            canvas.drawLine(x1, endY, x2, endY, bgPaint);
//        }
//        if (startX >= 0) {
//            canvas.drawLine(startX, y1, startX, y2, bgPaint);
//        }
//        if (endX < getWidth()) {
//            canvas.drawLine(endX, y1, endX, y2, bgPaint);
//        }
//
//        bgPaint.setStrokeWidth(3f * getMatrixScaleX());
//        float totalRowHeight = 0;
//        for (Row row : rows) {
//            float y = getTranslateY() + (mTitleHeight + header.height + totalRowHeight) * getMatrixScaleY();
//            if (y > 0 && y < getHeight())
//                canvas.drawLine(x1, y, x2, y, bgPaint);
//            totalRowHeight += row.height;
//        }
//        float right = startX;
//
//        for (int i = 1; i < header.columnCount; i++) {
//            Cell cell = header.getCell(i);
//            right += cell.width * getMatrixScaleX();
//            if (right > 0 && right < getWidth())
//                canvas.drawLine(right, y1, right, y2, bgPaint);
//        }
//    }

    private void drawTitle(@NonNull Canvas canvas) {
        bgPaint.setColor(Color.WHITE);

        RectF rectF = computeEdges(title);
        canvas.drawRect(rectF, bgPaint);

        textPaint.setTextSize(titleTextSize * getMatrixScaleX());
        textPaint.setStrokeWidth(10f * getMatrixScaleX());
        canvas.drawText(mTitle, rectF.centerX(), getBaseLine(rectF.centerY()), textPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isFirstDraw)
            firstDraw();
        canvas.drawColor(Color.parseColor("#efefef"));
        drawTitle(canvas);
        drawCells(canvas);
        drawHeader(canvas);
        if (showFixedColumn)
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

    public MyFormView(Context context) {
        super(context);
        initial(context);
    }

    public MyFormView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initial(context);
    }

    public MyFormView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initial(context);
    }

    public MyFormView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initial(context);
    }

    //定义 Cell点击接口
    public interface OnClick {
        void onClick(Cell cell);
    }

    // Row类
    public static class Row {
        public int rowIndex;
        public int width, height;
        private int defaultCellWidth = 300;
        private int columnCount = 8;
        public Cell[] cells;

        public Row(int rowIndex, int height, int columnCount, SparseIntArray widthArray) {
            super();
            this.rowIndex = rowIndex;
            this.columnCount = columnCount;
            this.height = height;
            this.width = 0;
            this.cells = new Cell[columnCount];
            for (int i = 0; i < columnCount; i++) {
                int cellWidth = widthArray.get(i, defaultCellWidth);
                cells[i] = new Cell(this, i);
                cells[i].width = cellWidth;
                cells[i].height = height;
                this.width += cellWidth;
            }
        }

        public Cell getCell(int index) {
            return cells[index];
        }
    }

    // Cell类
    public static class Cell {
        private Row row;
        private int columnIndex;
        public int width, height;
        public Object value = "";

        public int getColumnIndex() {
            return this.columnIndex;
        }

        public int getRowIndex() {
            return this.row.rowIndex;
        }

        public Cell(Row row, int columnIndex) {
            super();
            this.row = row;
            this.columnIndex = columnIndex;
//            this.value = "第 " + (row.rowIndex + 1) + " 行\n第 " + (columnIndex + 1) + " 列";
            this.value = "";
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof Cell) {
                Cell other = (Cell) obj;
                return other.row.rowIndex == this.row.rowIndex &&
                        other.columnIndex == this.columnIndex;
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
                RectF tempRectF = computeEdges(title);
                x = (int) tempRectF.left;
                y = (int) tempRectF.top;
                int minX = (int) (getWidth() - getContentWidth() * getMatrixScaleX() - mPadding);
                int maxX = (int) (mPadding);
                int minY = (int) (getHeight() - getContentHeight() * getMatrixScaleY() - mPadding);
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
}
