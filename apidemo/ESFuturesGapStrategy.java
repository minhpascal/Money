package apidemo;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.indicators.helpers.CrossIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.VolumeIndicator;
import eu.verdelhan.ta4j.indicators.trackers.EMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.MACDEMA;
import eu.verdelhan.ta4j.indicators.trackers.MACDIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;


public class ESFuturesGapStrategy extends Strategy {
	private static final double MACD_GAP_SEPARATION_FUDGE=0.8; //When price rises above short ma for entry
	private static final double MA_GAP_EXIT_FUDGE=0.5;   //When price crosses short ma for exit
	
	private ClosePriceIndicator closePrice;
    private SMAIndicator ssma;
    private SMAIndicator lsma;
    private MACDEMA macd;
    private EMAIndicator macdEMA;  //this is the slow line on the MACD
    private VolumeIndicator volume;
    private CrossIndicator maCrossUp;
    private CrossIndicator maCrossDown;
    private CrossIndicator closeShortMACrossUp;
    private CrossIndicator closeShortMACrossDown;
    
    public  boolean inTrade=false;
    public  boolean maLevel=true;
    public  boolean macdLevel=true;
    
    public ESFuturesGapStrategy(ClosePriceIndicator c, SMAIndicator s, SMAIndicator l, MACDEMA m, VolumeIndicator v) {
		super();
		
		closePrice = c;
	    ssma = s;
	    lsma = l;
	    macd = m;
	    volume = v;
	    inTrade = false;
	    
	    maCrossUp = new CrossIndicator(lsma,ssma);
	    maCrossDown = new CrossIndicator(ssma,lsma);
	    closeShortMACrossUp = new CrossIndicator(ssma,closePrice);
	    closeShortMACrossDown = new CrossIndicator(closePrice,ssma);
	    
	    maLevel = true;  //1 means short is above long
	    macdLevel = true;
	}
    
    //
    // This is called every period of the tick, so defaults to 30 seconds
    //
    @Override
	public boolean shouldEnter(int index, TradingRecord tradingRecord) {
        boolean enter=false; //= entryRule.isSatisfied(index, tradingRecord);
        
        Decimal cp = closePrice.getValue(index);
        Decimal ss = ssma.getValue(index);  	//short moving average
        Decimal ls = lsma.getValue(index);  	//long moving average
        Decimal macdFast = macd.macdResult(index);  //macd value
        Decimal macdSlow = macd.getValue(index);
        Decimal vo = volume.getValue(index);
        
        if (index <35)		//don't do anything for the first 35 periods
        	return false;
        
        if (inTrade) {
        	return false;
        }
        /*
        if (maLevel) {
        	if (ss.toDouble() < ls.toDouble()) {
        		maLevel = false;
        		System.out.format("\nClose Cross Down: Time %s Close:%4.2f ss:%4.2f ls:%4.2f",closePrice.getTimeSeries().getLastTick().toGoodString(),cp.toDouble(),ss.toDouble(),ls.toDouble());
        	}
        } else {
        	if (ss.toDouble() > ls.toDouble()) {
        		System.out.format("\nClose Cross Up  : Time %s Close:%4.2f ss:%4.2f ls:%4.2f",closePrice.getTimeSeries().getLastTick().toGoodString(),cp.toDouble(),ss.toDouble(),ls.toDouble());
        		maLevel = true;
        	}
        }
        */
        //System.out.format("\n%s macdFast:%4.3f macdSlow:%4.3f",closePrice.getTimeSeries().getLastTick().toGoodString(),macdFast.toDouble(),macdSlow.toDouble());
        if (macdLevel) {
        	if (macdFast.toDouble() < (macdSlow.toDouble() - 0.01)) {
        		macdLevel = false;
        		System.out.format("\nMACD Fast Cross Down fast: %2.3f slow:%2.3f: %s ss:%4.2f ls:%4.2f",macdFast.toDouble(),macdSlow.toDouble(),closePrice.getTimeSeries().getLastTick().toGoodString(),ss.toDouble(),ls.toDouble());
        	}
        } else {
        	if (macdFast.toDouble() > (macdSlow.toDouble() + 0.01)) {
        		System.out.format("\nMACD Fast Cross Up   fast: %2.3f slow:%2.3f: %s ss:%4.2f ls:%4.2f",macdFast.toDouble(),macdSlow.toDouble(),closePrice.getTimeSeries().getLastTick().toGoodString(),ss.toDouble(),ls.toDouble());
        		macdLevel = true;
        	}
        }
        
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
        Decimal cp = closePrice.getValue(index);
        Decimal ss = ssma.getValue(index);
        Decimal ls = lsma.getValue(index);
        Decimal ma = macd.getValue(index);
        Decimal vo = volume.getValue(index);
      
        if (inTrade) {
        	if (longTrade) {
        		//if price drops below the short ma + some variant then exit, or some time limit, or cash value exceeds $2k
        		
            	//Check price is less than the short ma + a fudge factor of 0.5
            	if (cp.isLessThan(ss.minus(Decimal.valueOf(MA_GAP_EXIT_FUDGE)))) {
            		exit = true;
            		inTrade=false;
            	}      		
        	}
        	else { //it's a short trade
        		//if price drops below the short ma + some variant then exit, or some time limit, or cash value exceeds $2k
            	if (cp.isGreaterThan(ss.plus(Decimal.valueOf(MA_GAP_EXIT_FUDGE)))) {
            		exit = true;
            		inTrade=false;
            	} 
        		inTrade=false;
        	}
        }
        traceShouldExit(index, exit);
        return exit;
    }
}