/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 27.11.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 *
 *
 * the unrar licence applies to all junrar source and binary distributions
 * you are not allowed to use this source to re-create the RAR compression algorithm
 *
 * Here some html entities which can be used for escaping javadoc tags:
 * "&":  "&#038;" or "&amp;"
 * "<":  "&#060;" or "&lt;"
 * ">":  "&#062;" or "&gt;"
 * "@":  "&#064;"
 */
package com.junrar.rarfile;

import com.junrar.io.Raw;
///import org.slf4j.Logger;
///import org.slf4j.LoggerFactory;


/**
 * extended archive CRC header
 *
 */
public class EAHeader extends SubBlockHeader {
    ///private static final Logger logger = LoggerFactory.getLogger(EAHeader.class);

    public static final short EAHeaderSize = 10;

    private final int unpSize;
    private byte unpVer;
    private byte method;
    private final int EACRC;

    public EAHeader(SubBlockHeader sb, byte[] eahead) {
        super(sb);
        int pos = 0;
        unpSize = Raw.readIntLittleEndian(eahead, pos);
        pos += 4;
        unpVer |= eahead[pos] & 0xff;
        pos++;
        method |= eahead[pos] & 0xff;
        pos++;
        EACRC = Raw.readIntLittleEndian(eahead, pos);
    }

    /**
     * @return the eACRC
     */
    public int getEACRC() {
        return EACRC;
    }

    /**
     * @return the method
     */
    public byte getMethod() {
        return method;
    }

    /**
     * @return the unpSize
     */
    public int getUnpSize() {
        return unpSize;
    }

    /**
     * @return the unpVer
     */
    public byte getUnpVer() {
        return unpVer;
    }

    @Override
	public void print() {
        super.print();
//        if (///logger.isInfoEnabled()) {
//            ///logger.info("unpSize: {}", unpSize);
//            ///logger.info("unpVersion: {}", unpVer);
//            ///logger.info("method: {}", method);
//            ///logger.info("EACRC: {}", EACRC);
//        }
    }
}

