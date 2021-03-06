package net.oukranos.oreadmonitor.types.config;

import java.util.ArrayList;
import java.util.List;

import net.oukranos.oreadmonitor.types.GenericData;
import net.oukranos.oreadmonitor.types.Status;


public class Data extends GenericData {
	private List<Data> _compoundDataList = null;
	
	public Data(String id, String type, String value) {
		super(id, type, value);
		
		if (this._type.equals("compound") == true) {
			_compoundDataList = new ArrayList<Data>();
		}
		
		return;
	}
	
	@Override
	public String getValue() {
		return (super.getValue().toString());
	}

	@Override
	public Status setType(String type) {
		if (type == null) {
			return Status.FAILED;
		}
		
		if (type.isEmpty() == true) {
			return Status.FAILED;
		}
		
		/* If this is being changed into a non-compound data, discard the contents oft
		 * the compound data list first */
		if ((this.getType().equals("compound") == true) &&
				(type.equals("compound") == false)) {
			this._compoundDataList.clear();
			this._compoundDataList = null;
		}

		/* If this is being changed into a compound data, init the compound data list first */
		if ((this.getType().equals("compound") == false) &&
				(type.equals("compound") == true)) {
			this._compoundDataList = new ArrayList<Data>();
		}
		
		super.setType(type);
		
		return Status.OK;
	}

    public Status addData(String id, String type, String value) {
    	if ((id == null) || (type == null) || (value == null)) {
    		return Status.FAILED;
    	}

    	if ((id.isEmpty() == true) || (type.isEmpty() == true)) {
    		return Status.FAILED;
    	}

        /* Check if our data list already contains a data w/ the same id */
        if (_compoundDataList == null) {
            return Status.FAILED;
        }
        for (Data d : _compoundDataList) {
            if (d.getId().equals(id) == true) {
                return Status.FAILED;
            }
        }

        _compoundDataList.add(new Data(id, type, value));

    	return Status.OK;
    }
	
	public String toString() {
		String dataStr = "{ data";
		
		dataStr += "id=\"" + this.getId() + "\" ";
		dataStr += "type=\"" + this.getType() + "\" ";
		dataStr += "value=\"" + this.getValue() + "\" ";
		
		if (this.getType().equals("compound") == true) {
			dataStr += "data-list=[ ";
			
			for (Data d : this._compoundDataList) {
				dataStr += "\n";
				dataStr += "    " + d.toString();
			}
			
			dataStr += " ]";
		}
		
		dataStr += " }";
		
		return dataStr;
	}
}
