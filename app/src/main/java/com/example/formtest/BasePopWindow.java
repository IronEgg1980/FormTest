package com.example.formtest;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BasePopWindow extends PopupWindow {
    private int screenWidth,screenHeight;
//    public interface OnPopWindowlClickListener {
//        void onClick(Object... values);
//    }
//
//    private OnPopWindowlClickListener clickListener;

    private void getScreenDim(Activity activity){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
    }

    public BasePopWindow(Activity activity, BaseAdapter baseAdapter) {
        getScreenDim(activity);
        initialView(activity,baseAdapter);
        setOutsideTouchable(true);
        setFocusable(true);
        setTouchable(true);
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setWidth(screenWidth);
        setHeight(screenHeight / 5);
    }

    private void initialView(Activity activity,BaseAdapter baseAdapter) {
        View view = LayoutInflater.from(activity).inflate(R.layout.edittext_popwindow_layout, null);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(baseAdapter);
        view.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        setContentView(view);
    }

    private int[] getLocation(FormView anchor){
        int[] result = new int[2];
        FormView.Cell cell = anchor.getCurrentCell();
        if (cell != null) {
            int[] position = new int[2];
            anchor.getLocationOnScreen(position);

            int x = (int) (cell.rect.right + 0.5f) - anchor.getScrollX() + position[0] +  50;
            int y = (int) (cell.rect.bottom + 0.5f) - anchor.getScrollY() + position[1] + 50;

            if (x + getWidth() > screenWidth) {
                x = (int) (cell.rect.left + 0.5f) - anchor.getScrollX() - getWidth() - 50 + position[0];
            }
            if (y + getHeight() > screenHeight) {
                y = (int) (cell.rect.top + 0.5f) - anchor.getScrollY() - getHeight() - 50 + position[1];
            }
            result[0] = x;
            result[1] = y;
        }
        return result;
    }

    public void show(FormView parent) {
        FormView.Cell cell = parent.getCurrentCell();
        if (cell == null)
            return;
        int[] location = getLocation(parent);
        showAtLocation(parent, Gravity.NO_GRAVITY, location[0], location[1]);
    }

    public void update(FormView parent) {
        FormView.Cell cell = parent.getCurrentCell();
        if (cell == null) {
            dismiss();
        } else {
            int[] location = getLocation(parent);
            update(location[0], location[1], -1, -1);
        }
    }
}
