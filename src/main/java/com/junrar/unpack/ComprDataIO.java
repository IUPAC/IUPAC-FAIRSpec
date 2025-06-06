/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 31.05.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
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
package com.junrar.unpack;

import com.junrar.Archive;
import com.junrar.UnrarCallback;
import com.junrar.crc.RarCRC;
import com.junrar.crypt.Rijndael;
import com.junrar.exception.CrcErrorException;
import com.junrar.exception.InitDeciphererFailedException;
import com.junrar.exception.RarException;
import com.junrar.io.RawDataIo;
import com.junrar.rarfile.FileHeader;
import com.junrar.volume.Volume;

import javax.crypto.Cipher;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * DOCUMENT ME
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class ComprDataIO {

    private final Archive archive;

    private long unpPackedSize;

    private boolean testMode;

    private boolean skipUnpCRC;

    private OutputStream outputStream;

    private FileHeader subHead;

    // cryptData Crypt;
    // cryptData Decrypt;
    private boolean packVolume;

    private boolean unpVolume;

    private boolean nextVolumeMissing;

    private long totalPackRead;

    private long unpArcSize;

    private long curPackRead, curPackWrite, curUnpRead, curUnpWrite;

    private long processedArcSize, totalArcSize;

    private long packFileCRC, unpFileCRC, packedCRC;

    private int encryption;

    private int decryption;

    private RawDataIo underlyingDataIo;

    public ComprDataIO(Archive arc) {
        this.archive = arc;
    }

    public void init(OutputStream outputStream) {
        this.outputStream = outputStream;
        unpPackedSize = 0;
        testMode = false;
        skipUnpCRC = false;
        packVolume = false;
        unpVolume = false;
        nextVolumeMissing = false;
        // command = null;
        encryption = 0;
        decryption = 0;
        totalPackRead = 0;
        curPackRead = curPackWrite = curUnpRead = curUnpWrite = 0;
        packFileCRC = unpFileCRC = packedCRC = 0xffffffff;
        subHead = null;
        processedArcSize = totalArcSize = 0;
    }

    public void init(FileHeader hd) throws IOException, RarException {
        long startPos = hd.getPositionInFile() + hd.getHeaderSize(archive.isEncrypted());
        unpPackedSize = hd.getFullPackSize();
        archive.getChannel().setPosition(startPos);
        this.underlyingDataIo = new RawDataIo(archive.getChannel());
        subHead = hd;
        curUnpRead = 0;
        curPackWrite = 0;
        packedCRC = 0xFFffFFff;

        if (hd.isEncrypted()) {
            try {
                Cipher cipher = Rijndael.buildDecipherer(archive.getPassword(), hd.getSalt());
                this.underlyingDataIo.setCipher(cipher);
            } catch (Exception e) {
                throw new InitDeciphererFailedException(e);
            }
        }
    }

    public int unpRead(byte[] addr, int offset, int count) throws IOException, RarException {
        int retCode = 0, totalRead = 0;
        while (count > 0) {
            int readSize = (count > unpPackedSize) ? (int) unpPackedSize : count;
            retCode = underlyingDataIo.read(addr, offset, readSize);
            if (retCode < 0) {
                throw new EOFException();
            }

            if (subHead.isSplitAfter()) {
                packedCRC = RarCRC.checkCrc((int) packedCRC, addr, offset, retCode);
            }

            totalRead += retCode;
            count -= retCode;
            offset += retCode;
            unpPackedSize -= retCode;
            curUnpRead += retCode;

            archive.bytesReadRead(retCode);

            if (unpPackedSize == 0 && subHead.isSplitAfter()) {
                Volume nextVolume = archive.getVolumeManager().nextVolume(archive, archive.getVolume());
                if (nextVolume == null) {
                    nextVolumeMissing = true;
                    return -1;
                }

                FileHeader hd = this.getSubHeader();
                if (hd.getUnpVersion() >= 20 && hd.getFileCRC() != 0xffffffff
                        && this.getPackedCRC() != ~hd.getFileCRC()) {
                    throw new CrcErrorException();
                }
                UnrarCallback callback = archive.getUnrarCallback();
                if ((callback != null) && !callback.isNextVolumeReady(nextVolume)) {
                    return -1;
                }
                archive.setVolume(nextVolume);
                hd = archive.nextFileHeader();
                if (hd == null) {
                    return -1;
                }
                this.init(hd);
            } else {
                break;
            }
        }

        if (retCode != -1) {
            retCode = totalRead;
        }
        return retCode;

    }

    public void unpWrite(byte[] addr, int offset, int count) throws IOException {
        if (!testMode) {
            // DestFile->Write(Addr,Count);
            outputStream.write(addr, offset, count);
        }

        curUnpWrite += count;

        if (!skipUnpCRC) {
            if (archive.isOldFormat()) {
                unpFileCRC = RarCRC.checkOldCrc((short) unpFileCRC, addr, count);
            } else {
                unpFileCRC = RarCRC.checkCrc((int) unpFileCRC, addr, offset, count);
            }
        }
        // if (!skipArcCRC) {
        // archive.updateDataCRC(Addr, offset, ReadSize);
        // }
    }

    public void setPackedSizeToRead(long size) {
        unpPackedSize = size;
    }

    public void setTestMode(boolean mode) {
        testMode = mode;
    }

    public void setSkipUnpCRC(boolean skip) {
        skipUnpCRC = skip;
    }

    public void setSubHeader(FileHeader hd) {
        subHead = hd;

    }

    public long getCurPackRead() {
        return curPackRead;
    }

    public void setCurPackRead(long curPackRead) {
        this.curPackRead = curPackRead;
    }

    public long getCurPackWrite() {
        return curPackWrite;
    }

    public void setCurPackWrite(long curPackWrite) {
        this.curPackWrite = curPackWrite;
    }

    public long getCurUnpRead() {
        return curUnpRead;
    }

    public void setCurUnpRead(long curUnpRead) {
        this.curUnpRead = curUnpRead;
    }

    public long getCurUnpWrite() {
        return curUnpWrite;
    }

    public void setCurUnpWrite(long curUnpWrite) {
        this.curUnpWrite = curUnpWrite;
    }

    public int getDecryption() {
        return decryption;
    }

    public void setDecryption(int decryption) {
        this.decryption = decryption;
    }

    public int getEncryption() {
        return encryption;
    }

    public void setEncryption(int encryption) {
        this.encryption = encryption;
    }

    public boolean isNextVolumeMissing() {
        return nextVolumeMissing;
    }

    public void setNextVolumeMissing(boolean nextVolumeMissing) {
        this.nextVolumeMissing = nextVolumeMissing;
    }

    public long getPackedCRC() {
        return packedCRC;
    }

    public void setPackedCRC(long packedCRC) {
        this.packedCRC = packedCRC;
    }

    public long getPackFileCRC() {
        return packFileCRC;
    }

    public void setPackFileCRC(long packFileCRC) {
        this.packFileCRC = packFileCRC;
    }

    public boolean isPackVolume() {
        return packVolume;
    }

    public void setPackVolume(boolean packVolume) {
        this.packVolume = packVolume;
    }

    public long getProcessedArcSize() {
        return processedArcSize;
    }

    public void setProcessedArcSize(long processedArcSize) {
        this.processedArcSize = processedArcSize;
    }

    public long getTotalArcSize() {
        return totalArcSize;
    }

    public void setTotalArcSize(long totalArcSize) {
        this.totalArcSize = totalArcSize;
    }

    public long getTotalPackRead() {
        return totalPackRead;
    }

    public void setTotalPackRead(long totalPackRead) {
        this.totalPackRead = totalPackRead;
    }

    public long getUnpArcSize() {
        return unpArcSize;
    }

    public void setUnpArcSize(long unpArcSize) {
        this.unpArcSize = unpArcSize;
    }

    public long getUnpFileCRC() {
        return unpFileCRC;
    }

    public void setUnpFileCRC(long unpFileCRC) {
        this.unpFileCRC = unpFileCRC;
    }

    public boolean isUnpVolume() {
        return unpVolume;
    }

    public void setUnpVolume(boolean unpVolume) {
        this.unpVolume = unpVolume;
    }

    public FileHeader getSubHeader() {
        return subHead;
    }
}
