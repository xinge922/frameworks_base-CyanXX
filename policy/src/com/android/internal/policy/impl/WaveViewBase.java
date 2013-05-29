/*
 * Copyright (C) 2012-2013 AChep@xda <artemchep@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.policy.impl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class WaveViewBase extends View {

	public static final int RINGS_MAX_NUMBER = 10;
	public static final int FINGERS_MAX_NUMBER = 10;
	public static final int RINGS_TIMEOUT_MILLIS = 2000;
	public static final int RINGS_DISABLE_SPEED_MILLIS = 300;

	private static final int[] RINGS_COLOR_RGB = new int[] { 255, 255, 255 };

	private int mRequiredRadius;
	private float mDensity;

	private Ring[] mRings;
	private int[] mFingersTrack;

	private OnActionListener mOnActionListener;

	public void setOnActionListener(OnActionListener onActionListener) {
		mOnActionListener = onActionListener;
	}

	public interface OnActionListener {
		public void onAction();

		public void onTouchDown(float x, float y);

		public void onTouchUp();
	};

	public WaveViewBase(Context context, OnActionListener onActionListener) {
		super(context);
		init(context);

		setOnActionListener(onActionListener);
	}

	public WaveViewBase(Context context) {
		super(context);
		init(context);
	}

	public WaveViewBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public WaveViewBase(Context context, AttributeSet attrs, int styles) {
		super(context, attrs, styles);
		init(context);
	}

	private void init(Context context) {
		mRings = new Ring[RINGS_MAX_NUMBER];
		mFingersTrack = new int[FINGERS_MAX_NUMBER];
		for (int i = 0; i < RINGS_MAX_NUMBER; i++) {
			mRings[i] = new Ring();
			mFingersTrack[i] = -1;
		}

		mDensity = (float) getResources().getDisplayMetrics().density;
	}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		// Calculate the size of required ring radius
		mRequiredRadius = (int) (Math.sqrt(w * w + h * h) / 3);
	}

	@Override
	public void onDraw(Canvas canvas) {
		for (int i = 0; i < mRings.length; i++) {
			final Ring ring = mRings[i];
			if (!ring.inUse())
				continue;

			ring.draw(canvas);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		int i = 0;
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			for (; i < mRings.length; i++)
				if (!mRings[i].inUse())
					break;
			if (i == mRings.length)
				return false;

			int index = event.getActionIndex();
			mFingersTrack[index] = i;
			mRings[i].reInit(event.getX(index), event.getY(index));
			break;
		case MotionEvent.ACTION_MOVE:
			int fingers = event.getPointerCount();
			if (fingers > FINGERS_MAX_NUMBER)
				fingers = FINGERS_MAX_NUMBER;
			for (; i < fingers; i++) {
				int ringId = mFingersTrack[i];
				if (ringId < 0)
					continue;
				mRings[ringId].setTouchMoveCoords(event.getX(i), event.getY(i));
			}
			break;
		case MotionEvent.ACTION_POINTER_UP:
			index = event.getActionIndex();
			disableRing(mFingersTrack[index]);
			mFingersTrack[index] = -1;
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			for (; i < mRings.length; i++) {
				if (!mRings[i].isInteractive())
					continue;
				disableRing(i);
			}
			for (i = 0; i < mFingersTrack.length; i++)
				mFingersTrack[i] = -1;
			break;
		}

		invalidate();
		return true;
	}

	private void disableRing(int i) {
		mRings[i].disableRing();
	}

	private class Ring {

		private double mRadius;
		private int mAlpha;
		private boolean mInteractive;
		private boolean mUsing;
		private long mInitTime;

		private float mTouchMoveX;
		private float mTouchMoveY;
		private float mTouchDownX;
		private float mTouchDownY;

		private final Paint mPaint;

		public Ring() {
			// Our painter
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
		}

		public void draw(Canvas canvas) {
			if (mRadius < 0 || !mUsing)
				// Do not draw if circle has negative radius or...
				return;

			// Draw center circle
			mPaint.setStyle(Style.FILL);
			mPaint.setStrokeWidth(2 * mDensity);
			mPaint.setARGB(mAlpha, RINGS_COLOR_RGB[0], RINGS_COLOR_RGB[1],
					RINGS_COLOR_RGB[2]);
			canvas.drawCircle(mTouchDownX, mTouchDownY, 3f * mDensity, mPaint);

			// Draw touch point
			if (mInteractive)
				// Do not draw the touch point after death
				canvas.drawCircle(mTouchMoveX, mTouchMoveY, 7f * mDensity,
						mPaint);

			// Draw circle with required radius
			mPaint.setStyle(Style.STROKE);
			mPaint.setStrokeWidth(2 * mDensity);
			mPaint.setAlpha(mAlpha / 5);
			canvas.drawCircle(mTouchDownX, mTouchDownY, mRequiredRadius, mPaint);

			// Draw control ring
			canvas.drawCircle(mTouchDownX, mTouchDownY, (float) mRadius, mPaint);

			// Draw waves
			final double deltaRadius = Math.sqrt(8l / (double) mRequiredRadius)
					* mRadius;
			final int deltaAlpha = (int) Math
					.round(270 * deltaRadius / mRadius);

			int alpha = (int) (mAlpha * mRadius / mRequiredRadius - deltaAlpha);
			float radius = (float) mRadius;
			while (radius > 0 && alpha > 0) {
				mPaint.setAlpha(alpha);
				canvas.drawCircle(mTouchDownX, mTouchDownY, radius, mPaint);

				radius -= deltaRadius;
				alpha -= deltaAlpha;
			}
		}

		public void disableRing() {
			if (mOnActionListener != null)
				mOnActionListener.onTouchUp();

			// We're shouldn't see touch point
			mInteractive = false;

			final double radius = mRadius;
			final long duration = Math.round(RINGS_DISABLE_SPEED_MILLIS
					* mRadius / mRequiredRadius);
			final long endTime = SystemClock.uptimeMillis() + duration;

			final Handler handler = new Handler();
			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					if (mInteractive)
						return;

					long deltaTime = endTime - SystemClock.uptimeMillis();
					if (deltaTime > 0) {
						float progress = 1f - (float) (duration - deltaTime)
								/ duration;
						mRadius = radius * progress;
						mAlpha = (int) (255f * progress);

						handler.postDelayed(this, 10);
					} else {
						mUsing = false;
					}
					postInvalidate();
				}
			};
			handler.post(runnable);
		}

		public void setTouchMoveCoords(float x, float y) {
			if (!mInteractive)
				return;

			if (System.currentTimeMillis() - mInitTime > RINGS_TIMEOUT_MILLIS) {
				disableRing();
				return;
			}

			float a = mTouchDownX - x;
			float b = mTouchDownY - y;
			double radius = Math.sqrt(a * a + b * b);
			if (radius > mRequiredRadius) {
				// Send message to level-up-class
				if (mOnActionListener != null)
					mOnActionListener.onAction();

				disableRing();
			} else {
				mRadius = radius;

				mTouchMoveX = x;
				mTouchMoveY = y;
			}
		}

		public boolean inUse() {
			return mUsing;
		}

		public boolean isInteractive() {
			return mInteractive;
		}

		public void reInit(float x, float y) {
			mAlpha = 255;
			mInteractive = true;
			mRadius = -1;
			mUsing = true;
			mInitTime = System.currentTimeMillis();

			// Remember the center of circle
			mTouchDownX = x;
			mTouchDownY = y;

			// Say activity that we began
			if (mOnActionListener != null)
				mOnActionListener.onTouchDown(x, y);
		};
	}

}
