package fr.xtof54.jtransapp;

/**
 * All the methods in this interface must be thread-safe!
 */
public interface ProgressDisplay {
	public void setIndeterminateProgress(String message);
	public void setProgress(String message, float f);
	public void setProgressDone();
}
