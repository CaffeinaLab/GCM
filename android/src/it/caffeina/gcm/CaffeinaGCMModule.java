package it.caffeina.gcm;

import android.app.Activity;
import android.content.Intent;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;
import org.appcelerator.titanium.TiApplication;

import com.google.android.gcm.GCMRegistrar;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

@Kroll.module(name="CaffeinaGCM", id="it.caffeina.gcm")
public class CaffeinaGCMModule extends KrollModule {

	private static final String LCAT = "it.caffeina.gcm.CaffeinaGCMModule";

	private static CaffeinaGCMModule instance = null;

	private KrollFunction successCallback = null;
	private KrollFunction errorCallback = null;
	private KrollFunction messageCallback = null;

	public CaffeinaGCMModule() {
		super();
		instance = this;
	}

	public static CaffeinaGCMModule getInstance() {
		return instance;
	}

	@Kroll.method
	@SuppressWarnings("unchecked")
	public void registerForPushNotifications(HashMap options) {
		String senderId = (String)options.get("senderId");
		successCallback = (KrollFunction)options.get("success");
		errorCallback = (KrollFunction)options.get("error");
		messageCallback = (KrollFunction)options.get("callback");

		if (senderId != null) {
			GCMRegistrar.register(TiApplication.getInstance(), senderId);

			String registrationId = getRegistrationId();
			if (registrationId != null && registrationId.length() > 0) {
				sendSuccess(registrationId);
			} else {
				Log.e(LCAT, "Error while gettings registrationId from GCM");
				sendError("Error while gettings registrationId from GCM");
			}

			//////////////////////////////////////
			// Send old notification if present //
			//////////////////////////////////////

			Intent intent = TiApplication.getInstance().getRootOrCurrentActivity().getIntent();

			String notif = intent.getStringExtra("notification");
			if (notif != null) {
				try {
					Map map = new Gson().fromJson(notif, Map.class);
					map.put("inBackground", true);
					sendMessage(new KrollDict(map));
				} catch (Exception ex) {}
			}

		} else {
			Log.e(LCAT, "No GCM senderId specified; get it from the Google Play Developer Console");
			sendError("No GCM senderId specified; get it from the Google Play Developer Console");
		}
	}

	@Kroll.method
	public void unregisterForPushNotifications() {
		GCMRegistrar.unregister(TiApplication.getInstance());
	}

	@Kroll.method
	@Kroll.getProperty
	public String getRegistrationId() {
		return GCMRegistrar.getRegistrationId(TiApplication.getInstance());
	}

	public void sendSuccess(String registrationId) {
		if (successCallback != null) {
			HashMap<String, Object> data = new HashMap<String, Object>();
			data.put("registrationId", registrationId);
			data.put("deviceToken", registrationId);

			successCallback.callAsync(getKrollObject(), data);
		}
	}

	public void sendError(String error) {
		if (errorCallback != null) {
			HashMap<String, Object> data = new HashMap<String, Object>();
			data.put("error", error);

			errorCallback.callAsync(getKrollObject(), data);
		}
	}

	public void sendMessage(HashMap<String, Object> messageData) {
		if (messageCallback != null) {
			HashMap<String, Object> data = new HashMap<String, Object>();
			data.put("data", messageData);

			messageCallback.call(getKrollObject(), data);
		}
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app) {
		Log.d(LCAT, "onAppCreate " + app + " (" + (instance != null) + ")");
	}

}

