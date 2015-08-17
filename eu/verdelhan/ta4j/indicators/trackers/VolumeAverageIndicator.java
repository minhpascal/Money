package eu.verdelhan.ta4j.indicators.trackers;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Indicator;
import eu.verdelhan.ta4j.indicators.CachedIndicator;

public class VolumeAverageIndicator extends CachedIndicator<Decimal> {

    private final int timeFrame;

    private final Indicator<Decimal> volume;

    private final Decimal multiplier;
    
    public VolumeAverageIndicator(Indicator<Decimal> indicator, int timeFrame) {
        super(indicator);
        this.timeFrame = timeFrame;
        this.volume = indicator;
        multiplier = Decimal.TWO.dividedBy(Decimal.valueOf(timeFrame + 1));
    }

    //
    //Volume Average here
    //
    @Override
    protected Decimal calculate(int index) {

        if (index + 1 < timeFrame) {
            // Starting point of the EMA
            return new SMAIndicator(volume, timeFrame).getValue(index);
        }
        if (index == 0) {
            // If the timeframe is bigger than the indicator's value count
            return volume.getValue(0);
        }
        Decimal volPrev = getValue(index - 1);
        return volume.getValue(index).minus(volPrev).multipliedBy(multiplier).plus(volPrev);
    }
}