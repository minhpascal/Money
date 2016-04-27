package apidemo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

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
import apidemo.MoneyFeed.MovingAverage;
import apidemo.MoneyFeed.SLOW_FAST;
import apidemo.util.HtmlButton;
import apidemo.util.VerticalPanel;
import apidemo.util.NewTabbedPanel.NewTabPanel;
import apidemo.util.VerticalPanel.StackPanel;

import com.ib.client.Builder;
import com.ib.client.Contract;
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
		
		/*
		//TBD let's create the list first
		Map<String, String> dictionary = new HashMap<String, String>();
		dictionary.put("PrimarySymbol","ESU15.CME");
		MoneyUtils.shared().writeMainProperties(dictionary);
		*/
		Map<String, String> readDict = MoneyUtils.readMainProperties();
		symbol = readDict.get("PrimarySymbol");
		System.out.println("READ PrimarySymbol KEY:"+symbol);
		
		m_contract = new NewContract();
		m_contract.symbol(symbol);
		m_contract.secType(SecType.FUT);
		m_contract.exchange("GLOBEX");
		m_contract.currency("USD");
		
		//Get the front month 
		//TBD this needs to be its own function since we will call this every new day as the front month will change
		final DateTime nowDT = new DateTime();
		int yearValue = nowDT.getYear();
		int monthValue = nowDT.getMonthOfYear();
		String monthStr = new String();
		if (monthValue == 11 || monthValue == 0 || monthValue == 1) {
			monthStr= "03";
		} else if (monthValue >=2 && monthValue <= 4) {
			monthStr= "06";
		} else if (monthValue >=5 && monthValue <= 7) {
			monthStr= "09";
		} else if (monthValue >=8 && monthValue <= 10) {
			monthStr= "12";
		} else {
			//Something is really wrong 			
		}
		final String contractDateStr = new String(Integer.toString(yearValue) + monthStr);
		//m_contract.expiry("201606");
		
		System.out.println("Date String is "+contractDateStr);
		m_contract.expiry(contractDateStr);
		
		initializeStrategies();
		
		//
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			  @Override
			  public void run() {
				//Start the receiving data right away so that we can continue processing as usual
				//KKTBD, maybe do a historical view here also
				enableRealtimeFeed();
			  }
		}, 1000);
	}
	
	private NewContract m_contract = new NewContract();
	private String symbol=new String("ES");
	
	//
	//Class variables
	//
	public ChartTickUpdateCallbackHandler _chartHandler=null;
	public int debugFlag=0;
	private boolean autoTrading=false;
	private boolean liveTrading=false;
	private boolean realtimeFeed=false;
	//private boolean liveAnalysis=false;
	private MoneyFeed moneyFeed = new MoneyFeed();
	
	private ArrayList<Strategy> strategies = new ArrayList<Strategy>(5); //queried from UX
	private TradingRecord tradingRecord = new TradingRecord();
	
	/*
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
    */
	
    //private static Decimal LAST_TICK_CLOSE_PRICE;
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
	
	public static MoneyCommandCenter shared() {
		return SingletonHolder.INSTANCE;
	}
	
	public MoneyFeed getMoneyFeed() {
		return moneyFeed;
	}
	
	public TradingRecord getTradingRecord() {
		return tradingRecord;
	}
	
	public interface ChartTickUpdateCallbackHandler {
		public void updateTick(Tick tick); // time is in seconds since epoch
	}
	public void setPanelDelegate(ChartTickUpdateCallbackHandler delegate)
	{
		_chartHandler = delegate;
	}
	
	public boolean autoTrading() {
		return autoTrading;
	}
	
	public void startAutoTrading() {
		autoTrading = true;
		
		requestHistoricalData();
		
		enableRealtimeFeed();
	}
	
	public void stopAutoTrading() {
		autoTrading = false;
		
		disableRealtimeFeed();
	}
	
	//Tell IB to enable realtime quotes
	public void enableRealtimeFeed() {
		if (!realtimeFeed) {
			ApiDemo.INSTANCE.controller().reqRealTimeBars(m_contract,Types.WhatToShow.TRADES, false, this);
			realtimeFeed = true;
		}
	}
	
	//Tell IB to disable realtime quotes
	public void disableRealtimeFeed() {
		if (realtimeFeed) {
			ApiDemo.INSTANCE.controller().cancelRealtimeBars(this);
			realtimeFeed = false;
		}
	}
	
	public void enableLiveTrading() {	
		liveTrading = true;
	}
	
	public void disableLiveTrading() {
		liveTrading = false;
	}
	
	public ArrayList<Strategy> getPossibleBacktestStrategies() {
		
		return strategies;
		//return  new String[] {"1-Red, White, Blue","2-Slow Move Pullback","3-Nightly Slow Move Pullback"};
	}

	public void initializeStrategies() {
		
		// Strategies - maybe down the road, read these from a file
		strategies.add(new ESGenericMACDCrossovers(moneyFeed));
		//strategies.add(new ESFuturesGapStrategy(moneyFeed));
		//strategies.add(new ESSlow5Min(moneyFeed));
	}

	@Override
	public void realtimeBar(Bar bar) {

		MoneyTickReady tickReady = moneyFeed.realtimeBar5Seconds(bar);
		
		//
		// For each strategy, check if we should enter the trade or not
		// TBD modify this for when we are in a trade and things go wonky, we must be able to exit.
		//
		if (tickReady.somethingsReady()) {
			for (Strategy s : strategies) {
				MovingAverage ma = s.maObject;
				boolean check=false;

				if (tickReady.min1Ready() && ma.getPeriodSeconds() == 1) {
					check = true;
				}
				else if (tickReady.min5Ready() && ma.getPeriodSeconds() == 5) {
					check = true;
				}
				else if (tickReady.min10Ready() && ma.getPeriodSeconds() == 10) {
					check = true;
				}
				else if (tickReady.min15Ready() && ma.getPeriodSeconds() == 15) {
					check = true;
				}
				if (check) {
					checkShouldEnterOrExit(s);
				}
			}
		}
 
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
	public void manualEnterLong() {
		
		//Default to the first strategy
		int endIndex = strategies.get(0).maObject.getEndIndex();
		if (liveTrading == false || endIndex < 1) {
			System.out.println("liveTrading is not enabled or no Ticks have been received");
			return;
		}
		
		Tick tick = strategies.get(0).maObject.series.getTick(endIndex);
		
        if (tradingRecord.getCurrentTrade().isNew()) {
            // Entering...
            tradingRecord.operate(endIndex, tick.getClosePrice(), Decimal.FIVE, true);  //buy/sell 5 contracts at the last price.
            Order entry = tradingRecord.getLastEntry();
            System.out.println("Entered LONG on " + entry.getIndex()
                    + " (price=" + entry.getPrice().toDouble()
                    + ", amount=" + entry.getAmount().toDouble() + ")");
        }
        else {
        	System.out.println("Can't ENTER, already in trade");
        }
	}
	
	public void manualEnterShort() {
		
		//Default to the first strategy
		int endIndex = strategies.get(0).maObject.getEndIndex();
		if (liveTrading == false || endIndex < 1) {
			System.out.println("liveTrading is not enabled or no Ticks have been received");
			return;
		}
		
		Tick tick = strategies.get(0).maObject.series.getTick(endIndex);
		
        if (tradingRecord.getCurrentTrade().isNew()) {
            // Entering...
            tradingRecord.operate(endIndex, tick.getClosePrice(), Decimal.ONE, false);  //buy/sell 5 contracts at the last price.
            Order entry = tradingRecord.getLastEntry();
            System.out.println("Entered LONG on " + entry.getIndex()
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
	public void manualExit() {
		
		int endIndex = strategies.get(0).maObject.getEndIndex();
		if (liveTrading == false || endIndex < 1 || !tradingRecord.getCurrentTrade().isOpened()) {
			System.out.println("No Ticks have been received or there's not active trade here");
			return;
		}
	
        System.out.println("Strategy should EXIT on " + 0);
        
		Tick tick = strategies.get(0).maObject.series.getTick(endIndex);
        
        if (tradingRecord.getCurrentTrade().isOpened()) {
            // Exiting...
        	Decimal quantity = tradingRecord.getCurrentTrade().getEntry().getAmount();
            tradingRecord.operate(endIndex, tick.getClosePrice(), quantity, false);
            Order exit = tradingRecord.getLastExit();
            System.out.println("Exited on " + exit.getIndex()
                    + " (price=" + exit.getPrice().toDouble()
                    + ", amount=" + exit.getAmount().toDouble() + ")");
        }
        else {
        	System.out.println("No known open trade, checking IB");
        	
        	//KK query open positions
        	
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
		ApiDemo.INSTANCE.controller().reqHistoricalData(m_contract, endDate, dtStart, DurationUnit.SECOND, BarSize._1_min, WhatToShow.TRADES, false, this); //WhatToShow.TRADES
		
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

	//
	//This is writing 1 minute bars to the file, not the 5 second ones
	//
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
			
			initializeStrategies();

			//if (liveTrading == true) {
				//Get the data set up correctly
				List<Bar> list = processFile(this.getFileNameFromHistNow());
				
				for (int i=0;i<list.size();i++) {
					Bar bar=list.get(i);
					//MoneyTickReady tickReady = moneyFeed.realtimeBar60Seconds(bar);
					moneyFeed.realtimeBar60Seconds(bar);

					for (Strategy s : strategies) {
						//MovingAverage ma = s.maObject;
						s.updateGetValues(i);
					}
				}
			//}
				/*
				 * TBD fix this
				 
				//Read the data into the mainTimeSeries from the whole day and request new realTimeBars
				List<Bar> list = processFile(this.getFileNameFromHistNow());
				mainTimeSeries = new TimeSeries("Main", new Period(3600));
				
				start();
				
				for (int i=0;i<list.size();i++) {
					Tick tick=list.get(i);
					mainTimeSeries.addTick(tick);
					strategy.updateGetValues(i);  //This must be done to get the strategy caught up
				}
				*/
			
		}
		catch (IOException e)
		{
			System.err.println("historicalDataEnd IOException: " + e.getMessage());
		}
	}
	
	static public ArrayList<Bar> processFile(String filename)  {
		//filename = "ES20150810.ticks30";
		Path filepath = Paths.get(filename);
		ArrayList<Bar> bars = new ArrayList<Bar>(1440);
		
		try {
			try (Scanner scanner =  new Scanner(filepath, StandardCharsets.UTF_8.name())){
				while (scanner.hasNextLine()){
					Bar bar = fromStringToBar(scanner.nextLine());	
					bars.add(bar);
				}
			}
		}
		catch (IOException e) {
			System.err.println("ProcessFile IOException: " + e.getMessage());
		}		
	    return bars;
	}
	
	//
	//Main Check Routine called for each tick and backtesting
	//
	private Decimal checkShouldEnterOrExit(Strategy s) {
			//Tick tick,int endIndex) {
	
		int endIndex = s.maObject.getEndIndex();
		Tick tick = s.maObject.closePrice.getTimeSeries().getLastTick();
		
		Decimal amount=Decimal.ZERO;
		
		if (s.shouldEnter(endIndex,tradingRecord)) {
			// Our strategy should enter
			//System.out.println("Strategy should ENTER on " + endIndex+ " Time:" + tick.toGoodString());

			if (tradingRecord.getCurrentTrade().isNew()) {
				// Entering...
				tradingRecord.operate(endIndex, tick.getClosePrice(), Decimal.TEN, s.isLongTrade());  //buy/sell 5 contracts at the last price.
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

		} else if (s.shouldExit(endIndex,tradingRecord)) {
			// Our strategy should exit
			//System.out.format("Strategy should EXIT on ",endIndex);

			if (tradingRecord.getCurrentTrade().isOpened()) {
				// Exiting...
				tradingRecord.operate(endIndex, tick.getClosePrice(), Decimal.TEN, s.isLongTrade()); //offset 5 contracts at the last price
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
		List<Bar> list = processFile(this.getFileNameFromHistNow());  //This gives me a list of 1 minute bars
		Decimal sum=Decimal.valueOf(0);
		
		/*
		TimeSeries testTimeSeries = new TimeSeries("Backtest", new Period(5000));	
		strategy = buildStrategy(testTimeSeries);
		*/
		
		for (int i=0;i<list.size();i++) {
			Bar bar=list.get(i);
			
			MoneyTickReady tickReady = moneyFeed.realtimeBar60Seconds(bar);
			
			//
			// For each strategy, check if we should enter the trade or not
			// TBD modifiy this for when we are in a trade and things go wonky, we must be able to exit.
			if (tickReady.somethingsReady()) {
				for (Strategy s : strategies) {
					MovingAverage ma = s.maObject;
					boolean check=false;

					if (tickReady.min1Ready() && ma.getPeriodMinutes() == 1) {
						check = true;
					}
					else if (tickReady.min5Ready() && ma.getPeriodMinutes() == 5) {
						check = true;
					}
					else if (tickReady.min10Ready() && ma.getPeriodMinutes() == 10) {
						check = true;
					}
					else if (tickReady.min15Ready() && ma.getPeriodMinutes() == 15) {
						check = true;
					}
					if (check) {
						sum = sum.plus(checkShouldEnterOrExit(s));
						break;
					}
				}
			}
			/*
			//Add this to the stored Series
			testTimeSeries.addTick(tick);		
			LAST_TICK_CLOSE_PRICE = testTimeSeries.getTick(testTimeSeries.getEnd()).getClosePrice();

			//Update each analyzer with this update
			int endIndex = testTimeSeries.getEnd();

			//Check if we should enter or Exit here
			sum = sum.plus(checkShouldEnterOrExit(tick,endIndex));
			*/
		}	
		System.out.println("*************************");
		System.out.format(" TOTAL PROFIT: $%4.2f \n",sum.toDouble());
		System.out.println("*************************");
		
		/*
		mainTimeSeries = testTimeSeries;
		currentStrategy = strategy;
		*/
		/*
        if (_chartHandler != null) {
        	_chartHandler.updateTick(tick);
        }
        */
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
			//String wap = scanner.next();
			//String count = scanner.next();
			
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
	
	static public Bar fromStringToBar(String s) {
		Bar bar;
		Scanner scanner = new Scanner(s);
		scanner.useDelimiter(",");
		if (scanner.hasNext()) {
			//assumes the line has a certain structure
			String time = scanner.next();
			String open = scanner.next();
			String high = scanner.next();
			String low = scanner.next();
			String close = scanner.next();
			String volume = scanner.next();
			String wap = scanner.next();
			String count = scanner.next();
			
			DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
			DateTime d = dtf.parseDateTime(time);
			
			bar = new Bar(d.getMillis() / 1000, Double.parseDouble(high),Double.parseDouble(low),
							Double.parseDouble(open),Double.parseDouble(close),
							Double.parseDouble(wap),Long.parseLong(volume),Integer.parseInt(count));
		}
		else {
			System.out.println("Empty or invalid line. Unable to process.");
			bar=null;
		}
		scanner.close();
		return bar;
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
		
		//KK for some reason we have to cancel the account summary
		ApiDemo.INSTANCE.controller().cancelAccountSummary(this);
	}

	@Override 
	public void accountSummaryEnd() {
		System.out.format("\nIB:accountSummaryEnd");
	}
}
