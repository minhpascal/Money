package apidemo;

import java.util.ArrayList;
import java.util.Iterator;

import org.joda.time.DateTime;
import org.joda.time.Period;

import com.ib.controller.Bar;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;

public class MoneyFeed {
	
	enum GENERAL_MODE {
		TRENDING,
		RANGE_BOUND,
		VOLATILE_BREAKOUTS
	}
	
	enum TREND {
		TREND_UNKNOWN,
		TREND_STARTING_UP,
		TREND_UP,
		TREND_UP_WANING,
		TREND_STARTING_DOWN,
		TREND_DOWN,
		TREND_DOWN_WANING
	};
	
	enum PERIOD {
		MONTH,
		WEEK,
		DAY,
		HOUR,
		MINUTE,
		SECOND
	};
	
	enum SLOW_FAST {
		SLOW,
		FAST
	};
	
	public class MovingAverage {
		
		public TimeSeries series;
		public ClosePriceIndicator closePrice;
		public SMAIndicator fastSMA8;
		public SMAIndicator slowSMA34;
		
		private int periodSeconds=0;
		public ArrayList<Bar> barAggregator = new ArrayList<Bar>(100);
		public int globalCount = 0;
		
		public MovingAverage(int periodSeconds) {
			
			series = new TimeSeries(String.valueOf(periodSeconds), new Period(periodSeconds*1000));
			//series.setMaximumTickCount(1000);
			
	        closePrice = new ClosePriceIndicator(series);
	        fastSMA8 = new SMAIndicator(closePrice, 8);
	        slowSMA34 = new SMAIndicator(closePrice, 34);
	        this.periodSeconds = periodSeconds;
		}
		
		private void syncToNow() {
			//TBD
		}
		
		public int getPeriodSeconds() {
			return periodSeconds;
		}
		
		public int getPeriodMinutes() {
			return periodSeconds/60;
		}
		
		public int getEndIndex() {
			return series.getEnd();
		}
		
		public Decimal getMA(int periodsBack, SLOW_FAST sf) {
			if (sf == SLOW_FAST.SLOW) {
				return slowSMA34.getValue(periodsBack);
			}
			else
				return fastSMA8.getValue(periodsBack);
		}
		
		public boolean realtimeBarSeconds(Bar bar, int frequency) {  //freq. is either 5 or 60, for the # of seconds between bars (or period)
			int aggMaxCount = periodSeconds/frequency;  
			
			//System.out.format("RAW REALTIME BAR %d Second Bar:%s BarTime:%d \n",aggMaxCount*frequency, bar.toString(), bar.time());
			
			barAggregator.add(bar);
			if (barAggregator.size() >= aggMaxCount) {  
				
				long endTime=bar.time() + frequency;  //end time will be this bars start time + 5 seconds.  End time is in seconds.
				//long m_time;   
				double high=0;
				double low=9999999;
				double open=barAggregator.get(0).open();  //first bar in list
				double close=bar.close(); //last bar in the list, which is this bar
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
				    
				    //iter.remove();  //empty this list of objects
				}
				wap = wap / aggMaxCount;
				
				Bar finalBar= new Bar(endTime, high,low, open , close, wap, volume,count); //create the new bar
				if (MoneyCommandCenter.shared().debugFlag != 0)
					System.out.format("\n%d Second Bar:%s\n",aggMaxCount*frequency, finalBar.toString());
						
				//Convert Bar to Tick
				Period p=new Period(frequency*aggMaxCount*1000);
				DateTime d=new DateTime(endTime*1000);
				
				//if (globalCount < 200)
				//	System.out.format("DateTime:"+d+" Period:"+frequency*aggMaxCount + "\n");
				
				Tick tick = new Tick(p,d, open, high, low, close, volume);  //In the future add the WAP and count to the Tick class
				series.addTick(tick);
				if (MoneyCommandCenter.shared().debugFlag != 0)
					System.out.format("TickAgain:o:%s h:%s l:%s c:%s v:%s", tick.getOpenPrice(), 
						tick.getMaxPrice(), tick.getMinPrice(), tick.getClosePrice(), tick.getVolume());
				//Clear out the aggregator, to be clean for next time.
				barAggregator.clear();		
				return true;
			}
			else
				return false;
			
			//System.out.println(tick.toGoodString());
		}
	}
	
	//
	//Main MoneyFeed Variables
	//
	public MovingAverage series1min = null;
	public MovingAverage series5min = null;
	public MovingAverage series10min = null;
	public MovingAverage series15min = null;
	
	public MoneyFeed() {
		
	}
	
	public TREND getTrend(PERIOD period)
	{
		TREND trend=TREND.TREND_STARTING_UP;
		
		//TBD add the code here to scan the indicators of the other levels
		
		return trend;
	}
	
	public MovingAverage getMAObject(int periodMinutes) {
		if (periodMinutes == 1) {
			if (series1min == null) {
				series1min = new MovingAverage(1*60); //1 minute
			}
			return series1min;
		}
		else if (periodMinutes == 5) {
			if (series5min == null) {
				series5min = new MovingAverage(5*60); //5 minutes
			}
			return series5min;
		}
		else if (periodMinutes == 10) {
			if (series10min == null) {
				series10min = new MovingAverage(10*60); //10 minutes
			}
			return series10min;
		}
		else if (periodMinutes == 15) {
			if (series15min == null) {
				series15min = new MovingAverage(15*60); //15 minutes
			}
			return series15min;
		}
		else 
			System.out.println("\nNot getting MA period of "+periodMinutes + " minutes");
		
		return null;
	}
	
	public MoneyTickReady realtimeBar5Seconds(Bar bar)
	{
		MoneyTickReady tickReady = new MoneyTickReady();
		boolean ready = false;
		
		if (series1min != null) {
			if (series1min.realtimeBarSeconds(bar,5)) {
				tickReady.min5Ready();
				ready=true;
			}
		}		
		if (series5min != null) {
			if (series5min.realtimeBarSeconds(bar,5)) {
				tickReady.min5Ready();
				ready=true;
			}
		}
		if (series10min != null) {
			if (series10min.realtimeBarSeconds(bar,5)) {
				tickReady.min5Ready();
				ready=true;
			}
		}
		if (series15min != null) {
			if (series15min.realtimeBarSeconds(bar,5)) {
				tickReady.min5Ready();
				ready=true;
			}
		}
		if (ready) {
			tickReady.setSomethingsReady();
		}
		return tickReady;
	}
	
	//
	//Called only from the backtester
	//
	public MoneyTickReady realtimeBar60Seconds(Bar bar)
	{
		MoneyTickReady tickReady = new MoneyTickReady();
		boolean ready = false;
		
		if (series1min != null) {
			if (series1min.realtimeBarSeconds(bar,60)) {
				tickReady.setMin1Ready();
				ready=true;
			}
		}		
		if (series5min != null) {
			if (series5min.realtimeBarSeconds(bar,60)) {
				tickReady.setMin5Ready();
				ready=true;
			}
		}
		if (series10min != null) {
			if (series10min.realtimeBarSeconds(bar,60)) {
				tickReady.setMin10Ready();
				ready=true;
			}
		}
		if (series15min != null) {
			if (series15min.realtimeBarSeconds(bar,60)) {
				tickReady.setMin15Ready();
				ready=true;
			}
		}
		if (ready) {
			tickReady.setSomethingsReady();
		}
		return tickReady;
	}
}
