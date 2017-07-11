package com.pokectwallet.hzhyq.refreshview;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;

import com.pokectwallet.hzhyq.refreshview.databinding.RefreshHeadViewBinding;

/**
 * Created by hzhyq on 2017/7/10.
 */

public class RefreshView extends LinearLayout {
    private static final String TAG = RefreshView.class.getSimpleName();
    //未触发的正常状态
    private static final int NOMAL = 0;
    //正在下拉
    private static final int REFRESH_IN_PULLDOWN = 1;
    //刷新时释放
    private static final int REFRESH_IN_RELEASE = 2;
    //正在刷新
    private static final int REFRESHING = 3;
    //刷新成功
    private static final int REFRESH_IN_SUCCESS = 4;
    //刷新失败
    private static final int REFRESH_IN_FAIL = 5;

    private RefreshListener refreshListener;
    private View refreshHeadView;
    private int headViewTopMargin = 0;
    private static int maxTopMargin;
    private int lastY;
    // 是否可刷新标记
    private boolean isRefreshEnabled = true;
    private int refreshState = NOMAL;
    private RefreshHeadViewBinding binding;
    private Context context;

    public interface RefreshListener {
        void onFresh();
    }

    public RefreshView(Context context) {
        this(context, null);
    }

    public RefreshView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.refresh_head_view, null, false);
        refreshHeadView = binding.getRoot();
        headViewTopMargin = (int) DensityUtil.dip2px(getContext(), 40);
        maxTopMargin = (int) DensityUtil.dip2px(getContext(), 70);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, headViewTopMargin);
        layoutParams.topMargin = -headViewTopMargin;
        layoutParams.gravity = Gravity.CENTER;
        setOrientation(VERTICAL);
        addView(binding.getRoot(), layoutParams);
    }

    public void setRefreshListener(RefreshListener refreshListener) {
        this.refreshListener = refreshListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int currentY = (int) event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastY = currentY;
                refreshInPullDown();
                break;
            case MotionEvent.ACTION_MOVE:
                handleMove(currentY);
                lastY = currentY;
                break;
            case MotionEvent.ACTION_UP:
                refreshInRelease();
                break;
        }
        return true;
    }

    private void handleMove(int currentY) {
        int pullSpace = currentY - lastY;
        LayoutParams l = (LayoutParams) refreshHeadView.getLayoutParams();
        float rate = 1f - (float)currentY / (float)getHeight();
        Log.i(TAG, "rate " + rate);
        int calMoveY = (int) (l.topMargin + pullSpace * rate);
        l.topMargin = calMoveY;
        refreshHeadView.setLayoutParams(l);
        refreshInPullDown();
        invalidate();
        if (calMoveY > maxTopMargin) {
            binding.txtStatus.setText("释放立即刷新");
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int y = (int) ev.getRawY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastY = y;
                return false;
            case MotionEvent.ACTION_MOVE:
                return y > lastY && shouldPull();
        }
        return false;
    }

    private boolean shouldPull() {
        if (getChildCount() > 1) {
            View child = getChildAt(1);
            //只处理recyclerview
            if (child instanceof RecyclerView) {
                RecyclerView.LayoutManager manager = ((RecyclerView) child).getLayoutManager();
                int position = 0;
                if (manager instanceof LinearLayoutManager) {
                    position = ((LinearLayoutManager) manager).findFirstVisibleItemPosition();
                } else if (manager instanceof StaggeredGridLayoutManager) {
                    position = ((StaggeredGridLayoutManager) manager).findFirstVisibleItemPositions(null)[0];
                }
                if (position == 0 && ((RecyclerView) child).getChildAt(0).getY() == 0) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    void refreshInPullDown() {
        binding.txtStatus.setText("下拉刷新");
    }

    void refreshInRelease() {
        refreshing();
        LayoutParams l = (LayoutParams) refreshHeadView.getLayoutParams();
        TranslateAnimation animator = new TranslateAnimation(0, 0, 0, -l.topMargin);
        animator.setDuration(500);
        animator.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                LayoutParams l = (LayoutParams) refreshHeadView.getLayoutParams();
                l.topMargin = 0;
                refreshHeadView.setLayoutParams(l);
                refreshHeadView.invalidate();
                request();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        startAnimation(animator);
    }

    private void request() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (refreshListener != null) {
                    refreshListener.onFresh();
                }
            }
        }, 500);
    }

    void refreshing() {
        binding.txtStatus.setText("正在刷新");
    }

    void refreshSucceed() {
        binding.txtStatus.setText("刷新成功");
        translateToOrigin();
    }

    void refreshFailed() {
        binding.txtStatus.setText("刷新失败");
        translateToOrigin();
    }

    public void finishRefresh(boolean result) {
        if (result) {
            refreshSucceed();
        } else {
            refreshFailed();
        }
    }

    void translateToOrigin() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                final LayoutParams l = (LayoutParams) refreshHeadView.getLayoutParams();
                TranslateAnimation animation = new TranslateAnimation(0, 0, 0, -headViewTopMargin);
                animation.setDuration(300);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        l.topMargin = -headViewTopMargin;
                        refreshHeadView.setLayoutParams(l);
                        refreshHeadView.invalidate();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                startAnimation(animation);

            }
        }, 500);
    }
}
