package com.li.jacky.rangeseekbar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.li.jacky.rangeseekbarlibrary.RangeSeekBar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        RangeSeekBar<Double> viewById = (RangeSeekBar<Double>) findViewById(R.id.range_seekbar2);
        viewById.setAbsoluteMaxValue(66.66666);
    }
}
