package apidemo;

import java.util.List;
import apidemo.MoneyFeed.MovingAverage;
import apidemo.MoneyFeed.SLOW_FAST;

/*
public class MovingMomentumStrategy {


    public static Strategy buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // The bias is bullish when the shorter-moving average moves above the longer moving average.
        // The bias is bearish when the shorter-moving average moves below the longer moving average.
        EMAIndicator shortEma = new EMAIndicator(closePrice, 9);
        EMAIndicator longEma = new EMAIndicator(closePrice, 26);

        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(series, 14);

        MACDIndicator macd = new MACDIndicator(closePrice, 9, 26);
        EMAIndicator emaMacd = new EMAIndicator(macd, 18);
        
        // Entry rule
        Rule entryRule = new OverIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedDownIndicatorRule(stochasticOscillK, Decimal.valueOf(20))) // Signal 1
                .and(new OverIndicatorRule(macd, emaMacd)); // Signal 2
        
        // Exit rule
        Rule exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedUpIndicatorRule(stochasticOscillK, Decimal.valueOf(80))) // Signal 1
                .and(new UnderIndicatorRule(macd, emaMacd)); // Signal 2
        
        return new Strategy(entryRule, exitRule);
    }
*/

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.indicators.simple.VolumeIndicator;
import eu.verdelhan.ta4j.indicators.trackers.EMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.MACDEMA;
import eu.verdelhan.ta4j.indicators.trackers.MACDIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.VolumeAverageIndicator;

public class ESFuturesGapStrategy extends Strategy {
	
	private static final double MACD_GAP_SEPARATION_FUDGE=0.2; //When MACD slow/fast comparison
	private static final double MA_GAP_EXIT_FUDGE=0.5;   //When price crosses short ma for exit
	private static final double MA_PRICE_GAP_SEPARATION_FUDGE=2; //When price rises above long ma for entry

	/*
	private ClosePriceIndicator closePrice;
    private SMAIndicator ssma;
    private SMAIndicator lsma;
    */
    private MACDEMA macd;
    private EMAIndicator macdEMA;  //this is the slow line on the MACD
    private VolumeAverageIndicator volumeAverage;
	
    public  boolean maAbove=true;
    public  boolean macdAbove=true;
    
    //
    //Assume Moneyfeed is setup ahead of time
    //
    
    public ESFuturesGapStrategy(MoneyFeed moneyfeed) {
    	//
    	//All new strategies must have the following 2 lines
    	//
		super(moneyfeed,"ESFuturesGapStrategy");
		maObject = moneyfeed.getMAObject(1);  //For 1 minute periods
		
        macd = new MACDEMA(moneyfeed.series1min.closePrice, 12, 26, 9);
        VolumeIndicator volume = new VolumeIndicator(moneyfeed.series1min.series);
        volumeAverage = new VolumeAverageIndicator(volume,8);
	    maAbove = true;  //1 means short is above long
	    macdAbove = true;
	}
    
    //Update the values that the Strategy is managing on it's own, not MoneyFeeds
    
    public void updateGetValues(int index) {
    	System.out.println("\nNOT DOING ANYTHING HERE updateGetValues");
    	/*
    	closePrice.getValue(index);
    	ssma.getValue(index);
    	lsma.getValue(index);
    	*/
    	macd.getValue(index);
    	volumeAverage.getValue(index);
    	
    }

    //
    // This is called every period of the tick, so defaults to 30 seconds
    // long:
    //		macd above 0, fast above slow, fast ma above slow ma, exit when macd crosses or $2000
    // short:
    //		macd below 0, fast below slow, fast ma below slow ma, exit when macd crosses or $2000 
    
    @Override
	public boolean shouldEnter(int index, TradingRecord tradingRecord) {
        boolean enter=false; //= entryRule.isSatisfied(index, tradingRecord);
        boolean trigger = false;
        
        Decimal cp = moneyfeed.series1min.closePrice.getValue(index);
        Decimal sma = moneyfeed.series1min.fastSMA8.getValue(index);  	//fast moving average
        Decimal lma = moneyfeed.series1min.slowSMA34.getValue(index);  	//slow moving average
        
        Decimal macdFast = macd.macdResult(index);  //macd value
        Decimal macdSlow = macd.getValue(index);
        Decimal vo = volumeAverage.getValue(index);
        
        if (index <35)		//don't do anything for the first 35 periods
        	return false;
        
        if (tradingRecord.isTradeOpen()) {
        	return false;
        }   
        //
        //Start Analysis
        //
        //System.out.format("\n%s macdFast:%4.3f macdSlow:%4.3f",moneyfeed.series1min.closePrice.getTimeSeries().getLastTick().toGoodString(),macdFast.toDouble(),macdSlow.toDouble());
        
        if (macdAbove) {
        	if (macdFast.toDouble() < (macdSlow.toDouble() - MACD_GAP_SEPARATION_FUDGE) && 
        	   (macdFast.toDouble() < 0) &&
        	   cp.toDouble() < (lma.toDouble() - MA_PRICE_GAP_SEPARATION_FUDGE)  //Compare to the 13 period average
        	   ) {
        		macdAbove = false;
        		trigger = true;
        		System.out.format("\nMACD Fast Cross Down fast: %2.3f slow:%2.3f: %s sma:%4.2f lma:%4.2f",macdFast.toDouble(),macdSlow.toDouble(),moneyfeed.series1min.closePrice.getTimeSeries().getLastTick().toGoodString(),sma.toDouble(),lma.toDouble());
        	}
        } else {
        	if (macdFast.toDouble() > (macdSlow.toDouble() + MACD_GAP_SEPARATION_FUDGE) && 
        	    (macdFast.toDouble() > 0) && 
        	    cp.toDouble() > (lma.toDouble() + MACD_GAP_SEPARATION_FUDGE)
        	   ) {
        		System.out.format("\nMACD Fast Cross Up   fast: %2.3f slow:%2.3f: %s sma:%4.2f lma:%4.2f",macdFast.toDouble(),macdSlow.toDouble(),moneyfeed.series1min.closePrice.getTimeSeries().getLastTick().toGoodString(),sma.toDouble(),lma.toDouble());
        		macdAbove = true;
        		trigger = true;
        	}
        }
        if (trigger) {
        	
        	if (macdAbove == true) {

        		enter = true;
        		longTrade = true;
        		
        	} else {

        		enter = true;
        		longTrade = false;
        	}
        }
        
        //System.out.format("\n%s macdFast:%4.3f macdSlow:%4.3f",closePrice.getTimeSeries().getLastTick().toGoodString(),macdFast.toDouble(),macdSlow.toDouble());
        
        if (enter) {
        	traceShouldEnter(index, enter);
        }
        return enter;
    }

    /**
     * @param index the tick index
     * @param tradingRecord the potentially needed trading history
     * @return true to recommend to exit, false otherwise
     */
    @Override
    public boolean shouldExit(int index, TradingRecord tradingRecord) {
        boolean exit=false; // = exitRule.isSatisfied(index, tradingRecord);
       
        Decimal cp = moneyfeed.series1min.closePrice.getValue(index);
        Decimal sma = moneyfeed.series1min.fastSMA8.getValue(index);  	//fast moving average
        Decimal lma = moneyfeed.series1min.slowSMA34.getValue(index);  	//slow moving average
        
        Decimal macdFast = macd.macdResult(index);  //macd value
        Decimal macdSlow = macd.getValue(index);
        
        if (!tradingRecord.isTradeOpen()) {
        	return false;
        }   
        //
        //Start Analysis
        //
        if (macdAbove) {
        	if (macdFast.toDouble() < (macdSlow.toDouble() - 0.01) ) { //&& (macdFast.toDouble() < 0) ) {
        		exit = true;
        		System.out.format("\nMACD Fast Cross Down fast: %2.3f slow:%2.3f: %s ",macdFast.toDouble(),macdSlow.toDouble(),moneyfeed.series1min.closePrice.getTimeSeries().getTick(index).toGoodString());
        	}
        } else {
        	if (macdFast.toDouble() > (macdSlow.toDouble() + 0.01)) {  //fast is greater than slow 
        			//(macdFast.toDouble() > 0)) { 
        		exit = true;
        		System.out.format("\nMACD Fast Cross Up   fast: %2.3f slow:%2.3f: %s ",macdFast.toDouble(),macdSlow.toDouble(),moneyfeed.series1min.closePrice.getTimeSeries().getLastTick().toGoodString());
        	}
        }
       // if (!exit) {
        	Order entry=tradingRecord.getLastOrder();
        	Decimal priceDiff;        	    			
        	Decimal amount;
        	
        	if (longTrade) {
        		priceDiff = cp.minus(entry.getPrice()); 
        	}
        	else {
        		priceDiff = entry.getPrice().minus(cp);
        	}
        	amount = priceDiff.multipliedBy(entry.getAmount().multipliedBy(Decimal.valueOf(50)));
        	
        	int min = -500;
        	int max = 3000;
        	
        	if (amount.toDouble() > max) {
        		exit = true;
        		System.out.format("\nExiting Trade b/c of Profit Max:"+max);
        	}
        	else if (amount.toDouble() < min) {
           		exit = true;
        		System.out.format("\nExiting Trade b/c of Profit Max:750"+min);
        	}
        	
       // }
 
        traceShouldExit(index, exit);
        return exit;
    }
  /*  
    public boolean shouldExit(int index) {
    	return shouldExit(index,tradingRecord);
    }
    }
    */
}