package apidemo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.joda.time.DateTime;
import org.joda.time.Period;

import apidemo.AutoPanel.BarResultsPanel.BarModel;
import apidemo.util.NewTabbedPanel.NewTabPanel;

import com.ib.controller.Bar;
import com.ib.controller.NewContract;
import com.ib.controller.ApiController.IHistoricalDataHandler;
import com.ib.controller.ApiController.IRealTimeBarHandler;
import com.ib.controller.Types;
import com.ib.controller.Types.SecType;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.indicators.helpers.AverageTrueRangeIndicator;
import eu.verdelhan.ta4j.indicators.helpers.StandardDeviationIndicator;
import eu.verdelhan.ta4j.indicators.oscillators.PPOIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.PriceVariationIndicator;
import eu.verdelhan.ta4j.indicators.simple.TypicalPriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.VolumeIndicator;
import eu.verdelhan.ta4j.indicators.trackers.EMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.MACDIndicator;
import eu.verdelhan.ta4j.indicators.trackers.ROCIndicator;
import eu.verdelhan.ta4j.indicators.trackers.RSIIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.WilliamsRIndicator;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

public class MoneyCommandCenter  implements IHistoricalDataHandler, IRealTimeBarHandler {

	// Private constructor. Prevents instantiation from other classes.
	private MoneyCommandCenter()  { }
	
	//Class variables
	private final NewContract m_contract = new NewContract();
	private ArrayList<Bar> barAggregator = new ArrayList<Bar>(12);
	private TimeSeries mainTimeSeries = new TimeSeries("Main", new Period(5000));
	
	//Indicator / Analyzers
	private ClosePriceIndicator closePrice;
	private TypicalPriceIndicator typicalPrice;
	private PriceVariationIndicator priceVariation;
	private SMAIndicator shortSma;
	private SMAIndicator longSma;
	private EMAIndicator shortEma;
    private EMAIndicator longEma;
    private PPOIndicator ppo;
    private ROCIndicator roc;
    private RSIIndicator rsi;
    private WilliamsRIndicator williamsR;
    private AverageTrueRangeIndicator atr;
    private StandardDeviationIndicator sd;
    
    private TradingRecord tradingRecord;
    // Building the trading strategy
    private Strategy strategy;
	
    /** Close price of the last tick */
    private static Decimal LAST_TICK_CLOSE_PRICE;

	/**
	 * Initializes singleton.
	 *
	 * SingletonHolder is loaded on the first execution of Singleton.getInstance()
	 * or the first access to SingletonHolder.INSTANCE, not before.
	 */
	private static class SingletonHolder {
		private static final MoneyCommandCenter INSTANCE = new MoneyCommandCenter();
	}

	public static MoneyCommandCenter shared() {
		return SingletonHolder.INSTANCE;
	}
	
	public void start() {
		
		
		//TBD:Request historical data from FeedManager
		//TBD:Enable feed to DB 
		//Enable Realtime quotes
		//Load current Execution information from File (m_contract)
		
		/*
		//TBD let's create the list first
		Map<String, String> dictionary = new HashMap<String, String>();
		dictionary.put("PrimarySymbol","ESU15.CME");
		MoneyUtils.shared().writeMainProperties(dictionary);
		*/
		
		Map<String, String> readDict = MoneyUtils.shared().readMainProperties();
		String symbol = readDict.get("PrimarySymbol");
		System.out.println("READ PrimarySymbol KEY:"+symbol);
		
		//Create Contract
		//TBD: Request Historical Data
		//Load Live Strategies
		//Load Feeds
		
		m_contract.symbol(symbol);
		m_contract.secType(SecType.FUT);
		m_contract.exchange("GLOBEX");
		m_contract.currency("USD");
		m_contract.expiry("201509");
		
		/*
		m_contract.symbol("IBM");
		m_contract.secType(SecType.STK);
		m_contract.exchange("SMART");
		m_contract.currency("USD");
		//m_contract.expiry("201509");
		 */
		
		ApiDemo.INSTANCE.controller().reqRealTimeBars(m_contract,Types.WhatToShow.TRADES, true, this);
		
		//TBD: At some point do this:ApiDemo.INSTANCE.controller().cancelRealtimeBars( this);
		
		//Cycle through the open Live Strategy to determine if possible exit
        closePrice = new ClosePriceIndicator(mainTimeSeries);
        // Typical price
        typicalPrice = new TypicalPriceIndicator(mainTimeSeries);
        // Price variation
        priceVariation = new PriceVariationIndicator(mainTimeSeries);
        // Simple moving averages
        shortSma = new SMAIndicator(closePrice, 13);
        longSma = new SMAIndicator(closePrice, 34);
        // Exponential moving averages
        shortEma = new EMAIndicator(closePrice, 8);
        longEma = new EMAIndicator(closePrice, 20);
        // Percentage price oscillator
        ppo = new PPOIndicator(closePrice, 12, 26);
        // Rate of change
        roc = new ROCIndicator(closePrice, 100);
        // Relative strength index
        rsi = new RSIIndicator(closePrice, 14);
        // Williams %R
        williamsR = new WilliamsRIndicator(mainTimeSeries, 20);  //get this from the strategy files
        // Average true range
        atr = new AverageTrueRangeIndicator(mainTimeSeries, 20);  //get this from the strategy files
        // Standard deviation
        sd = new StandardDeviationIndicator(closePrice, 14);
        
        // Initializing the trading history
        tradingRecord = new TradingRecord();

        // Building the trading strategy
        strategy = buildStrategy(mainTimeSeries);
        
		//TBD: Set the max number of units in this series, maybe adjust it real-time in the future
        mainTimeSeries.setMaximumTickCount(1000);
	}

    /**
     * @param series a time series
     * @return a dummy strategy
     */
    private static Strategy buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator ssma = new SMAIndicator(closePrice, 13);
        SMAIndicator lsma = new SMAIndicator(closePrice, 34);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        VolumeIndicator volume = new VolumeIndicator(series);
        
        return new ESFuturesGapStrategy(closePrice, ssma, lsma,macd, volume);

        /*KK Remved
        // Signals
        // Buy when SMA goes over close price
        // Sell when close price goes over SMA
        Strategy buySellSignals = new Strategy(
                new OverIndicatorRule(closePrice, sma),
                new UnderIndicatorRule(closePrice,sma)
        );
        return buySellSignals;
        */
    }

	@Override
	public void realtimeBar(Bar bar) {
		
		Bar finalBar;
		Tick tick;
		
		//System.out.format("\n5 Second Bar:%s", bar.toString());
		
		int aggMaxCount = 2;  // kktbd fix this to read from the config file
		
		barAggregator.add(bar);
		if (barAggregator.size() >= aggMaxCount) {  
			
			long endTime=bar.time() + 5;  //end time will be this bars start time + 5 seconds.  End time is in seconds.
			//long m_time;   
			double high=0;
			double low=10000;
			double open=barAggregator.get(0).open();
			double close=barAggregator.get(aggMaxCount-1).close();
			double wap=0;
			long   volume=0;
			int    count=0;
			
			for (Iterator<Bar> iter = barAggregator.listIterator(); iter.hasNext(); ) {
			    Bar b = iter.next();
			    //endTime = b.time + 5000;
			    if (b.high() > high) 
			    	high = b.high();
			    if (b.low() < low) 
			    	low = b.low();
			    wap += b.wap();
			    volume += b.volume();
			    count += b.count();
			    
			    iter.remove();  //empty this list of objects
			}
			wap = wap / aggMaxCount;
			
			finalBar= new Bar(endTime,high,low, open , close, wap, volume,count);
			System.out.format("\n%d Second Bar:%s\n",aggMaxCount*5, finalBar.toString());
					
			Period p=new Period(5*aggMaxCount);
			DateTime d=new DateTime(endTime*1000);
			tick = new Tick(p,d, open, high, low, close, volume);  //In the future add the WAP and count to the Tick class
			//System.out.println(tick.toGoodString());
			
			//System.out.format("TickAgain:o:%s h:%s l:%s c:%s v:%s", tick.getOpenPrice(), tick.getMaxPrice(), tick.getMinPrice(), tick.getClosePrice(), tick.getVolume());
		}
		else
			return;
		
		//Add this to the stored Series
		mainTimeSeries.addTick(tick);
		//mainTimeSeries.run(strategy);  //kktbd, fix this
		
        LAST_TICK_CLOSE_PRICE = mainTimeSeries.getTick(mainTimeSeries.getEnd()).getClosePrice();
        

       
		//Update each analyzer with this update
        int endIndex = mainTimeSeries.getEnd();
                
        if (strategy.shouldEnter(endIndex)) {
            // Our strategy should enter
            System.out.println("Strategy should ENTER on " + endIndex);
            
            if (tradingRecord.getCurrentTrade().isNew()) {
                // Entering...
                tradingRecord.operate(endIndex, tick.getClosePrice(), Decimal.TEN);  //buy/sell 10 contracts at the last price.
                Order entry = tradingRecord.getLastEntry();
                System.out.println("Entered on " + entry.getIndex()
                        + " (price=" + entry.getPrice().toDouble()
                        + ", amount=" + entry.getAmount().toDouble() + ")");
            }
            else {
            	System.out.println("Can't ENTER, already in trade");
            }
            
        } else if (strategy.shouldExit(endIndex)) {
            // Our strategy should exit
            System.out.println("Strategy should EXIT on " + endIndex);
            
            if (tradingRecord.getCurrentTrade().isOpened()) {
                // Exiting...
                tradingRecord.operate(endIndex, tick.getClosePrice(), Decimal.TEN); //offset 10 contracts at the last price
                Order exit = tradingRecord.getLastExit();
                System.out.println("Exited on " + exit.getIndex()
                        + " (price=" + exit.getPrice().toDouble()
                        + ", amount=" + exit.getAmount().toDouble() + ")");
            }
            else {
            	System.out.println("Can't EXIT, already in trade");
            }
            
        }
        
		
		//Update Chart in the background
	}

	@Override
	public void historicalData(Bar bar, boolean hasGaps) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void historicalDataEnd() {
		// TODO Auto-generated method stub
		
	}
}
