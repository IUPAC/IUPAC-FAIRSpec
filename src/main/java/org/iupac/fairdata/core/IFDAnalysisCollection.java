package org.iupac.fairdata.core;

@SuppressWarnings("serial")
public class IFDAnalysisCollection extends IFDCollection<IFDObject<?>> {

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDAnalysis.class };
	}
	
	public IFDAnalysisCollection(String name, IFDAnalysis data) {
		this(name);
		add(data);
	}

	public IFDAnalysisCollection(String name) {
		super(name, null);
	}

	public IFDAnalysisCollection(String name, String type) {
		super(name, type);
	}
	
	@Override
	public IFDAnalysis get(int i) {
		return (IFDAnalysis) super.get(i);
	}

	public boolean add(IFDAnalysis sd) {
		if (contains(sd))
			return false;
		super.add(sd);
		return true;		
	}
	
	@Override
	public boolean add(IFDObject<?> t) {
		System.err.println("IFDObject error: " + t);
		return false;
	}

}
