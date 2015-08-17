package apidemo;

import java.util.List;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Trade;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.indicators.helpers.CrossIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.VolumeIndicator;
import eu.verdelhan.ta4j.indicators.trackers.EMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.MACDEMA;
import eu.verdelhan.ta4j.indicators.trackers.MACDIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.VolumeAverageIndicator;


public class ESFuturesGapStrategy extends Strategy {
	private static final double MACD_GAP_SEPARATION_FUDGE=0.8; //When price rises above short ma for entry
	private static final double MA_GAP_EXIT_FUDGE=0.5;   //When price crosses short ma for exit
	
	private ClosePriceIndicator closePrice;
    private SMAIndicator ssma;
    private SMAIndicator lsma;
    private MACDEMA macd;
    private EMAIndicator macdEMA;  //this is the slow line on the MACD
    private VolumeAverageIndicator volumeAverage;

    public  boolean inTrade=false;
    public  boolean maAbove=true;
    public  boolean macdAbove=true;
    
    public ESFuturesGapStrategy(ClosePriceIndicator c, SMAIndicator s, SMAIndicator l, MACDEMA m, VolumeAverageIndicator v) {
		super();
		
		closePrice = c;
	    ssma = s;
	    lsma = l;
	    macd = m;
	    volumeAverage = v;
	    
	    inTrade = false;
	    maAbove = true;  //1 means short is above long
	    macdAbove = true;
	}
    
    public void updateGetValues(int index) {
    	closePrice.getValue(index);
    	ssma.getValue(index);
    	lsma.getValue(index);
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
        
        Decimal cp = closePrice.getValue(index);
        Decimal sma = ssma.getValue(index);  	//short moving average
        Decimal lma = lsma.getValue(index);  	//long moving average
        Decimal macdFast = macd.macdResult(index);  //macd value
        Decimal macdSlow = macd.getValue(index);
        Decimal vo = volumeAverage.getValue(index);
        
        if (index <35)		//don't do anything for the first 35 periods
        	return false;
        
        if (inTrade) {
        	return false;
        }    
        //
        //Start Analysis
        //
        if (macdAbove) {
        	if (macdFast.toDouble() < (macdSlow.toDouble() - 0.01) && (macdFast.toDouble() < 0)) {
        		macdAbove = false;
        		trigger = true;
        		System.out.format("\nMACD Fast Cross Down fast: %2.3f slow:%2.3f: %s ss:%4.2f ls:%4.2f",macdFast.toDouble(),macdSlow.toDouble(),closePrice.getTimeSeries().getLastTick().toGoodString(),sma.toDouble(),lma.toDouble());
        	}
        } else {
        	if (macdFast.toDouble() > (macdSlow.toDouble() + 0.01) && (macdFast.toDouble() > 0)) {
        		System.out.format("\nMACD Fast Cross Up   fast: %2.3f slow:%2.3f: %s ss:%4.2f ls:%4.2f",macdFast.toDouble(),macdSlow.toDouble(),closePrice.getTimeSeries().getLastTick().toGoodString(),sma.toDouble(),lma.toDouble());
        		macdAbove = true;
        		trigger = true;
        	}
        }
        
        if (trigger) {
        	
        	if (macdAbove == true) {

        		enter = true;
        		inTrade = true;
        		longTrade = true;
        		
        	} else {

        		enter = true;
        		inTrade = true;
        		longTrade = false;
        	}
        	/*
        	if (maAbove) {
        		if (sma.toDouble() < lma.toDouble()) {
        			maAbove = false;
        			//System.out.format("\nClose Cross Down: Time %s Close:%4.2f ss:%4.2f ls:%4.2f",closePrice.getTimeSeries().getLastTick().toGoodString(),cp.toDouble(),ss.toDouble(),ls.toDouble());
        		}
        	} else {
        		if (sma.toDouble() > lma.toDouble()) {
        			//System.out.format("\nClose Cross Up  : Time %s Close:%4.2f ss:%4.2f ls:%4.2f",closePrice.getTimeSeries().getLastTick().toGoodString(),cp.toDouble(),ss.toDouble(),ls.toDouble());
        			maAbove = true;
            		enter = true;
            		inTrade=true;
            		longTrade=true;
        		}
        	}
        	*/
        }
        
        //System.out.format("\n%s macdFast:%4.3f macdSlow:%4.3f",closePrice.getTimeSeries().getLastTick().toGoodString(),macdFast.toDouble(),macdSlow.toDouble());

        
        //System.out.format("\nShould Enter??: Close:%4.2f ss:%4.2f ls:%4.2f",cp.toDouble(),ss.toDouble(),ls.toDouble());
        /*
        if (closeShortMACrossUp.getValue(index)) {
            System.out.format("\nClose Cross Up  : Time:%s Close:%4.2f ss:%4.2f ls:%4.2f",closePrice.getTimeSeries().getLastTick().toGoodString(),cp.toDouble(),ss.toDouble(),ls.toDouble());
        } else if (closeShortMACrossDown.getValue(index)) {
        	 System.out.format("\nClose Cross Down: Time %s Close:%4.2f ss:%4.2f ls:%4.2f",closePrice.getTimeSeries().getLastTick().toGoodString(),cp.toDouble(),ss.toDouble(),ls.toDouble());
        }
        */
        
        /*
        //Check price is greater than short and long moving averages
        if (cp.isGreaterThan(ss) && ss.isGreaterThan(ls)) {
        	Decimal malV = mal.plus(Decimal.valueOf(MACD_GAP_SEPARATION_FUDGE));  //add variable
        	System.out.format("AAAAA");
        	if (ma.isGreaterThan(Decimal.valueOf(0)) && mas.isGreaterThan(malV)) {
        		//Add checking volume here
        		
        		enter = true;
        		inTrade=true;
        		longTrade=true;
        	}
        }
        else if (cp.isLessThan(ss) && ss.isLessThan(ls)) {
        	Decimal malV = mal.minus(Decimal.valueOf(MACD_GAP_SEPARATION_FUDGE));  //add variable
        	System.out.format("BBBBBB");
        	if (ma.isLessThan(Decimal.valueOf(0)) && mas.isLessThan(malV)) {
        		//Add checking volume here
        		
        		enter = true;
        		inTrade=true;
        		longTrade=false;
        	}
        }
        */
        
        
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
        
        Decimal macdFast = macd.macdResult(index);  //macd value
        Decimal macdSlow = macd.getValue(index);
        Decimal cp = closePrice.getValue(index);
        
        if (inTrade == false) {
        	return false;
        }
        //
        //Start Analysis
        //
        if (macdAbove) {
        	if (macdFast.toDouble() < (macdSlow.toDouble() - 0.01) && (macdFast.toDouble() < 0)) {
        		exit = true;
        		inTrade=false;
        		System.out.format("\nMACD Fast Cross Down fast: %2.3f slow:%2.3f: %s ",macdFast.toDouble(),macdSlow.toDouble(),closePrice.getTimeSeries().getTick(index).toGoodString());
        	}
        } else {
        	if (macdFast.toDouble() > (macdSlow.toDouble() + 0.01) && (macdFast.toDouble() > 0)) {
        		exit = true;
        		inTrade=false;
        		System.out.format("\nMACD Fast Cross Up   fast: %2.3f slow:%2.3f: %s ",macdFast.toDouble(),macdSlow.toDouble(),closePrice.getTimeSeries().getLastTick().toGoodString());
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
  /*  
    public boolean shouldExit(int index) {
    	return shouldExit(index,tradingRecord);
    }
    }
    */
}