package com.jordanro.guitarweirdo.tuner;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class FadeButton extends ImageView {
    public FadeButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public FadeButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FadeButton(Context context) {
        super(context);
        init();
    }

    //set the ontouch listener
    private void init() {
        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        ImageView view = (ImageView) v;
                        view.setAlpha(0.5f);
                        view.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        ImageView view = (ImageView) v;
                        view.setAlpha(1f);
                        view.invalidate();
                        break;
                    }
                }
                return false;
            }
        });
    }

}
