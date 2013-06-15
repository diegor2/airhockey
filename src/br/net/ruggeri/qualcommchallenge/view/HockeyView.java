package br.net.ruggeri.qualcommchallenge.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

public class HockeyView extends View {

	private int mPucketRadius;

	private int mMalletRadius;

	private Point mPucket;

	private Point mMallet;

	private Paint mPaint;
	private boolean mShowPucket;

	public HockeyView(Context context) {
		super(context);
		init();
	}

	public HockeyView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public HockeyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		mMalletRadius = 0;
		mPucketRadius = 0;
		mMallet = new Point();
		mPucket = new Point();
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(0xff008800);
		mPaint.setColor(Color.RED);
		canvas.drawCircle(mPucket.x, mPucket.y, mPucketRadius, mPaint);
		mPaint.setColor(0xff000088);
		canvas.drawCircle(mMallet.x, mMallet.y, mMalletRadius, mPaint);
		mPaint.setColor(0xff0000ff);
		canvas.drawCircle(mMallet.x, mMallet.y, mMalletRadius / 2, mPaint);
	}

	public void setPucketPosition(int x, int y) {
		if (null != mPucket) {
			mPucket.x = x;
			mPucket.y = y;
		} else {
			mPucket = new Point(x, y);
		}
		invalidate();
	}

	public Point getPucketPosition() {
		return mPucket;
	}

	public Point getMalletPosition() {
		return mMallet;
	}

	public void setMalletPosition(int x, int y) {
		if (null != mMallet) {
			mMallet.x = x;
			mMallet.y = y;
		} else {
			mMallet = new Point(x, y);
		}
		invalidate();
	}

	public int getPucketRadius() {
		return mPucketRadius;
	}

	public void setPucketRadius(int pucketRadius) {
		mPucketRadius = pucketRadius;
	}

	public int getMalletRadius() {
		return mMalletRadius;
	}

	public void setMalletRadius(int malletRadius) {
		mMalletRadius = malletRadius;
	}

	public void setShowPucket(boolean show) {
		mShowPucket = show;
	}

}
