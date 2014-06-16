/**
 * 
 */
package io.github.vladast.avrcommunicator.db.dao;

/**
 * @author vladimir.stankovic
 *
 */
public class DeviceDAO extends EventRecorderDAO {

	/**
	 * @param onDatabaseRequestListener
	 */
	public DeviceDAO(OnDatabaseRequestListener onDatabaseRequestListener) {
		super(onDatabaseRequestListener);
		// TODO Auto-generated constructor stub
	}

	/*
	create table device (
		    id integer primary key autoincrement not null, 
		    type integer not null, 
		    code integer, 
		    description text, 
		    vid integer, 
		    pid integer
		);
	*/
	
	private int mType;
	
	public int getType() {
		return mType;
	}
	
	public void setType(int type) {
		mType = type;
	}
	
	private int mCode;
	
	public int getCode() {
		return mCode;
	}
	
	public void setCode(int code) {
		mCode = code;
	}
	
	private String mDescription;
	
	public String getDescription() {
		return mDescription;
	}
	
	public void setDescription(String description) {
		mDescription = description;
	}
	
	private int mVendorId;
	
	public int getVendorId() {
		return mVendorId;
	}
	
	public void setVendorId(int vendorId) {
		mVendorId = vendorId;
	}
	
	private int mProductId;
	
	public int getProductId() {
		return mProductId;
	}
	
	public void setProductId(int productId) {
		mProductId = productId;
	}
}
