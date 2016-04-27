/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2015 Marc de Verdelhan & respective authors (see AUTHORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package eu.verdelhan.ta4j;

import com.ib.controller.NewContract;
import com.ib.controller.NewOrder;
import com.ib.controller.NewOrderState;
import com.ib.controller.OrderStatus;
import com.ib.controller.ApiController.IOrderHandler;
import com.ib.controller.Types.Action;
import com.ib.controller.Types.SecType;

import apidemo.ApiDemo;
import apidemo.MoneyCommandCenter;

import com.ib.controller.ApiController.IOrderHandler;
import eu.verdelhan.ta4j.Order.OrderType;

/**
 * Pair of two {@link Order orders}.
 * <p>
 * The exit order has the complement type of the entry order.<br>
 * I.e.:
 *   entry == BUY --> exit == SELL
 *   entry == SELL --> exit == BUY
 */
public class Trade implements IOrderHandler {

	private NewContract ibContract;
	private NewOrder    ibOrderEntry;
	private NewOrder	ibOrderExit;
		
    /** The entry order */
    private Order entry;

    /** The exit order */
    private Order exit;

    /** The type of the entry order */
    private OrderType startingType;

    /**
     * Constructor.
     */
    public Trade() {
        this(OrderType.BUY);
    }
    
    public void init() {
    	ibContract = new NewContract();
    	ibContract.symbol("ES");
    	ibContract.secType(SecType.FUT);
    	ibContract.exchange("GLOBEX");
    	ibContract.currency("USD");
    	ibContract.expiry("201512");
    }

    /**
     * Constructor.
     * @param startingType the starting {@link OrderType order type} of the trade (i.e. type of the entry order)
     */
    public Trade(OrderType startingType) {
        if (startingType == null) {
            throw new IllegalArgumentException("Starting type must not be null");
        }
        this.startingType = startingType;
        this.init();
        
    }
    
    public void setOrderType(OrderType type) {
    	startingType = type;
    }

    /**
     * Constructor.
     * @param entry the entry {@link Order order}
     * @param exit the exit {@link Order order}
     */
    public Trade(Order entry, Order exit) {
        if (entry.getType().equals(exit.getType())) {
            throw new IllegalArgumentException("Both orders must have different types");
        }
        this.startingType = entry.getType();
        this.entry = entry;
        this.exit = exit;
        
        this.init();
    }

    /**
     * @return the entry {@link Order order} of the trade
     */
    public Order getEntry() {
        return entry;
    }

    /**
     * @return the exit {@link Order order} of the trade
     */
    public Order getExit() {
        return exit;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Trade) {
            Trade t = (Trade) obj;
            return entry.equals(t.getEntry()) && exit.equals(t.getExit());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (entry.hashCode() * 31) + (exit.hashCode() * 17);
    }

    /**
     * Operates the trade at the index-th position
     * @param index the tick index
     * @return the order
     */
    /*
    public Order operate(int index) {
        return operate(index, Decimal.NaN, Decimal.NaN);
    }
    */

    /**
     * Operates the trade at the index-th position
     * @param index the tick index
     * @param price the price
     * @param amount the amount
     * @return the order
     */
    public Order operate(int index, Decimal price, Decimal amount,boolean longTrade) {
    	
        Order order = null;
        if (isNew()) {
        	startingType = (longTrade) ? Order.OrderType.BUY : Order.OrderType.SELL;
            order = new Order(index, startingType, price, amount);
            entry = order;
            
            //KK IB Order to send to exchange
            ibOrderEntry = new NewOrder();
            ibOrderEntry.action((longTrade) ? Action.BUY : Action.SELL);
            ibOrderEntry.totalQuantity( (int)amount.toDouble());
            ibOrderEntry.orderType(com.ib.controller.OrderType.MKT);
            
			//b.send( contract.secIdType() );
			//b.send( contract.secId() );
 //kkundo           if (MoneyCommandCenter.shared().liveTrading()) {
 //kkundo           	placeRealOrderIn();
 //kkundo           }
             
        } else if (isOpened()) {
            if (index < entry.getIndex()) {
                throw new IllegalStateException("The index i is less than the entryOrder index");
            }
            
            //Just use the complementType and not the argument passed in
            Action action = (startingType.complementType() == Order.OrderType.BUY) ? Action.BUY : Action.SELL;
            order = new Order(index, startingType.complementType(), price, amount);
            exit = order;
            
            //KK IB Order to send to exchange
            ibOrderExit = new NewOrder();
            ibOrderExit.action(action);
            ibOrderExit.totalQuantity( ibOrderEntry.totalQuantity()); 
            ibOrderExit.orderType(com.ib.controller.OrderType.MKT);
            
            //KK Send order to exchange
  //kkundo          if (MoneyCommandCenter.shared().liveTrading()) {
  //kkundo          	placeRealOrderOut();
  //kkundo          }
        }
        return order;
    }

    /**
     * @return true if the trade is closed, false otherwise
     */
    public boolean isClosed() {
        return (entry != null) && (exit != null);
    }

    /**
     * @return true if the trade is opened, false otherwise
     */
    public boolean isOpened() {
        return (entry != null) && (exit == null);
    }

    /**
     * @return true if the trade is new, false otherwise
     */
    public boolean isNew() {
        return (entry == null) && (exit == null);
    }

    @Override
    public String toString() {
        return "Entry: " + entry + " exit: " + exit;
    }
    
	//
	// Function that actually places the live order to the exchange
	// Contract and the order need to be filled out
	//
	
	public void placeRealOrderIn() {
	
		ApiDemo.INSTANCE.controller().placeOrModifyOrder( ibContract, ibOrderEntry, this);	
	}
	
	public void placeRealOrderOut() {
		
		ApiDemo.INSTANCE.controller().placeOrModifyOrder( ibContract, ibOrderExit, this);	
	}
	
	//
	//Callbacks of the iOrderHNdler
	//
	public void orderState(NewOrderState orderState)
	{
		System.out.format("\nGot an orderState callback:Commission %4.2f",orderState.commission());
		String state="None";
		
		switch (orderState.status()) {
			case ApiPending:
				state = "ApiPending"; break;
			case ApiCancelled:
				state = "ApiCancelled"; break;
			case PreSubmitted:
				state = "PreSubmitted"; break;
			case PendingCancel:
				state = "PendingCancel"; break;
			case Cancelled:
				state = "Cancelled"; break;
			case Submitted:
				state = "Submitted"; break;
			case Filled:
				state = "Filled"; break;
			case Inactive:
				state = "Inactive"; break;
			case PendingSubmit:
				state = "PendingSubmit"; break;
		}
		System.out.println(" "+state);
	}
	
	public void handle(int errorCode, String errorMsg)
	{
		System.out.format("\nGot a handle callback:Error:%d Msg:%s",errorCode,errorMsg);
	}
}
