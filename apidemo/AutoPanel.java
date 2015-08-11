/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package apidemo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Timer;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.SwingUtilities;

import apidemo.MarketDataPanel.BarResultsPanel;
import apidemo.MoneyCommandCenter.ChartTickUpdateCallbackHandler;
import apidemo.util.HtmlButton;
import apidemo.util.NewTabbedPanel;
import apidemo.util.TCombo;
import apidemo.util.UpperField;
import apidemo.util.VerticalPanel;
import apidemo.util.NewTabbedPanel.NewTabPanel;
import apidemo.util.VerticalPanel.StackPanel;
import apidemo.MoneyCommandCenter;
import com.ib.client.ScannerSubscription;
import com.ib.controller.Bar;
import com.ib.controller.Instrument;
import com.ib.controller.NewContract;
import com.ib.controller.NewContractDetails;
import com.ib.controller.ScanCode;
import com.ib.controller.ApiController.IDeepMktDataHandler;
import com.ib.controller.ApiController.IHistoricalDataHandler;
import com.ib.controller.ApiController.IRealTimeBarHandler;
import com.ib.controller.ApiController.IScannerHandler;
import com.ib.controller.Types.BarSize;
import com.ib.controller.Types.DeepSide;
import com.ib.controller.Types.DeepType;
import com.ib.controller.Types.DurationUnit;
import com.ib.controller.Types.MktDataType;
import com.ib.controller.Types.WhatToShow;

//Charting
import eu.verdelhan.ta4j.Indicator;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.bollingerbands.BollingerBandsLowerIndicator;
import eu.verdelhan.ta4j.indicators.trackers.bollingerbands.BollingerBandsMiddleIndicator;
import eu.verdelhan.ta4j.indicators.trackers.bollingerbands.BollingerBandsUpperIndicator;

import java.text.SimpleDateFormat;

import org.joda.time.Period;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import ta4jexamples.loaders.CsvTicksLoader;
import ta4jexamples.loaders.CsvTradesLoader;

public class AutoPanel extends JPanel  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final NewContract m_contract = new NewContract();
	private final NewTabbedPanel m_autoPanel = new NewTabbedPanel();
	private final NewTabbedPanel m_bPanel = new NewTabbedPanel();
	private TopResultsPanel m_topResultPanel;
	
	private final MainRequestPanel m_mainPanel = new MainRequestPanel();

	AutoPanel() {
		
		m_autoPanel.addTab( "Main", m_mainPanel);
		m_autoPanel.addTab( "Backtest", new BacktestRequestPanel() );
		
		//kk this is the layout of the tab position
		setLayout( new BorderLayout() );
		add( m_autoPanel, BorderLayout.NORTH);
		add( m_bPanel);
		
		
		/*
		m_requestPanel.addTab( "Historical Data", new HistRequestPanel() );
		m_requestPanel.addTab( "Real-time Bars", new RealtimeRequestPanel() );
		m_requestPanel.addTab( "Market Scanner", new ScannerRequestPanel() );
		*/
		
		//setLayout( new BorderLayout() );
		//displayImage("red");
		
		/*
		JPanel p4 = new JPanel( new BorderLayout() );
		p4.add( p1, BorderLayout.WEST);
		p4.add( p2);
		p4.add( p3, BorderLayout.SOUTH);
		*/
	}

	//
	// KK Just display a single graphic
	//
	public void displayImage(String inColor)
	{
		m_mainPanel.displayImage(inColor);	

	}

	
	protected ImageIcon createImageIcon(String path,
			String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	/*
	//Done do display the connected Icon
	   @Override
	    protected void paintComponent(Graphics g) {
	        super.paintComponent(g);
	       
			g.drawImage(connectedImage, 0, 0, null); // see javadoc for more info on the parameters            
	    }
	  */  
	
	public class MainRequestPanel extends JPanel implements ItemListener, ActionListener, ChartTickUpdateCallbackHandler {
	
		private static final long serialVersionUID = -8994622134449501051L;
		final ContractPanel m_contractPanel = new ContractPanel(m_contract);
		private final JLabel m_status = new JLabel("Disconnected");  //removed
		private JLabel connectedLabel = null;
		public JCheckBox m_enableAuto = null;
		private final UpperField m_shortMA = new UpperField();
		private final UpperField m_mediumMA = new UpperField();
		private final UpperField m_longMA = new UpperField();
		private MoneyChart _moneyChart=new MoneyChart();
		
		JPanel _chartPanel=null;
		
		MainRequestPanel() {

			//Initialize Data Items
			MoneyCommandCenter.shared().setPanelDelegate(this);  //done for the chart update call back
			m_enableAuto = new JCheckBox();
			m_enableAuto.setSelected(false);
			m_enableAuto.addItemListener(this);
			
			m_shortMA.addActionListener(this);
			m_mediumMA.addActionListener(this);
			m_longMA.addActionListener(this);
			
			//Initialize Panel View
			setLayout(new BorderLayout());
			
			JPanel sub = new JPanel(new SpringLayout());
			sub.add(new JLabel("Enable Auto Trading"));
			sub.add(m_enableAuto);
			sub.add(new JLabel("Short Moving Average"));
			sub.add(m_shortMA);
			sub.add(new JLabel("Medium Moving Average"));
			sub.add(m_mediumMA);
			sub.add(new JLabel("Long Moving Average"));
			sub.add(m_longMA);
			JButton refresh = new JButton("Req Hist");
			refresh.addActionListener(new ActionListener() {
			  public void actionPerformed(ActionEvent e) {
				  MoneyCommandCenter.shared().requestHistoricalData();
			  }
			});
			JButton refresh2 = new JButton("Read Hist");
			refresh2.addActionListener(new ActionListener() {
			  public void actionPerformed(ActionEvent e) {
				  MoneyCommandCenter.shared().runBacktest(1);
			  }
			});
			sub.add(refresh);
			sub.add(refresh2);
			JButton enter = new JButton("Enter");
			enter.addActionListener(new ActionListener() {
			  public void actionPerformed(ActionEvent e) {
				  MoneyCommandCenter.shared().testEnter();
			  }
			});
			JButton exit = new JButton("Exit");
			exit.addActionListener(new ActionListener() {
			  public void actionPerformed(ActionEvent e) {
				  MoneyCommandCenter.shared().testExit();
			  }
			});
			sub.add(enter);
			sub.add(exit);
			
		      //Lay out the panel.
	        SpringUtilities.makeCompactGrid(sub,
	                                        6, 2, //rows, cols
	                                        6, 6,        //initX, initY
	                                        6, 6);       //xPad, yPad		
			add(sub,BorderLayout.WEST);
			
			displayImage("red");
			add(connectedLabel,BorderLayout.EAST);
	
			//Display the Chart on his own thread
			//(new Thread(_moneyChart)).start();
			_moneyChart.run();	
			while (_moneyChart.getChartPanel() == null) { 
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			add(_moneyChart.getChartPanel(),BorderLayout.SOUTH);	
			setVisible( true);
		}
		
		public void updateTick(Tick tick)
		{
			_moneyChart.updateTick(tick);
			add(_moneyChart.getChartPanel(),BorderLayout.SOUTH);	
			setVisible( true);
		}
		
		protected void onHistReqToday() {
			//m_contractPanel.onOK();
			//BarResultsPanel panel = new BarResultsPanel( true);
			//ApiDemo.INSTANCE.controller().reqHistoricalData(m_contract, m_end.getText(), m_duration.getInt(), m_durationUnit.getSelectedItem(), m_barSize.getSelectedItem(), m_whatToShow.getSelectedItem(), m_rthOnly.isSelected(), panel);
			//m_resultsPanel.addTab( "Historical " + m_contract.symbol(), panel, true, true);
		}

		//
		// Callback for when items are checked
		//
		public void itemStateChanged(ItemEvent e) {
			Object source = e.getItemSelectable();
			
			
			if (source == m_enableAuto) {
				if (e.getStateChange() == ItemEvent.SELECTED)  {
					System.out.format("\nAuto Enabled Selected");
					
					BarResultsPanel panel = new BarResultsPanel( false);
	//KKTBD				ApiDemo.INSTANCE.controller().reqRealTimeBars(m_contract, m_whatToShow.getSelectedItem(), m_rthOnly.isSelected(), panel);
					//m_bPanel.addTab( "Real-time " + m_contract.symbol(), panel, true, true);
					MoneyCommandCenter.shared().LiveTrading = true;
					
				} else {
					System.out.format("\nAuto Enabled Deselected");
					MoneyCommandCenter.shared().LiveTrading = false;
				}
			}
		}
		
		 public void actionPerformed(ActionEvent e) {
			Object source=e.getSource();
			//System.out.format("\nActionCommand:%s",source);
			
			System.out.format("\nKK TBD add a way to globally update this for strategies and charts");
			
			
			if (source == m_shortMA) {
				System.out.format("\nShort MA changed");
			} else if (source == m_mediumMA) {
				System.out.format("\nMedium MA changed");
			} else if (source == m_longMA) {
				System.out.format("\nLong MA changed");
			}
		}
		
		public void displayImage(String inColor)
		{
			String colorFile="";
			String text="";
			
			if (inColor == "green") {
				colorFile = "../green.jpg";
				m_status.setText( "connected");
				text = "Connected";
			}
			else {
				colorFile = "../red.jpg";
				m_status.setText( "disconnected");
				text = "Disconnected";
			}
			ImageIcon icon = createImageIcon(colorFile,"");
			
			if (connectedLabel == null) {
				connectedLabel = new JLabel("Disconnected",icon, JLabel.LEFT);
				connectedLabel.setVerticalTextPosition(JLabel.BOTTOM);
				connectedLabel.setHorizontalTextPosition(JLabel.RIGHT);
				
				//add(connectedLabel,location);
				connectedLabel.setSize( 50, 50);
			}
			else {
				connectedLabel.setIcon(icon);
				connectedLabel.setText(text);
			}
			setVisible( true);

			System.out.format("Trying to display image:%s",colorFile);
		}
		

		protected void onTop() {
			m_contractPanel.onOK();
			if (m_topResultPanel == null) {
				m_topResultPanel = new TopResultsPanel();
				m_autoPanel.addTab( "Top Data", m_topResultPanel, true, true);
			}
			
			m_topResultPanel.m_model.addRow( m_contract);
		}
	}
	
	private class TopResultsPanel extends NewTabPanel {
		final TopModel m_model = new TopModel();
		final JTable m_tab = new TopTable( m_model);
		final TCombo<MktDataType> m_typeCombo = new TCombo<MktDataType>( MktDataType.values() );

		TopResultsPanel() {
			m_typeCombo.removeItemAt( 0);

			JScrollPane scroll = new JScrollPane( m_tab);

			HtmlButton reqType = new HtmlButton( "Go") {
				@Override protected void actionPerformed() {
					onReqType();
				}
			};

			VerticalPanel butPanel = new VerticalPanel();
			butPanel.add( "Market data type", m_typeCombo, reqType);
			
			setLayout( new BorderLayout() );
			add( scroll);
			add( butPanel, BorderLayout.SOUTH);
		}
		
		/** Called when the tab is first visited. */
		@Override public void activated() {
		}

		/** Called when the tab is closed by clicking the X. */
		@Override public void closed() {
			m_model.desubscribe();
			m_topResultPanel = null;
		}

		void onReqType() {
			ApiDemo.INSTANCE.controller().reqMktDataType( m_typeCombo.getSelectedItem() );
		}
		
		class TopTable extends JTable {
			public TopTable(TopModel model) { super( model); }

			@Override public TableCellRenderer getCellRenderer(int rowIn, int column) {
				TableCellRenderer rend = super.getCellRenderer(rowIn, column);
				m_model.color( rend, rowIn, getForeground() );
				return rend;
			}
		}
	}		
	
	private class BacktestRequestPanel extends JPanel implements ActionListener{  //ActionListener is for timer
		final ContractPanel m_contractPanel = new ContractPanel(m_contract);
		Timer timer;
		
		BacktestRequestPanel() {
			HtmlButton reqDeep = new HtmlButton( "Request Deep Market Data") {
				@Override protected void actionPerformed() {
					onDeep();
				}
			};
			
			VerticalPanel butPanel = new VerticalPanel();
			butPanel.add( reqDeep);
			
			setLayout( new BoxLayout( this, BoxLayout.X_AXIS) );
			//add( m_contractPanel);
			add( Box.createHorizontalStrut(20));
			add( butPanel);
			
			//
			//Timer Experiment
			//
			 //Set up timer to drive animation events.
	        //timer = new Timer(1000, this);
			//timer = new Timer(1000);
	        //timer.setInitialDelay(pause);
	        //timer.start(); 
	 
	    }
	    //Handle timer event. Update the loopslot (frame number) and the
	    //offset.  If it's the last frame, restart the timer to get a long
	    //pause between loops.
	    public void actionPerformed(ActionEvent e) {
	        //If still loading, can't animate.
	    	//System.out.format("Timer Fired");	
	    	
	    	//kck add update the chart here
		}

		protected void onDeep() {
			m_contractPanel.onOK();
			DeepResultsPanel resultPanel = new DeepResultsPanel();
			m_bPanel.addTab( "Deep " + m_contract.symbol(), resultPanel, true, true);
			ApiDemo.INSTANCE.controller().reqDeepMktData(m_contract, 6, resultPanel);
		}
	}

	private static class DeepResultsPanel extends NewTabPanel implements IDeepMktDataHandler {
		final DeepModel m_buy = new DeepModel();
		final DeepModel m_sell = new DeepModel();

		DeepResultsPanel() {
			HtmlButton desub = new HtmlButton( "Desubscribe") {
				public void actionPerformed() {
					onDesub();
				}
			};
			
			JTable buyTab = new JTable( m_buy);
			JTable sellTab = new JTable( m_sell);
			
			JScrollPane buyScroll = new JScrollPane( buyTab);
			JScrollPane sellScroll = new JScrollPane( sellTab);
			
			JPanel mid = new JPanel( new GridLayout( 1, 2) );
			mid.add( buyScroll);
			mid.add( sellScroll);
			
			setLayout( new BorderLayout() );
			add( mid);
			add( desub, BorderLayout.SOUTH);
		}
		
		protected void onDesub() {
			ApiDemo.INSTANCE.controller().cancelDeepMktData( this);
		}

		@Override public void activated() {
		}

		/** Called when the tab is closed by clicking the X. */
		@Override public void closed() {
			ApiDemo.INSTANCE.controller().cancelDeepMktData( this);
		}
		
		@Override public void updateMktDepth(int pos, String mm, DeepType operation, DeepSide side, double price, int size) {
			if (side == DeepSide.BUY) {
				m_buy.updateMktDepth(pos, mm, operation, price, size);
			}
			else {
				m_sell.updateMktDepth(pos, mm, operation, price, size);
			}
		}

		class DeepModel extends AbstractTableModel {
			final ArrayList<DeepRow> m_rows = new ArrayList<DeepRow>();

			@Override public int getRowCount() {
				return m_rows.size();
			}

			public void updateMktDepth(int pos, String mm, DeepType operation, double price, int size) {
				switch( operation) {
					case INSERT:
						m_rows.add( pos, new DeepRow( mm, price, size) );
						fireTableRowsInserted(pos, pos);
						break;
					case UPDATE:
						m_rows.get( pos).update( mm, price, size);
						fireTableRowsUpdated(pos, pos);
						break;
					case DELETE:
						if (pos < m_rows.size() ) {
							m_rows.remove( pos);
						}
						else {
							// this happens but seems to be harmless
							// System.out.println( "can't remove " + pos);
						}
						fireTableRowsDeleted(pos, pos);
						break;
				}
			}

			@Override public int getColumnCount() {
				return 3;
			}
			
			@Override public String getColumnName(int col) {
				switch( col) {
					case 0: return "Mkt Maker";
					case 1: return "Price";
					case 2: return "Size";
					default: return null;
				}
			}

			@Override public Object getValueAt(int rowIn, int col) {
				DeepRow row = m_rows.get( rowIn);
				
				switch( col) {
					case 0: return row.m_mm;
					case 1: return row.m_price;
					case 2: return row.m_size;
					default: return null;
				}
			}
		}
		
		static class DeepRow {
			String m_mm;
			double m_price;
			int m_size;

			public DeepRow(String mm, double price, int size) {
				update( mm, price, size);
			}
			
			void update( String mm, double price, int size) {
				m_mm = mm;
				m_price = price;
				m_size = size;
			}
		}
	}

	private class HistRequestPanel extends JPanel {
		final ContractPanel m_contractPanel = new ContractPanel(m_contract);
		final UpperField m_end = new UpperField();
		final UpperField m_duration = new UpperField();
		final TCombo<DurationUnit> m_durationUnit = new TCombo<DurationUnit>( DurationUnit.values() );
		final TCombo<BarSize> m_barSize = new TCombo<BarSize>( BarSize.values() );
		final TCombo<WhatToShow> m_whatToShow = new TCombo<WhatToShow>( WhatToShow.values() );
		final JCheckBox m_rthOnly = new JCheckBox();
		
		HistRequestPanel() { 		
			m_end.setText( "20150728 12:00:00");
			m_duration.setText( "1");
			m_durationUnit.setSelectedItem( DurationUnit.WEEK);
			m_barSize.setSelectedItem( BarSize._1_hour);
			
			HtmlButton button = new HtmlButton( "Request historical data") {
				@Override protected void actionPerformed() {
					onHistorical();
				}
			};
			
	    	VerticalPanel paramPanel = new VerticalPanel();
			paramPanel.add( "End", m_end);
			paramPanel.add( "Duration", m_duration);
			paramPanel.add( "Duration unit", m_durationUnit);
			paramPanel.add( "Bar size", m_barSize);
			paramPanel.add( "What to show", m_whatToShow);
			paramPanel.add( "RTH only", m_rthOnly);
			
			VerticalPanel butPanel = new VerticalPanel();
			butPanel.add( button);
			
			JPanel rightPanel = new StackPanel();
			rightPanel.add( paramPanel);
			rightPanel.add( Box.createVerticalStrut( 20));
			rightPanel.add( butPanel);
			
			setLayout( new BoxLayout( this, BoxLayout.X_AXIS) );
			add( m_contractPanel);
			add( Box.createHorizontalStrut(20) );
			add( rightPanel);
		}
	
		protected void onHistorical() {
			m_contractPanel.onOK();
			BarResultsPanel panel = new BarResultsPanel( true);
			ApiDemo.INSTANCE.controller().reqHistoricalData(m_contract, m_end.getText(), m_duration.getInt(), m_durationUnit.getSelectedItem(), m_barSize.getSelectedItem(), m_whatToShow.getSelectedItem(), m_rthOnly.isSelected(), panel);
			m_bPanel.addTab( "Historical " + m_contract.symbol(), panel, true, true);
		}
	}

	private class RealtimeRequestPanel extends JPanel {
		final ContractPanel m_contractPanel = new ContractPanel(m_contract);
		final TCombo<WhatToShow> m_whatToShow = new TCombo<WhatToShow>( WhatToShow.values() );
		final JCheckBox m_rthOnly = new JCheckBox();
		
		RealtimeRequestPanel() { 		
			HtmlButton button = new HtmlButton( "Request real-time bars") {
				@Override protected void actionPerformed() {
					onRealTime();
				}
			};
	
	    	VerticalPanel paramPanel = new VerticalPanel();
			paramPanel.add( "What to show", m_whatToShow);
			paramPanel.add( "RTH only", m_rthOnly);
			
			VerticalPanel butPanel = new VerticalPanel();
			butPanel.add( button);
			
			JPanel rightPanel = new StackPanel();
			rightPanel.add( paramPanel);
			rightPanel.add( Box.createVerticalStrut( 20));
			rightPanel.add( butPanel);
			
			setLayout( new BoxLayout( this, BoxLayout.X_AXIS) );
			add( m_contractPanel);
			add( Box.createHorizontalStrut(20) );
			add( rightPanel);
		}
	
		protected void onRealTime() {
			m_contractPanel.onOK();
			BarResultsPanel panel = new BarResultsPanel( false);
			ApiDemo.INSTANCE.controller().reqRealTimeBars(m_contract, m_whatToShow.getSelectedItem(), m_rthOnly.isSelected(), panel);
			m_bPanel.addTab( "Real-time " + m_contract.symbol(), panel, true, true);
		}
	}
	
	static class BarResultsPanel extends NewTabPanel implements IHistoricalDataHandler, IRealTimeBarHandler {
		final BarModel m_model = new BarModel();
		final ArrayList<Bar> m_rows = new ArrayList<Bar>();
		final boolean m_historical;
		final Chart m_chart = new Chart( m_rows);
		
		BarResultsPanel( boolean historical) {
			m_historical = historical;
			
			JTable tab = new JTable( m_model);
			JScrollPane scroll = new JScrollPane( tab) {
				public Dimension getPreferredSize() {
					Dimension d = super.getPreferredSize();
					d.width = 500;
					return d;
				}
			};

			JScrollPane chartScroll = new JScrollPane( m_chart);

			setLayout( new BorderLayout() );
			add( scroll, BorderLayout.WEST);
			add( chartScroll, BorderLayout.CENTER);
		}

		/** Called when the tab is first visited. */
		@Override public void activated() {
		}

		/** Called when the tab is closed by clicking the X. */
		@Override public void closed() {
			if (m_historical) {
				ApiDemo.INSTANCE.controller().cancelHistoricalData( this);
			}
			else {
				ApiDemo.INSTANCE.controller().cancelRealtimeBars( this);
			}
		}

		@Override public void historicalData(Bar bar, boolean hasGaps) {
			m_rows.add( bar);
		}
		
		@Override public void historicalDataEnd() {
			fire();
		}

		@Override public void realtimeBar(Bar bar) {
			//kk adding received bar into panel, this the handler call back
			m_rows.add( bar); 
			System.out.format("KK11BarResults:%s",bar.toString());
			
			//Central command updating 
			//KKTBDMoneyCommandCenter.getInstance().addRealtimeBar(bar);
			
			//fire();
		}
		
		private void fire() {
			SwingUtilities.invokeLater( new Runnable() {
				@Override public void run() {
					m_model.fireTableRowsInserted( m_rows.size() - 1, m_rows.size() - 1);
					m_chart.repaint();
				}
			});
		}

		class BarModel extends AbstractTableModel {
			@Override public int getRowCount() {
				return m_rows.size();
			}

			@Override public int getColumnCount() {
				return 7;
			}
			
			@Override public String getColumnName(int col) {
				switch( col) {
					case 0: return "Date/time";
					case 1: return "Open";
					case 2: return "High";
					case 3: return "Low";
					case 4: return "Close";
					case 5: return "Volume";
					case 6: return "WAP";
					default: return null;
				}
			}

			@Override public Object getValueAt(int rowIn, int col) {
				Bar row = m_rows.get( rowIn);
				switch( col) {
					case 0: return row.formattedTime();
					case 1: return row.open();
					case 2: return row.high();
					case 3: return row.low();
					case 4: return row.close();
					case 5: return row.volume();
					case 6: return row.wap();
					default: return null;
				}
			}
		}		
	}
	
	private class ScannerRequestPanel extends JPanel {
		final UpperField m_numRows = new UpperField( "15");
		final TCombo<ScanCode> m_scanCode = new TCombo<ScanCode>( ScanCode.values() );
		final TCombo<Instrument> m_instrument = new TCombo<Instrument>( Instrument.values() );
		final UpperField m_location = new UpperField( "STK.US.MAJOR", 9);
		final TCombo<String> m_stockType = new TCombo<String>( "ALL", "STOCK", "ETF");
		
		ScannerRequestPanel() {
			HtmlButton go = new HtmlButton( "Go") {
				@Override protected void actionPerformed() {
					onGo();
				}
			};
			
			VerticalPanel paramsPanel = new VerticalPanel();
			paramsPanel.add( "Scan code", m_scanCode);
			paramsPanel.add( "Instrument", m_instrument);
			paramsPanel.add( "Location", m_location, Box.createHorizontalStrut(10), go);
			paramsPanel.add( "Stock type", m_stockType);
			paramsPanel.add( "Num rows", m_numRows);
			
			setLayout( new BorderLayout() );
			add( paramsPanel, BorderLayout.NORTH);
		}

		protected void onGo() {
			ScannerSubscription sub = new ScannerSubscription();
			sub.numberOfRows( m_numRows.getInt() );
			sub.scanCode( m_scanCode.getSelectedItem().toString() );
			sub.instrument( m_instrument.getSelectedItem().toString() );
			sub.locationCode( m_location.getText() );
			sub.stockTypeFilter( m_stockType.getSelectedItem().toString() );
			
			ScannerResultsPanel resultsPanel = new ScannerResultsPanel();
			m_bPanel.addTab( sub.scanCode(), resultsPanel, true, true);

			ApiDemo.INSTANCE.controller().reqScannerSubscription( sub, resultsPanel);
		}
	}

	static class ScannerResultsPanel extends NewTabPanel implements IScannerHandler {
		final HashSet<Integer> m_conids = new HashSet<Integer>();
		final TopModel m_model = new TopModel();

		ScannerResultsPanel() {
			JTable table = new JTable( m_model);
			JScrollPane scroll = new JScrollPane( table);
			setLayout( new BorderLayout() );
			add( scroll);
		}

		/** Called when the tab is first visited. */
		@Override public void activated() {
		}

		/** Called when the tab is closed by clicking the X. */
		@Override public void closed() {
			ApiDemo.INSTANCE.controller().cancelScannerSubscription( this);
			m_model.desubscribe();
		}

		@Override public void scannerParameters(String xml) {
			try {
				File file = File.createTempFile( "pre", ".xml");
				FileWriter writer = new FileWriter( file);
				writer.write( xml);
				writer.close();

				Desktop.getDesktop().open( file);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override public void scannerData(int rank, final NewContractDetails contractDetails, String legsStr) {
			if (!m_conids.contains( contractDetails.conid() ) ) {
				m_conids.add( contractDetails.conid() );
				SwingUtilities.invokeLater( new Runnable() {
					@Override public void run() {
						m_model.addRow( contractDetails.contract() );
					}
				});
			}
		}

		@Override public void scannerDataEnd() {
			// we could sort here
		}
	}
}

