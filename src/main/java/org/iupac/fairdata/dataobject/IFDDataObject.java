package org.iupac.fairdata.dataobject;

import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDProperty;
import org.iupac.fairdata.core.IFDRepresentableObject;

/**
 * A generic class indicating some sort of data. 
 * 
 * Allows for multiple named representations. 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFDDataObject extends IFDRepresentableObject<IFDDataObjectRepresentation> {

	private static String propertyPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_DATAOBJECT_FLAG);

	/**
	 * replaced and to be ignored, but does not need to be reported (MNOVA file)
	 */
	private boolean replaced;

	@Override
	protected String getIFDPropertyPrefix() {
		return propertyPrefix;
	}
	
	public IFDDataObject() {
		super(null, null);
	}

	
	
	
	/** This adds the IFD.dataObject properties for a any subclass.
	 * 
	 */
	@Override
	protected void setProperties(String subclassPropertyPrefix, String notKey) {
		super.setProperties(propertyPrefix, null);
		super.setProperties(subclassPropertyPrefix, null);
	}

	@Override
	public IFDDataObject clone() {
		IFDDataObject o = null;
		try {
			o = (IFDDataObject) super.clone();//getClass().newInstance();
		} catch (Exception e) {
			// ignore
		}
		return o;
	}

	 @Override
	public String getObjectFlag() {
		return IFDConst.IFD_DATAOBJECT_FLAG;
	};
	
	/**
	 * Turn IFD.dataobject.originating_sample_id into
	 * IFD.dataobject.fairspec.nmr.originating_sample_id
	 */
	@Override
	public IFDProperty setPropertyValue(String key, Object value) {
		IFDProperty p = IFDConst.getIFDProperty(htProps, key);
		if (p == null) {
			String prefix = getIFDPropertyPrefix();
			if (key.startsWith(IFDConst.IFD_PROPERTY_DATAOBJECT_FLAG) && !key.startsWith(prefix)) {
				key = prefix + key.substring(key.lastIndexOf("."));
			}
		}
		return super.setPropertyValue(key, value);
	}


	@Override
	public String toString() {
		return (label == null ? super.toString()
				: "[" + type + " " + (parentCollection != null) + " " + index + " id=" + id + " label=" + label + " rep[0]=" + (size() > 0 ? get(0) : null) + "]");
	}

	public void setReplaced() {
		replaced = true;
		setValid(false);
	}
	
	public boolean isReplaced() {
		return replaced;
	}


	private String instr_manufacturer_name;

	public String getInstrManufacturerName() {
		return instr_manufacturer_name;
	}

	private String expt_originating_sample_id;

	public String getOriginatingSampleID() {
		return expt_originating_sample_id;
	}

	private String expt_title;

	public String getExptTitle() {
		return expt_title;
	}

	/**
	 * an identifyable time stamp 
	 * 
	 */
	private Long expt_timestamp;

	public Long getTimestamp() {
		return expt_timestamp;
	}

	public String getTimestampDate() {
		return (expt_timestamp == null ? "" : Instant.ofEpochSecond(expt_timestamp.longValue()).toString());
	}

	@Override
	protected boolean checkFieldProperties(String key, Object value) {
		String lckey = key.toLowerCase();
		if (lckey.contains("dataobject")) {
			if (lckey.endsWith(IFDConst.IFD_DATAOBJECT_EXPT_TIMESTAMP_FLAG)) {
				expt_timestamp = (Long) value;
				return true;
			}
			if (lckey.endsWith(IFDConst.IFD_DATAOBJECT_EXPT_TITLE_FLAG)) {
				expt_title = (String) value;
				return true;
			}
			if (lckey.endsWith(IFDConst.IFD_DATAOBJECT_EXPT_ORIGINATING_SAMPLE_ID_FLAG)) {
				expt_originating_sample_id = (String) value;
				return true;
			}
			if (lckey.endsWith(IFDConst.IFD_DATAOBJECT_INSTR_MANUFACTURER_NAME_FLAG)) {
				instr_manufacturer_name = (String) value;
				return true;
			}
		}
		return super.checkFieldProperties(key, value);
	}

	
	public static boolean areIdentical(IFDDataObject o1, IFDDataObject o2) {
		Long l1 = o1.getTimestamp();
		Long l2 = o2.getTimestamp();
		if (l1 == null || l2 == null)
			return false;
		long t1 = l1.longValue();
		long t2 = l2.longValue();
		
		System.out.println(o1.toString() + t1);
		System.out.println(o2.toString() + t2);

		if (Math.abs(t2 - t1) > 86400) // within a day, but may not both be Z
			return false;
		Map<String, IFDProperty> props1 = o1.getProperties();
		Map<String, IFDProperty> props2 = o2.getProperties();
		
		for (Entry<String, IFDProperty> e : props1.entrySet()) {
			String key = e.getKey();
			Object v1 = e.getValue().getValue();
			// skip date and manufaturer
			if (v1 == null || key.indexOf("_date_") >= 0 || 
					key.indexOf("_manufacturer_") >= 0) {
				continue;
			}
			IFDProperty p2 = props2.get(key);
			Object v2;
			if (p2 != null && (v2 = p2.getValue()) != null) {
				if (!v1.equals(v2)) {
					return false;
				}
			}			
		}
		return true;
	}

	@Override
	protected void serializeTop(IFDSerializerI serializer) {
		super.serializeTop(serializer);
		if (expt_timestamp != null)
			serializer.addAttrInt("expt_timestamp", expt_timestamp);
		if (expt_originating_sample_id != null)
			serializer.addAttr("expt_originating_sample_id", expt_originating_sample_id);
		if (expt_title != null)
			serializer.addAttr("expt_title", expt_title);
		if (instr_manufacturer_name != null)
			serializer.addAttr("instr_manufacturer_name", instr_manufacturer_name);
	}


}
