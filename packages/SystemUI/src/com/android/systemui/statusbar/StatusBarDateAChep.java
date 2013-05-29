package com.android.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class StatusBarDateAChep extends TextView {

	private final static String format = "MMMMMMMM dd, yyyy";
	private Context mContext;
	private boolean mUpdating = false;

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_TIME_TICK)
					|| action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				updateClock();
			}
		}
	};

	public StatusBarDateAChep(Context context) {
		super(context);
		mContext = context;
	}

	public StatusBarDateAChep(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		setUpdates(true);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		setUpdates(false);
	}

	@Override
	protected int getSuggestedMinimumWidth() {
		// makes the large background bitmap not force us to full width
		return 0;
	}

	private final void updateClock() {
		setText(new SimpleDateFormat(format).format(Calendar.getInstance()
				.getTime()));
		invalidate();
	}

	void setUpdates(boolean update) {
		if (update != mUpdating) {
			mUpdating = update;
			if (update) {
				// Register for Intent broadcasts for the clock and battery
				IntentFilter filter = new IntentFilter();
				filter.addAction(Intent.ACTION_TIME_TICK);
				filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
				mContext.registerReceiver(mIntentReceiver, filter, null, null);
				updateClock();
			} else {
				mContext.unregisterReceiver(mIntentReceiver);
			}
		}
	}

}