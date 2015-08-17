package apidemo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import apidemo.AutoPanel.BarResultsPanel;
import apidemo.AutoPanel.BarResultsPanel.BarModel;
import apidemo.util.HtmlButton;
import apidemo.util.VerticalPanel;
import apidemo.util.NewTabbedPanel.NewTabPanel;
import apidemo.util.VerticalPanel.StackPanel;

import com.ib.client.Builder;
import com.ib.client.EClientErrors;
import com.ib.client.TagValue;
import com.ib.controller.AccountSummaryTag;
import com.ib.controller.Bar;
import com.ib.controller.DeltaNeutralContract;
import com.ib.controller.NewComboLeg;
import com.ib.controller.NewContract;
import com.ib.controller.NewOrder;
import com.ib.controller.NewOrderState;
import com.ib.controller.OrderStatus;
import com.ib.controller.OrderType;
import com.ib.controller.ApiController.IAccountSummaryHandler;
import com.ib.controller.ApiController.IHistoricalDataHandler;
import com.ib.controller.ApiController.ILiveOrderHandler;
import com.ib.controller.ApiController.IOrderHandler;
import com.ib.controller.ApiController.IRealTimeBarHandler;
import com.ib.controller.Types;
import com.ib.controller.Types.AlgoStrategy;
import com.ib.controller.Types.BarSize;
import com.ib.controller.Types.DurationUnit;
import com.ib.controller.Types.HedgeType;
import com.ib.controller.Types.Right;
import com.ib.controller.Types.SecIdType;
import com.ib.controller.Types.SecType;
import com.ib.controller.Types.WhatToShow;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.Trade;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.indicators.helpers.AverageTrueRangeIndicator;
import eu.verdelhan.ta4j.indicators.helpers.StandardDeviationIndicator;
import eu.verdelhan.ta4j.indicators.oscillators.PPOIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.PriceVariationIndicator;
import eu.verdelhan.ta4j.indicators.simple.TypicalPriceIndicator;
import eu.verdelhan.ta4j.indicators.simple.VolumeIndicator;
import eu.verdelhan.ta4j.indicators.trackers.EMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.MACDEMA;
import eu.verdelhan.ta4j.indicators.trackers.MACDIndicator;
import eu.verdelhan.ta4j.indicators.trackers.ROCIndicator;
import eu.verdelhan.ta4j.indicators.trackers.RSIIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.VolumeAverageIndicator;
import eu.verdelhan.ta4j.indicators.trackers.WilliamsRIndicator;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;


public class MoneyCommandCenter  implements IHistoricalDataHandler, IRealTimeBarHandler, ILiveOrderHandler, IAccountSummaryHandler {
	
	// Private constructor. Prevents instantiation from other classes.
	private MoneyCommandCenter()  { 
		
		Map<String, String> readDict = MoneyUtils.readMainProperties();
		symbol = readDict.get("PrimarySymbol");
		System.out.println("READ PrimarySymbol KEY:"+symbol);
		
		m_contract = new NewContract();
		m_contract.symbol(symbol);
		m_contract.secType(SecType.FUT);
		m_contract.exchange("GLOBEX");
		m_contract.currency("USD");
		m_contract.expiry("201509");
	}
	
	private NewContract m_contract = new NewContract();
	
	private String symbol=new String("ES");
	
	//Class variables
	public ChartTickUpdateCallbackHandler _chartHandler=null;
	public int debugFlag=0;
	private boolean liveTrading=false;
	private ArrayList<Bar> barAggregator = new ArrayList<Bar>(12);
	private TimeSeries mainTimeSeries = new TimeSeries("Main", new Period(5000));
	
	//Indicator / Analyzers
	public ClosePriceIndicator closePrice;
	public TypicalPriceIndicator typicalPrice;
	public PriceVariationIndicator priceVariation;
	public SMAIndicator shortSma;
	public SMAIndicator longSma;
	public EMAIndicator shortEma;
	public EMAIndicator longEma;
	public PPOIndicator ppo;
	public ROCIndicator roc;
	public RSIIndicator rsi;
	public WilliamsRIndicator williamsR;
	public AverageTrueRangeIndicator atr;
	public StandardDeviationIndicator sd;
    
    private TradingRecord tradingRecord = new TradingRecord();
    
    // Building the trading strategy
    private Strategy strategy;
    private Strategy currentStrategy;
	
    /** Close price of the last tick */
    private static Decimal LAST_TICK_CLOSE_PRICE;
    
    private BufferedWriter dayBarWriter;
    
	private DateTime histNow;
	private String histDay;
	private String histTime;
    
	/**
	 * Initializes singleton.
	 *
	 * SingletonHolder is loaded on the first execution of Singleton.getInstance()
	 * or the first access to SingletonHolder.INSTANCE, not before.
	 */
	private static class SingletonHolder {
		private static final MoneyCommandCenter INSTANCE = new MoneyCommandCenter();
	}
	
	public TimeSeries getMainTimeSeries() {
		return mainTimeSeries;
	}
	
	public interface ChartTickUpdateCallbackHandler {
		public void updateTick(Tick tick); // time is in seconds since epoch
	}

	public static MoneyCommandCenter shared() {
		return SingletonHolder.INSTANCE;
	}
	
	public void setPanelDelegate(ChartTickUpdateCallbackHandler delegate)
	{
		_chartHandler = delegate;
	}
	
	public void startLiveTrading() {
		liveTrading = true;
		
		requestHistoricalData();
		
	}
	
	public void stopLiveTrading() {
		liveTrading = false;
		
		ApiDemo.INSTANCE.controller().cancelRealtimeBars( this);
	}
	
	public void start() {
		//Called after we have loaded the historical data
		//Load current Execution information from File (m_contract)
		//Load Live Strategies
		//Load Feeds		
		//Cycle through the open Live Strategy to determine if possible exit
		
		/*
		//TBD let's create the list first
		Map<String, String> dictionary = new HashMap<String, String>();
		dictionary.put("PrimarySymbol","ESU15.CME");
		MoneyUtils.shared().writeMainProperties(dictionary);
		*/
		
        

        // Building the trading strategy, TBD cycle through all of the strategies to build them based off of the config file.
        strategy = buildStrategy(mainTimeSeries);
        
		// TODO: Set the max number of units in this series, maybe adjust it real-time in the future
        mainTimeSeries.setMaximumTickCount(1000);
        
        // Request the realtimebars
        ApiDemo.INSTANCE.controller().reqRealTimeBars(m_contract,Types.WhatToShow.TRADES, false, this);
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
        SMAIndicator ssma = new SMAIndicator(closePrice, 8);
        SMAIndicator lsma = new SMAIndicator(closePrice, 34);
        MACDEMA macd = new MACDEMA(closePrice, 12, 26, 9);
        VolumeIndicator volume = new VolumeIndicator(series);
        VolumeAverageIndicator volumeAverage = new VolumeAverageIndicator(volume,8);
        
        return new ESFuturesGapStrategy(closePrice, ssma, lsma,macd, volumeAverage);
    }

	@Override
	public void realtimeBar(Bar bar) {
		
		Bar finalBar;
		Tick tick;
		
		//System.out.format("\n5 Second Bar:%s", bar.toString());
		int barSize = 5; //1 minute candlesticks, should be read from config file.
		int aggMaxCount = barSize/5;  
		
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
			
			if (debugFlag != 0) 
				System.out.format("\n%d Second Bar:%s\n",aggMaxCount*5, finalBar.toString());
					
			Period p=new Period(5*aggMaxCount);
			DateTime d=new DateTime(endTime*1000);
			tick = new Tick(p,d, open, high, low, close, volume);  //In the future add the WAP and count to the Tick class
			//System.out.println(tick.toGoodString());			
			//System.out.format("TickAgain:o:%s h:%s l:%s c:%s v:%s", tick.getOpenPrice(), tick.getMaxPrice(), tick.getMinPrice(), tick.getClosePrice(), tick.getVolume());
		}
		else
			return;
		
		System.out.println(tick.toGoodString());
		
		//Add this to the stored Series
		mainTimeSeries.addTick(tick);		
        LAST_TICK_CLOSE_PRICE = mainTimeSeries.getTick(mainTimeSeries.getEnd()).getClosePrice();
        
		//Update each analyzer with this update
        int endIndex = mainTimeSeries.getEnd();
        checkShouldEnterOrExit(tick,endIndex);  //KKTBD add the total for the day, sum is returned
 
        /*
		//Update Chart in the background
        if (_chartHandler != null) {
        	_chartHandler.updateTick(tick);
        }
        */
	}
	
	//
	//KK Manually Entered
	//
	public void testEnter() {
		
		if (liveTrading == false || mainTimeSeries.getTick(mainTimeSeries.getEnd()).getClosePrice().toDouble() < 1) {
			System.out.println("liveTrading is not enabled or no Ticks have been received");
			return;
		}
		
		Tick tick = mainTimeSeries.getTick(mainTimeSeries.getEnd());
		
        if (tradingRecord.getCurrentTrade().isNew()) {
            // Entering...
            tradingRecord.operate(mainTimeSeries.getEnd(), tick.getClosePrice(), Decimal.FIVE, true);  //buy/sell 5 contracts at the last price.
            Order entry = tradingRecord.getLastEntry();
            System.out.println("Entered on " + entry.getIndex()
                    + " (price=" + entry.getPrice().toDouble()
                    + ", amount=" + entry.getAmount().toDouble() + ")");
        }
        else {
        	System.out.println("Can't ENTER, already in trade");
        }
	}
	
	//
	//KK Manually Exited
	//
	public void testExit() {
		
		if (mainTimeSeries.getTick(mainTimeSeries.getEnd()).getClosePrice().toDouble() < 1 || !tradingRecord.getCurrentTrade().isOpened()) {
			System.out.println("No Ticks have been received or there's not active trade here");
			//KKTBD maybe kick off an account status
			return;
		}
		
        System.out.println("Strategy should EXIT on " + 0);
        
        Tick tick = mainTimeSeries.getTick(mainTimeSeries.getEnd());
        
        if (tradingRecord.getCurrentTrade().isOpened()) {
            // Exiting...
            tradingRecord.operate(mainTimeSeries.getEnd(), tick.getClosePrice(), Decimal.FIVE, false); //offset 5 contracts at the last price
            Order exit = tradingRecord.getLastExit();
            System.out.println("Exited on " + exit.getIndex()
                    + " (price=" + exit.getPrice().toDouble()
                    + ", amount=" + exit.getAmount().toDouble() + ")");
        }
        else {
        	System.out.println("Can't EXIT, already in trade");
        }
	}
	public void reqAcctSummary() {
		//TotalCashValue
		AccountSummaryTag[] tags = {AccountSummaryTag.TotalCashValue};
		ApiDemo.INSTANCE.controller().reqAccountSummary("All", tags, this);
	}
	
	public void reqLiveOrders() {
		ApiDemo.INSTANCE.controller().reqLiveOrders(this);
	}
	
	public void histUpdateNow() {
		
		DateTimeFormatter dtfDay = DateTimeFormat.forPattern("yyyyMMdd");
		DateTimeFormatter dtfTime = DateTimeFormat.forPattern("HH:mm:ss");	
		histNow = new DateTime();
		histDay = dtfDay.print(histNow);
		histTime = dtfTime.print(histNow);
	}
	
	public String getFileNameFromHistNow() {
		return (new String(symbol + histDay + ".ticks30"));
	}
	
	public void requestHistoricalData() {
		
		NewContract histContract = new NewContract();
		histContract.symbol(symbol); 
		
		histUpdateNow();  //Update the current time to query 1 day back
		
		System.out.println("Day:" + getFileNameFromHistNow());
		
		String endDate = new String(histDay + " " + histTime);
		
		//DateTime dtStart = histNow.withTimeAtStartOfDay();
		int dtStart = histNow.getSecondOfDay();
		ApiDemo.INSTANCE.controller().reqHistoricalData(m_contract, endDate, dtStart, DurationUnit.SECOND, BarSize._1_min, WhatToShow.TRADES, false, this);
		
		//
		//Just do today
		//
		File fout = new File(getFileNameFromHistNow());
		try {
			
			FileOutputStream fos = new FileOutputStream(fout);	 
			dayBarWriter = new BufferedWriter(new OutputStreamWriter(fos));
		}
		catch (IOException e)
		{
			System.err.println("ReqHistData IOException: " + e.getMessage());
		}
	}

	@Override
	public void historicalData(Bar bar, boolean hasGaps) {

		System.out.format("\nHistorical Bar:%s", bar.toString());
		try {
			dayBarWriter.write(bar.toStringToFile());
			dayBarWriter.newLine();
		}
		catch (IOException e){
			System.err.println("historicalData IOException: " + e.getMessage());
		}
	}
	
	@Override
	public void historicalDataEnd()  {

		System.out.println("Historical Data End");
		try {
			dayBarWriter.close();
			
			if (liveTrading == true) {
				//Read the data into the mainTimeSeries from the whole day and request new realTimeBars
				List<Tick> list = processFile(this.getFileNameFromHistNow());
				mainTimeSeries = new TimeSeries("Main", new Period(3600));
				
				start();
				
				for (int i=0;i<list.size();i++) {
					Tick tick=list.get(i);
					mainTimeSeries.addTick(tick);
					strategy.updateGetValues(i);  //This must be done to get the strategy caught up
				}
			}
		}
		catch (IOException e)
		{
			System.err.println("historicalDataEnd IOException: " + e.getMessage());
		}
	}
	
	static public ArrayList<Tick> processFile(String filename)  {
		//filename = "ES20150810.ticks30";
		Path filepath = Paths.get(filename);
		ArrayList<Tick> ticks = new ArrayList<Tick>(1440);
		
		try {
			try (Scanner scanner =  new Scanner(filepath, StandardCharsets.UTF_8.name())){
				while (scanner.hasNextLine()){
					Tick tick = fromStringToTick(scanner.nextLine());	
					ticks.add(tick);
				}
			}
		}
		catch (IOException e) {
			System.err.println("ProcessFile IOException: " + e.getMessage());
		}		
	    return ticks;
	}
	
	//
	//Main Check Routine called for each tick and backtesting
	//
	private Decimal checkShouldEnterOrExit(Tick tick,int endIndex) {
		Decimal amount=Decimal.ZERO;
		
		if (strategy.shouldEnter(endIndex)) {
			// Our strategy should enter
			//System.out.println("Strategy should ENTER on " + endIndex+ " Time:" + tick.toGoodString());

			if (tradingRecord.getCurrentTrade().isNew()) {
				// Entering...
				tradingRecord.operate(endIndex, tick.getClosePrice(), Decimal.TEN, strategy.isLongTrade());  //buy/sell 5 contracts at the last price.
				Order entry = tradingRecord.getLastEntry();
				System.out.println("\nENTER "
						+ entry.getType().toString()
						+ " on " + entry.getIndex()
						+ " (price=" + entry.getPrice().toDouble()
						+ ", amount=" + entry.getAmount().toDouble() + ")"
						+ tick.toGoodString());
			}
			else {
				System.out.println("Can't ENTER, already in trade");
			}

		} else if (strategy.shouldExit(endIndex,tradingRecord)) {
			// Our strategy should exit
			//System.out.format("Strategy should EXIT on ",endIndex);

			if (tradingRecord.getCurrentTrade().isOpened()) {
				// Exiting...
				tradingRecord.operate(endIndex, tick.getClosePrice(), Decimal.TEN, strategy.isLongTrade()); //offset 5 contracts at the last price
				Order exit = tradingRecord.getLastExit();
				System.out.println("\nEXIT  " 					
						+ exit.getType().toString()
						+ " on " + exit.getIndex()
						+ " (price=" + exit.getPrice().toDouble()
						+ ", amount=" + exit.getAmount().toDouble() + ")"
						+ tick.toGoodString());

				Trade trade = tradingRecord.getLastTrade();
				Order orderEntry= trade.getEntry();
				Order orderExit = trade.getExit();
				Decimal priceDiff;
				if (orderEntry.getType() == Order.OrderType.BUY) {	 
					priceDiff = orderExit.getPrice().minus(orderEntry.getPrice());
				}
				else {
					priceDiff = orderEntry.getPrice().minus(orderExit.getPrice());
				}
				amount = priceDiff.multipliedBy(orderEntry.getAmount().multipliedBy(Decimal.valueOf(50)));  //KKTBD get the 50 from config
				System.out.format("***PROFIT:$%4.2f \n",amount.toDouble());
			}
			else {
				System.out.println("Can't EXIT, already in trade");
			}
			
		}
		return amount;
	}
	//
	//KKTBD Clean this up later
	//
	public void runBacktest(int backtestType) {
		histUpdateNow();
		List<Tick> list = processFile(this.getFileNameFromHistNow());
		TimeSeries testTimeSeries = new TimeSeries("Backtest", new Period(5000));
		Decimal sum=Decimal.valueOf(0);
		
		strategy = buildStrategy(testTimeSeries);
		
		for (int i=0;i<list.size();i++) {
			Tick tick=list.get(i);

			//Add this to the stored Series
			testTimeSeries.addTick(tick);		
			LAST_TICK_CLOSE_PRICE = testTimeSeries.getTick(testTimeSeries.getEnd()).getClosePrice();

			//Update each analyzer with this update
			int endIndex = testTimeSeries.getEnd();

			//Check if we should enter or Exit here
			sum = sum.plus(checkShouldEnterOrExit(tick,endIndex));
		}	
		System.out.println("*************************");
		System.out.format(" TOTAL PROFIT: $%4.2f \n",sum.toDouble());
		System.out.println("*************************");
		
		mainTimeSeries = testTimeSeries;
		currentStrategy = strategy;
		/*
        if (_chartHandler != null) {
        	_chartHandler.updateTick(tick);
        }
        */
	}
	public Strategy getCurrentStrategy() {
		return currentStrategy;
	}
	//
	// Used when reading back stored ticks in file
	//
	static public Tick fromStringToTick(String s) {
		Tick tick;
		Scanner scanner = new Scanner(s);
		scanner.useDelimiter(",");
		if (scanner.hasNext()){
			//assumes the line has a certain structure
			String time = scanner.next();
			String open = scanner.next();
			String high = scanner.next();
			String low = scanner.next();
			String close = scanner.next();
			String volume = scanner.next();
			String wap = scanner.next();
			String count = scanner.next();
			
			Period p=new Period(5*12);  // This is for 1 second
			DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
			DateTime d = dtf.parseDateTime(time);
			
			tick = new Tick(p.plusSeconds(5),d, Double.parseDouble(open),Double.parseDouble(high),Double.parseDouble(low),
					Double.parseDouble(close),Integer.parseInt(volume));
				
			//System.out.format("ReadTick:Time:%s o:%s h:%s l:%s c:%s v:%s\n", tick.toGoodString(),tick.getOpenPrice(), tick.getMaxPrice(), tick.getMinPrice(), tick.getClosePrice(), tick.getVolume());
			
		}
		else {
			System.out.println("Empty or invalid line. Unable to process.");
			tick=null;
		}
		scanner.close();
		return tick;
	}
	
	//
	// ILiveOrderHandler callback methods
	//
	@Override
	public void openOrder(NewContract contract, NewOrder order, NewOrderState orderState) {
		System.out.format("\nIB:openOrder:Contract: %s Order:%s State:%s",contract.toString(),order.toString(),orderState.toString());
	}
	@Override
	public void openOrderEnd() {
		System.out.format("\nIB:openOrderEnd");
	}
	@Override
	public void orderStatus(int orderId, OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
		System.out.format("\nIB:openStatus:%s",status.toString());
	}
	@Override
	public void handle(int orderId, int errorCode, String errorMsg) {  // add permId?
		System.out.format("\nIB:LiveOrderCallback:id:%d errorCode:%d errorMsg:%s",orderId,errorCode,errorMsg.toString());
	}
	
	//
	//IAccountSummaryHandler callback methods
	//
	@Override 
	public void accountSummary(String account, AccountSummaryTag tag, String value, String currency) {
		System.out.format("\nIB:accountSummary:account:%s value:$%s currency:%s",account.toString(),value.toString(),currency.toString());
	}

	@Override 
	public void accountSummaryEnd() {
		System.out.format("\nIB:accountSummaryEnd");
	}
}
