/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 22.05.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 *
 * the unrar licence applies to all junrar source and binary distributions
 * you are not allowed to use this source to re-create the RAR compression
 * algorithm
 *
 * Here some html entities which can be used for escaping javadoc tags:
 * "&":  "&#038;" or "&amp;"
 * "<":  "&#060;" or "&lt;"
 * ">":  "&#062;" or "&gt;"
 * "@":  "&#064;"
 */
package com.junrar;

import com.junrar.crypt.Rijndael;
import com.junrar.exception.BadRarArchiveException;
import com.junrar.exception.CorruptHeaderException;
import com.junrar.exception.CrcErrorException;
import com.junrar.exception.HeaderNotInArchiveException;
import com.junrar.exception.InitDeciphererFailedException;
import com.junrar.exception.MainHeaderNullException;
import com.junrar.exception.NotRarArchiveException;
import com.junrar.exception.RarException;
import com.junrar.exception.UnsupportedRarEncryptedException;
import com.junrar.exception.UnsupportedRarV5Exception;
import com.junrar.io.RawDataIo;
import com.junrar.io.SeekableReadOnlyByteChannel;
import com.junrar.rarfile.AVHeader;
import com.junrar.rarfile.BaseBlock;
import com.junrar.rarfile.BlockHeader;
import com.junrar.rarfile.CommentHeader;
import com.junrar.rarfile.EAHeader;
import com.junrar.rarfile.EndArcHeader;
import com.junrar.rarfile.FileHeader;
import com.junrar.rarfile.MacInfoHeader;
import com.junrar.rarfile.MainHeader;
import com.junrar.rarfile.MarkHeader;
import com.junrar.rarfile.ProtectHeader;
import com.junrar.rarfile.RARVersion;
import com.junrar.rarfile.SignHeader;
import com.junrar.rarfile.SubBlockHeader;
import com.junrar.rarfile.SubBlockHeaderType;
import com.junrar.rarfile.UnixOwnersHeader;
import com.junrar.rarfile.UnrarHeadertype;
import com.junrar.unpack.ComprDataIO;
import com.junrar.unpack.Unpack;
import com.junrar.volume.FileVolumeManager;
import com.junrar.volume.InputStreamVolumeManager;
import com.junrar.volume.Volume;
import com.junrar.volume.VolumeManager;
///import org.slf4j.Logger;
///import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * The Main Rar Class; represents a rar Archive
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class Archive implements Closeable, Iterable<FileHeader> {

    ///private static final Logger logger = LoggerFactory.getLogger(Archive.class);

    private static final int MAX_HEADER_SIZE = 20971520; //20MB

    private static final int PIPE_BUFFER_SIZE = getPropertyAs(
        "junrar.extractor.buffer-size",
        Integer::parseInt,
        32 * 1024
    );

    private static final boolean USE_EXECUTOR = getPropertyAs(
        "junrar.extractor.use-executor",
        Boolean::parseBoolean,
        true
    );

    private SeekableReadOnlyByteChannel channel;

    private final UnrarCallback unrarCallback;

    private final ComprDataIO dataIO;

    private final List<BaseBlock> headers = new ArrayList<>();

    private MarkHeader markHead = null;

    private MainHeader newMhd = null;

    private Unpack unpack;

    private int currentHeaderIndex;

    /**
     * Size of packed data in current file.
     */
    private long totalPackedSize = 0L;

    /**
     * Number of bytes of compressed data read from current file.
     */
    private long totalPackedRead = 0L;

    private VolumeManager volumeManager;
    private Volume volume;

    private FileHeader nextFileHeader;
    private String password;

    public Archive(
        final VolumeManager volumeManager,
        final UnrarCallback unrarCallback,
        final String password
    ) throws RarException, IOException {

        this.volumeManager = volumeManager;
        this.unrarCallback = unrarCallback;
        this.password = password;

        try {
            setVolume(this.volumeManager.nextVolume(this, null));
        } catch (IOException | RarException e) {
            try {
                close();
            } catch (IOException e1) {
                ///logger.error("Failed to close the archive after an internal error!");
            }
            throw e;
        }
        this.dataIO = new ComprDataIO(this);
    }

    public Archive(final File firstVolume) throws RarException, IOException {
        this(new FileVolumeManager(firstVolume), null, null);
    }

    public Archive(final File firstVolume, final UnrarCallback unrarCallback) throws RarException, IOException {
        this(new FileVolumeManager(firstVolume), unrarCallback, null);
    }

    public Archive(final File firstVolume, final String password) throws RarException, IOException {
        this(new FileVolumeManager(firstVolume), null, password);
    }

    public Archive(final File firstVolume, final UnrarCallback unrarCallback, final String password) throws RarException, IOException {
        this(new FileVolumeManager(firstVolume), unrarCallback, password);
    }

    public Archive(final InputStream rarAsStream) throws RarException, IOException {
        this(new InputStreamVolumeManager(rarAsStream), null, null);
    }

    public Archive(final InputStream rarAsStream, final UnrarCallback unrarCallback) throws RarException, IOException {
        this(new InputStreamVolumeManager(rarAsStream), unrarCallback, null);
    }

    public Archive(final InputStream rarAsStream, final String password) throws IOException, RarException {
        this(new InputStreamVolumeManager(rarAsStream), null, password);
    }

    public Archive(final InputStream rarAsStream, final UnrarCallback unrarCallback, final String password) throws IOException, RarException {
        this(new InputStreamVolumeManager(rarAsStream), unrarCallback, password);
    }

    private void setChannel(final SeekableReadOnlyByteChannel channel, final long length) throws IOException, RarException {
        this.totalPackedSize = 0L;
        this.totalPackedRead = 0L;
        close();
        this.channel = channel;
        try {
            readHeaders(length);
        } catch (UnsupportedRarEncryptedException | UnsupportedRarV5Exception | CorruptHeaderException | BadRarArchiveException e) {
            ///logger.warn("exception in archive constructor maybe file is encrypted, corrupt or support not yet implemented", e);
            throw e;
        } catch (final Exception e) {
            ///logger.warn("exception in archive constructor maybe file is encrypted, corrupt or support not yet implemented", e);
            // ignore exceptions to allow extraction of working files in corrupt archive
        }
        // Calculate size of packed data
        for (final BaseBlock block : this.headers) {
            if (block.getHeaderType() == UnrarHeadertype.FileHeader) {
                this.totalPackedSize += ((FileHeader) block).getFullPackSize();
            }
        }
        if (this.unrarCallback != null) {
            this.unrarCallback.volumeProgressChanged(this.totalPackedRead,
                this.totalPackedSize);
        }
    }

    public void bytesReadRead(final int count) {
        if (count > 0) {
            this.totalPackedRead += count;
            if (this.unrarCallback != null) {
                this.unrarCallback.volumeProgressChanged(this.totalPackedRead,
                    this.totalPackedSize);
            }
        }
    }

    public SeekableReadOnlyByteChannel getChannel() {
        return this.channel;
    }

    /**
     * Gets all of the headers in the archive.
     *
     * @return returns the headers.
     */
    public List<BaseBlock> getHeaders() {
        return new ArrayList<>(this.headers);
    }

    /**
     * @return returns all file headers of the archive
     */
    public List<FileHeader> getFileHeaders() {
        final List<FileHeader> list = new ArrayList<>();
        for (final BaseBlock block : this.headers) {
            if (block.getHeaderType().equals(UnrarHeadertype.FileHeader)) {
                list.add((FileHeader) block);
            }
        }
        return list;
    }

    public FileHeader nextFileHeader() {
        final int n = this.headers.size();
        while (this.currentHeaderIndex < n) {
            final BaseBlock block = this.headers.get(this.currentHeaderIndex++);
            if (block.getHeaderType() == UnrarHeadertype.FileHeader) {
                return (FileHeader) block;
            }
        }
        return null;
    }

    public UnrarCallback getUnrarCallback() {
        return this.unrarCallback;
    }

    /**
     * @return whether the archive is encrypted
     * @throws RarException when the main header is not present
     */
    public boolean isEncrypted() throws RarException {
        if (this.newMhd != null) {
            return this.newMhd.isEncrypted();
        } else {
            throw new MainHeaderNullException();
        }
    }

    /**
     * @return whether the archive content is password protected
     * @throws RarException when the main header is not present
     */
    public boolean isPasswordProtected() throws RarException {
        if (isEncrypted()) return true;
        return getFileHeaders().stream().anyMatch(FileHeader::isEncrypted);
    }

    /**
     * Read the headers of the archive
     *
     * @param fileLength Length of file.
     * @throws IOException, RarException
     */
    private void readHeaders(final long fileLength) throws IOException, RarException {
        this.markHead = null;
        this.newMhd = null;
        this.headers.clear();
        this.currentHeaderIndex = 0;
        int toRead = 0;

        //keep track of positions already processed for
        //more robustness against corrupt files
        final Set<Long> processedPositions = new HashSet<>();
        while (true) {
            int size = 0;
            long newpos = 0;
            RawDataIo rawData = new RawDataIo(channel);
            final byte[] baseBlockBuffer = safelyAllocate(BaseBlock.BaseBlockSize, MAX_HEADER_SIZE);

            // if header is encrypted,there is a 8-byte salt before each header
            if (newMhd != null && newMhd.isEncrypted()) {
                byte[] salt = new byte[8];
                rawData.readFully(salt, 8);
                try {
                    Cipher cipher = Rijndael.buildDecipherer(password, salt);
                    rawData.setCipher(cipher);
                } catch (Exception e) {
                    throw new InitDeciphererFailedException(e);
                }
            }

            final long position = this.channel.getPosition();

            // Weird, but is trying to read beyond the end of the file
            if (position >= fileLength) {
                break;
            }

            // ///logger.info("\n--------reading header--------");
            size = rawData.readFully(baseBlockBuffer, baseBlockBuffer.length);

            if (size == 0) {
                break;
            }
            final BaseBlock block = new BaseBlock(baseBlockBuffer);

            block.setPositionInFile(position);

            UnrarHeadertype headerType = block.getHeaderType();
            if (headerType == null) {
                ///logger.warn("unknown block header!");
                throw new CorruptHeaderException();
            }
            switch (headerType) {

                case MarkHeader:
                    this.markHead = new MarkHeader(block);
                    if (!this.markHead.isSignature()) {
                        if (markHead.getVersion() == RARVersion.V5) {
                            ///logger.warn("Support for rar version 5 is not yet implemented!");
                            throw new UnsupportedRarV5Exception();
                        } else {
                            throw new BadRarArchiveException();
                        }
                    }
                    if (!markHead.isValid()) {
                        throw new CorruptHeaderException("Invalid Mark Header");
                    }
                    this.headers.add(this.markHead);
                    // markHead.print();
                    break;

                case MainHeader:
                    toRead = block.hasEncryptVersion() ? MainHeader.mainHeaderSizeWithEnc
                        : MainHeader.mainHeaderSize;
                    final byte[] mainbuff = safelyAllocate(toRead, MAX_HEADER_SIZE);
                    rawData.readFully(mainbuff, mainbuff.length);
                    final MainHeader mainhead = new MainHeader(block, mainbuff);
                    this.headers.add(mainhead);
                    this.newMhd = mainhead;
                    break;

                case SignHeader:
                    toRead = SignHeader.signHeaderSize;
                    final byte[] signBuff = safelyAllocate(toRead, MAX_HEADER_SIZE);
                    rawData.readFully(signBuff, signBuff.length);
                    final SignHeader signHead = new SignHeader(block, signBuff);
                    this.headers.add(signHead);
                    break;

                case AvHeader:
                    toRead = AVHeader.avHeaderSize;
                    final byte[] avBuff = safelyAllocate(toRead, MAX_HEADER_SIZE);
                    rawData.readFully(avBuff, avBuff.length);
                    final AVHeader avHead = new AVHeader(block, avBuff);
                    this.headers.add(avHead);
                    break;

                case CommHeader:
                    toRead = CommentHeader.commentHeaderSize;
                    final byte[] commBuff = safelyAllocate(toRead, MAX_HEADER_SIZE);
                    rawData.readFully(commBuff, commBuff.length);
                    final CommentHeader commHead = new CommentHeader(block, commBuff);
                    this.headers.add(commHead);

                    newpos = commHead.getPositionInFile() + commHead.getHeaderSize(isEncrypted());
                    this.channel.setPosition(newpos);

                    if (processedPositions.contains(newpos)) {
                        throw new BadRarArchiveException();
                    }
                    processedPositions.add(newpos);

                    break;
                case EndArcHeader:

                    toRead = 0;
                    if (block.hasArchiveDataCRC()) {
                        toRead += EndArcHeader.endArcArchiveDataCrcSize;
                    }
                    if (block.hasVolumeNumber()) {
                        toRead += EndArcHeader.endArcVolumeNumberSize;
                    }
                    EndArcHeader endArcHead;
                    if (toRead > 0) {
                        final byte[] endArchBuff = safelyAllocate(toRead, MAX_HEADER_SIZE);
                        rawData.readFully(endArchBuff, endArchBuff.length);
                        endArcHead = new EndArcHeader(block, endArchBuff);
                    } else {
                        endArcHead = new EndArcHeader(block, null);
                    }
                    if (!this.newMhd.isMultiVolume() && !endArcHead.isValid()) {
                        throw new CorruptHeaderException("Invalid End Archive Header");
                    }
                    this.headers.add(endArcHead);
                    return;

                default:
                    final byte[] blockHeaderBuffer = safelyAllocate(BlockHeader.blockHeaderSize, MAX_HEADER_SIZE);
                    rawData.readFully(blockHeaderBuffer, blockHeaderBuffer.length);
                    final BlockHeader blockHead = new BlockHeader(block,
                        blockHeaderBuffer);

                    switch (blockHead.getHeaderType()) {
                        case NewSubHeader:
                        case FileHeader:
                            toRead = blockHead.getHeaderSize(false)
                                - BlockHeader.BaseBlockSize
                                - BlockHeader.blockHeaderSize;
                            final byte[] fileHeaderBuffer = safelyAllocate(toRead, MAX_HEADER_SIZE);
                            try {
                                rawData.readFully(fileHeaderBuffer, fileHeaderBuffer.length);
                            } catch (EOFException e) {
                                throw new CorruptHeaderException("Unexpected end of file");
                            }

                            final FileHeader fh = new FileHeader(blockHead, fileHeaderBuffer);
                            this.headers.add(fh);
                            newpos = fh.getPositionInFile() + fh.getHeaderSize(isEncrypted()) + fh.getFullPackSize();
                            this.channel.setPosition(newpos);

                            if (processedPositions.contains(newpos)) {
                                throw new BadRarArchiveException();
                            }
                            processedPositions.add(newpos);
                            break;

                        case ProtectHeader:
                            toRead = blockHead.getHeaderSize(false)
                                - BlockHeader.BaseBlockSize
                                - BlockHeader.blockHeaderSize;
                            final byte[] protectHeaderBuffer = safelyAllocate(toRead, MAX_HEADER_SIZE);
                            rawData.readFully(protectHeaderBuffer, protectHeaderBuffer.length);
                            final ProtectHeader ph = new ProtectHeader(blockHead, protectHeaderBuffer);
                            newpos = ph.getPositionInFile() + ph.getHeaderSize(isEncrypted()) + ph.getDataSize();
                            this.channel.setPosition(newpos);

                            if (processedPositions.contains(newpos)) {
                                throw new BadRarArchiveException();
                            }
                            processedPositions.add(newpos);
                            break;

                        case SubHeader: {
                            final byte[] subHeadbuffer = safelyAllocate(SubBlockHeader.SubBlockHeaderSize, MAX_HEADER_SIZE);
                            rawData.readFully(subHeadbuffer, subHeadbuffer.length);
                            final SubBlockHeader subHead = new SubBlockHeader(blockHead,
                                subHeadbuffer);
                            subHead.print();
                            SubBlockHeaderType subType = subHead.getSubType();
                            if (subType == null) break;
                            switch (subType) {
                                case MAC_HEAD: {
                                    final byte[] macHeaderbuffer = safelyAllocate(MacInfoHeader.MacInfoHeaderSize, MAX_HEADER_SIZE);
                                    rawData.readFully(macHeaderbuffer, macHeaderbuffer.length);
                                    final MacInfoHeader macHeader = new MacInfoHeader(subHead,
                                        macHeaderbuffer);
                                    macHeader.print();
                                    this.headers.add(macHeader);

                                    break;
                                }
                                // TODO implement other subheaders
                                case BEEA_HEAD:
                                    break;
                                case EA_HEAD: {
                                    final byte[] eaHeaderBuffer = safelyAllocate(EAHeader.EAHeaderSize, MAX_HEADER_SIZE);
                                    rawData.readFully(eaHeaderBuffer, eaHeaderBuffer.length);
                                    final EAHeader eaHeader = new EAHeader(subHead,
                                        eaHeaderBuffer);
                                    eaHeader.print();
                                    this.headers.add(eaHeader);

                                    break;
                                }
                                case NTACL_HEAD:
                                    break;
                                case STREAM_HEAD:
                                    break;
                                case UO_HEAD:
                                    toRead = subHead.getHeaderSize(false);
                                    toRead -= BaseBlock.BaseBlockSize;
                                    toRead -= BlockHeader.blockHeaderSize;
                                    toRead -= SubBlockHeader.SubBlockHeaderSize;
                                    final byte[] uoHeaderBuffer = safelyAllocate(toRead, MAX_HEADER_SIZE);
                                    rawData.readFully(uoHeaderBuffer, uoHeaderBuffer.length);
                                    final UnixOwnersHeader uoHeader = new UnixOwnersHeader(
                                        subHead, uoHeaderBuffer);
                                    uoHeader.print();
                                    this.headers.add(uoHeader);
                                    break;
                                default:
                                    break;
                            }

                            break;
                        }
                        default:
                            ///logger.warn("Unknown Header");
                            throw new NotRarArchiveException();

                    }
            }
            // ///logger.info("\n--------end header--------");
        }
    }

    private static byte[] safelyAllocate(final long len, final int maxSize) throws RarException {
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxsize must be >= 0");
        }
        if (len < 0 || len > maxSize) {
            throw new BadRarArchiveException();
        }
        return new byte[(int) len];
    }

    /**
     * Extract the file specified by the given header and write it to the
     * supplied output stream
     *
     * @param hd the header to be extracted
     * @param os the outputstream
     * @throws RarException .
     */
    public void extractFile(final FileHeader hd, final OutputStream os) throws RarException {
        if (!this.headers.contains(hd)) {
            throw new HeaderNotInArchiveException();
        }
        try {
            doExtractFile(hd, os);
        } catch (final Exception e) {
            if (e instanceof RarException) {
                throw (RarException) e;
            } else {
                throw new RarException(e);
            }
        }
    }

    /**
     * Class to ensure the lazy initialization of the {@link ThreadPoolExecutor} upon first usage.<br><br>
     * <p>
     * Using a cached thread pool executor is more efficient and creating a new thread for each extraction.
     * The total number of threads will only increase if there are tasks on its queue and all current threads are busy.
     * If there are available threads, those will be reused instead of a new one being created.
     * <br><br>
     * <p>
     * Configuration options:
     * <ul>
     * <li>To avoid the possibility of too many simultaneous active threads being started, the maximum
     * number of threads can be configured through the {@code junrar.extractor.max-threads} system property.
     * The default maximum number of threads is unbounded.</li>
     * <li>The keep alive time can be configured through the {@code junrar.extractor.thread-keep-alive-seconds} system property.
     * The default is 5s.</li>
     * </ul>
     */
    private static final class ExtractorExecutorHolder {
        private ExtractorExecutorHolder() {
        }

        private static final AtomicLong threadIndex = new AtomicLong();

        /**
         * Equivalent to {@link java.util.concurrent.Executors#newCachedThreadPool()}, but customizable through system properties.
         */
        private static final ExecutorService cachedExecutorService = new ThreadPoolExecutor(
            0, getMaxThreads(),
            getThreadKeepAlive(), TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            r -> {
                Thread t = new Thread(r, "junrar-extractor-" + threadIndex.getAndIncrement());
                t.setDaemon(true);
                return t;
            });

        private static int getMaxThreads() {
            return getPropertyAs("junrar.extractor.max-threads", Integer::parseInt, Integer.MAX_VALUE);
        }

        private static int getThreadKeepAlive() {
            return getPropertyAs("junrar.extractor.thread-keep-alive-seconds", Integer::parseInt, 5);
        }
    }

    private static <T> T getPropertyAs(String key, Function<String, T> function, T defaultValue) {
        Objects.requireNonNull(defaultValue, "default value must not be null");
        try {
            String integerString = System.getProperty(key);
            if (integerString != null && !integerString.isEmpty()) {
                return function.apply(integerString);
            }
        } catch (SecurityException | NumberFormatException e) {
            ///logger.error(
//                "Could not parse the System Property '{}' into an '{}'. Defaulting to '{}'",
//                key,
//                defaultValue.getClass().getTypeName(),
//                defaultValue,
//                e
//            );
        }
        return defaultValue;
    }

    /**
     * An empty {@link InputStream}.
     */
    private static final class EmptyInputStream extends InputStream {

        @Override
        public int available() {
            return 0;
        }

        @Override
        public int read() {
            return -1;
        }
    }

    /**
     * Returns an {@link InputStream} that will allow to read the file and stream it. <br>
     * Please note that this method will create a pair of Pipe streams and either: <br>
     *
     * <ul>
     *     <li>delegate the work to a {@link ThreadPoolExecutor}, via {@link ExtractorExecutorHolder}; or</li>
     *     <li>delegate the work to a newly created thread on each call</li>
     * </ul>
     * <p>
     * You can choose which strategy to use by setting the {@code junrar.extractor.use-executor} system property.<br>
     * Defaults to using the {@link ThreadPoolExecutor}.
     *
     * @param hd the header to be extracted
     * @return an {@link InputStream} from which you can read the uncompressed bytes
     * @throws IOException if any I/O error occur
     * @see ExtractorExecutorHolder
     */
    public InputStream getInputStream(final FileHeader hd) throws IOException {
        // If the file is empty, return an empty InputStream
        // This saves adding a task on the executor that will effectively do nothing
        if (hd.getFullUnpackSize() <= 0) {
            return new EmptyInputStream();
        }

        // Small optimization to prevent the creation of large buffers for very small files
        // Never allocate more than needed, but ensure the buffer will be at least 1-byte long
        final int bufferSize = (int) Math.max(Math.min(hd.getFullUnpackSize(), PIPE_BUFFER_SIZE), 1);

        final PipedInputStream in = new PipedInputStream(bufferSize);
        final PipedOutputStream out = new PipedOutputStream(in);

        // Data will be available in another InputStream, connected to the OutputStream
        // Delegates execution to the cached executor service.
        Runnable r = () -> {
            try {
                extractFile(hd, out);
            } catch (final RarException ignored) {
            } finally {
                try {
                    out.close();
                } catch (final IOException ignored) {
                }
            }
        };
        if (USE_EXECUTOR) {
            ExtractorExecutorHolder.cachedExecutorService.submit(r);
        } else {
            new Thread(r).start();
        }

        return in;
    }

    private void doExtractFile(FileHeader hd, final OutputStream os)
        throws RarException, IOException {
        this.dataIO.init(os);
        this.dataIO.init(hd);
        this.dataIO.setUnpFileCRC(this.isOldFormat() ? 0 : 0xffFFffFF);
        if (this.unpack == null) {
            this.unpack = new Unpack(this.dataIO);
        }
        if (!hd.isSolid()) {
            this.unpack.init(null);
        }
        this.unpack.setDestSize(hd.getFullUnpackSize());
        try {
            this.unpack.doUnpack(hd.getUnpVersion(), hd.isSolid());
            // Verify file CRC
            hd = this.dataIO.getSubHeader();
            final long actualCRC = hd.isSplitAfter() ? ~this.dataIO.getPackedCRC()
                : ~this.dataIO.getUnpFileCRC();
            final int expectedCRC = hd.getFileCRC();
            if (actualCRC != expectedCRC) {
                throw new CrcErrorException();
            }
            // if (!hd.isSplitAfter()) {
            // // Verify file CRC
            // if(~dataIO.getUnpFileCRC() != hd.getFileCRC()){
            // throw new RarException(RarExceptionType.crcError);
            // }
            // }
        } catch (final Exception e) {
            this.unpack.cleanUp();
            if (e instanceof RarException) {
                // throw new RarException((RarException)e);
                throw (RarException) e;
            } else {
                throw new RarException(e);
            }
        }
    }

    /**
     * @return returns the main header of this archive
     */
    public MainHeader getMainHeader() {
        return this.newMhd;
    }

    /**
     * @return whether the archive is old format
     */
    public boolean isOldFormat() {
        return this.markHead.isOldFormat();
    }

    /**
     * Close the underlying compressed file.
     */
    @Override
    public void close() throws IOException {
        if (this.channel != null) {
            this.channel.close();
            this.channel = null;
        }
        if (this.unpack != null) {
            this.unpack.cleanUp();
        }
    }

    /**
     * @return the volumeManager
     */
    public VolumeManager getVolumeManager() {
        return this.volumeManager;
    }

    /**
     * @param volumeManager the volumeManager to set
     */
    public void setVolumeManager(final VolumeManager volumeManager) {
        this.volumeManager = volumeManager;
    }

    /**
     * @return the volume
     */
    public Volume getVolume() {
        return this.volume;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    /**
     * @param volume the volume to set
     * @throws IOException  .
     * @throws RarException .
     */
    public void setVolume(final Volume volume) throws IOException, RarException {
        this.volume = volume;
        setChannel(volume.getChannel(), volume.getLength());
    }

    @Override
    public Iterator<FileHeader> iterator() {
        return new Iterator<FileHeader>() {

            @Override
            public FileHeader next() {
                FileHeader next;
                if (Archive.this.nextFileHeader != null) {
                    next = Archive.this.nextFileHeader;
                } else {
                    next = nextFileHeader();
                }
                return next;
            }

            @Override
            public boolean hasNext() {
                Archive.this.nextFileHeader = nextFileHeader();
                return Archive.this.nextFileHeader != null;
            }
        };
    }

    public final static void main(String[] args) {
    	 try {
    		 String root = "c:/temp/rar/";
             File rarFile = new File("c:/temp/NMR.rar"); 
             Archive archive = new Archive(rarFile);

             FileHeader fh;
             List<FileHeader> list = new ArrayList<>();
             while ((fh = archive.nextFileHeader()) != null) {
            	 list.add(fh);
             }
             for (int i = list.size(); --i >= 0;) { 
                 fh = list.get(i);
            	 String name = fh.getFileName();
                 if (fh.isDirectory()) {
                     // Create directory if needed
                 } else {
                     // Extract file
                	 FileOutputStream os = null;
                	 String fout = root + name;
                	 try {
                    	 os = new FileOutputStream(fout);
                     } catch (Exception e) {
                         new File(fout).getParentFile().mkdirs(); 
                    	 os = new FileOutputStream(fout);
                     }
                     archive.extractFile(fh, os);
                     os.close();                 }
             }
             archive.close();
         } catch (Exception e) {
             e.printStackTrace();
         }
    }
}
