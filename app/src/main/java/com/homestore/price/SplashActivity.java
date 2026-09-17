package com.homestore.price;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int color = AppPrefs.getColor(this);
        int dark = darken(color);
        int light = lighten(color);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{light, color, dark});
        root.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("荣芹超市");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 52);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setShadowLayer(8, 0, 5, 0x59000000);
        root.addView(title);

        View bar = new View(this);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(0xB3FFFFFF);
        barBg.setCornerRadius(dp(3));
        bar.setBackground(barBg);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(72), dp(5));
        barLp.topMargin = dp(20);
        bar.setLayoutParams(barLp);
        root.addView(bar);

        TextView sub = new TextView(this);
        sub.setText("商品价格");
        sub.setTextColor(Color.WHITE);
        sub.setAlpha(0.95f);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(16);
        sub.setLayoutParams(subLp);
        root.addView(sub);

        TextView welcome = new TextView(this);
        welcome.setText("—— 欢迎光临 ——");
        welcome.setTextColor(0xAAFFFFFF);
        welcome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wLp.topMargin = dp(8);
        welcome.setLayoutParams(wLp);
        root.addView(welcome);

        setContentView(root);

        AnimationSet anim = new AnimationSet(true);
        anim.addAnimation(new AlphaAnimation(0f, 1f));
        anim.addAnimation(new ScaleAnimation(0.82f, 1f, 0.82f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
        anim.setDuration(900);
        root.startAnimation(anim);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 1900);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private int darken(int c) {
        return Color.rgb(Color.red(c) * 70 / 100,
                Color.green(c) * 70 / 100, Color.blue(c) * 70 / 100);
    }

    private int lighten(int c) {
        return Color.rgb(Color.red(c) + (255 - Color.red(c)) * 25 / 100,
                Color.green(c) + (255 - Color.green(c)) * 25 / 100,
                Color.blue(c) + (255 - Color.blue(c)) * 25 / 100);
    }
}
