package com.qdreamer.audiowave.charting.interfaces.dataprovider;

import com.qdreamer.audiowave.charting.components.YAxis.AxisDependency;
import com.qdreamer.audiowave.charting.data.BarLineScatterCandleBubbleData;
import com.qdreamer.audiowave.charting.utils.Transformer;

public interface BarLineScatterCandleBubbleDataProvider extends ChartInterface {

    Transformer getTransformer(AxisDependency axis);
    boolean isInverted(AxisDependency axis);
    
    float getLowestVisibleX();
    float getHighestVisibleX();

    BarLineScatterCandleBubbleData getData();
}
