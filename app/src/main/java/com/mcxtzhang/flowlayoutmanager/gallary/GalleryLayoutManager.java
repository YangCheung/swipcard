package com.mcxtzhang.flowlayoutmanager.gallary;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * 介绍：一个酷炫画廊效果，假设所有Item大小一样
 * 作者：zhangxutong
 * 邮箱：mcxtzhang@163.com
 * 主页：http://blog.csdn.net/zxt0601
 * 时间： 2016/12/23.
 */

public class GalleryLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = "zxt/画廊";
    //private int mFirstVisiblePosition, mLastVisiblePosition;

    private int mChildWidth, mChildHeight;

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
        if (getItemCount() == 0) {//没有Item，界面空着吧
            detachAndScrapAttachedViews(recycler);
            return;
        }
        if (getChildCount() == 0 && state.isPreLayout()) {//state.isPreLayout()是支持动画的
            return;
        }
        //onLayoutChildren方法在RecyclerView 初始化时 会执行两遍
        detachAndScrapAttachedViews(recycler);

        View firstView = recycler.getViewForPosition(0);
        addView(firstView);
        measureChildWithMargins(firstView, 0, 0);
        mChildWidth = getDecoratedMeasuredWidth(firstView);
        mChildHeight = getDecoratedMeasuredHeight(firstView);
        removeAndRecycleView(firstView, recycler);

        //mFirstVisiblePosition = 0;

        fill(recycler, state);


    }


    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int realOffset;
        //先考虑滑动位移进行View的回收、填充(fill（）函数)，然后再真正的位移这些子Item。
        realOffset = fill(recycler, state, dx);
        offsetChildrenHorizontal(-realOffset);
        return realOffset;
    }


    private void fill(RecyclerView.Recycler recycler, RecyclerView.State state) {
        fill(recycler, state, 0);
    }

    /**
     * @param recycler
     * @param state
     * @param dx       >0,load more , <0.load left
     * @return
     */
    public int fill(RecyclerView.Recycler recycler, RecyclerView.State state, int dx) {
        Log.d(TAG, "fill() called with: recycler = [" + recycler + "], state = [" + state + "], dx = [" + dx + "]");
        //step 1 :回收越界子View
        int childCount = getChildCount();
        if (childCount > 0 && dx != 0) {
            for (int i = childCount - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (dx > 0) {
                    //load right,recycle left
                    //child的右边不再屏幕内 recycle
                    if (getDecoratedRight(child) - dx < getPaddingLeft()) {
                        removeAndRecycleView(child, recycler);
                    } else {
                        //mFirstVisiblePosition = i;
                        continue;
                    }
                } else {
                    //load left,recycle right
                    //child 的左边 不在屏幕内 recycle
                    if (getDecoratedLeft(child) - dx > getWidth() - getPaddingRight()) {
                        removeAndRecycleView(child, recycler);
                    } else {
                        //mLastVisiblePosition = i;
                        continue;
                    }
                }
            }
        }

        int itemCount = getItemCount();
        if (dx >= 0) {
            View child;
            int startPos = 0;
            int left = getPaddingLeft();
            int top = getPaddingTop();

            //如果界面上还有子View
            if (getChildCount() > 0) {
                child = getChildAt(getChildCount() - 1);
                int lastPosition = getPosition(child);
                startPos = lastPosition + 1;
                left = getNextViewLeft(child);
            }

            for (int i = startPos; i < itemCount; i++) {
                //如果左边界已经大于屏幕可见
                if (left > getWidth() - getPaddingRight()) {
                    break;
                }

                child = recycler.getViewForPosition(i);
                addView(child);
                //measure 还是需要的
                measureChildWithMargins(child, 0, 0);
/*
            int width = getDecoratedMeasuredWidth(child);
            int height = getDecoratedMeasuredHeight(child);*/
                layoutDecoratedWithMargins(child, left, top
                        , left + mChildWidth, top + mChildHeight);
                left += mChildWidth;
            }
        } else {
            //这种情况屏幕上一定有子View
            View leftChild = getChildAt(0);
            int endPos = getPosition(leftChild) - 1;
            int right = getLastViewRight(leftChild);
            int top = getPaddingTop();

            for (int pos = endPos; pos >= 0; pos--) {
                //只layout可见的
                if (right < getPaddingLeft()) {
                    break;
                }
                leftChild = recycler.getViewForPosition(pos);
                //这里是重点重点重点！！！作者每次在这里都踩坑，
                addView(leftChild, 0);
                measureChildWithMargins(leftChild, 0, 0);
                layoutDecoratedWithMargins(leftChild, right - mChildWidth, top
                        , right, top + mChildHeight);
                right -= mChildWidth;

            }

        }


        return dx;
    }

    //由于上述方法没有考虑margin的存在，所以我参考LinearLayoutManager的源码：

    /**
     * 获取某个childView在水平方向所占的空间
     *
     * @param view
     * @return
     */
    public int getDecoratedMeasurementHorizontal(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedMeasuredWidth(view) + params.leftMargin
                + params.rightMargin;
    }

    /**
     * 获取下一个View的left
     *
     * @param view
     * @return
     */
    public int getNextViewLeft(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedRight(view) + params.rightMargin;
    }

    /**
     * 获取上一个View的Right
     *
     * @param view
     * @return
     */
    public int getLastViewRight(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedLeft(view) - params.leftMargin;
    }

    /**
     * 获取某个childView在竖直方向所占的空间
     *
     * @param view
     * @return
     */
    public int getDecoratedMeasurementVertical(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedMeasuredHeight(view) + params.topMargin
                + params.bottomMargin;
    }


    public int getVerticalSpace() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    public int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }
}