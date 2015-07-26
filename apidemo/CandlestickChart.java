package apidemo;

import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;

import java.awt.Color;
import java.util.Date;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.joda.time.Period;

import ta4jexamples.loaders.CsvTradesLoader;

/**
 * This class builds a traditional candlestick chart.
 */
public class CandlestickChart {

    /**
     * Builds a JFreeChart OHLC dataset from a ta4j time series.
     * @param series a time series
     * @return an Open-High-Low-Close dataset
     */
    public static OHLCDataset createOHLCDataset(TimeSeries series) {
        final int nbTicks = series.getTickCount();
        
        Date[] dates = new Date[nbTicks];
        double[] opens = new double[nbTicks];
        double[] highs = new double[nbTicks];
        double[] lows = new double[nbTicks];
        double[] closes = new double[nbTicks];
        double[] volumes = new double[nbTicks];
        
        for (int i = 0; i < nbTicks; i++) {
            Tick tick = series.getTick(i);
            dates[i] = tick.getEndTime().toDate();
            opens[i] = tick.getOpenPrice().toDouble();
            highs[i] = tick.getMaxPrice().toDouble();
            lows[i] = tick.getMinPrice().toDouble();
            closes[i] = tick.getClosePrice().toDouble();
            volumes[i] = tick.getVolume().toDouble();
        }
        
        OHLCDataset dataset = new DefaultHighLowDataset("btc", dates, highs, lows, opens, closes, volumes);
        
        return dataset;
    }
    
    /**
     * Builds an additional JFreeChart dataset from a ta4j time series.
     * @param series a time series
     * @return an additional dataset
     */
    public static TimeSeriesCollection createAdditionalDataset(TimeSeries series) {
        ClosePriceIndicator indicator = new ClosePriceIndicator(series);
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries("Btc price");
        for (int i = 0; i < series.getTickCount(); i++) {
            Tick tick = series.getTick(i);
            chartTimeSeries.add(new Second(tick.getEndTime().toDate()), indicator.getValue(i).toDouble());
        }
        dataset.addSeries(chartTimeSeries);
        return dataset;
    }

    /**
     * Displays a chart in a frame.
     * @param chart the chart to be displayed
     */
    public static JPanel displayChart(JFreeChart chart) {
        // Chart panel
        ChartPanel panel = new ChartPanel(chart);
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        panel.setPreferredSize(new java.awt.Dimension(740, 300));
        // Application frame
        /*
        ApplicationFrame frame = new ApplicationFrame("Ta4j example - Candlestick chart");
        frame.setContentPane(panel);
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
        */
        
        return panel;
    }
}