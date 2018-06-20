package com.lzp.test.refreshablerecyclerview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * 为RecyclerView增加下拉刷新功能
 * 通过给RecyclerView的Adapter增加一个代理ProxyAdapter实现。
 * ProxyAdapter中起始位置增加了一下下拉刷新RefreshHeaderLayout，设置的下拉刷新布局将作为这个RefreshHeaderLayout的子view存在。
 */
public class RefreshableRecyclerView extends RecyclerView {
    private static final int INVALID_POINTER = -1;

    private int STATE_IDLE = -1;
    private int STATE_PULLING = 2;
    private int STATE_REFRESHING = 3;
    private boolean mInit = false;

    private boolean mEnableRefreshingheader;
    private RefreshHeaderLayout mHeaderlayout;
    private View mHeader;

    private int mLastTouchX, mLastTouchY;
    private int mTouchSlop;
    private float mMulitiplier = 0.5f;
    private int mDraggedPointerId = INVALID_POINTER;
    private int mLastDraggedPointerId = INVALID_POINTER;

    private RefreshListener mRefreshListener;
    private int mState = STATE_IDLE;

    public interface RefreshListener {
        void onPullingDown(int dy);

        void onRefreshing();
    }

    public RefreshableRecyclerView(Context context) {
        this(context, null);
    }

    public RefreshableRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshableRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        Log.e("Test", "mTouchSlop=" + mTouchSlop);
    }

    public void setRefreshHeader(View view) {
        if (mHeader != null) {
            removeRefreshHeader();
        }
        if (mHeader != view) {
            mHeader = view;
            initRefreshHeaderLayout();
            mHeaderlayout.addView(mHeader);
        }
    }

    public void setRefreshHeader(int resId) {
        initRefreshHeaderLayout();
        View view = LayoutInflater.from(getContext()).inflate(resId, mHeaderlayout, false);
        if (view != null) {
            setRefreshHeader(view);
        }
    }

    public void setRefreshListener(RefreshListener listener) {
        mRefreshListener = listener;
    }

    /**
     * 是否开启下拉刷新
     * 开启后，将会禁用RecyclerView的OVER_SCROLL
     *
     * @param enable
     */
    public void enableRefreshHeader(boolean enable) {
        mEnableRefreshingheader = enable;
        if (mEnableRefreshingheader) {
            setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        initRefreshHeaderLayout();
        super.setAdapter(new ProxyAdapter(adapter, mHeaderlayout));
    }

    private void removeRefreshHeader() {
        if (mHeaderlayout != null) {
            mHeaderlayout.removeView(mHeader);
        }
    }

    //初始化RefreshHeaderLayout，设置高度为0
    private void initRefreshHeaderLayout() {
        if (mHeaderlayout == null) {
            mHeaderlayout = new RefreshHeaderLayout(getContext());
            mHeaderlayout.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        }
    }

    private void setState(int state) {
        if (mState == state) return;
        mState = state;
    }

    private void resetFlag() {
        mInit = false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (mEnableRefreshingheader && isRefreshHeaderVisiable()) {
            boolean intercept = false;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    //只有在state为pulling或refreshing时，才需要拦截
                    if (mState != STATE_IDLE) {
                        intercept = true;
                    }
                    break;
                //为什么不加上ACTION_MOVE的判断？因为在满足isRefreshHeaderVisiable()这个的条件下，不会有ACTION_MOVE事件传递到这里，所以不需要。
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    //只有在state为pulling或refreshing时，需要拦截
                    if (mState != STATE_IDLE) {
                        intercept = true;
                    }
                    break;
            }
            if (intercept) {
                return true;
            }
        }
        return super.onInterceptTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        final int actionIdenx = e.getActionIndex();

        if (mEnableRefreshingheader && isRefreshHeaderVisiable()) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mDraggedPointerId = e.getPointerId(0);
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    mDraggedPointerId = e.getPointerId(actionIdenx);
                    break;
                case MotionEvent.ACTION_MOVE:
                    final int index = e.findPointerIndex(mDraggedPointerId);
                    if (index < 0) {
                        Log.e("Test", "Error processing scroll; pointer index for id "
                                + mDraggedPointerId + " not found. Did any MotionEvents get skipped?");
                        return false;
                    }

                    //记录上次事件的坐标。header的拖动，是根据最近两次事件的坐标差值计算的
                    //如果是多点触控，重新计算起始点坐标
                    if (!mInit || (mLastDraggedPointerId != mDraggedPointerId)) {
                        mLastTouchX = (int) e.getX(index);
                        mLastTouchY = (int) e.getY(index);
                        mInit = true;
                        mLastDraggedPointerId = mDraggedPointerId;
                    }

                    int curX = (int) e.getX(index);
                    int curY = (int) e.getY(index);
                    int deltaY = curY - mLastTouchY;
                    int deltaX = curX - mLastTouchX;

                    if (isIninitState() && deltaY < 0) {//recyclerview在最初的位置，现在要往上滑动，需要重置状态，让RecyclerView处理向上滑动
                        setState(STATE_IDLE);//重置状态
                    } else if (mState != STATE_PULLING && Math.abs(deltaY) > mTouchSlop) {//首次拖动header时，需要判断拖动的距离是否大于mTouchSlop
                        setState(STATE_PULLING);
                    }

                    if (mState == STATE_PULLING) {
                        setRefreshHeaderLayoutHeight(deltaY);
                        mLastTouchY = curY;
                        mLastTouchX = curX;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    enterRefreshingState();
                    resetFlag();
                    break;
            }
            if (mState == STATE_PULLING || mState == STATE_REFRESHING) {
                return true;
            }
        }

        return super.onTouchEvent(e);
    }

    /**
     * 设置RefreshHeaderLayout的高度
     *
     * @param dy dy>0向下滑动 dy<0向上滑动
     */
    private void setRefreshHeaderLayoutHeight(int dy) {
        int realDy = (int) (dy * mMulitiplier);//乘以一个系数，让拖动产生阻尼效果
        if (mHeaderlayout != null) {
            ViewGroup.LayoutParams params = mHeaderlayout.getLayoutParams();
            if (params.height == 0 && realDy <= 0) {
                return;
            }
            params.height += realDy;
            if (params.height < 0) {
                params.height = 0;
            }
            mHeaderlayout.requestLayout();
        }

        if (mRefreshListener != null) {
            mRefreshListener.onPullingDown(realDy);
        }
    }

    /**
     * 进入到刷新状态
     */
    private void enterRefreshingState() {
        if (mHeaderlayout != null) {
            final ViewGroup.LayoutParams params = mHeaderlayout.getLayoutParams();
            if (canEnterRefreshing(params.height)) {
                fixRefreshHeaderlayoutHeigt();
                setState(STATE_REFRESHING);

                if (mRefreshListener != null) {
                    mRefreshListener.onRefreshing();
                }
            } else {
                resetRefreshHeaderLayoutHeightWithAnim();
            }
        }
    }

    /**
     * 当RefreshingHeaderLayout的高度大于RefreshingHeaderLayout中的第一个child的高度时，
     * 将RefreshingHeaderLayout的高度设置为第一个child的高度
     */
    private void fixRefreshHeaderlayoutHeigt() {
        if (mHeaderlayout != null) {
            View child = mHeaderlayout.getChildAt(0);
            if (child != null) {
                if (mHeaderlayout.getHeight() > child.getHeight()) {
                    mHeaderlayout.getLayoutParams().height = child.getHeight();
                    mHeaderlayout.requestLayout();
                }
            }
        }
    }

    /**
     * 判断RefreshingHeaderLayout中的第一个child是否完全显示出来
     *
     * @param height
     * @return
     */
    private boolean canEnterRefreshing(int height) {
        if (mHeaderlayout != null) {
            View child = mHeaderlayout.getChildAt(0);
            if (child != null) {
                return height > child.getHeight();
            }
        }
        return false;
    }

    public void stopRefresh() {
        if (mState == STATE_REFRESHING) {
            resetRefreshHeaderLayoutHeightWithAnim();
        }
    }

    private void resetRefreshHeaderLayoutHeightWithAnim() {
        final ViewGroup.LayoutParams params = mHeaderlayout.getLayoutParams();
        int height = params.height;
        if (height > 0) {
            ValueAnimator animator = ValueAnimator.ofInt(height, 0);
            animator.setDuration(200);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int value = (int) animation.getAnimatedValue();
                    if (value == 0) {
                        setState(STATE_IDLE);
                    }
                    params.height = value;
                    mHeaderlayout.requestLayout();
                }
            });
            animator.start();
        }
    }

    /**
     * 判断是否可以拖动RefreshHeaderLayout
     *
     * @return
     */
    private boolean isRefreshHeaderVisiable() {
        if (getAdapter() == null || getAdapter().getItemCount() == 0) {
            return false;
        }
        int pos = ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
        if (pos == 1) {
            return getLayoutManager().getChildAt(0).getTop() == 0;
        } else if (pos == 0) {
            return true;
        }
        return false;
    }

    /**
     * 判断第一个显示的item的pos是否为1，并且这个item的top==0，即没有拖动也没有滑动的状态(初始状态)
     *
     * @return
     */
    private boolean isIninitState() {
        if (getAdapter() == null || getAdapter().getItemCount() == 0) {
            return false;
        }
        int pos = ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
//        Log.e("Test", "FirstVisibleItemPosition =" + pos);
        if (pos == 1) {
//            Log.e("Test", "FirstVisibleItemPosition top=" + getLayoutManager().getChildAt(0).getTop());
            return getLayoutManager().getChildAt(0).getTop() == 0;
        }
        return false;
    }

    /**********************************************************************/
    /*                             ProxyAdapter                           */

    /**********************************************************************/
    public static class ProxyAdapter extends RecyclerView.Adapter<ViewHolder> {
        private static final int ITEM_TYPE_HEADER = Integer.MIN_VALUE;
        private Adapter mBase;
        private RefreshHeaderLayout mHeaderLayout;

        public ProxyAdapter(RecyclerView.Adapter base, RefreshHeaderLayout headerLayout) {
            mBase = base;
            mHeaderLayout = headerLayout;
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            if (mBase != null) {
                mBase.onAttachedToRecyclerView(recyclerView);
            }
        }

        @Override
        public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
            if (mBase != null) {
                mBase.onViewAttachedToWindow(holder);
            }
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
            if (mBase != null) {
                mBase.onViewDetachedFromWindow(holder);
            }
        }


        @Override
        public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
            if (mBase != null) {
                mBase.onDetachedFromRecyclerView(recyclerView);
            }
        }

        @Override
        public int getItemCount() {
            return mBase.getItemCount() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return ITEM_TYPE_HEADER;
            }
            if (position > 0 && position < mBase.getItemCount() + 1) {
                return mBase.getItemViewType(position - 1);
            }
            throw new IllegalArgumentException("Wrong type! position = " + position);
        }


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == ITEM_TYPE_HEADER) {
                return new HeaderViewHolder(this.mHeaderLayout);
            } else {
                return mBase.onCreateViewHolder(parent, viewType);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position > 0 && position < this.mBase.getItemCount()) {
                this.mBase.onBindViewHolder(holder, position - 1);
            }
        }

        public static class HeaderViewHolder extends ViewHolder {
            public HeaderViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
