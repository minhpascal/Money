package apidemo;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.OHLCDataset;
import org.joda.time.Period;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import ta4jexamples.loaders.CsvTicksLoader;
import ta4jexamples.loaders.CsvTradesLoader;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.Trade;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.bollingerbands.BollingerBandsLowerIndicator;
import eu.verdelhan.ta4j.indicators.trackers.bollingerbands.BollingerBandsMiddleIndicator;
import eu.verdelhan.ta4j.indicators.trackers.bollingerbands.BollingerBandsUpperIndicator;

public class MoneyChart implements Runnable {

	private boolean _chartThreadRunning=false;
	public JPanel 	_chartPanel=null;
	private Strategy strategy;
	//private TimeSeries series; 
	
	MoneyChart() {
	
	}
	/*
	public void setStrategy(Strategy strategy) {
		this.strategy = strategy;
	}
	*/
	
	public void run() {
		
		//All UI should be run on this thread
		SwingUtilities.invokeLater(new Runnable() {
		      public void run() {
		    	  //_run();
		      }
		});
		_chartThreadRunning=true;
	}
	
	public JPanel getChartPanel() {
		return _chartPanel;
	}
	
	public void updateTick(Tick tick) {
		run();
	}
	
	//
	//Private Methods
	//
    /**
     * Runs a strategy over a time series and adds the value markers
     * corresponding to buy/sell signals to the plot.
     * @param series a time series
     * @param strategy a trading strategy
     * @param plot the plot
     */
    private static void addBuySellSignals(TimeSeries series, Strategy strategy, XYPlot plot) {
        // Running the strategy
        List<Trade> trades = series.run(strategy).getTrades();
        // Adding markers to plot
        for (Trade trade : trades) {
            // Buy signal
            double buySignalTickTime = new Minute(series.getTick(trade.getEntry().getIndex()).getEndTime().toDate()).getFirstMillisecond();
            Marker buyMarker = new ValueMarker(buySignalTickTime);
            buyMarker.setPaint(Color.GREEN);
            buyMarker.setLabel("B");
            plot.addDomainMarker(buyMarker);
            // Sell signal
            double sellSignalTickTime = new Minute(series.getTick(trade.getExit().getIndex()).getEndTime().toDate()).getFirstMillisecond();
            Marker sellMarker = new ValueMarker(sellSignalTickTime);
            buyMarker.setPaint(Color.RED);
            sellMarker.setLabel("S");
            plot.addDomainMarker(sellMarker);
        }
    }
    
	public void _run() {
		return;

		/*
		//series = CsvTradesLoader.loadBitstampSeries().subseries(0, Period.hours(6));
		TimeSeries series = MoneyCommandCenter.shared().getMoneyFeed().series1min.series;
        
        
        //* Creating the OHLC dataset
        
        OHLCDataset ohlcDataset = CandlestickChart.createOHLCDataset(series);
        
        
        // * Creating the additional dataset
       
        TimeSeriesCollection xyDataset = CandlestickChart.createAdditionalDataset(series);
        
       
        //Creating the chart
         
        JFreeChart chart = ChartFactory.createCandlestickChart(
                "ESU15",
                "Time",
                "USD",
                ohlcDataset,
                true);
        // Candlestick rendering
        CandlestickRenderer renderer = new CandlestickRenderer();
        renderer.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_SMALLEST);
        XYPlot plot = chart.getXYPlot();
        plot.setRenderer(renderer);
        // Additional dataset
        int index = 1;
        plot.setDataset(index, xyDataset);
        plot.mapDatasetToRangeAxis(index, 0);
        XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer(true, false);
        renderer2.setSeriesPaint(index, Color.blue);
        plot.setRenderer(index, renderer2);
        // Misc
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setBackgroundPaint(Color.white);
        NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
        numberAxis.setAutoRangeIncludesZero(false);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        
        
        //Running the strategy and adding the buy and sell signals to plot
        
        addBuySellSignals(series, MoneyCommandCenter.shared().getCurrentStrategy(), plot);
        
        _chartPanel = CandlestickChart.displayChart(chart);
        _chartPanel.setVisible(true);
        */
	}
	


	public void chartA() {
	   /**
	   * Getting time series
	   */
	  TimeSeries series = CsvTicksLoader.loadAppleIncSeries();

	  /**
	   * Creating indicators
	   */
	  // Close price
	  ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
	  // Bollinger bands
	  BollingerBandsMiddleIndicator middleBBand = new BollingerBandsMiddleIndicator(closePrice);
	  BollingerBandsLowerIndicator lowBBand = new BollingerBandsLowerIndicator(middleBBand, closePrice);
	  BollingerBandsUpperIndicator upBBand = new BollingerBandsUpperIndicator(middleBBand, closePrice);

	  /**
	   * Building chart dataset
	   */
	  TimeSeriesCollection dataset = new TimeSeriesCollection();
	  dataset.addSeries(IndicatorsToChart.buildChartTimeSeries(series, closePrice, "Apple Inc. (AAPL) - NASDAQ GS"));
	  dataset.addSeries(IndicatorsToChart.buildChartTimeSeries(series, lowBBand, "Low Bollinger Band"));
	  dataset.addSeries(IndicatorsToChart.buildChartTimeSeries(series, upBBand, "High Bollinger Band"));

	  /**
	   * Creating the chart
	   */
	  JFreeChart chart = ChartFactory.createTimeSeriesChart(
	          "Apple Inc. 2013 Close Prices", // title
	          "Date", // x-axis label
	          "Price Per Unit", // y-axis label
	          dataset, // data
	          true, // create legend?
	          true, // generate tooltips?
	          false // generate URLs?
	          );
	  XYPlot plot = (XYPlot) chart.getPlot();
	  DateAxis axis = (DateAxis) plot.getDomainAxis();
	  axis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));

	  /**
	   * Displaying the chart
	   */
	  IndicatorsToChart.displayChart(chart);
	}
}



