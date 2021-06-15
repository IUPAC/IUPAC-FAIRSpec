package org.iupac.fairspec.api;

/**
 * A class that implements IFSAbstractObjectI, though still an IFSObject, does
 * not allow for representations. For example, an IFSCollection is a description
 * of a set of digital items (ultimately an array of bytes), but it itself is
 * not representable as such. Or, to put it another way, it is "pure metadata."
 * 
 * Similarly, an IFSStructureDataAssociation links IFSStructure and
 * IFSDataObject instances with each other, but it itself does not have a byte
 * representation other than just a metadata description.
 * 
 * This interface is the complement of IFSRepresentableObjectI. 
 * 
 * 
 * 
 * @author hansonr
 *
 */
public interface IFSAbstractObjectI {

}
