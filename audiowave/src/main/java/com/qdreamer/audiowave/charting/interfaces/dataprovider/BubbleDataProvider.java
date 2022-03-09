package com.qdreamer.audiowave.charting.interfaces.dataprovider;

import com.qdreamer.audiowave.charting.data.BubbleData;

public interface BubbleDataProvider extends BarLineScatterCandleBubbleDataProvider {

    BubbleData getBubbleData();
}
