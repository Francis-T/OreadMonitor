package net.oukranos.oreadmonitor.fragments;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import net.oukranos.oreadmonitor.R;
import net.oukranos.oreadmonitor.interfaces.OreadServiceApi;
import net.oukranos.oreadmonitor.interfaces.OreadServiceListener;
import net.oukranos.oreadmonitor.types.ProcChangeInfo;
import net.oukranos.oreadmonitor.types.ProcStateChangeInfo;
import net.oukranos.oreadmonitor.types.TaskChangeInfo;
import net.oukranos.oreadmonitor.types.WaterQualityData;
import net.oukranos.oreadmonitor.util.OreadLogger;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class FragmentReadings extends Fragment {
	/* Get an instance of the OreadLogger class to handle logging */
	private static final OreadLogger OLog = OreadLogger.getInstance();
	
	private View _viewRef = null;
	private OreadServiceApi _serviceAPI = null;
	private HashMap<String, SensorDataGroup> _fieldMap = null;
	private WaterQualityDataMatrix _wqdMatrix = null;
	
	private ReadFragmentSvcListener _listener = null;
	private Activity _parent = null;
	
	private TextView _logView = null;
	
	public FragmentReadings() {
		_fieldMap = new HashMap<String, FragmentReadings.SensorDataGroup>();
		return;
	}
	
	public FragmentReadings(OreadServiceApi api) {
		_fieldMap = new HashMap<String, FragmentReadings.SensorDataGroup>();
		this._serviceAPI = api;
		return;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (container == null) {
			// We have different layouts, and in one of them this
			// fragment's containing frame doesn't exist. The fragment
			// may still be created from its saved state, but there is
			// no reason to try to create its view hierarchy because it
			// won't be displayed. Note this is not needed -- we could
			// just run the code below, where we would create and return
			// the view hierarchy; it would just never be used.
			return null;
		}
		_viewRef = inflater.inflate(R.layout.frag_readings, container, false);
		
		/* Setup click listeners for the start button */
		Button btn_start = (Button) _viewRef.findViewById(R.id.btn_start);
		btn_start.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) { startService(); }}
		);

		/* Setup click listeners for the stop button */
		Button btn_stop = (Button) _viewRef.findViewById(R.id.btn_stop);
		btn_stop.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) { stopService(); }}
		);

		_parent = this.getActivity();

//		/* Setup the double tap listener */
//		final GestureDetector gestureDetect = 
//				new GestureDetector(_parent, new DoubleTapListener());
		
		_logView = (TextView) _viewRef.findViewById(R.id.txt_log);
//		_logView.setOnTouchListener( new OnTouchListener() {
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
//				// TODO Auto-generated method stub
//				if (gestureDetect.onTouchEvent(event)) {
//					return false;
//				}
//				return true;
//			}
//		});
		
		/* Initialize the field mappings */
		initializeFieldMap(_viewRef);
		
		WaterQualityData data = null;
		
		if (_serviceAPI != null) {
			OLog.info("Service available");
			
			/* Attempt to retrieve the data from the service */
			try {
				data = _serviceAPI.getData();
			} catch (RemoteException e) {
				OLog.err("Failed to get data: " + e.getMessage());
				return _viewRef;
			} catch (Exception e) {
				OLog.err("Failed to get data: " + e.getMessage());
				return _viewRef;
			}
			
			/* Start a field map update task */
			new FieldMapUpdateTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data);
		}
		
		return _viewRef;
	}

	@Override
	public void onDestroyView() {
		if (this._serviceAPI != null) {
			if (_listener != null) {
				try {
					_serviceAPI.removeListener(_listener);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
		
		super.onDestroyView();
	}

	@Override
  	public void onActivityCreated(Bundle savedInstanceState) {
	  	super.onActivityCreated(savedInstanceState);
	  	return;
  	}
	
//	private class DoubleTapListener extends SimpleOnGestureListener {
//		@Override
//		public boolean onDoubleTapEvent(MotionEvent e) {
//			if (_parent != null) {
//				String logText = "";
//				
//				/* TODO HACKY */
//				if (_serviceAPI != null) {
//					try {
//						logText = _serviceAPI.getLogs(5);
//					} catch (RemoteException e1) {
//						e1.printStackTrace();
//					}
//				} else {
//					OLog.err("Service Unavailable");
//				}
//				
//				_logView.setText(logText);
//			}
//			return super.onDoubleTapEvent(e);
//		}
//		
//	}
	  
	/********************/
	/** Public Methods **/
	/********************/
	public OreadServiceApi getServiceHandle() {
		return this._serviceAPI;
	}
	
	public void setServiceHandle(OreadServiceApi api) {
		this._serviceAPI = api;
		return;
	}
  
	/*********************/
	/** Private Methods **/
	/*********************/
  	private void startService() {
		if (_serviceAPI != null) {
			
			if (_listener == null) {
				_listener = new ReadFragmentSvcListener();
			}
			
			try {
//				OreadServiceControllerStatus cs = _serviceAPI.getStatus();
//				if (cs == null) {
//					_serviceAPI.start();
//					_serviceAPI.addListener(_listener);
//				} else if (cs.getState() == ControllerState.UNKNOWN) {
//					_serviceAPI.start();
//					_serviceAPI.addListener(_listener);
//				}
				_serviceAPI.start();
				_serviceAPI.addListener(_listener);
				
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			OLog.err("Service Unavailable");
		}
		
		return;
  	}
  	
  	private void stopService() {
		if (_serviceAPI != null) {
			try {
				_serviceAPI.stop();
				
				if (_listener != null) {
					_serviceAPI.removeListener(_listener);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			OLog.err("Service Unavailable");
		}
		
		return;
  	}
  	
  	private void initializeFieldMap(View v) {
  		TextView tv = null;
  		SensorDataGroup sdg = null;

  		/* Find the pH text fields and write them to the field map */
  		sdg = new SensorDataGroup("pH");
  		tv = (TextView) v.findViewById(R.id.txt_ph_val);
  		sdg.setValueField(tv);
  		
  		_fieldMap.put("pH", sdg);

  		/* Find the DO2 text fields and write them to the field map */
  		sdg = new SensorDataGroup("DO2");
  		
  		tv = (TextView) v.findViewById(R.id.txt_do2_val);
  		sdg.setValueField(tv);
  		
  		_fieldMap.put("DO2", sdg);

  		/* Find the EC text fields and write them to the field map */
  		sdg = new SensorDataGroup("Cond");
  		
  		tv = (TextView) v.findViewById(R.id.txt_cond_val);
  		sdg.setValueField(tv);
  		
  		_fieldMap.put("Cond", sdg);

  		/* Find the turbidity text fields and write them to the field map */
  		sdg = new SensorDataGroup("Turb");
  		
  		tv = (TextView) v.findViewById(R.id.txt_turb_val);
  		sdg.setValueField(tv);
  		
  		_fieldMap.put("Turb", sdg);

  		/* Find the temperature text fields and write them to the field map */
  		sdg = new SensorDataGroup("Temp");
  		
  		tv = (TextView) v.findViewById(R.id.txt_temp_val);
  		sdg.setValueField(tv);
  		
  		_fieldMap.put("Temp", sdg);
  		
  		return;
  	}

	/*******************/
	/** Inner Classes **/
	/*******************/
  	private class SensorDataGroup {
  		private String _sensor = "";
  		private TextView _txtValue = null;
  		private TextView _txtStab = null;
  		private TextView _txtResp = null;
  		
  		public SensorDataGroup(String sensorName) {
  			_sensor = sensorName;
  			return;
  		}
  		
  		public String getSensor() {
  			return _sensor;
  		}
  		
  		public TextView getValueField() {
  			return _txtValue;
  		}
  		
  		public TextView getVarianceField() {
  			return _txtStab;
  		}
  		
  		public TextView getResponseTimeField() {
  			return _txtResp;
  		}
  		
  		public void setValueField(TextView field) {
  			_txtValue = field;
  			return;
  		}
  		
  		public void setVarianceField(TextView field) {
  			_txtStab = field;
  			return;
  		}
  		
  		public void setResponseTimeField(TextView field) {
  			_txtResp = field;
  			return;
  		}
  	}
  	
  	private class WaterQualityDataMatrix {
  		private static final int MAX_LIST_LEN = 10;
  		
  		private List<Float> _phDataList = null;
  		private List<Float> _do2DataList = null;
  		private List<Float> _condDataList = null;
  		private List<Float> _tempDataList = null;
  		private List<Float> _turbDataList = null;
  		
  		private List<Float> _varianceList = null;
  		
  		public WaterQualityDataMatrix() {
  			_phDataList = new ArrayList<Float>();
  			_do2DataList = new ArrayList<Float>();
  			_condDataList = new ArrayList<Float>();
  			_tempDataList = new ArrayList<Float>();
  			_turbDataList = new ArrayList<Float>();
  			_varianceList = new ArrayList<Float>();
  			
  			_varianceList.add((float) 0.0);
  			_varianceList.add((float) 0.0);
  			_varianceList.add((float) 0.0);
  			_varianceList.add((float) 0.0);
  			_varianceList.add((float) 0.0);
  			
  			return;
  		}
  		
  		public void storeData(WaterQualityData data) {
  			
  			_phDataList.add((float) data.pH);
  			if (_phDataList.size() > MAX_LIST_LEN) {
  				_phDataList.remove(0);
  			}
  			
  			_do2DataList.add((float) data.dissolved_oxygen);
  			if (_do2DataList.size() > MAX_LIST_LEN) {
  				_do2DataList.remove(0);
  			}
  			
  			_condDataList.add((float) data.conductivity);
  			if (_condDataList.size() > MAX_LIST_LEN) {
  				_condDataList.remove(0);
  			}
  			
  			_turbDataList.add((float) data.turbidity);
  			if (_turbDataList.size() > MAX_LIST_LEN) {
  				_turbDataList.remove(0);
  			}
  			
  			_tempDataList.add((float) data.temperature);
  			if (_tempDataList.size() > MAX_LIST_LEN) {
  				_tempDataList.remove(0);
  			}
  			
  			_varianceList.set(0, (float) calculateVariance(_phDataList));
  			_varianceList.set(1, (float) calculateVariance(_do2DataList));
  			_varianceList.set(2, (float) calculateVariance(_condDataList));
  			_varianceList.set(3, (float) calculateVariance(_turbDataList));
  			_varianceList.set(4, (float) calculateVariance(_tempDataList));
  			
  			return;
  		}
  		
  		public float getVariance(int idx) {
  			float variance = (float) -1.0;
  			
  			switch(idx) {
  				case 0:
  					variance = _varianceList.get(0);
  					break;
  				case 1:
  					variance = _varianceList.get(1);
  					break;
  				case 2:
  					variance = _varianceList.get(2);
  					break;
  				case 3:
  					variance = _varianceList.get(3);
  					break;
  				case 4:
  					variance = _varianceList.get(4);
  					break;
  				default:
  					break;
  			}
  			
  			return variance;
  		}
  		
  		public List<Float> getVarianceList() {
  			return this._varianceList;
  		}
  		
  		/* Private Methods */
  		private double calculateVariance(List<Float> dataList) {
  			if (dataList == null) {
  				return -1.0;
  			}
  			
  			double average = 0.0;
  			int total = dataList.size();
  			
  			if (total > 0) {
  				average /= (double) total;
  			} else {
					return -1.0;
  			}
  			
  			double variance = 0.0;

  			for (double d : dataList) {
  				variance += (d - average)*(d - average);
  			}
  			
  			if (total > 0) {
  				variance /= (double) total;
  			} else {
  				return -1.0;
  			}
  			
  			return variance;
  		}
  	}
  	
  	private String _procString = "";
  	private String _taskString = "";
  	private String _stateString = "";
			
    private class ReadFragmentSvcListener extends OreadServiceListener.Stub {
		@Override
		public void handleWaterQualityData() throws RemoteException {
			WaterQualityData data = null;
			
			if (_serviceAPI == null) {
				OLog.err("Service unavailable");
				return;
			}
			
			/* Attempt to retrieve the data from the service */
			try {
				data = _serviceAPI.getData();
			} catch (RemoteException e) {
				OLog.err("Failed to get data");
			}
			
			/* Start a field map update task */
			new FieldMapUpdateTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data);
			
			return;
		}

		@Override
		public void handleOperationProcStateChanged() throws RemoteException {
			if (_serviceAPI == null) {
				OLog.err("Service unavailable");
				return;
			}
			
			ProcStateChangeInfo info = null;
			try {
				info = _serviceAPI.getProcStates();
			} catch (RemoteException e) {
				OLog.err("Failed to get proc states");
			}
			
			if (info == null) {
				return;
			}
			
			/* Update the display to show state changes */
			new AsyncTask<ProcStateChangeInfo, Void, String>() {
				@Override
				protected String doInBackground(ProcStateChangeInfo... stateInfo) {
					_stateString = stateInfo[0].getStateInfo();
					
					/* Build the string */
					String msg = "";
					msg += "Proc: " + _procString + "\n";
					msg += "Task: " + _taskString + "\n";
					msg += "==================================\n";
					msg += _stateString;
					
					return msg;
				}

				@Override
				protected void onPostExecute(String info) {
					if (info != null) {
						_logView.setText(info);
					}
					return;
				}
				
			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, info);
			
			return;
		}

		@Override
		public void handleOperationProcChanged() throws RemoteException {
			if (_serviceAPI == null) {
				OLog.err("Service unavailable");
				return;
			}
			
			ProcChangeInfo info = null;
			try {
				info = _serviceAPI.getProc();
			} catch (RemoteException e) {
				OLog.err("Failed to get proc");
			}
			
			if (info == null) {
				return;
			}
			
			/* Update the display to show state changes */
			new AsyncTask<ProcChangeInfo, Void, String>() {
				@Override
				protected String doInBackground(ProcChangeInfo... procInfo) {
					_procString = procInfo[0].getProcedure();
					
					/* Build the string */
					String msg = "";
					msg += "Proc: " + _procString + "\n";
					msg += "Task: " + _taskString + "\n";
					msg += "==================================\n";
					msg += _stateString;
					
					return msg;
				}

				@Override
				protected void onPostExecute(String info) {
					if (info != null) {
						_logView.setText(info);
					}
					return;
				}
				
			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, info);
			
			return;
		}

		@Override
		public void handleOperationTaskChanged() throws RemoteException {
			if (_serviceAPI == null) {
				OLog.err("Service unavailable");
				return;
			}
			
			TaskChangeInfo info = null;
			try {
				info = _serviceAPI.getTask();
			} catch (RemoteException e) {
				OLog.err("Failed to get task");
			}
			
			if (info == null) {
				return;
			}
			
			/* Update the display to show state changes */
			new AsyncTask<TaskChangeInfo, Void, String>() {
				@Override
				protected String doInBackground(TaskChangeInfo... taskInfo) {
					_taskString = taskInfo[0].getTask();
					
					/* Build the string */
					String msg = "";
					msg += "Proc: " + _procString + "\n";
					msg += "Task: " + _taskString + "\n";
					msg += "==================================\n";
					msg += _stateString;
					
					return msg;
				}

				@Override
				protected void onPostExecute(String info) {
					if (info != null) {
						_logView.setText(info);
					}
					return;
				}
				
			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, info);
			
			return;
		}
	}
    
    private class FieldMapUpdateTask extends AsyncTask<WaterQualityData, Void, WaterQualityData> {

		@Override
		protected WaterQualityData doInBackground(WaterQualityData... data) {
			if (data == null) {
				return null;
			}
			
			/* Initialize the data matrix if it hasn't been initialized yet */
			if (_wqdMatrix == null) {
				_wqdMatrix = new WaterQualityDataMatrix();
			}
			
			_wqdMatrix.storeData(data[0]);
			
			return data[0];
		}

		@Override
		protected void onPostExecute(WaterQualityData data) {
			super.onPostExecute(data);
			
			/* Update the fields */
			String keyVal[] = { "pH", "DO2", "Cond", "Turb", "Temp" };
			String unitVal[] = { "", "mg/L", "uS/cm", "NTU", "deg C" };
			double readVal[] = { data.pH, 
								 data.dissolved_oxygen,
								 data.conductivity,
								 data.turbidity,
								 data.temperature };
			int len = keyVal.length;
			
			for ( int i = 0; i < len; i++ ) {
				TextView valField = _fieldMap.get(keyVal[i]).getValueField();
				if (valField != null) {
					valField.setText( Float.toString((float)readVal[i]) 
														+ " " + unitVal[i] );
				}
				
//				valField = _fieldMap.get(keyVal[i]).getVarianceField();
//				if (valField != null) {
//					valField.setText( Float.toString(_wqdMatrix.getVariance(i)) 
//														+ " " + unitVal[i] );
//				}
			}
			
			logToFile( " pH: " + Float.toString((float)data.pH) + 
					   " DO2: " + Float.toString((float)data.dissolved_oxygen) +
					   " COND: " + Float.toString((float)data.conductivity) +
					   " TURB: " + Float.toString((float)data.turbidity) +
					   " TEMP: " + Float.toString((float)data.temperature) );
		}
    	
    }
    

	public void logToFile(String message) {
		final String root_sd = Environment.getExternalStorageDirectory().toString();
		String savePath = root_sd + "/OreadPrototype";
		File saveDir = new File(savePath);
		
		if (!saveDir.exists())
		{
			saveDir.mkdirs();
		}
		
		Calendar calInstance = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		int year = calInstance.get(Calendar.YEAR);
		int month = calInstance.get(Calendar.MONTH) + 1;	/* January == 0, so we always add 1 to get sensible months */
		int day = calInstance.get(Calendar.DAY_OF_MONTH);
		int hour = calInstance.get(Calendar.HOUR_OF_DAY);
		int min = calInstance.get(Calendar.MINUTE);
		int sec = calInstance.get(Calendar.SECOND);
		
		String dateStr = Integer.toString(year) + "." + Integer.toString(month) + "." + Integer.toString(day);
		String hourStr = (hour < 10 ? "0" + Integer.toString(hour) : Integer.toString(hour));
		String minStr = (min < 10 ? "0" + Integer.toString(min) : Integer.toString(min));
		String secStr = (sec < 10 ? "0" + Integer.toString(sec) : Integer.toString(sec));
		
		String logMessage = "[" + dateStr + " " + hourStr + ":" + minStr + "." + secStr + "]" + message + "\n";
		
		File saveFile = null;
		saveFile = new File(saveDir, ("OREAD_Readings.txt"));
		
		if (saveFile.exists() == false) {
			try {
				if (!saveFile.createNewFile())
				{
					Log.e("logToFile", "Error: Failed to create save file!");
					return;
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		try {
			FileOutputStream saveFileStream = new FileOutputStream(saveFile, true);
			saveFileStream.write(logMessage.getBytes());
			
			saveFileStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return;
	}
	
}
