package com.klinker.android.messaging_sliding.slide_over;

import android.app.ActivityManager;
import android.app.Service;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.klinker.android.messaging_donate.R;

import java.util.List;

public class SlideOverService extends Service {

    public HaloView haloView;
    public ArcView arcView;
    public AnimationView animationView;

    public WindowManager.LayoutParams haloParams;
    public WindowManager.LayoutParams haloHiddenParams;
    public WindowManager.LayoutParams arcParams;
    public WindowManager.LayoutParams animationParams;

    public Context mContext;

    public WindowManager haloWindow;
    public WindowManager arcWindow;
    public WindowManager animationWindow;

    public SharedPreferences sharedPrefs;

    public static int SWIPE_MIN_DISTANCE = 0;
    public static double HALO_SLIVER_RATIO = .33;
    public static double PERCENT_DOWN_SCREEN = 0;
    public static float ARC_BREAK_POINT = 0;

    public static final int START_ALPHA = 60;
    public static final int START_ALPHA2 = 120;
    public static final int TOUCHED_ALPHA = 230;
    
    public static boolean HAPTIC_FEEDBACK = true;

    public int numberNewConv;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        final Bitmap halo = BitmapFactory.decodeResource(getResources(),
                R.drawable.halo_bg);

        Display d = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final int height = d.getHeight();
        final int width = d.getWidth();

        HALO_SLIVER_RATIO = sharedPrefs.getInt("slideover_sliver", 33)/100.0;

        PERCENT_DOWN_SCREEN = sharedPrefs.getInt("slideover_vertical", 50)/100.0;
        PERCENT_DOWN_SCREEN -= PERCENT_DOWN_SCREEN * (halo.getHeight()/(double)height);
        
        HAPTIC_FEEDBACK = sharedPrefs.getBoolean("slideover_haptic_feedback", true);

        int breakAngle = sharedPrefs.getInt("slideover_break_point", 33);
        float breakAng = 0;
        breakAng += breakAngle;

        ARC_BREAK_POINT = breakAng * .9f;

        haloParams = new WindowManager.LayoutParams(
                halo.getWidth(),
                halo.getHeight(),
                sharedPrefs.getString("slideover_side", "left").equals("left") ? (int) (-1 * (1 - HALO_SLIVER_RATIO) * halo.getWidth()) : (int) (width - (halo.getWidth() * (HALO_SLIVER_RATIO))),
                (int)(height * PERCENT_DOWN_SCREEN),
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        |WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        |WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        haloParams.gravity = Gravity.TOP | Gravity.LEFT;
        haloParams.windowAnimations = android.R.anim.fade_in;

        haloHiddenParams = new WindowManager.LayoutParams(
                halo.getWidth(),
                halo.getHeight(),
                sharedPrefs.getString("slideover_side", "left").equals("left") ? -1 * halo.getWidth() : width + halo.getWidth(),
                (int)(height * PERCENT_DOWN_SCREEN),
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        |WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        |WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        haloHiddenParams.gravity = Gravity.TOP | Gravity.LEFT;
        haloHiddenParams.windowAnimations = android.R.anim.fade_out;

        arcParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        |WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        |WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);
        arcParams.dimAmount=.7f;

        animationParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        |WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        |WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        haloWindow = (WindowManager) getSystemService(WINDOW_SERVICE);
        arcWindow = (WindowManager) getSystemService(WINDOW_SERVICE);
        animationWindow = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (width > height)
            SWIPE_MIN_DISTANCE = (int)(height * (sharedPrefs.getInt("slideover_activation", 33)/100.0));
        else
            SWIPE_MIN_DISTANCE = (int)(width * (sharedPrefs.getInt("slideover_activation", 33)/100.0));

        haloView = new HaloView(this);
        arcView = new ArcView(this, halo, SWIPE_MIN_DISTANCE, ARC_BREAK_POINT, HALO_SLIVER_RATIO);
        animationView = new AnimationView(this, halo);

        numberNewConv = arcView.newConversations.size();

        haloView.setOnTouchListener(new View.OnTouchListener() {
            private boolean needDetection = false;
            private boolean vibrateNeeded = true;
            private boolean inDash = false;
            private boolean inFlat = true;
            private boolean initial = true;
            private boolean zoneChange = false;
            private boolean fromDash = true;
            private boolean inButtons = false;
            private boolean topVibrate = true;
            private boolean inClose = false;
            private boolean inMove = false;

            private int lastZone = 0;
            private int currentZone = 0;
            private int zoneWidth;

            private float initX;
            private float initY;

            private double xPortion;
            private double yPortion;

            private double distance = 0;
            private double angle = 0;

            @Override
            public boolean onTouch(View view, MotionEvent event) {

                if ((event.getX() > haloView.getX() && event.getX() < haloView.getX() + halo.getWidth() && event.getY() > haloView.getY() && event.getY() < haloView.getY() + halo.getHeight()) || needDetection) {
                    final int type = event.getActionMasked();
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

                    if (numberNewConv == 0) { // no messages to display
                        switch (type) {
                            case MotionEvent.ACTION_DOWN:

                                if (HAPTIC_FEEDBACK) {
                                    v.vibrate(10);
                                }

                                initX = event.getX();
                                initY = event.getY();

                                arcView.newMessagePaint.setAlpha(START_ALPHA2);

                                arcWindow.addView(arcView, arcParams);
                                haloWindow.updateViewLayout(haloView, haloHiddenParams);
                                needDetection = true;
                                return true;

                            case MotionEvent.ACTION_MOVE:

                                xPortion = initX - event.getX();
                                yPortion = initY - event.getY();

                                distance = Math.sqrt(Math.pow(xPortion, 2) + Math.pow(yPortion, 2));
                                angle = Math.toDegrees(Math.atan(yPortion / xPortion));

                                if (!sharedPrefs.getString("slideover_side", "left").equals("left"))
                                    angle *= -1;

                                float rawY = event.getRawY();

                                if ((rawY < 120 && PERCENT_DOWN_SCREEN > .5) || (rawY > height - 120 && PERCENT_DOWN_SCREEN < .5)) // in Top Area
                                {
                                    inButtons = true;

                                    if (HAPTIC_FEEDBACK && topVibrate) {
                                        v.vibrate(25);
                                        vibrateNeeded = true;
                                        topVibrate = false;
                                    }

                                    if(event.getRawX() < width/2) // Close Zone
                                    {
                                        inClose = true;
                                        if(HAPTIC_FEEDBACK && inMove)
                                        {
                                            v.vibrate(25);
                                            inMove = false;
                                        }
                                        arcView.closePaint.setAlpha(TOUCHED_ALPHA);
                                        arcView.movePaint.setAlpha(START_ALPHA2);
                                        arcView.newMessagePaint.setAlpha(START_ALPHA2);
                                        arcView.conversationsPaint.setAlpha(START_ALPHA);
                                    } else // Move Zone
                                    {
                                        inMove = true;
                                        if(HAPTIC_FEEDBACK && inClose)
                                        {
                                            v.vibrate(25);
                                            inClose = false;
                                        }
                                        arcView.closePaint.setAlpha(START_ALPHA2);
                                        arcView.movePaint.setAlpha(TOUCHED_ALPHA);
                                        arcView.newMessagePaint.setAlpha(START_ALPHA2);
                                        arcView.conversationsPaint.setAlpha(START_ALPHA);
                                    }

                                    arcView.invalidate();
                                    arcWindow.updateViewLayout(arcView, arcParams);

                                } else if ((!(initY > event.getY()) && angle > ARC_BREAK_POINT)) // in dash area
                                {
                                    if(inButtons)
                                    {
                                        arcView.closePaint.setAlpha(START_ALPHA);
                                        arcView.movePaint.setAlpha(START_ALPHA);

                                        inButtons = false;

                                        inClose = false;
                                        inMove = false;

                                        topVibrate = true;

                                    }

                                    if (inFlat && distance > SWIPE_MIN_DISTANCE) {
                                        inFlat = false;
                                        inDash = true;

                                        arcView.conversationsPaint.setAlpha(START_ALPHA2 + 20);
                                        arcView.newMessagePaint.setAlpha(START_ALPHA2);

                                        arcView.invalidate();
                                        arcWindow.updateViewLayout(arcView, arcParams);

                                        if (!initial) {
                                            if (HAPTIC_FEEDBACK) {
                                                v.vibrate(25);
                                            }
                                        } else {
                                            initial = false;
                                        }
                                    }

                                    if (distance > SWIPE_MIN_DISTANCE && vibrateNeeded) {
                                        arcView.conversationsPaint.setAlpha(START_ALPHA2 + 20);
                                        arcView.newMessagePaint.setAlpha(START_ALPHA2);
                                        arcView.invalidate();
                                        arcWindow.updateViewLayout(arcView, arcParams);

                                        if (HAPTIC_FEEDBACK) {
                                            v.vibrate(25);
                                        }

                                        vibrateNeeded = false;
                                    }

                                    if (distance < SWIPE_MIN_DISTANCE) {
                                        arcView.conversationsPaint.setAlpha(START_ALPHA);
                                        arcView.newMessagePaint.setAlpha(START_ALPHA2);
                                        arcView.invalidate();
                                        arcWindow.updateViewLayout(arcView, arcParams);
                                        vibrateNeeded = true;
                                    }


                                } else // in flat area
                                {
                                    if(inButtons)
                                    {
                                        arcView.closePaint.setAlpha(START_ALPHA);
                                        arcView.movePaint.setAlpha(START_ALPHA);

                                        inButtons = false;

                                        inClose = false;
                                        inMove = false;

                                        topVibrate = true;
                                    }

                                    if (inDash && distance > SWIPE_MIN_DISTANCE) {
                                        inDash = false;
                                        inFlat = true;

                                        arcView.newMessagePaint.setAlpha(TOUCHED_ALPHA);
                                        arcView.conversationsPaint.setAlpha(START_ALPHA);
                                        arcView.invalidate();
                                        arcWindow.updateViewLayout(arcView, arcParams);

                                        if (!initial) {
                                            if (HAPTIC_FEEDBACK) {
                                                v.vibrate(25);
                                            }
                                        } else {
                                            initial = false;
                                        }
                                    }
                                    if (distance > SWIPE_MIN_DISTANCE && vibrateNeeded) {
                                        arcView.newMessagePaint.setAlpha(TOUCHED_ALPHA);
                                        arcView.conversationsPaint.setAlpha(START_ALPHA);
                                        arcView.invalidate();
                                        arcWindow.updateViewLayout(arcView, arcParams);

                                        if (HAPTIC_FEEDBACK) {
                                            v.vibrate(25);
                                        }

                                        vibrateNeeded = false;
                                    }

                                    if (distance < SWIPE_MIN_DISTANCE) {
                                        arcView.newMessagePaint.setAlpha(START_ALPHA2);
                                        arcView.conversationsPaint.setAlpha(START_ALPHA);
                                        arcView.invalidate();
                                        arcWindow.updateViewLayout(arcView, arcParams);
                                        vibrateNeeded = true;
                                    }
                                }

                                return true;

                            case MotionEvent.ACTION_UP:

                                haloWindow.updateViewLayout(haloView, haloParams);

                                // now will fire a different intent depending on what view you are in
                                if(inButtons)
                                {
                                    if(inMove) // move button was clicked
                                    {
                                        Intent intent = new Intent(getBaseContext(), com.klinker.android.messaging_sliding.slide_over.SlideOverSettings.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);

                                    } else // close button was clicked
                                    {
                                        sharedPrefs.edit().putBoolean("slideover_enabled", false).commit();

                                        Intent service = new Intent();
                                        service.setAction("com.klinker.android.messaging.STOP_HALO");
                                        sendBroadcast(service);
                                    }
                                }
                                else if (distance > SWIPE_MIN_DISTANCE && inFlat) {
                                    if (isRunning(getApplication())) {
                                        Intent intent = new Intent();
                                        intent.setAction("com.klinker.android.messaging_donate.KILL_FOR_HALO");
                                        sendBroadcast(intent);
                                    }

                                    Intent intent = new Intent(getBaseContext(), com.klinker.android.messaging_sliding.MainActivityPopup.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("fromHalo", true);
                                    startActivity(intent);
                                } else if (distance > SWIPE_MIN_DISTANCE && inDash) {
                                    if (isRunning(getApplication())) {
                                        Intent intent = new Intent();
                                        intent.setAction("com.klinker.android.messaging_donate.KILL_FOR_HALO");
                                        sendBroadcast(intent);
                                    }

                                    Intent intent = new Intent(getBaseContext(), com.klinker.android.messaging_sliding.MainActivityPopup.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("fromHalo", true);
                                    intent.putExtra("secAction", true);
                                    intent.putExtra("secondaryType", sharedPrefs.getString("slideover_secondary_action", "conversations"));
                                    startActivity(intent);
                                }

                                arcView.newMessagePaint.setAlpha(START_ALPHA2);

                                arcWindow.removeViewImmediate(arcView);

                                needDetection = true;

                                return true;
                        }
                    } else // if they have a new message to display
                    {
                        zoneWidth = (width - SWIPE_MIN_DISTANCE) / (numberNewConv);

                        switch (type) {
                            case MotionEvent.ACTION_DOWN:

                                if (HAPTIC_FEEDBACK) {
                                    v.vibrate(10);
                                }

                                initX = event.getX();
                                initY = event.getY();

                                arcView.newMessagePaint.setAlpha(START_ALPHA2);

                                arcWindow.addView(arcView, arcParams);
                                haloWindow.updateViewLayout(haloView, haloHiddenParams);
                                needDetection = true;
                                return true;

                            case MotionEvent.ACTION_MOVE:

                                xPortion = initX - event.getX();
                                yPortion = initY - event.getY();

                                distance = Math.sqrt(Math.pow(xPortion, 2) + Math.pow(yPortion, 2));
                                angle = Math.toDegrees(Math.atan(yPortion / xPortion));

                                if (!sharedPrefs.getString("slideover_side", "left").equals("left"))
                                    angle *= -1;

                                if (event.getRawY() < 120) // in Top Area
                                {
                                    inButtons = true;
                                    resetZoneAlphas();

                                    if (HAPTIC_FEEDBACK && topVibrate) {
                                        v.vibrate(25);
                                        vibrateNeeded = true;
                                        topVibrate = false;
                                    }

                                    if(event.getRawX() < width/2) // Close Zone
                                    {
                                        inClose = true;
                                        if(HAPTIC_FEEDBACK && inMove)
                                        {
                                            v.vibrate(25);
                                            inMove = false;
                                        }
                                        arcView.closePaint.setAlpha(TOUCHED_ALPHA);
                                        arcView.movePaint.setAlpha(START_ALPHA2);
                                        arcView.newMessagePaint.setAlpha(START_ALPHA2);
                                    } else // Move Zone
                                    {
                                        inMove = true;
                                        if(HAPTIC_FEEDBACK && inClose)
                                        {
                                            v.vibrate(25);
                                            inClose = false;
                                        }
                                        arcView.closePaint.setAlpha(START_ALPHA2);
                                        arcView.movePaint.setAlpha(TOUCHED_ALPHA);
                                        arcView.newMessagePaint.setAlpha(START_ALPHA2);
                                    }

                                    arcView.invalidate();
                                    arcWindow.updateViewLayout(arcView, arcParams);

                                } else if ((!(initY > event.getY()) && angle > ARC_BREAK_POINT)) // in dash area
                                {
                                    if(inButtons)
                                    {
                                        arcView.closePaint.setAlpha(START_ALPHA);
                                        arcView.movePaint.setAlpha(START_ALPHA);

                                        inButtons = false;

                                        inClose = false;
                                        inMove = false;

                                        topVibrate = true;
                                    }
                                    resetZoneAlphas();
                                    lastZone = 0;
                                    currentZone = 0;

                                    fromDash = true;

                                    if (inFlat && distance > SWIPE_MIN_DISTANCE) {
                                        inFlat = false;
                                        inDash = true;

                                        arcView.conversationsPaint.setAlpha(START_ALPHA2 + 20);
                                        arcView.newMessagePaint.setAlpha(START_ALPHA2);
                                        arcView.invalidate();
                                        arcWindow.updateViewLayout(arcView, arcParams);

                                        if (!initial) {
                                            if (HAPTIC_FEEDBACK) {
                                                v.vibrate(25);
                                            }
                                        } else {
                                            initial = false;
                                        }
                                    }

                                    if (distance > SWIPE_MIN_DISTANCE && vibrateNeeded) {
                                        arcView.conversationsPaint.setAlpha(START_ALPHA2 + 20);
                                        arcView.newMessagePaint.setAlpha(START_ALPHA2);
                                        arcView.invalidate();
                                        arcWindow.updateViewLayout(arcView, arcParams);

                                        if (HAPTIC_FEEDBACK) {
                                            v.vibrate(25);
                                        }

                                        vibrateNeeded = false;
                                    }

                                    if (distance < SWIPE_MIN_DISTANCE) {
                                        arcView.conversationsPaint.setAlpha(START_ALPHA);
                                        arcView.newMessagePaint.setAlpha(START_ALPHA2);
                                        arcView.invalidate();
                                        arcWindow.updateViewLayout(arcView, arcParams);
                                        vibrateNeeded = true;
                                    }


                                } else // in flat area
                                {
                                    currentZone = getCurrentZone(distance, zoneWidth, SWIPE_MIN_DISTANCE, numberNewConv);

                                    if (lastZone != currentZone) {
                                        zoneChange = true;
                                        lastZone = currentZone;
                                    } else {
                                        zoneChange = false;
                                    }

                                    if(inButtons)
                                    {
                                        arcView.closePaint.setAlpha(START_ALPHA);
                                        arcView.movePaint.setAlpha(START_ALPHA);

                                        inButtons = false;

                                        inClose = false;
                                        inMove = false;

                                        topVibrate = true;
                                        zoneChange = true;
                                    }

                                    if (inDash && distance > SWIPE_MIN_DISTANCE) {
                                        inDash = false;
                                        inFlat = true;

                                        arcView.newMessagePaint.setAlpha(TOUCHED_ALPHA);
                                        arcView.conversationsPaint.setAlpha(START_ALPHA);
                                        arcView.invalidate();
                                        arcWindow.updateViewLayout(arcView, arcParams);

                                        if (!initial) {
                                            if (HAPTIC_FEEDBACK) {
                                                v.vibrate(25);
                                            }
                                        } else {
                                            initial = false;
                                        }
                                    }
                                    if (distance > SWIPE_MIN_DISTANCE && vibrateNeeded) {
                                        arcView.newMessagePaint.setAlpha(TOUCHED_ALPHA);
                                        arcView.conversationsPaint.setAlpha(START_ALPHA);
                                        arcView.invalidate();
                                        arcWindow.updateViewLayout(arcView, arcParams);

                                        if (HAPTIC_FEEDBACK) {
                                            v.vibrate(25);
                                        }

                                        vibrateNeeded = false;
                                    }

                                    if (distance < SWIPE_MIN_DISTANCE) {
                                        resetZoneAlphas();
                                        lastZone = 0;
                                        currentZone = 0;

                                        arcView.newMessagePaint.setAlpha(START_ALPHA2);
                                        arcView.conversationsPaint.setAlpha(START_ALPHA);
                                        arcView.invalidate();
                                        arcWindow.updateViewLayout(arcView, arcParams);
                                        vibrateNeeded = true;
                                        fromDash = true;
                                    }

                                    if (zoneChange) {
                                        resetZoneAlphas();

                                        if (!fromDash) {
                                            if (HAPTIC_FEEDBACK) {
                                                v.vibrate(25);
                                            }
                                        } else {
                                            fromDash = false;
                                        }


                                        if (currentZone != 0)
                                            arcView.textPaint[currentZone - 1].setAlpha(TOUCHED_ALPHA);

                                        zoneChange = false;
                                        arcView.invalidate();
                                        arcWindow.updateViewLayout(arcView, arcParams);
                                    }

                                }

                                return true;

                            case MotionEvent.ACTION_UP:

                                haloWindow.updateViewLayout(haloView, haloParams);

                                // now will fire a different intent depending on what view you are in
                                if(inButtons)
                                {
                                    if(inMove) // move button was clicked
                                    {
                                        Intent intent = new Intent(getBaseContext(), com.klinker.android.messaging_sliding.slide_over.SlideOverSettings.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);

                                    } else // close button was clicked
                                    {
                                        sharedPrefs.edit().putBoolean("slideover_enabled", false).commit();

                                        Intent service = new Intent();
                                        service.setAction("com.klinker.android.messaging.STOP_HALO");
                                        sendBroadcast(service);
                                    }
                                }
                                else if (distance > SWIPE_MIN_DISTANCE && inFlat) {
                                    if (isRunning(getApplication())) {
                                        Intent intent = new Intent();
                                        intent.setAction("com.klinker.android.messaging_donate.KILL_FOR_HALO");
                                        sendBroadcast(intent);
                                    }

                                    Intent intent = new Intent(getBaseContext(), com.klinker.android.messaging_sliding.MainActivityPopup.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("fromHalo", true);

                                    if (currentZone == 0)
                                        intent.putExtra("openToPage", 0);
                                    else
                                        intent.putExtra("openToPage", currentZone - 1);

                                    startActivity(intent);
                                } else if (distance > SWIPE_MIN_DISTANCE && inDash) {
                                    if (isRunning(getApplication())) {
                                        Intent intent = new Intent();
                                        intent.setAction("com.klinker.android.messaging_donate.KILL_FOR_HALO");
                                        sendBroadcast(intent);
                                    }

                                    Intent intent = new Intent(getBaseContext(), com.klinker.android.messaging_sliding.MainActivityPopup.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("fromHalo", true);
                                    intent.putExtra("secAction", true);
                                    intent.putExtra("secondaryType", sharedPrefs.getString("slideover_secondary_action", "conversations"));
                                    startActivity(intent);
                                }

                                if (distance > SWIPE_MIN_DISTANCE) {
                                    arcView.newConversations.clear();

                                    haloView.setRegularHalo();
                                    haloView.invalidate();
                                    haloWindow.updateViewLayout(haloView, haloParams);

                                    numberNewConv = 0;
                                }

                                arcView.newMessagePaint.setAlpha(START_ALPHA2);
                                resetZoneAlphas();

                                arcWindow.removeViewImmediate(arcView);

                                needDetection = true;

                                return true;
                        }
                    }

                }

                return false;
            }
        });

        haloWindow.addView(haloView, haloParams);

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.messaging.STOP_HALO");
        registerReceiver(stopSlideover, filter);

        filter = new IntentFilter();
        filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        this.registerReceiver(mBroadcastReceiver, filter);
        
        filter = new IntentFilter();
        filter.addAction("com.klinker.android.messaging.UPDATE_HALO");
        this.registerReceiver(newMessageReceived, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.messaging.CLEAR_MESSAGES");
        this.registerReceiver(clearMessages, filter);
    }

    public int getCurrentZone(double distance, int zoneWidth, int arcLength, int maxZones)
    {
        int extraDist = (int) distance - arcLength;
        int currZone = 0;

        while(extraDist > 0 && currZone < maxZones)
        {
            currZone++;
            extraDist -= zoneWidth;
        }

        return currZone;
    }

    public void resetZoneAlphas()
    {
        for (int i = 0; i < numberNewConv; i++)
        {
            arcView.textPaint[i].setAlpha(START_ALPHA2);
        }

        arcView.invalidate();
        arcWindow.updateViewLayout(arcView, arcParams);
    }

    public boolean isRunning(Context ctx) {
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);

        for (ActivityManager.RunningTaskInfo task : tasks) {
            if (ctx.getPackageName().equalsIgnoreCase(task.baseActivity.getPackageName()))
                return true;
        }

        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(stopSlideover);
            unregisterReceiver(newMessageReceived);
            unregisterReceiver(mBroadcastReceiver);
            unregisterReceiver(clearMessages);
        } catch (Exception e) {

        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent myIntent) {
            stopService(new Intent(context, SlideOverService.class));
            haloView.invalidate();
            haloWindow.removeViewImmediate(haloView);
            unregisterReceiver(this);
            startService(new Intent(context, SlideOverService.class));
        }
    };

    public BroadcastReceiver stopSlideover = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            haloView.invalidate();
            haloWindow.removeViewImmediate(haloView);
            stopSelf();
            unregisterReceiver(this);
        }
    };
    
    public BroadcastReceiver newMessageReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String name = intent.getStringExtra("name");
            String message = intent.getStringExtra("message");

            int index;
            boolean exists = false;
            boolean same = false;

            // check for mms messages, group texts really messed things up with the opening to conversations. So now it will remove the old mms notification and throw the new one to the first spot.
            // Good for group mms. But more than one conversation at once won't work.
            // They will also eat the normal mms messages notifications
            // Don't see any way around this though, and i don't think many people will run into this problem, if they do, they won't be able to replicate  it hehe :)

            for (index = 0; index < numberNewConv; index++)
            {
                if (message.equals(arcView.newConversations.get(index)[1]))
                {
                    same = true;
                    break;
                }
            }

            if(same)
            {
                arcView.newConversations.add(new String[] {name, message});
                arcView.newConversations.remove(index);
            }else
            {
                for (index = 0; index < numberNewConv; index++)
                {
                    if (name.equals(arcView.newConversations.get(index)[0]))
                    {
                        exists = true;
                        break;
                    }
                }
            }

            if (!exists && !same)
                arcView.newConversations.add(new String[] {name, message});
            else if (!same)
            {
                String oldMessage = arcView.newConversations.get(index)[1];
                arcView.newConversations.add(new String[] {name, oldMessage + " | " + message});
                arcView.newConversations.remove(index);
            }

            numberNewConv = arcView.newConversations.size();

            arcView.updateTextPaint();
            arcView.invalidate();

            // set the icon to the red, recieved, icon
            haloView.setRecievedHalo();
            haloView.invalidate();
            haloWindow.updateViewLayout(haloView, haloParams);

            if (!animationView.circleText) {
                if (!sharedPrefs.getBoolean("popup_reply", false) || (sharedPrefs.getBoolean("popup_reply", true) && sharedPrefs.getBoolean("slideover_popup_lockscreen_only", false))) {
                    // start the animation
                    animationView.circleText = true;
                    animationView.firstText = true;
                    animationView.arcOffset = AnimationView.ORIG_ARC_OFFSET;
                    animationView.name = new String[] {name, message.length() > 50 ? message.substring(0, 50) + "..." : message};
                    animationWindow.addView(animationView, animationParams);

                    final NewMessageAnimation animation = new NewMessageAnimation(animationView, (float)(3 * (sharedPrefs.getInt("slideover_animation_speed", 33)/100.0) + 1), haloWindow);
                    animation.setRunning(true);
                    animation.start();
                }
            }
        }
    };

    public BroadcastReceiver clearMessages = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            arcView.newConversations.clear();

            haloView.setRegularHalo();
            haloView.invalidate();
            
            numberNewConv = 0;
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
}
