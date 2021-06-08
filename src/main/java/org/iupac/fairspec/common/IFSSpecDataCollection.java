package org.iupac.fairspec.common;

@SuppressWarnings("serial")
public class IFSSpecDataCollection extends IFSCollection<IFSSpecData> {

	private ObjectType dataType;


	public IFSSpecDataCollection(String name, ObjectType dataType) {
		super(name, ObjectType.SpecDataCollection);
		this.dataType = dataType;
	}

	
	public void addSpecData(IFSSpecData sd) {
		super.add(sd);
	}
	
	public ObjectType getDataType() {
		return dataType;
	}

}