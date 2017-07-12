package com.pokectwallet.hzhyq.refreshview;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.IntDef;
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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

    @IntDef({NOMAL, REFRESH_IN_PULLDOWN, REFRESH_IN_RELEASE, REFRESHING, REFRESH_IN_SUCCESS, REFRESH_IN_FAIL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {

    }

    private RefreshListener refreshListener;
    private LinearLayout refreshHeadView;
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
        setRefreshState(NOMAL);
        binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.refresh_head_view, null, false);
        refreshHeadView = binding.container;
        headViewTopMargin = (int) DensityUtil.dip2px(getContext(), 40);
        maxTopMargin = (int) DensityUtil.dip2px(getContext(), 70);
        LayoutParams layoutParams = (LayoutParams) refreshHeadView.getLayoutParams();
        layoutParams.topMargin = -headViewTopMargin;
        refreshHeadView.setLayoutParams(layoutParams);
        setGravity(Gravity.CENTER);
        setOrientation(VERTICAL);
        addView(binding.getRoot());
    }

    public void setRefreshListener(RefreshListener refreshListener) {
        this.refreshListener = refreshListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isRefreshEnabled) {
            return false;
        }
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
                if (getRefreshState() <= REFRESH_IN_PULLDOWN) {
                    startHeadViewAnimation(((LayoutParams) refreshHeadView.getLayoutParams()).topMargin, -headViewTopMargin,300,null);
                } else {
                    refreshInRelease();
                }
                break;
        }
        return true;
    }

    private void startHeadViewAnimation(int startHeight, final int endHeight, int duration,@Nullable final Runnable runnable) {
        ObjectAnimator animator = ObjectAnimator.ofInt(refreshHeadView, "topMargin",startHeight,endHeight)
                .setDuration(duration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Log.i(TAG,"update "+animation.getAnimatedValue());
                LayoutParams params = (LayoutParams) refreshHeadView.getLayoutParams();
                params.topMargin = (int) animation.getAnimatedValue();
                refreshHeadView.setLayoutParams(params);
                invalidate();
                if(((int)animation.getAnimatedValue()) == endHeight){
                    if(runnable != null){
                        runnable.run();
                    }
                }
            }
        });
        animator.start();
    }

    private void handleMove(int currentY) {
        setRefreshState(REFRESH_IN_PULLDOWN);
        int pullSpace = currentY - lastY;
        LayoutParams l = (LayoutParams) refreshHeadView.getLayoutParams();
        double rate = Math.cos((float) currentY / (float) getHeight() * Math.PI / 2);
        // Log.i(TAG, "rate " + rate);
        int calMoveY = (int) (l.topMargin + pullSpace * (rate <= 0.2 ? 0.2 : rate));
        l.topMargin = calMoveY;
        refreshHeadView.setLayoutParams(l);
        refreshInPullDown();
        invalidate();
        if (calMoveY > maxTopMargin) {
            setRefreshState(REFRESH_IN_RELEASE);
            binding.txtStatus.setText("释放立即刷新");
        }
    }

    public int getRefreshState() {
        return refreshState;
    }

    public void setRefreshState(@State int refreshState) {
        this.refreshState = refreshState;
        if (refreshState > REFRESH_IN_RELEASE) {
            isRefreshEnabled = false;
        } else {
            isRefreshEnabled = true;
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
        setRefreshState(REFRESH_IN_RELEASE);
        refreshing();
        final LayoutParams l = (LayoutParams) refreshHeadView.getLayoutParams();
        startHeadViewAnimation(l.topMargin, 0, 300, new Runnable() {
            @Override
            public void run() {
                request();
            }
        });
    }

    private void request() {
        setRefreshState(REFRESHING);
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
        setRefreshState(REFRESH_IN_SUCCESS);
        binding.txtStatus.setText("刷新成功");
        translateToOrigin();
    }

    void refreshFailed() {
        setRefreshState(REFRESH_IN_FAIL);
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
                        setRefreshState(NOMAL);
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
