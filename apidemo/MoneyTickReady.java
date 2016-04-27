package apidemo;

public class MoneyTickReady {
	private boolean somethingsReady=false;
	private boolean min1=false;
	private boolean min5=false;
	private boolean min10=false;
	private boolean min15=false;
	
	//Sets
	public void setMin1Ready() {
		min1 = true;
	}
	public void setMin5Ready() {
		min5 = true;
	}
	public void setMin10Ready() {
		min10 = true;
	}
	public void setMin15Ready() {
		min15 = true;
	}
	public void setSomethingsReady() {
		somethingsReady = true;
	}
	//Gets
	public boolean min1Ready() {
		return min1;
	}
	public boolean min5Ready() {
		return min5;
	}
	public boolean min10Ready() {
		return min10;
	}
	public boolean min15Ready() {
		return min15;
	}
	public boolean somethingsReady() {
		return somethingsReady;
	}
}
