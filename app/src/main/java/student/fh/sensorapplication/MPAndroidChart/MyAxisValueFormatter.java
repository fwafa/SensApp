package student.fh.sensorapplication.MPAndroidChart;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;


public class MyAxisValueFormatter implements IAxisValueFormatter
{
    @Override
    public String getFormattedValue(float value, AxisBase axis)
    {
        int second = (int) value / 10;
        return String.valueOf(second);
    }
}
