package com.miz.mizuu;

import static com.miz.functions.MizLib.FILESOURCE;
import static com.miz.functions.MizLib.MOVIE;
import static com.miz.functions.MizLib.SERIAL_NUMBER;
import static com.miz.functions.MizLib.SERVER;
import static com.miz.functions.MizLib.TV_SHOW;
import static com.miz.functions.MizLib.TYPE;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.miz.functions.FileSource;
import com.miz.service.WireUpnpService;

public class AddUpnpFilesourceDialog extends Activity {

	private ListView mListView;
	private boolean isMovie = false;

	private AndroidUpnpService upnpService;
	private DeviceListRegistryListener deviceListRegistryListener;

	private ArrayAdapter<UpnpDevice> deviceListAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		setContentView(R.layout.upnp_list);

		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setEmptyView(findViewById(R.id.progressBar1));

		isMovie = getIntent().getExtras().getString("type").equals("movie");

		deviceListRegistryListener = new DeviceListRegistryListener();

		deviceListAdapter = new ArrayAdapter<UpnpDevice>(this,
				android.R.layout.simple_list_item_1);

		mListView.setAdapter(deviceListAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Intent intent = new Intent();
				intent.setClass(getApplicationContext(), FileSourceBrowser.class);
				intent.putExtra(SERIAL_NUMBER, deviceListAdapter.getItem(arg2).getSerialNumber());
				intent.putExtra(SERVER, deviceListAdapter.getItem(arg2).getName());
				intent.putExtra(TYPE, isMovie ? MOVIE : TV_SHOW);
				intent.putExtra(FILESOURCE, FileSource.UPNP);
				startActivity(intent);
				finish();
			}
		});

		getApplicationContext().bindService(new Intent(this, WireUpnpService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (upnpService != null)
			upnpService.getRegistry().removeListener(deviceListRegistryListener);
		
		getApplicationContext().unbindService(serviceConnection);
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			upnpService = (AndroidUpnpService) service;

			//deviceListAdapter.clear();
			for (Device<?, ?, ?> device : upnpService.getRegistry().getDevices()) {
				deviceListRegistryListener.deviceAdded(new UpnpDevice(device));
			}

			// Getting ready for future device advertisements
			upnpService.getRegistry().addListener(deviceListRegistryListener);
			// Refresh device list
			upnpService.getControlPoint().search();
		}

		public void onServiceDisconnected(ComponentName className) {
			upnpService = null;
		}
	};

	public class DeviceListRegistryListener extends DefaultRegistryListener {
		@Override
		public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
			if (device.getType().getNamespace().equals("schemas-upnp-org")
					&& device.getType().getType().equals("MediaServer")) {
				deviceAdded(new UpnpDevice(device));
			}
		}

		@Override
		public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
			deviceRemoved(new UpnpDevice(device));
		}

		@Override
		public void localDeviceAdded(Registry registry, LocalDevice device) {
			deviceAdded(new UpnpDevice(device));
		}

		@Override
		public void localDeviceRemoved(Registry registry, LocalDevice device) {
			deviceRemoved(new UpnpDevice(device));
		}

		public void deviceAdded(final UpnpDevice di) {
			runOnUiThread(new Runnable() {
				public void run() {
					int position = deviceListAdapter.getPosition(di);
					if (position >= 0) {
						deviceListAdapter.remove(di);
						deviceListAdapter.insert(di, position);
					} else {
						deviceListAdapter.add(di);
					}
				}
			});
		}

		public void deviceRemoved(final UpnpDevice di) {
			runOnUiThread(new Runnable() {
				public void run() {
					deviceListAdapter.remove(di);
				}
			});
		}
	}

	private class UpnpDevice {

		private Device<?, ?, ?> device;

		public UpnpDevice(Device<?, ?, ?> device) {
			this.device = device;
		}

		@Override
		public String toString() {
			return device.getDetails().getFriendlyName();
		}

		private String getName() {
			return toString();
		}

		private String getSerialNumber() {
			return device.getDetails().getSerialNumber();
		}
	}
}