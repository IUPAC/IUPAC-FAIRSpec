package org.iupac.fairspec.common;

import java.util.Map;
import java.util.zip.ZipEntry;

import org.iupac.fairspec.api.IFSObjectAPI;

@SuppressWarnings("serial")
public class IFSFindingAid extends IFSCollection<IFSCollection<?>> {

	public Map<String, ZipEntry> zipContents;

	public IFSFindingAid(String name) {
		super(name, IFSObjectAPI.ObjectType.FindingAid);
	}

}