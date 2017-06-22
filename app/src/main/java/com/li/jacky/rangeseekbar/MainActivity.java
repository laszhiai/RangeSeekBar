package com.li.jacky.rangeseekbar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import com.li.jacky.rangeseekbar.RangeSeekBar.OnRangeSeekBarChangeListener;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RangeSeekBar<Double> sb = (RangeSeekBar<Double>) findViewById(R.id.range_seekbar);
        sb.setOnRangeSeekBarChangeListener(new OnRangeSeekBarChangeListener<Double>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Double minValue, Double maxValue) {
                Log.i("mtag", "onRangeSeekBarValuesChanged:      " + minValue + "  " + maxValue);

            }
        });
    }
}
