package com.android.systemui.statusbar;

import java.util.Calendar;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StatusBarJellyHeaderView extends LinearLayout {

	private final Context context;

	private TextView mDateView;
	private TextView mClockView;

	private boolean mUpdating = false;

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_TIME_TICK)
					|| action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				updateViews();
			}
		}
	};
	
	public StatusBarJellyHeaderView(Context context) {
		super(context);
		this.context = context;		
		initViews();
	}
		
	public StatusBarJellyHeaderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;	
		initViews();
	}
	
	private void initViews(){
		final String[] str = getTimeText();

		final LayoutParams layoutParams = new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layoutParams.leftMargin = 8;

		mClockView = new TextView(context);
		mClockView.setTextSize(32);
		mClockView.setTextColor(-1);
		mClockView.setText(str[0]);
		mClockView.setLayoutParams(layoutParams);

		mDateView = new TextView(context);
		mDateView.setTextColor(Color.parseColor("#cccccc"));
		mDateView.setTextSize(12);
		mDateView.setText(str[1]);
		mDateView.setLayoutParams(layoutParams);

		addView(mClockView);
		addView(mDateView);

		setGravity(Gravity.CENTER_VERTICAL);
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

	private final void updateViews() {
		final String[] str = getTimeText();
		mClockView.setText(str[0]);
		mDateView.setText(str[1]);
		
		invalidate();
	}

	void setUpdates(boolean update) {
		if (update != mUpdating) {
			mUpdating = update;
			if (update) {
				IntentFilter filter = new IntentFilter();
				filter.addAction(Intent.ACTION_TIME_TICK);
				filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
				context.registerReceiver(mIntentReceiver, filter, null, null);
				updateViews();
			} else {
				context.unregisterReceiver(mIntentReceiver);
			}
		}
	}

	private String[] getTimeText() {
		Calendar calendar = Calendar.getInstance();
		Date date = calendar.getTime();
		int min = calendar.get(Calendar.MINUTE);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		if (hour > 12 && DateFormat.is24HourFormat(context) != true)
			hour -= 12;
		return new String[] {
				hour + ":" + (min > 9 ? min : "0" + min),
				(DateFormat.format("EEEE", date) + "\n" + DateFormat
						.getLongDateFormat(context).format(date)).toUpperCase() };
	}

}