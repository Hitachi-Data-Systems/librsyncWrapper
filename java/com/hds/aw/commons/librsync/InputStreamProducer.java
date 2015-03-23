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

import java.io.*;
import java.nio.ByteBuffer;

/**
 * Simple implementation of RsyncInputProducer that produces input from an InputStream
 *
 * @author Beth Tirado, Hitachi Data Systems
 */
public class InputStreamProducer implements RsyncInputProducer {

    private InputStream in;
    private boolean done = false;
    private byte[] bytes;
    private long totalBytesRead = 0;

    /**
     * Construct an InputStreamProducer
     * 
     * @param inputStream
     *            the InputStream that produceInput() will read from
     */
    public InputStreamProducer(InputStream inputStream) {
        if (inputStream instanceof BufferedInputStream) {
            in = inputStream;
        } else {
            in = new BufferedInputStream(inputStream);
        }
    }

    /**
     * Reads from the InputStream provided in the constructor and writes to buf
     * 
     * @param buf
     *            buffer into which to write data
     * @return true if the end of the input stream has been reached and no data is being written to,
     *         false otherwise
     * @throws IOException
     */
    public boolean produceInput(ByteBuffer buf) throws IOException {
        if (!done) {
            if (bytes == null || bytes.length < buf.remaining()) {
                bytes = new byte[buf.capacity()];
            }
            while (!done && buf.remaining() > 0) {
                int readCnt = in.read(bytes, 0, buf.remaining());
                if (readCnt < 0) {
                    done = true;
                } else {
                    buf.put(bytes, 0, readCnt);
                    totalBytesRead += readCnt;
                }
            }
        }
        return done;
    }

}
