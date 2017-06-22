package com.li.jacky.rangeseekbar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import com.li.jacky.rangeseekbar.RangeSeekBar.OnRangeSeekBarChangeListener;
import java.math.BigDecimal;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        RangeSeekBar<Integer> sb1 = (RangeSeekBar<Integer>) findViewById(R.id.range_seekbar1);
        RangeSeekBar<Double> sb2 = (RangeSeekBar<Double>) findViewById(R.id.range_seekbar2);
        RangeSeekBar<BigDecimal> sb7 = (RangeSeekBar<BigDecimal>) findViewById(R.id.range_seekbar3);
    }
}
