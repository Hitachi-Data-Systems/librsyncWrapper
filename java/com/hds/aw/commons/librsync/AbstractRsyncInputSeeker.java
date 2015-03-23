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

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract RsyncInputSeeker that gathers seek statistics
 *
 * @author Beth Tirado, Hitachi Data Systems
 */
public abstract class AbstractRsyncInputSeeker implements RsyncInputSeeker {
    private final static Logger LOGGER = Logger.getLogger(LibrsyncWrapper.class.getName());

    RsyncStatistics stats;

    /**
     * Construct an AbstractRsyncInputSeeker
     * 
     * @param stats
     *            if non-null, statisitics will be collected and written to this object with every
     *            call to seek()
     */
    public AbstractRsyncInputSeeker(RsyncStatistics stats) {
        this.stats = stats;
    }

    /**
     * Records statistics about the seek operation, and then delegates to doSeek()
     */
    @Override
    public ByteBuffer seek(long position, int len) {
        if (stats != null) {
            stats.totalSeeks++;
            stats.totalSeekLen += len;
            stats.maxSeekLen = Math.max(stats.maxSeekLen, len);
            stats.minSeekLen = Math.min(stats.minSeekLen, len);

            LOGGER.log(Level.FINE, "Rsync job seeking {0} bytes from base file at position {1}",
                       new Object[] { len, position });
        }

        return doSeek(position, len);
    }

    /**
     * Called by seek(). Performs the actual work of seeking the desired location in the file.
     *
     * @param position
     *            the position in the file to start reading from
     * @param len
     *            the number of bytes to read
     * @return the ByteBuffer into which bytes were read if successful, null if any error occurred
     */
    public abstract ByteBuffer doSeek(long position, int len);
}
