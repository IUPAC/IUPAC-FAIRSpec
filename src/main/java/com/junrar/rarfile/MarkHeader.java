/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 24.05.2007
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
 * the header to recognize a file to be a rar archive
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class MarkHeader extends BaseBlock {

//    private final Logger logger = LoggerFactory.getLogger(MarkHeader.class.getName());

    private RARVersion version;

    public MarkHeader(BaseBlock bb) {
        super(bb);
    }
    public boolean isValid() {
        if (!(getHeadCRC() == 0x6152)) {
            return false;
        }
        if (!(getHeaderType() == UnrarHeadertype.MarkHeader)) {
            return false;
        }
        if (!(getFlags() == 0x1a21)) {
            return false;
        }
        return getHeaderSize(false) == BaseBlockSize;
    }

    public boolean isSignature() {
        byte[] d = new byte[BaseBlock.BaseBlockSize];
        Raw.writeShortLittleEndian(d, 0, headCRC);
        d[2] = headerType;
        Raw.writeShortLittleEndian(d, 3, flags);
        Raw.writeShortLittleEndian(d, 5, headerSize);

        if (d[0] == 0x52) {
            if (d[1] == 0x45 && d[2] == 0x7e && d[3] == 0x5e) {
                version = RARVersion.OLD;
            } else if (d[1] == 0x61 && d[2] == 0x72 && d[3] == 0x21 && d[4] == 0x1a && d[5] == 0x07) {
                if (d[6] == 0x00) {
                    version = RARVersion.V4;
                } else if (d[6] == 0x01) {
                    version = RARVersion.V5;
                }
            }
        }
        return version == RARVersion.OLD || version == RARVersion.V4;
    }

    public boolean isOldFormat() {
        return RARVersion.isOldFormat(version);
    }

    public RARVersion getVersion() {
        return version;
    }

    @Override
	public void print() {
        super.print();
//        if (///logger.isInfoEnabled()) {
//            ///logger.info("valid: {}", isValid());
//        }
    }
}
