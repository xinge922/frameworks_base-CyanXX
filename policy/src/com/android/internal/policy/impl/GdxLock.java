/*
 * Copyright (C) 2011 doixanh@xda
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

import java.util.Calendar;
import java.text.SimpleDateFormat;
import com.android.internal.R;
import com.android.internal.widget.LockPatternUtils;
import android.os.Handler;
import android.os.SystemClock;
import android.content.Context;
import android.content.res.Configuration;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.format.DateFormat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;

public class GdxLock extends LinearLayout implements KeyguardScreen {
	private boolean DBG = false;
	private String TAG = "GDXLock";
	private String DATE_FORMAT = "dd MMMMMM yyyy";
	
	// constants
	private int ringWidth = 160;
	private int ringHeight = 160;

	private int mDisplayWidth;
	private int mDisplayHeight;

	// controls
	private ImageView ring;
	private Context mContext;
	private KeyguardScreenCallback mCallback;
    private TextView mdxLeft;
    private TextView mdxRight;
    
       // date & time
       private TextView mClock, mDate;
       private Runnable mTicker;
       private Handler mHandler;
       private boolean mTickerStopped = false;

	// flinging calculation
	public GestureDetector gestureDetector;
	private DecelerateInterpolator interpolator;
	private AccelerateInterpolator zoomIntp;
	private long startTime;
	private long endTime;
	private int totalAnimDx;
	private int totalAnimDy;
	private int startFlingX;
	private int startFlingY;
	private int endFlingX;
	private int endFlingY;
	private boolean isFlinging;
	private boolean isZooming;
	
	
    public GdxLock(Context context, Configuration configuration, LockPatternUtils lockPatternUtils,
            KeyguardUpdateMonitor updateMonitor,
            KeyguardScreenCallback callback) {
		super(context);

		mContext = context;
		mCallback = callback;		

		// init gesture detector
		//gestureDetector = new GestureDetector(new MyGestureDetector());

		// initialize lockscreen layout
		final LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.gdxlock_sense3, this, true);
		
		// init screen width and height
        Display d = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mDisplayWidth = d.getWidth();
        mDisplayHeight = d.getHeight();

		// init ring
		ring = (ImageView) findViewById(R.id.ring);

		// move the ring to default position
		int left = (int) (mDisplayWidth / 2 - ringWidth / 2);
		int top = (int) (mDisplayHeight - ringHeight / 2 - 20 + 110);
		ring.setPadding(left, top, 0, 0);

		// set background...
		ViewGroup lockWallpaper = (ViewGroup) findViewById(R.id.root);
		setBackground(mContext, lockWallpaper);
		
        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        
        //time & date setup
        mClock = (TextView) findViewById(R.id.clock);
        mDate = (TextView) findViewById(R.id.date);
        
        // label setup
        mdxLeft = (TextView) findViewById(R.id.dxLeft);
        mdxRight = (TextView) findViewById(R.id.dxRight);
        mdxLeft.setVisibility(View.VISIBLE);
        mdxLeft.setText(" ");
        mdxLeft.setTextColor(0xffffffff);
        mdxRight.setVisibility(View.VISIBLE);
        mdxRight.setText(" ");
        mdxRight.setTextColor(0xffffffff);
	}
	
	private void zoomRing(int newWidth, int newHeight) {
		ring.setLayoutParams(new LayoutParams(newWidth, newHeight));
		ringWidth = newWidth;
		ringHeight = newHeight;
	}

	// moves the ring to a specific position
	private void moveRing(int left, int top) {
		// check screen border
		if (left < -ringWidth / 2 || left > mDisplayWidth - ringWidth / 2 || 
			top < -ringHeight / 2 || top > mDisplayHeight - ringHeight / 4)
			return;
		// above half?
//		if ((int) (top + ringWidth / 2) < mDisplayHeight / 2) {
//			// unlock it
//			mCallback.goToUnlockScreen();			
//		}
		ring.setPadding(left, top, 0, 0);
	}

	// handles touch event
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isFlinging) {
			int orgLeft = ring.getPaddingLeft();
			int orgTop = ring.getPaddingTop();
			int left = (int) (event.getRawX() - ringWidth / 2 - 10);
			int top = (int) (event.getRawY() - ringHeight / 2 - 20);
			// check touch position
			int dist = (left - orgLeft) * (left - orgLeft) + (top - orgTop)
					* (top - orgTop);
			if (DBG)
				Log.i(TAG, "distance : " + dist);
	
			if (dist < ringWidth * ringHeight) {
				// move the ring
				moveRing(left, top);
				// and check flinging
				// gestureDetector.onTouchEvent(event);
				
				// released?
				if (event.getAction() == MotionEvent.ACTION_UP) {
					// check place, 
					if ((int) (top + ringWidth / 2) < 3 * mDisplayHeight / 4) {
						// start moving to center animation 
						startAnimateToCenter(150);						
					}
				}
			}
		}
		return false;
	}
	
	// initialize moving to center animation
	private void startAnimateToCenter(long duration) {
		startFlingX = ring.getPaddingLeft();
		startFlingY = ring.getPaddingTop();
		endFlingX = (mDisplayWidth - ringWidth) / 2;
		endFlingY = (mDisplayHeight - ringHeight) / 2;
		startTime = System.currentTimeMillis();
		endTime = startTime + duration;
		interpolator = new DecelerateInterpolator();
		isFlinging = true;
		
		if (DBG) Log.i(TAG, "startAnimateToCenter: sx=" + startFlingX + " sy=" + startFlingY + " ex=" + endFlingX + " ey=" + endFlingY);
		post(new Runnable() {
			@Override
			public void run() {
				animateToCenterStep();
			}
		});
	}
	
	private void animateToCenterStep() {
		long curTime = System.currentTimeMillis();
		float percentTime = (float) (curTime - startTime)
				/ (float) (endTime - startTime);
		float percentDistance = interpolator.getInterpolation(percentTime);
		
		if (DBG) Log.i(TAG, "animateToCenterStep: %t=" + percentDistance + " %d=" + percentDistance + 
							" x=" + (int) (percentDistance * (endFlingX - startFlingX)) + 
							" y=" + (int) (percentDistance * (endFlingY - startFlingY)));
		moveRing((int) (startFlingX + percentDistance * (endFlingX - startFlingX)), 
				(int) (startFlingY + percentDistance * (endFlingY - startFlingY)));

		// not yet finished?
		if (percentTime < 1.0f) {
			// more!
			post(new Runnable() {
				@Override
				public void run() {
					animateToCenterStep();
				}
			});
		} else {
			// finished
			isFlinging = false;
			mCallback.goToUnlockScreen();
		}
		
	}
	
	
	/* 
	// detects fling gesture
	class MyGestureDetector extends SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			try {
				if (DBG)
					Log.i(TAG, "flinging : vx=" + velocityX + " vy="
							+ velocityY);
				final float distanceTimeFactor = 0.3f;
				final float totalDx = (distanceTimeFactor * velocityX / 2);
				final float totalDy = (distanceTimeFactor * velocityY / 2);

				animateFling(ring.getPaddingLeft(), ring.getPaddingTop(),
						totalDx, totalDy, (long) (1000 * distanceTimeFactor));
				return true;
			} catch (Exception e) {
				// nothing
			}
			return false;
		}
	}
	
	// initalize fling
	public void animateFling(int sx, int sy, float dx, float dy, long duration) {
		//interpolator = new DecelerateInterpolator();
		startTime = System.currentTimeMillis();
		endTime = startTime + duration;
		totalAnimDx = (int) dx;
		totalAnimDy = (int) dy;
		startFlingX = sx;
		startFlingY = sy;
		isFlinging = true;
		post(new Runnable() {
			@Override
			public void run() {
				animateStep();
			}
		});
	}

	// fling steps
	private void animateStep() {
		long curTime = System.currentTimeMillis();
		float percentTime = (float) (curTime - startTime)
				/ (float) (endTime - startTime);
		float percentDistance = interpolator.getInterpolation(percentTime);
		float curDx = percentDistance * totalAnimDx;
		float curDy = percentDistance * totalAnimDy;
		// translate.set(animateStart);
		moveRing((int) (startFlingX + curDx), (int) (startFlingY + curDy));
		
		// not yet finished?
		if (percentTime < 1.0f) {
			// more!
			post(new Runnable() {
				@Override
				public void run() {
					animateStep();
				}
			});
		} else {
			// finished
			isFlinging = false;
		}
	} */

	// set wallpaper as background
	private void setBackground(Context bcontext, ViewGroup layout) {
		// Settings.System.LOCKSCREEN_BACKGROUND
		String mLockBack = Settings.System.getString(
				bcontext.getContentResolver(), "lockscreen_background");
		if (mLockBack != null) {
			if (!mLockBack.isEmpty()) {
				try {
					layout.setBackgroundColor(Integer.parseInt(mLockBack));
				} catch (NumberFormatException e) {
				}
			} else {
				String lockWallpaper = "";
				try {
					lockWallpaper = bcontext.createPackageContext(
							"com.cyanogenmod.cmparts", 0).getFilesDir()
							+ "/lockwallpaper";
				} catch (NameNotFoundException e1) {
				}
				if (!lockWallpaper.isEmpty()) {
					Bitmap lockb = BitmapFactory.decodeFile(lockWallpaper);
					layout.setBackgroundDrawable(new BitmapDrawable(lockb));
				}
			}
		}
	}
	
    public boolean needsInput() {
        return false;
    }
    
    private void updateClock(){
    	mClock.setText(Hour()+":"+Minute());
    	mDate.setText(new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance()
				.getTime()));
    }
    
    private String Hour() {
	int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
	if (!DateFormat.is24HourFormat(mContext) && hour > 12)
		hour = hour - 12;
	return (hour < 10) ? "0" + hour : "" + hour;
    }
    
    private String Minute() {
	int minute = Calendar.getInstance().get(Calendar.MINUTE);
	return (minute < 10) ? "0" + minute : "" + minute;
    }
    
    @Override
    protected void onAttachedToWindow() {
        mTickerStopped = false;
        super.onAttachedToWindow();
        mHandler = new Handler();
        mTicker = new Runnable() {
                public void run() {
                    if (mTickerStopped) return;
                    updateClock();
                    long now = SystemClock.uptimeMillis();
                    long next = now + (1000 - now % 1000);
                    mHandler.postAtTime(mTicker, next);
                }
            };
        mTicker.run();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mTickerStopped = true;
    }
    
    public void onPause() {			
    }

    public void onResume() {
    }

    public void cleanUp() {
    }

}
