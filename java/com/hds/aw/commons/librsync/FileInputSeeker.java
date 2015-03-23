//
// The MIT License (MIT)
//
// Copyright (c) 2015 Hitachi Data Systems
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
//

package com.hds.aw.commons.librsync;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Called by a librsync patch job. Reads data from a requested location in a base file. This
 * implmentation of RsyncInputSeeker reads from a provided RandomAccessFile.
 *
 * @author Beth Tirado, Hitachi Data Systems
 */
public class FileInputSeeker implements RsyncInputSeeker {

    private static Logger LOGGER = Logger.getLogger(FileInputSeeker.class.getName());

    private RandomAccessFile file;
    private FileChannel fileChannel;
    private ByteBuffer byteBuffer;

    /**
     * Constructs a FileInputSeeker that will read from the provided RandomAccessFile
     * 
     * @param f
     * @throws IOException
     */
    public FileInputSeeker(RandomAccessFile f) throws IOException {
        file = f;
        fileChannel = f.getChannel();
    }

    /**
     * Reads the specified number of bytes from the specified location in the base file
     * 
     * @param position
     *            offset in file of where to begin reading bytes
     * @param len
     *            number of bytes to read starting at position
     * @return a ByteBuffer into which the data was read, or null if any error occurred
     */
    public ByteBuffer seek(long position, int len) {
        try {
            file.seek(position);

            // @todo - use a byte buffer pool
            if (byteBuffer == null || byteBuffer.capacity() < len) {
                byteBuffer = ByteBuffer.allocateDirect(len);
            }

            byteBuffer.clear();
            int totalRead = 0;
            while (totalRead < len) {
                int cnt = fileChannel.read(byteBuffer);
                if (cnt == -1) {
                    throw new EOFException();
                }
                totalRead += cnt;
            }
            byteBuffer.flip();

            return byteBuffer;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, String
                    .format("An error occurred seeking %d bytes starting at position %d", len,
                            position), e);
            return null;
        }
    }

}
