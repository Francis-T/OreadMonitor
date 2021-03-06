package net.oukranos.oreadmonitor.types;

import net.oukranos.oreadmonitor.util.OreadLogger;

public class DataStoreObject {
	/* Get an instance of the OreadLogger class to handle logging */
	private static final OreadLogger OLog = OreadLogger.getInstance();
	
	private String _id = "";
	private String _type = "";
	private Object _obj = null;
	
	private DataStoreObject(String id, String type, Object obj) {
		this._id = id;
		this._type = type;
		this._obj = obj;
		return;
	}
	
	public static DataStoreObject createNewInstance(String id, String type, Object obj) {
		if ((id == null) || (type == null) || (obj == null)) {
			OLog.err("Invalid input parameter/s" +
					" in DataStoreObject.createNewInstance()");
			return null;
		}
		
		if (id.isEmpty() || type.isEmpty()) {
			OLog.err("Blank input parameter/s" +
					" in DataStoreObject.createNewInstance()");
			return null;
		}
		
		return (new DataStoreObject(id, type, obj));
	}
	
	public String getId() {
		return this._id;
	}
	
	public String getType() {
		return this._type;
	}
	
	public Object getObject() {
		return this._obj;
	}
	
	public Status setObject(Object newObj) {
		this._obj = newObj;
		return Status.OK;
	}
}
