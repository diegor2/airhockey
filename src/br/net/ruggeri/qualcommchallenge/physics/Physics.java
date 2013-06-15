package br.net.ruggeri.qualcommchallenge.physics;

import android.os.Handler;
import br.net.ruggeri.qualcommchallenge.activities.MainActivity;

public class Physics {

	public interface OnPysicalObjectsUpdatedListener {

		public void onUpdate(int x, int y);

	}

	/* Mallet mass / Pucket mass */
	private static final int MASS_RATIO = 5;

	private static final long DELAY = 30;

	private static final float FRICTION = 1.0e-2f;
	/** Smallest velocity */
	private static final float EPS = 0.01f;
	/** Log tag */
	protected static final String TAG = MainActivity.class.getSimpleName();

	private static final float BOUNCE_LOSS = 0.2f;

	private float mXVelocity;
	private float mYVelocity;
	private boolean mRunning;
	private Handler mHandler;
	private int mWidth;
	private int mHeight;
	private int mPucketRadius;
	private int mMalletRadius;
	private int mXPucket;
	private int mYPucket;
	private OnPysicalObjectsUpdatedListener mListener;

	private Runnable mMoveRunnable = new Runnable() {
		@Override
		public void run() {
			if (mRunning && null != mListener) {
				update();
			}
		}
	};

	private boolean mBoncingTop;

	public Physics(OnPysicalObjectsUpdatedListener l, int width, int height,
			int pucketRadius, int malletRadius, int xPucket, int yPucket) {
		mListener = l;
		mWidth = width;
		mHeight = height;
		mPucketRadius = pucketRadius;
		mMalletRadius = malletRadius;
		mXPucket = xPucket;
		mYPucket = yPucket;
		mHandler = new Handler();
	}

	public void setVelocity(float x, float y) {
		mXVelocity = x * MASS_RATIO;
		mYVelocity = y * MASS_RATIO;
	}

	public int getMassRatio() {
		return MASS_RATIO;
	}

	public void move() {
		mRunning = true;
		mHandler.postDelayed(mMoveRunnable, DELAY);
	}

	public void stop() {
		mRunning = false;
	}

	public void reset() {
		stop();
		mXVelocity = 0;
		mYVelocity = 0;
	}

	public float getXVelocity() {
		return mXVelocity;
	}

	public float getYVelocity() {
		return mYVelocity;
	}

	public void setBoncingTop(boolean bounce) {
		mBoncingTop = bounce;
	}

	public int getXPucket() {
		return mXPucket;
	}

	public void setPucketPosition(int x, int y) {
		mXPucket = x;
		mYPucket = y;
	}

	private void update() {
		mXPucket += mXVelocity;
		mYPucket += mYVelocity;
		mXVelocity *= (1 - FRICTION);
		mYVelocity *= (1 - FRICTION);

		// bouncing left
		if ((mXPucket - mPucketRadius) <= 0) {
			mXVelocity *= -(1 - BOUNCE_LOSS);
			mXPucket = mPucketRadius;
		}
		// bouncing right
		if ((mXPucket + mPucketRadius) >= mWidth) {
			mXVelocity *= -(1 - BOUNCE_LOSS);
			mXPucket = mWidth - mPucketRadius;
		}
		// bouncing top
		if (mBoncingTop) {
			if ((mYPucket - mPucketRadius) <= 0) {
				mYVelocity *= -(1 - BOUNCE_LOSS);
				mYPucket = mPucketRadius;
			}
		}
		// bouncing bottom
		if ((mYPucket + mPucketRadius) >= mHeight) {
			mYVelocity *= -(1 - BOUNCE_LOSS);
			mYPucket = mHeight - mPucketRadius;
		}

		mListener.onUpdate(mXPucket, mYPucket);
		if (Math.abs(mXVelocity) > EPS || Math.abs(mYVelocity) > EPS) {
			move();
		} else {
			stop();
		}
	}
}
