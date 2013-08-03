package com.klinker.android.messaging_sliding.slide_over;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.telephony.SmsManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.klinker.android.messaging_donate.R;

import static android.support.v4.app.ActivityCompat.startActivity;

/**
 * Created by luke on 8/2/13.
 */
public class SlideOverView extends ViewGroup {
    Bitmap halo;

    public SlideOverView(Context context,Bitmap halo) {
        super(context);

        this.halo=halo;



    }


    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);


        // delete below line if you want transparent back color, but to understand the sizes use back color
        canvas.drawColor(Color.BLACK);

        canvas.drawBitmap(halo,0 , 0, null);


        //canvas.drawText("Hello World", 5, 15, mLoadPaint);

    }


    protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
    }

    public boolean onTouchEvent(MotionEvent event) {
        //return super.onTouchEvent(event);
        // Toast.makeText(getContext(),"onTouchEvent", Toast.LENGTH_LONG).show();

        return true;
    }
}
