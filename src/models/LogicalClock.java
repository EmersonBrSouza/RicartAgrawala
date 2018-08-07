package models;

public class LogicalClock implements ILamportClock{
	
	private Integer timestamp = 0;
	
	/**
	 * Pulse the logical clock
	 * 
	 * @param void
	 * @return void
	 * */
	@Override
	public void tick() {
		this.timestamp++;
	}
	
	/**
	 * Pulse the logical clock based in Max(timestamp, anotherTimestamp)
	 * 
	 * @param Integer anotherTimestamp - The timestamp of another clock
	 * @return void
	 * */
	@Override
	public void tick(Integer anotherTimestamp) {
		this.timestamp = this.compareTimestamps(anotherTimestamp) + 1;
	}
	
	/**
	 * Return the current timestamp
	 * 
	 * @param void
	 * @return Integer timestamp - The current timestamp
	 * */
	@Override
	public Integer getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Evaluate the max value between the current timestamp and another timestamp
	 * 
	 * @param Integer anotherTimestamp - The timestamp of another clock
	 * @return Integer maxTimestamp - The max(currentTimestamp, anotherTimestamp)
	 * */
	@Override
	public Integer compareTimestamps(Integer anotherTimestamp) {
		return Integer.max(this.timestamp, anotherTimestamp);
	}

	

}
