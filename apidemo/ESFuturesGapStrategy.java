package apidemo;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.VolumeIndicator;
import eu.verdelhan.ta4j.indicators.trackers.MACDIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;


public class ESFuturesGapStrategy extends Strategy {
	private static final double MACD_GAP_SEPARATION_FUDGE=0.8; //When price rises above short ma for entry
	private static final double MA_GAP_EXIT_FUDGE=0.5;   //When price crosses short ma for exit
	
	private ClosePriceIndicator closePrice;
    private SMAIndicator ssma;
    private SMAIndicator lsma;
    private MACDIndicator macd;
    private VolumeIndicator volume;
    
    public  boolean inTrade=false;
    
    public ESFuturesGapStrategy(ClosePriceIndicator c, SMAIndicator s, SMAIndicator l, MACDIndicator m, VolumeIndicator v) {
		super();
		
		closePrice = c;
	    ssma = s;
	    lsma = l;
	    macd = m;
	    volume = v;
	    inTrade = false;
	}
    
    //
    // This is called every period of the tick, so defaults to 30 seconds
    //
    @Override
	public boolean shouldEnter(int index, TradingRecord tradingRecord) {
        boolean enter=false; //= entryRule.isSatisfied(index, tradingRecord);
        
        Decimal cp = closePrice.getValue(index);
        Decimal ss = ssma.getValue(index);
        Decimal ls = lsma.getValue(index);
        Decimal ma = macd.getValue(index);
        Decimal mas = macd.shortTermValue(index);
        Decimal mal = macd.longTermValue(index);
        Decimal vo = volume.getValue(index);
        
        if (inTrade) {
        	return false;
        }
      
        System.out.format("\nShould Enter??: Close:%4.2f ss:%4.2f ls:%4.2f",cp.toDouble(),ss.toDouble(),ls.toDouble());
        
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
        Decimal mas = macd.shortTermValue(index);
        Decimal mal = macd.longTermValue(index);
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