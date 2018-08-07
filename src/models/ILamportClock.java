package models;

public interface ILamportClock {

	public void tick ();
	
	public void tick (Integer anotherTimestamp);
	
	public Integer getTimestamp();
	
	public Integer compareTimestamps(Integer anotherTimestamp);
}
