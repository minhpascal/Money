package apidemo;

import java.util.List;
import apidemo.MoneyFeed.MovingAverage;
import apidemo.MoneyFeed.SLOW_FAST;
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

public class ESGenericMACDCrossovers extends Strategy {
	
    private MACDIndicator macd;
    private EMAIndicator macdEMA;  //this is the slow line on the MACD
    
    private VolumeIndicator volume;
    private VolumeAverageIndicator volumeAverage;
	
    public  boolean maAbove=true;
    public  boolean macdAbove=true;
    
    //
    //Assume Moneyfeed is setup ahead of time
    //
    public ESGenericMACDCrossovers(MoneyFeed moneyfeed) {
    	//
    	//All new strategies must have the following 2 lines
    	//
		super(moneyfeed,"ESGenericMACDCrossovers");
		maObject = moneyfeed.getMAObject(1);  //For 1 minute periods
		
        macd = new MACDIndicator(moneyfeed.series1min.closePrice, 9, 26);
        macdEMA = new EMAIndicator(macd, 18);
		/*
        macd = new MACDEMA(moneyfeed.series1min.closePrice, 12, 26, 9);
        */
        volume = new VolumeIndicator(moneyfeed.series1min.series);
        volumeAverage = new VolumeAverageIndicator(volume,8);
		
	    maAbove = true;  //1 means short is above long
	    macdAbove = true;
	}
    
    //Update the values that the Strategy is managing on it's own, not MoneyFeeds
    
    public void updateGetValues(int index) {
    	macd.getValue(index);
    	macdEMA.getValue(index);
    	volumeAverage.getValue(index);
    }

    //
    // This is called every period of the tick, so defaults to 30 seconds
    // long:
    //		price is 4 above fast ma, which is above slow ma and volume is above 3x average
    // short:
    //		fast macd crosses slow macd or price goes opposite by over double the ATR
    
    @Override
	public boolean shouldEnter(int index, TradingRecord tradingRecord) {
        boolean enter=false; //= entryRule.isSatisfied(index, tradingRecord);
        
        Decimal cp = moneyfeed.series1min.closePrice.getValue(index);
        Decimal fma = moneyfeed.series1min.fastSMA8.getValue(index);  	//fast moving average
        Decimal sma = moneyfeed.series1min.slowSMA34.getValue(index);  	//slow moving average
        
        Decimal macdFast = macd.getValue(index);
        Decimal macdSlow = macdEMA.getValue(index);
        Decimal vol = volume.getValue(index);
        Decimal volAverage = volumeAverage.getValue(index);
        
        if (index < 5)		//don't do anything for the first 35 periods
        	return false;

        if (tradingRecord.isTradeOpen()) {
        	return false;
        }     
        //
        //Start Analysis
        //
        int ENTER_PRICE_DIFF = 4;
        
    	System.out.format("\n%s TICK DATA: fastma:%2.2f slowma:%2.2f: %s ",this.getClass().getName(),fma.toDouble(),sma.toDouble(),
    			moneyfeed.series1min.closePrice.getTimeSeries().getTick(index).toGoodString());
        
        if (fma.toDouble() > sma.toDouble() && cp.toDouble() > (fma.toDouble() + ENTER_PRICE_DIFF) && 
        		vol.isGreaterThan(volAverage.multipliedBy(Decimal.THREE)  )) {
        	enter = true;
        	longTrade = true;
        	macdAbove = true;
        	System.out.format("\n%s SHOULD ENTER: fastma:%2.2f slowma:%2.2f: %s ",this.getClass().getName(),fma.toDouble(),sma.toDouble(),
        			moneyfeed.series5min.closePrice.getTimeSeries().getTick(index).toGoodString());
        }
        else if (fma.toDouble() < sma.toDouble() && cp.toDouble() < (fma.toDouble() - ENTER_PRICE_DIFF) &&
        		vol.isGreaterThan(volAverage.multipliedBy(Decimal.THREE)  )) {
        	enter = true;
        	longTrade = false;
        	macdAbove = false;
        	System.out.format("\n%s SHOULD ENTER: fastma:%2.2f slowma:%2.2f: %s ",this.getClass().getName(),fma.toDouble(),sma.toDouble(),
        			moneyfeed.series1min.closePrice.getTimeSeries().getTick(index).toGoodString());
        }

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
        Decimal fma = moneyfeed.series1min.fastSMA8.getValue(index);  	//fast moving average
        Decimal sma = moneyfeed.series1min.slowSMA34.getValue(index);  	//slow moving average
        
        Decimal macdFast = macd.getValue(index);
        Decimal macdSlow = macdEMA.getValue(index);
        
        if (!tradingRecord.isTradeOpen()) {
        	return false;
        }   
        //
        //Start Analysis
        //
        if (macdAbove) {
        	if (macdFast.toDouble() < (macdSlow.toDouble() - 0.01) && (macdFast.toDouble() < 0)) {
        		exit = true;
        		System.out.format("\nMACD Fast Cross Down fast: %2.3f slow:%2.3f: %s ",macdFast.toDouble(),macdSlow.toDouble(),moneyfeed.series1min.closePrice.getTimeSeries().getTick(index).toGoodString());
        	}
        } else {
        	if (macdFast.toDouble() > (macdSlow.toDouble() + 0.01) && (macdFast.toDouble() > 0)) {
        		exit = true;
        		System.out.format("\nMACD Fast Cross Up   fast: %2.3f slow:%2.3f: %s ",macdFast.toDouble(),macdSlow.toDouble(),moneyfeed.series1min.closePrice.getTimeSeries().getLastTick().toGoodString());
        	}
        }
        if (!exit) {
        	Order entry=tradingRecord.getLastOrder();
        	
        	Decimal priceDiff;        	
        	priceDiff = cp.minus(entry.getPrice()).abs();       			
        	Decimal amount = priceDiff.multipliedBy(entry.getAmount().multipliedBy(Decimal.valueOf(50)));
        
        }
 
        traceShouldExit(index, exit);
        return exit;
    }
}