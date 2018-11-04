package com.sdl.hellosdlandroid;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.IBinder;
import android.security.NetworkSecurityPolicy;
import android.util.Log;

import com.smartdevicelink.managers.CompletionListener;
import com.smartdevicelink.managers.SdlManager;
import com.smartdevicelink.managers.SdlManagerListener;
import com.smartdevicelink.managers.file.filetypes.SdlArtwork;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCNotification;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.TTSChunkFactory;
import com.smartdevicelink.proxy.rpc.AddCommand;
import com.smartdevicelink.proxy.rpc.MenuParams;
import com.smartdevicelink.proxy.rpc.OnCommand;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.OnVehicleData;
import com.smartdevicelink.proxy.rpc.Speak;
import com.smartdevicelink.proxy.rpc.SubscribeVehicleData;
import com.smartdevicelink.proxy.rpc.enums.AppHMIType;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCResponseListener;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.transport.MultiplexTransportConfig;
import com.smartdevicelink.transport.TCPTransportConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import static com.smartdevicelink.proxy.constants.Names.SubscribeVehicleData;

public class SdlService extends Service {

	private static final String TAG 					= "SDL Service";

	private static final String APP_NAME 				= "Hello Sdl";
	private static final String APP_ID 					= "8678309";

	private static final String ICON_FILENAME 			= "hello_sdl_icon.png";
	private static final String SDL_IMAGE_FILENAME  	= "sdl_full_image.png";

	private static final String WELCOME_SHOW 			= "Welcome to HelloSDL";
	private static final String WELCOME_SPEAK 			= "Welcome to Hello S D L";

	private static final String TEST_COMMAND_NAME 		= "Test Command";
	private static final int TEST_COMMAND_ID 			= 1;

	private static final int FOREGROUND_SERVICE_ID = 111;

	// TCP/IP transport config
	// The default port is 12345
	// The IP is of the machine that is running SDL Core
	private static final int TCP_PORT = 12345;
	private static final String DEV_MACHINE_IP_ADDRESS = "192.168.1.13";//"10.142.40.116";

	// variable to create and call functions of the SyncProxy
	private SdlManager sdlManager = null;

	private SubscribeVehicleData subscribeRequest = null;

	private ArrayList<Double>speeds = new ArrayList<>();
	private ArrayList<Double>angles = new ArrayList<>();

	private ArrayList<Double>averageSpeeds = new ArrayList<>();

	private int batch = 5;

	private final String CLOUD_URL = "http://localhost:5000";//"http://b17c0f7d.ngrok.io/";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		averageSpeeds.add(55.0);
		averageSpeeds.add(65.0);
		averageSpeeds.add(63.5);
		averageSpeeds.add(70.0);
		Log.d(TAG, "onCreate");
		super.onCreate();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			enterForeground();
		}
	}

	// Helper method to let the service enter foreground mode
	@SuppressLint("NewApi")
	public void enterForeground() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(APP_ID, "SdlService", NotificationManager.IMPORTANCE_DEFAULT);
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			if (notificationManager != null) {
				notificationManager.createNotificationChannel(channel);
				Notification serviceNotification = new Notification.Builder(this, channel.getId())
						.setContentTitle("Connected through SDL")
						.setSmallIcon(R.drawable.ic_sdl)
						.build();
				startForeground(FOREGROUND_SERVICE_ID, serviceNotification);
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		NetworkSecurityPolicy policy = NetworkSecurityPolicy.getInstance();
		Log.i("cleartextpermitted", policy.isCleartextTrafficPermitted() + "");
		startProxy();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			stopForeground(true);
		}

		if (sdlManager != null) {
			sdlManager.dispose();
		}

		super.onDestroy();
	}

	private void sendDataToCloud(ArrayList<Double>values) {
		Log.i("sending", "sending data to cloud: " + values);
		URL url;
		try {
			url = new URL(CLOUD_URL);
		} catch (MalformedURLException e) {
			return;
		}
		HttpURLConnection urlConnection;

		try {
			Log.i("tag", "set connection");
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("POST");
		} catch (IOException e) {
			Log.i("tag", "IOException");
			return;
		}
		try {

			JSONObject json = new JSONObject();
			JSONArray array = new JSONArray();
			for (Double val : values)
				array.put(val.doubleValue());
			String jsonString = json.put("value", array).toString();

			OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
			writer.write(jsonString);
			writer.flush();
			writer.close();
			out.close();
			Log.i("tag", "wrote data");
		}
		catch (Exception e) {
			Log.i("error", e.toString());
		}
		finally {
			urlConnection.disconnect();
		}
	}

	private boolean detectBrake(ArrayList<Double>speeds) {
		double eps = 30;
		for(int i = 0; i < speeds.size()-1;i++){
			if(speeds.get(i) - speeds.get(i+1) > eps)
				return true;
		}
		return false;
	}

	private double recommendedSpeed(double speed) {
		// pull data from server with neighboring mobile phones
		double avg = speed;
		for(Double d : this.averageSpeeds)
			avg += d.doubleValue();
		avg /= this.averageSpeeds.size();
		return avg;
	}

	private boolean detectHardTurn(ArrayList<Double>speeds) {
		double eps = 30;
		for(int i = 0; i < speeds.size()-1;i++){
			if(Math.abs(speeds.get(i) - speeds.get(i+1)) > eps)
				return true;
		}
		return false;
	}


	private void startProxy() {
		// This logic is to select the correct transport and security levels defined in the selected build flavor
		// Build flavors are selected by the "build variants" tab typically located in the bottom left of Android Studio
		// Typically in your app, you will only set one of these.
		if (sdlManager == null) {
			Log.i(TAG, "Starting SDL Proxy");
			BaseTransportConfig transport = null;
			if (BuildConfig.TRANSPORT.equals("MULTI")) {
				int securityLevel;
				if (BuildConfig.SECURITY.equals("HIGH")) {
					securityLevel = MultiplexTransportConfig.FLAG_MULTI_SECURITY_HIGH;
				} else if (BuildConfig.SECURITY.equals("MED")) {
					securityLevel = MultiplexTransportConfig.FLAG_MULTI_SECURITY_MED;
				} else if (BuildConfig.SECURITY.equals("LOW")) {
					securityLevel = MultiplexTransportConfig.FLAG_MULTI_SECURITY_LOW;
				} else {
					securityLevel = MultiplexTransportConfig.FLAG_MULTI_SECURITY_OFF;
				}
				transport = new MultiplexTransportConfig(this, APP_ID, securityLevel);
			} else if (BuildConfig.TRANSPORT.equals("TCP")) {
				transport = new TCPTransportConfig(TCP_PORT, DEV_MACHINE_IP_ADDRESS, true);
			} else if (BuildConfig.TRANSPORT.equals("MULTI_HB")) {
				MultiplexTransportConfig mtc = new MultiplexTransportConfig(this, APP_ID, MultiplexTransportConfig.FLAG_MULTI_SECURITY_OFF);
				mtc.setRequiresHighBandwidth(true);
				transport = mtc;
			}

			// The app type to be used
			Vector<AppHMIType> appType = new Vector<>();
			appType.add(AppHMIType.MEDIA);

			subscribeRequest = new SubscribeVehicleData();
			subscribeRequest.setSpeed(true);
			subscribeRequest.setSteeringWheelAngle(true);
			subscribeRequest.setOnRPCResponseListener(new OnRPCResponseListener() {
				@Override
				public void onResponse(int correlationId, RPCResponse response) {
					if(response.getSuccess()){
						Log.i("SdlService", "Successfully subscribed to vehicle data.");
					}else{
						Log.i("SdlService", "Request to subscribe to vehicle data was rejected.");
					}
				}
			});

			// The manager listener helps you know when certain events that pertain to the SDL Manager happen
			// Here we will listen for ON_HMI_STATUS and ON_COMMAND notifications
			SdlManagerListener listener = new SdlManagerListener() {
				@Override
				public void onStart() {
					// HMI Status Listener
					sdlManager.addOnRPCNotificationListener(FunctionID.ON_HMI_STATUS, new OnRPCNotificationListener() {
						@Override
						public void onNotified(RPCNotification notification) {
							OnHMIStatus status = (OnHMIStatus) notification;
							if (status.getHmiLevel() == HMILevel.HMI_LIMITED || status.getHmiLevel() == HMILevel.HMI_BACKGROUND
								|| status.getHmiLevel() == HMILevel.HMI_FULL)
								sdlManager.sendRPC(subscribeRequest);
							if (status.getHmiLevel() == HMILevel.HMI_FULL && ((OnHMIStatus) notification).getFirstRun()) {
								sendCommands();
//								performWelcomeSpeak();
//								performWelcomeShow();
							}
						}
					});

					// Menu Selected Listener
					sdlManager.addOnRPCNotificationListener(FunctionID.ON_COMMAND, new OnRPCNotificationListener() {
						@Override
						public void onNotified(RPCNotification notification) {
							OnCommand command = (OnCommand) notification;
							Integer id = command.getCmdID();
							if (id != null) {
								switch (id) {
									case TEST_COMMAND_ID:
										showTest();
										break;
								}
							}
						}
					});

					sdlManager.addOnRPCNotificationListener(FunctionID.ON_VEHICLE_DATA, new OnRPCNotificationListener() {
						@Override
						public void onNotified(RPCNotification notification) {
							OnVehicleData vehicleData = (OnVehicleData) notification;
							if (vehicleData.getSpeed() != null ) {
								speeds.add(vehicleData.getSpeed());
								if (speeds.size() == batch) {
									// TODO: send to server
									sendDataToCloud(speeds);
									if (detectBrake(speeds)) {
										Log.i("tag", "abrupt brake");
										sdlManager.getScreenManager().beginTransaction();
										sdlManager.getScreenManager().setTextField1("Abrupt Brake!!!");
										sdlManager.getScreenManager().commit(new CompletionListener() {
											@Override
											public void onComplete(boolean success) {
												if (success){
													Log.i(TAG, "abrupt brake show successful");
												}
											}
										});
									} else {
										Log.i("tag", "Regular brake");
										sdlManager.getScreenManager().beginTransaction();
										sdlManager.getScreenManager().setTextField1("Regular Drive");
										sdlManager.getScreenManager().commit(new CompletionListener() {
											@Override
											public void onComplete(boolean success) {
												if (success){
													Log.i(TAG, "regular drive show successful");
												}
											}
										});
									}

									Log.i("tag", "Recommended speed");
									sdlManager.getScreenManager().beginTransaction();
									sdlManager.getScreenManager().setTextField3("Recommended speed: " + recommendedSpeed(speeds.get(speeds.size()-1)) + " km/h");
									sdlManager.getScreenManager().commit(new CompletionListener() {
										@Override
										public void onComplete(boolean success) {
											if (success){
												Log.i(TAG, "recommended speed show successful");
											}
										}
									});

									speeds = new ArrayList<>();
								}
								Log.i("SdlService", "Speed status was updated to: " + vehicleData.getSpeed());
							}
							if (vehicleData.getSteeringWheelAngle() != null) {
								angles.add(vehicleData.getSteeringWheelAngle());
								if (angles.size() == batch) {
									// TODO: send to server
									sendDataToCloud(angles);

									if (detectHardTurn(angles)) {
										Log.i("tag", "abrupt angle");
										sdlManager.getScreenManager().beginTransaction();
										sdlManager.getScreenManager().setTextField2("Hard Turn!!!");
										sdlManager.getScreenManager().commit(new CompletionListener() {
											@Override
											public void onComplete(boolean success) {
												if (success) {
													Log.i(TAG, "hard turn show successful");
												}
											}
										});
									} else {
										Log.i("tag", "Regular angle");
										sdlManager.getScreenManager().beginTransaction();
										sdlManager.getScreenManager().setTextField2("Regular steering");
										sdlManager.getScreenManager().commit(new CompletionListener() {
											@Override
											public void onComplete(boolean success) {
												if (success){
													Log.i(TAG, "regular angle show successful");
												}
											}
										});
									}

									angles = new ArrayList<>();
								}
								Log.i("SdlService", "Angle status was updated to: " + vehicleData.getSteeringWheelAngle());
							}


						}
					});

				}
				@Override
				public void onDestroy() {
					SdlService.this.stopSelf();
				}

				@Override
				public void onError(String info, Exception e) {
				}
			};

			// Create App Icon, this is set in the SdlManager builder
			SdlArtwork appIcon = new SdlArtwork(ICON_FILENAME, FileType.GRAPHIC_PNG, R.mipmap.ic_launcher, true);

			// The manager builder sets options for your session
			SdlManager.Builder builder = new SdlManager.Builder(this, APP_ID, APP_NAME, listener);
			builder.setAppTypes(appType);
			builder.setTransportType(transport);
			builder.setAppIcon(appIcon);
			sdlManager = builder.build();
			sdlManager.start();
		}
	}

	/**
	 *  Add commands for the app on SDL.
	 */
	private void sendCommands(){
		AddCommand command = new AddCommand();
		MenuParams params = new MenuParams();
		params.setMenuName(TEST_COMMAND_NAME);
		command.setCmdID(TEST_COMMAND_ID);
		command.setMenuParams(params);
		command.setVrCommands(Collections.singletonList(TEST_COMMAND_NAME));
		sdlManager.sendRPC(command);
	}

	/**
	 * Will speak a sample welcome message
	 */
	private void performWelcomeSpeak(){
		sdlManager.sendRPC(new Speak(TTSChunkFactory.createSimpleTTSChunks(WELCOME_SPEAK)));
	}

	/**
	 * Use the Screen Manager to set the initial screen text and set the image.
	 * Because we are setting multiple items, we will call beginTransaction() first,
	 * and finish with commit() when we are done.
	 */
	private void performWelcomeShow() {
		sdlManager.getScreenManager().beginTransaction();
		sdlManager.getScreenManager().setTextField1(APP_NAME);
		sdlManager.getScreenManager().setTextField2(WELCOME_SHOW);
		sdlManager.getScreenManager().setPrimaryGraphic(new SdlArtwork(SDL_IMAGE_FILENAME, FileType.GRAPHIC_PNG, R.drawable.sdl, true));
		sdlManager.getScreenManager().commit(new CompletionListener() {
			@Override
			public void onComplete(boolean success) {
				if (success){
					Log.i(TAG, "welcome show successful");
				}
			}
		});
	}

	/**
	 * Will show a sample test message on screen as well as speak a sample test message
	 */
	private void showTest(){
		sdlManager.getScreenManager().beginTransaction();
		sdlManager.getScreenManager().setTextField1("Command has been selected");
		sdlManager.getScreenManager().setTextField2("");
		sdlManager.getScreenManager().commit(null);

		sdlManager.sendRPC(new Speak(TTSChunkFactory.createSimpleTTSChunks(TEST_COMMAND_NAME)));
	}


}
