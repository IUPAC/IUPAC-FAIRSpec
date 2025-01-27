package com.junrar.volume;

import com.junrar.Archive;
import com.junrar.io.SeekableReadOnlyByteChannel;
import com.junrar.io.SeekableReadOnlyFile;

import java.io.File;
import java.io.IOException;


/**
 * @author <a href="http://www.rogiel.com">Rogiel</a>
 *
 */
public class FileVolume implements Volume {
    private final Archive archive;
    private final File file;

    /**
     * @param archive .
     * @param file .
     */
    public FileVolume(Archive archive, File file) {
        this.archive = archive;
        this.file = file;
    }

    @Override
    public SeekableReadOnlyByteChannel getChannel() throws IOException {
        return new SeekableReadOnlyFile(file);
    }

    @Override
    public long getLength() {
        return file.length();
    }

    @Override
    public Archive getArchive() {
        return archive;
    }

    /**
     * @return the file
     */
    public File getFile() {
        return file;
    }
}
