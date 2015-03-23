//
// The MIT License (MIT)
//
// Copyright (c) 2015 Hitachi Data Systems Corporation
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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Called by librsync patch job to seek a chunk of the base file.
 *
 * @author Beth Tirado, Hitachi Data Systems
 */
public interface RsyncInputSeeker {
    /**
     * Seek a chunk of the base file, starting at position and containing len bytes.
     * 
     * If an error occurs, null should be returned.
     * 
     * @param position
     *            the position in the file to start reading from
     * @param len
     *            the number of bytes to read
     * @return the ByteBuffer into which bytes were read if successful, null if any error occurred
     */
    public ByteBuffer seek(long position, int len);
}
