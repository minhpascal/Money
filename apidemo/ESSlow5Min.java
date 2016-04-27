package apidemo;

import java.util.List;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.indicators.simple.VolumeIndicator;
import eu.verdelhan.ta4j.indicators.trackers.EMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.MACDEMA;
import eu.verdelhan.ta4j.indicators.trackers.VolumeAverageIndicator;

public class ESSlow5Min extends Strategy {

    private MACDEMA macd;
    private EMAIndicator macdEMA;  //this is the slow line on the MACD
    private VolumeAverageIndicator volumeAverage;
	
    public  boolean maAbove=true;
    public  boolean macdAbove=true;
    
    public ESSlow5Min(MoneyFeed moneyfeed) {
    	super(moneyfeed,"ESSlow5Min");
		maObject = moneyfeed.getMAObject(5);  //For 5 minute periods MUST HAVE FOR EACH STRATEGY
		
        macd = new MACDEMA(moneyfeed.series5min.closePrice, 12, 26, 9);
        VolumeIndicator volume = new VolumeIndicator(moneyfeed.series5min.series);
        volumeAverage = new VolumeAverageIndicator(volume,8);
        
	    maAbove = true;  //1 means short is above long
	    macdAbove = true;
	}
    
    public void updateGetValues(int index) {
    	macd.getValue(index);
    	volumeAverage.getValue(index);
    }

    // Enter: price leads slow ma and fast ma by 8
    // Exit: price goes below fast ma by 5
    
    @Override
	public boolean shouldEnter(int index, TradingRecord tradingRecord) {
    	
        boolean enter=false; 
        boolean trigger = false;
        
        Decimal cp = moneyfeed.series5min.closePrice.getValue(index);
        Decimal fma = moneyfeed.series5min.fastSMA8.getValue(index);  	//fast moving average
        Decimal sma = moneyfeed.series5min.slowSMA34.getValue(index);  	//slow moving average
        
        Decimal macdFast = macd.macdResult(index);  //macd value
        Decimal macdSlow = macd.getValue(index);
        Decimal vo = volumeAverage.getValue(index);
        
        int ENTER_PRICE_DIFF = 4;
        
        if (index < 5)	{ //don't do anything for the first 5 periods
        	return false;
    	}
    
        if (tradingRecord.isTradeOpen()) {
        	return false;
        }   
        
    	System.out.format("\nESSlow5Min TICK DATA: fastma:%2.2f slowma:%2.2f: %s ",fma.toDouble(),sma.toDouble(),
    			moneyfeed.series5min.closePrice.getTimeSeries().getTick(index).toGoodString());
        

        if (fma.toDouble() > sma.toDouble() && cp.toDouble() > (fma.toDouble() + ENTER_PRICE_DIFF) ) {
        	enter = true;
        	longTrade = true;
        	System.out.format("\nESSlow5Min SHOULD ENTER: fastma:%2.2f slowma:%2.2f: %s ",fma.toDouble(),sma.toDouble(),
        			moneyfeed.series5min.closePrice.getTimeSeries().getTick(index).toGoodString());
        }
        else if (fma.toDouble() < sma.toDouble() && cp.toDouble() < (fma.toDouble() - ENTER_PRICE_DIFF) ) {
        	enter = true;
        	longTrade = false;
        	System.out.format("\nESSlow5Min SHOULD ENTER: fastma:%2.2f slowma:%2.2f: %s ",fma.toDouble(),sma.toDouble(),
        			moneyfeed.series5min.closePrice.getTimeSeries().getTick(index).toGoodString());
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
       
        Decimal cp = moneyfeed.series5min.closePrice.getValue(index);
        Decimal fma = moneyfeed.series5min.fastSMA8.getValue(index);  	//fast moving average
        Decimal sma = moneyfeed.series5min.slowSMA34.getValue(index);  	//slow moving average
        
        Decimal macdFast = macd.macdResult(index);  //macd value
        Decimal macdSlow = macd.getValue(index);
        
        int EXIT_PRICE_DIFF = 3;
        
        if (!tradingRecord.isTradeOpen()) {
        	return false;
        } 
        
        if (longTrade) {
        	if (cp.toDouble() < (fma.toDouble() - EXIT_PRICE_DIFF)) {
        		exit = true;
        		System.out.format("\nESSlow5Min Exit: fastma:%2.2f slowma:%2.2f: %s ",fma.toDouble(),sma.toDouble(),
        				moneyfeed.series5min.closePrice.getTimeSeries().getTick(index).toGoodString());
        	}
        }
        else {
        	if (cp.toDouble() > (fma.toDouble() + EXIT_PRICE_DIFF)) {
        		exit = true;
        		System.out.format("\nESSlow5Min Exit: fastma:%2.2f slowma:%2.2f: %s ",fma.toDouble(),sma.toDouble(),
        				moneyfeed.series5min.closePrice.getTimeSeries().getTick(index).toGoodString());
        	}
        }
        //
        //Start Analysis
        //
        if (macdAbove) {
        	if (macdFast.toDouble() < (macdSlow.toDouble() - 0.01) && (macdFast.toDouble() < 0)) {
        		exit = true;
        		System.out.format("\nMACD Fast Cross Down fast: %2.2f slow:%2.2f: %s ",macdFast.toDouble(),macdSlow.toDouble(),moneyfeed.series5min.closePrice.getTimeSeries().getTick(index).toGoodString());
        	}
        } else {
        	if (macdFast.toDouble() > (macdSlow.toDouble() + 0.01) && (macdFast.toDouble() > 0)) {
        		exit = true;
        		System.out.format("\nMACD Fast Cross Up   fast: %2.2f slow:%2.2f: %s ",macdFast.toDouble(),macdSlow.toDouble(),moneyfeed.series5min.closePrice.getTimeSeries().getLastTick().toGoodString());
        	}
        }
        if (!exit) {
        	Order entry=tradingRecord.getLastOrder();
        	
        	Decimal priceDiff;        	
        	priceDiff = cp.minus(entry.getPrice()).abs();       			
        	Decimal amount = priceDiff.multipliedBy(entry.getAmount().multipliedBy(Decimal.valueOf(50)));
        	
        	/*
        	if (amount.toDouble() > 3000) {
        		exit = true;
        		inTrade = false;
        		System.out.format("\nExiting Trade b/c of Profit Max");
        	}
        	*/
        }
 
        traceShouldExit(index, exit);
        return exit;
    }
 }