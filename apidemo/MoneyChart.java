package apidemo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ChartPanel;
import org.jfree.data.time.Day;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.Minute;
import org.jfree.data.time.RegularTimePeriod;
//import org.jfree.data.time.TimeSeries; 
//import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesCollection;

import eu.verdelhan.ta4j.Order.OrderType;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.Trade;
import eu.verdelhan.ta4j.TradingRecord;

import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.OHLCDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

public class MoneyChart implements Runnable {

	private boolean _chartThreadRunning = false;
	public JPanel chartPanel = null;
	private Strategy strategy;

	// Chart Stuff
	private OHLCSeries series;
	private OHLCSeriesCollection seriesCollection;
	private JFreeChart chart;

	DateAxis domainAxis;
	private AbstractXYItemRenderer Renderer;
	CombinedDomainXYPlot combinedplot;
	XYPlot mainPlot;

	MoneyChart() {
		createChart(null, null, 1, 10);
	}
	
	public class PriceChartPanel extends ChartPanel implements MouseWheelListener {

	    public PriceChartPanel(JFreeChart chart) {
			super(chart);
			// TODO Auto-generated constructor stub
		}

	    public void mouseWheelMoved(MouseWheelEvent e) {
	        if (e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) return;
	        if (e.getWheelRotation()< 0) increaseZoom((ChartPanel)e.getComponent(), true);
	        else                          decreaseZoom((ChartPanel)e.getComponent(), true);
	    }
	   
	    public synchronized void increaseZoom(JComponent chart, boolean saveAction){
	        ChartPanel ch = (ChartPanel)chart;
	        zoomChartAxis(ch, true);
	    } 
	   
	    public synchronized void decreaseZoom(JComponent chart, boolean saveAction){
	        ChartPanel ch = (ChartPanel)chart;
	        zoomChartAxis(ch, false);
	    } 
	   
	    private void zoomChartAxis(ChartPanel chartP, boolean increase){              
	        int width = chartP.getMaximumDrawWidth() - chartP.getMinimumDrawWidth();
	        int height = chartP.getMaximumDrawHeight() - chartP.getMinimumDrawWidth();       
	        if(increase){
	           chartP.zoomInBoth(width/2, height/2);
	        }else{
	           chartP.zoomOutBoth(width/2, height/2);
	        }
	    }
	}


	private void createChart(OHLCSeries s, TimeSeries timeSeries, double min, double max) {

		seriesCollection = createSeriesCollection(s);
		// series = seriesCollection.getSeries(0);

		chart = ChartFactory.createCandlestickChart("ES", "Date Time", "Price", seriesCollection, true);

		chart.getXYPlot().getRangeAxis().setRange(min, max);

		if (timeSeries != null) {
			addBuySellSignals(timeSeries, chart.getXYPlot());
		}

		chartPanel = new PriceChartPanel(chart);
		
		if (s == null) {
			createData(seriesCollection);
		}
		chartPanel.setVisible(true);
	}

	private OHLCSeriesCollection createSeriesCollection(OHLCSeries s) {
		OHLCSeries series = new OHLCSeries("Futures data");
		OHLCSeriesCollection seriesCollection = new OHLCSeriesCollection();
		if (s == null) {
			seriesCollection.addSeries(series);
		} else {
			seriesCollection.addSeries(s);
		}
		return seriesCollection;
	}

	// This is the default case, eventually erase this
	private void createData(OHLCSeriesCollection seriesCollection) {
		series = seriesCollection.getSeries(0);
		for (int i = 0; i < 10; i++) {
			// Generate new bar time
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, i);
			FixedMillisecond fm = new FixedMillisecond(cal.getTime());
			// Add bar to the data. Let's repeat the same bar
			series.add(fm, 100, 110, 90, 105);
		}
	}

	public void run() {

		// All UI should be run on this thread
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// _run();
			}
		});
		_chartThreadRunning = true;
	}

	public JPanel getChartPanel() {
		return chartPanel;
	}

	public void updateTick(Tick tick) {
		// run();
	}

	private OHLCDataItem[] getSeriesDataList(TimeSeries inSeries) {
		List<OHLCDataItem> dataItems = new ArrayList<OHLCDataItem>();
		for (int i = 0; i < (inSeries.getEnd() - 2); i++) {
			Tick t = inSeries.getTick(i);
			OHLCDataItem item = new OHLCDataItem(t.getEndTime().toDate(), t.getOpenPrice().toDouble(),
					t.getMaxPrice().toDouble(), t.getMinPrice().toDouble(), t.getClosePrice().toDouble(),
					t.getVolume().toDouble());

			dataItems.add(item);
		}
		OHLCDataItem[] data = dataItems.toArray(new OHLCDataItem[dataItems.size()]);
		return data;
	}

	protected AbstractXYDataset getDataSet(TimeSeries inSeries) {
		// This is the dataset we are going to create
		DefaultOHLCDataset result = null;
		// This is the data needed for the dataset
		OHLCDataItem[] data;

		// This is where we go get the data, replace with your own data source
		data = getSeriesDataList(inSeries);

		// Create a dataset, an Open, High, Low, Close dataset
		result = new DefaultOHLCDataset("ES", data);

		return result;
	}

	private TimeSeriesCollection createMA8Dataset(TimeSeries inSeries) {
		org.jfree.data.time.TimeSeries s1 = new org.jfree.data.time.TimeSeries("Price", "Time", "$");

		for (int i = 0; i < (inSeries.getEnd() - 2); i++) {
			Tick t = inSeries.getTick(i);
			s1.add(new Minute(t.getEndTime().toDate()),
					MoneyCommandCenter.shared().getMoneyFeed().series1min.fastSMA8.getValue(i).toDouble());
		}
		TimeSeriesCollection timeseriescollection = new TimeSeriesCollection(s1);
		return timeseriescollection;
	}

	// Called from the Panel
	public void updateChart(TimeSeries inSeries) {

		domainAxis = new DateAxis("Date");
		NumberAxis rangeAxis = new NumberAxis("Price");
		Renderer = new CandlestickRenderer();
		XYDataset dataset = getDataSet(inSeries); // getDataSet(stockSymbol);

		mainPlot = new XYPlot(dataset, domainAxis, rangeAxis, Renderer);
		// Create a line series
		TimeSeriesCollection timecollection = createMA8Dataset(inSeries);
		mainPlot.setDataset(1, timecollection);
		mainPlot.setRenderer(1, new XYLineAndShapeRenderer(true, false));
		mainPlot.getRenderer(1).setSeriesPaint(0, Color.blue);

		XYToolTipGenerator xyToolTipGenerator = new XYToolTipGenerator() {
			public String generateToolTip(XYDataset dataset, int series, int item) {
				Number x1 = dataset.getX(series, item);
				Number y1 = dataset.getY(series, item);
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(String.format("<html><p style='color:#0000ff;'>Series: '%s'</p>",
						dataset.getSeriesKey(series)));
				// stringBuilder.append(String.format("X:'%d'<br/>",
				// x1.intValue()));
				stringBuilder.append(String.format("Volume:'%d'", y1.intValue()));
				stringBuilder.append("</html>");
				return stringBuilder.toString();
			}
		};

		mainPlot.getRenderer(1).setBaseToolTipGenerator(xyToolTipGenerator);
		/*
		 * mainPlot.getRenderer(1) .setToolTipGenerator(new
		 * StandardXYToolTipGenerator(
		 * StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT, new
		 * SimpleDateFormat("HH:mm"), new DecimalFormat("0000.00") ) );
		 */

		combinedplot = new CombinedDomainXYPlot(domainAxis);
		combinedplot.setDomainGridlinePaint(Color.white);
		combinedplot.setDomainGridlinesVisible(true);
		combinedplot.add(mainPlot, 3);

		// Do some setting up, see the API Doc
		Renderer.setSeriesPaint(0, Color.BLACK);
		rangeAxis.setAutoRangeIncludesZero(false);

		// Now create the chart and chart panel
		chart = new JFreeChart("ES", null, combinedplot, false);
		addBuySellSignals(inSeries, mainPlot);

		chartPanel = new PriceChartPanel(chart);
		chartPanel.setPreferredSize(new Dimension(600, 500));
		chartPanel.setVisible(true);
		chartPanel.addMouseWheelListener(chartPanel);

		// Now I want to add a volume chart
		IntervalXYDataset volumeDataset = createVolumeDataset(inSeries);
		XYPlot volumePlot = createVolumePlot(volumeDataset);
		volumePlot.getRenderer().setBaseToolTipGenerator(xyToolTipGenerator);
		combinedplot.add(volumePlot);

	}

	// New Series Eventually
	private void createNewSeries(int type, TimeSeries inSeries) {

		XYToolTipGenerator xyToolTipGenerator = new XYToolTipGenerator() {
			public String generateToolTip(XYDataset dataset, int series, int item) {
				Number x1 = dataset.getX(series, item);
				Number y1 = dataset.getY(series, item);
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(String.format("<html><p style='color:#0000ff;'>Series: '%s'</p>",
						dataset.getSeriesKey(series)));
				// stringBuilder.append(String.format("X:'%d'<br/>",
				// x1.intValue()));
				stringBuilder.append(String.format("Volume:'%d'", y1.intValue()));
				stringBuilder.append("</html>");
				return stringBuilder.toString();
			}
		};
		// Now I want to add a volume chart
		IntervalXYDataset volumeDataset = createVolumeDataset(inSeries);
		XYPlot volumePlot = createVolumePlot(volumeDataset);
		volumePlot.getRenderer().setBaseToolTipGenerator(xyToolTipGenerator);
		combinedplot.add(volumePlot);

	}

	// Volume Stuff
	private IntervalXYDataset createVolumeDataset(TimeSeries inSeries) {
		org.jfree.data.time.TimeSeries timeseries = new org.jfree.data.time.TimeSeries("Volume");

		for (int i = 0; i < (inSeries.getEnd() - 2); i++) {
			Tick t = inSeries.getTick(i);
			RegularTimePeriod period = new Minute(t.getEndTime().toDate());
			timeseries.add(period, t.getVolume().toDouble());
		}
		return new org.jfree.data.time.TimeSeriesCollection(timeseries);
	}

	public XYPlot createVolumePlot(XYDataset dataset) {
		NumberAxis RangeaxisValue = new NumberAxis();

		XYItemRenderer renderer = new XYBarRenderer(0.2);
		renderer.setSeriesPaint(0, Color.green);
		renderer.setSeriesPaint(1, Color.blue);

		XYPlot newPlot = new XYPlot(dataset, domainAxis, RangeaxisValue, renderer);
		newPlot.setBackgroundPaint(Color.white);
		newPlot.setDomainGridlinePaint(Color.gray);
		newPlot.setRangeGridlinePaint(Color.gray);
		newPlot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		return newPlot;
	}

	// Read trades from the trade record
	private void addBuySellSignals(TimeSeries series, XYPlot plot) {

		TradingRecord tradingRecord = MoneyCommandCenter.shared().getTradingRecord();
		List<Trade> trades = tradingRecord.getTrades();
		// Adding markers to plot
		for (Trade trade : trades) {
			// Buy signal
			double entrySignalTickTime = new Minute(series.getTick(trade.getEntry().getIndex()).getEndTime().toDate())
					.getFirstMillisecond();
			Marker entryMarker = new ValueMarker(entrySignalTickTime);
			entryMarker.setStroke(new BasicStroke(2f));

			double exitSignalTickTime = new Minute(series.getTick(trade.getExit().getIndex()).getEndTime().toDate())
					.getFirstMillisecond();
			Marker exitMarker = new ValueMarker(exitSignalTickTime);
			exitMarker.setStroke(new BasicStroke(2f));

			if (trade.getStartingType() == OrderType.BUY) {
				entryMarker.setPaint(Color.GREEN);
				entryMarker.setLabel("B");
				exitMarker.setPaint(Color.RED);
				exitMarker.setLabel("S");
			} else {
				entryMarker.setPaint(Color.RED);
				entryMarker.setLabel("S");
				exitMarker.setPaint(Color.GREEN);
				exitMarker.setLabel("B");
			}
			plot.addDomainMarker(entryMarker);
			plot.addDomainMarker(exitMarker);
		}
	}

	public void _run() {
		return;

		/*
		 * //series = CsvTradesLoader.loadBitstampSeries().subseries(0,
		 * Period.hours(6)); TimeSeries series =
		 * MoneyCommandCenter.shared().getMoneyFeed().series1min.series;
		 * 
		 * 
		 * //* Creating the OHLC dataset
		 * 
		 * OHLCDataset ohlcDataset = CandlestickChart.createOHLCDataset(series);
		 * 
		 * 
		 * // * Creating the additional dataset
		 * 
		 * TimeSeriesCollection xyDataset =
		 * CandlestickChart.createAdditionalDataset(series);
		 * 
		 * 
		 * //Creating the chart
		 * 
		 * JFreeChart chart = ChartFactory.createCandlestickChart( "ESU15",
		 * "Time", "USD", ohlcDataset, true); // Candlestick rendering
		 * CandlestickRenderer renderer = new CandlestickRenderer();
		 * renderer.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_SMALLEST)
		 * ; XYPlot plot = chart.getXYPlot(); plot.setRenderer(renderer); //
		 * Additional dataset int index = 1; plot.setDataset(index, xyDataset);
		 * plot.mapDatasetToRangeAxis(index, 0); XYLineAndShapeRenderer
		 * renderer2 = new XYLineAndShapeRenderer(true, false);
		 * renderer2.setSeriesPaint(index, Color.blue); plot.setRenderer(index,
		 * renderer2); // Misc plot.setRangeGridlinePaint(Color.lightGray);
		 * plot.setBackgroundPaint(Color.white); NumberAxis numberAxis =
		 * (NumberAxis) plot.getRangeAxis();
		 * numberAxis.setAutoRangeIncludesZero(false);
		 * plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		 * 
		 * 
		 * //Running the strategy and adding the buy and sell signals to plot
		 * 
		 * addBuySellSignals(series,
		 * MoneyCommandCenter.shared().getCurrentStrategy(), plot);
		 * 
		 * _chartPanel = CandlestickChart.displayChart(chart);
		 * _chartPanel.setVisible(true);
		 */
	}

	public void chartA() {
		/*
		 * was here
		 * 
		 * // Getting time series
		 * 
		 * TimeSeries series = CsvTicksLoader.loadAppleIncSeries();
		 * 
		 * 
		 * //* Creating indicators
		 * 
		 * // Close price ClosePriceIndicator closePrice = new
		 * ClosePriceIndicator(series); // Bollinger bands
		 * BollingerBandsMiddleIndicator middleBBand = new
		 * BollingerBandsMiddleIndicator(closePrice);
		 * BollingerBandsLowerIndicator lowBBand = new
		 * BollingerBandsLowerIndicator(middleBBand, closePrice);
		 * BollingerBandsUpperIndicator upBBand = new
		 * BollingerBandsUpperIndicator(middleBBand, closePrice);
		 * 
		 * 
		 * // * Building chart dataset
		 * 
		 * TimeSeriesCollection dataset = new TimeSeriesCollection();
		 * dataset.addSeries(IndicatorsToChart.buildChartTimeSeries(series,
		 * closePrice, "Apple Inc. (AAPL) - NASDAQ GS"));
		 * dataset.addSeries(IndicatorsToChart.buildChartTimeSeries(series,
		 * lowBBand, "Low Bollinger Band"));
		 * dataset.addSeries(IndicatorsToChart.buildChartTimeSeries(series,
		 * upBBand, "High Bollinger Band"));
		 * 
		 * 
		 * //* Creating the chart
		 * 
		 * JFreeChart chart = ChartFactory.createTimeSeriesChart(
		 * "Apple Inc. 2013 Close Prices", // title "Date", // x-axis label
		 * "Price Per Unit", // y-axis label dataset, // data true, // create
		 * legend? true, // generate tooltips? false // generate URLs? ); XYPlot
		 * plot = (XYPlot) chart.getPlot(); DateAxis axis = (DateAxis)
		 * plot.getDomainAxis(); axis.setDateFormatOverride(new
		 * SimpleDateFormat("yyyy-MM-dd"));
		 * 
		 * IndicatorsToChart.displayChart(chart);
		 */
	}

}
