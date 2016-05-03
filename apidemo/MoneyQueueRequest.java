package apidemo;

public class MoneyQueueRequest<RequestObjType> {

	public enum Type {
		HISTORICAL_PRICE_DATA_REQUEST
	}
	public enum Status {
		NOT_STARTED,
		STARTED
	}
	private Type type;
	public RequestObjType request = null;
	private Status status;
	
	MoneyQueueRequest(Type type, RequestObjType request) {
		this.type = type;
		this.request = request;
		this.status = Status.NOT_STARTED;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public Type getType() {
		return type;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public RequestObjType getRequest() {
		return request;
	}
	
}
