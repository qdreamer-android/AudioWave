package com.qdreamer.audiowave.charting.interfaces.dataprovider;

import com.qdreamer.audiowave.charting.data.CandleData;

public interface CandleDataProvider extends BarLineScatterCandleBubbleDataProvider {

    CandleData getCandleData();
}
