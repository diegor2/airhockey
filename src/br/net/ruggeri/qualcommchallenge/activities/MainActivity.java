package br.net.ruggeri.qualcommchallenge.activities;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.alljoyn.DaemonInit;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.widget.Toast;
import br.net.ruggeri.qualcommchalenge.R;
import br.net.ruggeri.qualcommchallenge.alljoyn.HockeyInterface;
import br.net.ruggeri.qualcommchallenge.physics.Physics;
import br.net.ruggeri.qualcommchallenge.physics.Physics.OnPysicalObjectsUpdatedListener;
import br.net.ruggeri.qualcommchallenge.view.HockeyView;

public class MainActivity extends Activity implements
		OnPysicalObjectsUpdatedListener {
	/* Load the native alljoyn_java library. */
	static {
		System.loadLibrary("alljoyn_java");
	}

	private static final int PUCKET_RADIUS = 40;
	private static final int MALLET_RADIUS = 80;
	private static final String SERVICE_NAME = MainActivity.class
			.getCanonicalName();
	private static final String TAG = MainActivity.class.getSimpleName();
	private static final short CONTACT_PORT = 42;
	private HockeyView mView;

	private BusAttachment mServerBus;
	private BusAttachment mClientBus;

	private VelocityTracker mTracker;
	private Physics mPysics;
	private SimpleService mSimpleService;
	private HockeyInterface mSimpleInterface;
	private Handler mHandler;
	private ProxyBusObject mProxyObj;
	private int mSessionID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mView = (HockeyView) findViewById(R.id.main_hockey);
		mTracker = VelocityTracker.obtain();
		mHandler = new Handler();
		initPhysics();
	}

	private void initAllJoynServer() {
		DaemonInit.PrepareDaemon(getApplicationContext());
		mSimpleService = new SimpleService();
		mServerBus = new BusAttachment(getPackageName(),
				BusAttachment.RemoteMessage.Receive);
		mServerBus.registerBusListener(new BusListener());
		Status status = mServerBus.registerBusObject(mSimpleService,
				"/SimpleService");
		Log.d(TAG, "registerBusObject " + status.name());
		status = mServerBus.connect();
		Log.d(TAG, "connect " + status.name());
		Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
		SessionOpts sessionOpts = new SessionOpts();
		sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
		sessionOpts.isMultipoint = false;
		sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
		sessionOpts.transports = SessionOpts.TRANSPORT_ANY
				+ SessionOpts.TRANSPORT_WFD;
		status = mServerBus.bindSessionPort(contactPort, sessionOpts,
				new SessionPortListener() {
					@Override
					public boolean acceptSessionJoiner(short sessionPort,
							String joiner, SessionOpts sessionOpts) {
						if (sessionPort == CONTACT_PORT) {
							return true;
						} else {
							return false;
						}
					}
				});
		Log.d(TAG, "bindSessionPort " + status.name());
		int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING
				| BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
		status = mServerBus.requestName(SERVICE_NAME, flag);
		Log.d(TAG, "requestName " + status.name());
		if (status == Status.OK) {
			/*
			 * If we successfully obtain a well-known name from the bus
			 * advertise the same well-known name
			 */
			status = mServerBus.advertiseName(SERVICE_NAME,
					sessionOpts.transports);

			if (status != Status.OK) {
				/*
				 * If we are unable to advertise the name, release the
				 * well-known name from the local bus.
				 */
				status = mServerBus.releaseName(SERVICE_NAME);
			} else {
				Toast.makeText(this, R.string.toast_server, Toast.LENGTH_SHORT)
						.show();
			}

		}
	}

	private void initAllJoynClient() {
		Status status;
		mClientBus = new BusAttachment(getPackageName(),
				BusAttachment.RemoteMessage.Receive);
		mClientBus.registerBusListener(new BusListener() {
			@Override
			public void foundAdvertisedName(String name, short transport,
					String namePrefix) {
				short contactPort = CONTACT_PORT;
				SessionOpts sessionOpts = new SessionOpts();
				sessionOpts.transports = transport;
				Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
				mClientBus.enableConcurrentCallbacks();
				Status status = mClientBus.joinSession(namePrefix, contactPort,
						sessionId, sessionOpts, new SessionListener() {
							@Override
							public void sessionLost(int sessionId) {
								Toast.makeText(MainActivity.this,
										R.string.toast_lost, Toast.LENGTH_SHORT)
										.show();
							}

							@Override
							public void sessionMemberAdded(int sessionId,
									String uniqueName) {
								Toast.makeText(MainActivity.this,
										R.string.toast_added,
										Toast.LENGTH_SHORT).show();
							}

							@Override
							public void sessionMemberRemoved(int sessionId,
									String uniqueName) {
								Toast.makeText(MainActivity.this,
										R.string.toast_removed,
										Toast.LENGTH_SHORT).show();
							}
						});
				Log.d(TAG, "joinSession " + status);
				if (Status.OK.equals(status)) {
					mProxyObj = mClientBus.getProxyBusObject(SERVICE_NAME,
							"/SimpleService", sessionId.value,
							new Class<?>[] { HockeyInterface.class });
					mSimpleInterface = mProxyObj
							.getInterface(HockeyInterface.class);
					mSessionID = sessionId.value;
					final String id = Integer.toHexString(sessionId.value)
							+ " " + namePrefix;
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(
									MainActivity.this,
									getString(R.string.toast_client) + " " + id,
									Toast.LENGTH_SHORT).show();
						}
					});
				}
			}
		});

		status = mClientBus.connect();
		Log.d(TAG, "connect " + status);
		status = mClientBus.findAdvertisedName(SERVICE_NAME);
		Log.d(TAG, "findAdvertisedName " + status);
	}

	@Override
	protected void onDestroy() {
		// mClientBus.unregisterBusObject(mSimpleService);
		mClientBus.disconnect();
		mServerBus.unregisterBusObject(mSimpleService);
		mServerBus.disconnect();
		super.onDestroy();
	}

	private void initPhysics() {
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		int width = outMetrics.widthPixels;
		int height = outMetrics.heightPixels;
		int pucketStartX = width / 2;
		int pucketStartY = height - height / 4;
		int malletStartX = width / 2;
		int malletStartY = height - MALLET_RADIUS;

		mView.setPucketRadius(PUCKET_RADIUS);
		mView.setMalletRadius(MALLET_RADIUS);
		mView.setPucketPosition(pucketStartX, pucketStartY);
		mView.setMalletPosition(malletStartX, malletStartY);

		mPysics = new Physics(this, width, height, PUCKET_RADIUS,
				MALLET_RADIUS, pucketStartX, pucketStartY);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_reset: {
			initPhysics();
		}
			break;
		case R.id.menu_server: {
			initAllJoynServer();
		}
			break;
		case R.id.menu_connect: {
			initAllJoynClient();
		}
			break;
		case R.id.menu_ping: {
			ping();
		}
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean hit = false;
		mTracker.addMovement(event);
		float x = event.getX();
		float y = event.getY();
		Point mallet = mView.getMalletPosition();
		int malletRadius = mView.getMalletRadius();
		Point pucket = mView.getPucketPosition();
		int pucketRadius = mView.getPucketRadius();

		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN: {
			if ((mallet.x - malletRadius) < x && x < (mallet.x + malletRadius)
					&& (mallet.y - malletRadius) < y
					&& y < (mallet.y + malletRadius)) {
				// Hit the mallet
				hit = true;
			}
		}
			break;
		case MotionEvent.ACTION_MOVE: {
			mView.setMalletPosition((int) event.getX(), (int) event.getY());
			if (Math.hypot(Math.abs(pucket.x - mallet.x),
					Math.abs(pucket.y - mallet.y)) < (pucketRadius + malletRadius)) {
				mTracker.computeCurrentVelocity(mPysics.getMassRatio());
				mPysics.setVelocity(mTracker.getXVelocity(),
						mTracker.getYVelocity());
				mPysics.move();
			}
		}
			break;
		}
		return hit;
	}

	public void onUpdate(int x, int y) {
		Log.d(TAG, "onUpdate x:" + x + " y:" + y);
		if (mSimpleInterface != null) {
			mPysics.setBoncingTop(false);
			mView.setShowPucket(false);
			if (y <= 0) {
				mPysics.stop();
				double velocityX = (double) mPysics.getXVelocity();
				double velocityY = (double) mPysics.getYVelocity();
				int positionX = mPysics.getXPucket();
				try {
					mSimpleInterface.sendPucket(velocityX, velocityY,
							positionX, mSessionID);
				} catch (BusException e) {
					e.printStackTrace();
				}
			} else {
				mView.setPucketPosition(x, y);
			}
		} else {
			mPysics.setBoncingTop(true);
			mView.setPucketPosition(x, y);
		}

	}

	public void ping() {
		if (null != mSimpleInterface) {
			try {
				mSimpleInterface.ping();
			} catch (BusException e) {
				e.printStackTrace();
			}
		}
	}

	public class SimpleService implements HockeyInterface, BusObject {
		@Override
		public void ping() throws BusException {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(MainActivity.this, R.string.toast_ping,
							Toast.LENGTH_SHORT).show();
				}
			});
		}

		@Override
		public void sendPucket(double velocityX, double velocityY,
				int positionX, int from) throws BusException {
			if (from != mSessionID) {
				final float vx = (float) -velocityX;
				final float vy = (float) -velocityY;
				final int pos = positionX;
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mPysics.setVelocity(vx, vy);
						mPysics.setPucketPosition(pos, PUCKET_RADIUS);
						mPysics.move();
						mView.setShowPucket(true);
					}
				});
			}
		}
	}

}
