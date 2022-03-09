package com.qdreamer.audiowave.charting.interfaces.dataprovider;

import com.qdreamer.audiowave.charting.data.ScatterData;

public interface ScatterDataProvider extends BarLineScatterCandleBubbleDataProvider {

    ScatterData getScatterData();
}
