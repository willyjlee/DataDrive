package com.smartdevicelink.proxy.rpc;

import android.support.annotation.NonNull;

import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.rpc.enums.AudioStreamingIndicator;
import com.smartdevicelink.proxy.rpc.enums.UpdateMode;

import java.util.Hashtable;

/**
 * Sets the media clock/timer value and the update method (e.g.count-up,
 * count-down, etc.)
 * 
 * <p>Function Group: Base </p>
 * <p><b>HMILevel needs to be FULL, LIMITIED or BACKGROUND</b></p>
 * 
 * <p><b>Parameter List</b></p>
 * 
 * <table border="1" rules="all">
 * 		<tr>
 * 			<th>Param Name</th>
 * 			<th>Type</th>
 * 			<th>Description</th>
 *                 <th> Req.</th>
 * 			<th>Notes</th>
 * 			<th>Version Available</th>
 * 		</tr>
 * 		<tr>
 * 			<td>startTime</td>
 * 			<td>StartTime</td>
 * 			<td>StartTime struct specifying hour, minute, second values to which media clock timer is set.</td>
 *                 <td>N</td>
 * 			<td> </td>
 * 			<td>SmartDeviceLink 1.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>endTime</td>
 * 			<td>StartTime</td>
 * 			<td> EndTime can be provided for "COUNTUP" and "COUNTDOWN"; to be used to calculate any visual progress bar (if not provided, this feature is ignored)
 * If endTime is greater then startTime for COUNTDOWN or less than startTime for COUNTUP, then the request will return an INVALID_DATA.
 * endTime will be ignored for "RESUME", and "CLEAR"
 * endTime can be sent for "PAUSE", in which case it will update the paused endTime</td>
 *                 <td>N</td>
 * 			<td>Array must have at least one element.<p>Only optional it helpPrompt has been specified</p> minsize: 1; maxsize: 100</td>
 * 			<td>SmartDeviceLink 1.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>updateMode</td>
 * 			<td>UpdateMode</td>
 * 			<td>Specifies how the media clock/timer is to be updated (COUNTUP/COUNTDOWN/PAUSE/RESUME), based at the startTime.</td>
 *                 <td>Y</td>
 * 			<td>If "updateMode" is COUNTUP or COUNTDOWN, this parameter must be provided. Will be ignored for PAUSE,RESUME and CLEAR</td>
 * 			<td>SmartDeviceLink 1.0</td>
 * 		</tr>
 * 		<tr>
 * 			<td>audioStreamingIndicator</td>
 * 			<td>AudioStreamingIndicator</td>
 * 			<td></td>
 *                 <td>N</td>
 * 			<td></td>
 * 			<td>SmartDeviceLink 5.0</td>
 * 		</tr>
 *
 *  </table>
 *  
 *<p><b>Response </b></p>
 *
 *<p><b> Non-default Result Codes: </b></p>
 *
 *	<p> SUCCESS </p>
 *	<p> INVALID_DATA</p>
 *	<p> OUT_OF_MEMORY</p>
 *  <p>   TOO_MANY_PENDING_REQUESTS</p>
 *   <p>  APPLICATION_NOT_REGISTERED</p>
 *    <p> GENERIC_ERROR</p>
 *   <p>   REJECTED </p>
 *    <p>  IGNORED </p>
 * 
 * @since SmartDeviceLink 1.0
 */
public class SetMediaClockTimer extends RPCRequest {
	public static final String KEY_START_TIME = "startTime";
	public static final String KEY_END_TIME = "endTime";
	public static final String KEY_UPDATE_MODE = "updateMode";
	public static final String KEY_AUDIO_STREAMING_INDICATOR = "audioStreamingIndicator";
	/**
	 * Constructs a new SetMediaClockTimer object
	 */
    public SetMediaClockTimer() {
        super(FunctionID.SET_MEDIA_CLOCK_TIMER.toString());
    }
	/**
	 * Constructs a new SetMediaClockTimer object indicated by the Hashtable
	 * parameter
	 * <p></p>
	 * 
	 * @param hash The Hashtable to use
	 */    
    public SetMediaClockTimer(Hashtable<String, Object> hash) {
        super(hash);
    }
	/**
	 * Constructs a new SetMediaClockTimer object
	 * @param updateMode a Enumeration value (COUNTUP/COUNTDOWN/PAUSE/RESUME) <br>
	 * <b>Notes: </b>
	 *      <ul>
	 *            <li>When updateMode is PAUSE, RESUME or CLEAR, the start time value
	 *            is ignored</li>
	 *            <li>When updateMode is RESUME, the timer resumes counting from
	 *            the timer's value when it was paused</li>
	 *      </ul>
	 */
	public SetMediaClockTimer(@NonNull UpdateMode updateMode) {
		this();
		setUpdateMode(updateMode);
	}
	/**
	 * Gets the Start Time which media clock timer is set
	 * 
	 * @return StartTime -a StartTime object specifying hour, minute, second
	 *         values
	 */    
    @SuppressWarnings("unchecked")
    public StartTime getStartTime() {
		return (StartTime) getObject(StartTime.class, KEY_START_TIME);
    }
	/**
	 * Sets a Start Time with specifying hour, minute, second values
	 * 
	 * @param startTime
	 *            a startTime object with specifying hour, minute, second values
	 *            <p></p>
	 *            <b>Notes: </b>
	 *            <ul>
	 *            <li>If "updateMode" is COUNTUP or COUNTDOWN, this parameter
	 *            must be provided</li>
	 *            <li>Will be ignored for PAUSE/RESUME and CLEAR</li>
	 *            </ul>
	 */    
    public void setStartTime( StartTime startTime ) {
		setParameters(KEY_START_TIME, startTime);
    }
    
    @SuppressWarnings("unchecked")
    public StartTime getEndTime() {
		return (StartTime) getObject(StartTime.class, KEY_END_TIME);
    }
    
    public void setEndTime( StartTime endTime ) {
		setParameters(KEY_END_TIME, endTime);
    }
    
	/**
	 * Gets the media clock/timer update mode (COUNTUP/COUNTDOWN/PAUSE/RESUME)
	 * 
	 * @return UpdateMode -a Enumeration value (COUNTUP/COUNTDOWN/PAUSE/RESUME)
	 */    
    public UpdateMode getUpdateMode() {
		return (UpdateMode) getObject(UpdateMode.class, KEY_UPDATE_MODE);
    }
	/**
	 * Sets the media clock/timer update mode (COUNTUP/COUNTDOWN/PAUSE/RESUME)
	 * 
	 * @param updateMode
	 *            a Enumeration value (COUNTUP/COUNTDOWN/PAUSE/RESUME)
	 *            <p></p>
	 *            <b>Notes: </b>
	 *            <ul>
	 *            <li>When updateMode is PAUSE, RESUME or CLEAR, the start time value
	 *            is ignored</li>
	 *            <li>When updateMode is RESUME, the timer resumes counting from
	 *            the timer's value when it was paused</li>
	 *            </ul>
	 */    
    public void setUpdateMode( @NonNull UpdateMode updateMode ) {
		setParameters(KEY_UPDATE_MODE, updateMode);
    }

	/**
	 * Gets the playback status of a media app
	 *
	 * @return AudioStreamingIndicator - a Enumeration value
	 */
	public AudioStreamingIndicator getAudioStreamingIndicator() {
		return (AudioStreamingIndicator) getObject(AudioStreamingIndicator.class, KEY_AUDIO_STREAMING_INDICATOR);
	}

	/**
	 * Sets the playback status of a media app
	 */
	public void setAudioStreamingIndicator(AudioStreamingIndicator audioStreamingIndicator ) {
		setParameters(KEY_AUDIO_STREAMING_INDICATOR, audioStreamingIndicator);
	}
}
