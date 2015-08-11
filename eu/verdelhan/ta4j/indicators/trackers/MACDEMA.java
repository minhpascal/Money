package eu.verdelhan.ta4j.indicators.trackers;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Indicator;
import eu.verdelhan.ta4j.indicators.CachedIndicator;

public class MACDEMA extends CachedIndicator<Decimal> {

    private final int timeFrame;

    private final MACDIndicator macd;
    
    private final Decimal multiplier;

    public MACDEMA(Indicator<Decimal> indicator, int shortTimeFrame, int longTimeFrame, int timeFrame) {
        super(indicator);
        this.timeFrame = timeFrame;
        this.macd = new MACDIndicator(indicator, shortTimeFrame,longTimeFrame);
        multiplier = Decimal.TWO.dividedBy(Decimal.valueOf(timeFrame + 1));
    }

    //
    //This is the slow line: Take EMA of the macd values here
    //
    @Override
    protected Decimal calculate(int index) {

        if (index + 1 < timeFrame) {
            // Starting point of the EMA
            return new SMAIndicator(macd, timeFrame).getValue(index);
        }
        if (index == 0) {
            // If the timeframe is bigger than the indicator's value count
            return macd.getValue(0);
        }
        Decimal emaPrev = getValue(index - 1);
        return macd.getValue(index).minus(emaPrev).multipliedBy(multiplier).plus(emaPrev);
 
    }
    
    //
    //This is the fast line
    //
    public Decimal macdResult(int index) {
    	return macd.getValue(index);
    }
}


