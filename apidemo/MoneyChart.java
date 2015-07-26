package apidemo;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.OHLCDataset;
import org.joda.time.Period;

import ta4jexamples.loaders.CsvTradesLoader;
import eu.verdelhan.ta4j.TimeSeries;

public class MoneyChart implements Runnable {

	private boolean _chartThreadRunning=false;
	public JPanel 	_chartPanel=null;
	
	MoneyChart() {
		
		
	}
	
	public void run() {
		
		//All UI should be run on this thread
		SwingUtilities.invokeLater(new Runnable() {
		      public void run() {
		    	  _run();
		      }
		});
		_chartThreadRunning=true;
	}
	
	public JPanel getChartPanel() {
		return _chartPanel;
	}
	
	//
	//This is coming directly from the feed for marketTrades
	public void addTrade(OHLCDataset publicTrade) {
		
	}
	
	//
	//Private Methods
	//
	private void _run() {
		
        TimeSeries series = CsvTradesLoader.loadBitstampSeries().subseries(0, Period.hours(6));
        
        /**
         * Creating the OHLC dataset
         */
        OHLCDataset ohlcDataset = CandlestickChart.createOHLCDataset(series);
        
        /**
         * Creating the additional dataset
         */
        TimeSeriesCollection xyDataset = CandlestickChart.createAdditionalDataset(series);
        
        /**
         * Creating the chart
         */
        JFreeChart chart = ChartFactory.createCandlestickChart(
                "Bitstamp BTC price",
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
        
        _chartPanel = CandlestickChart.displayChart(chart);
	}
}
