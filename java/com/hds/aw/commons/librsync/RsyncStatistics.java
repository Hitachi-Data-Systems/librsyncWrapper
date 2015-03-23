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

/**
 * Holds statistics about an Rsync operation
 *
 * @author Beth Tirado, Hitachi Data Systems
 */
public class RsyncStatistics {
    public long iterations;
    public long startTimeMs;
    public long endTimeMs;

    public int maxInputConsumed;
    public int minInputConsumed;
    public long totalInputConsumed;

    public int maxInputNotConsumed;
    public int minInputNotConsumed;
    public long totalInputNotConsumed;

    public int maxOutputProduced;
    public int minOutputProduced;
    public long totalOutputProduced;

    public int maxSeekLen;
    public int minSeekLen;
    public long totalSeekLen;
    public long totalSeeks;

    /**
     * Generate empty statistics. Rysnc jobs will fill in statistics. If seek statistics are desired
     * on a patch job, then the RsyncInputSeeker must gather these statistics. See
     * AbstractRsyncInputSeeker
     */
    public RsyncStatistics() {
    }

    @Override
    public String toString() {
        long s = (endTimeMs - startTimeMs) / 1000; // duration in seconds
        String duration = String.format("%dh:%02dm:%02ds", s / 3600, (s % 3600) / 60, (s % 60));

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\n  job iterations: ").append(iterations);
        sb.append("\n  duration: ").append(duration);

        sb.append("\n  total input consumed: ").append(totalInputConsumed);
        sb.append("\n  max input consumed: ").append(maxInputConsumed);
        sb.append("\n  min input consumed: ").append(minInputConsumed);
        if (iterations > 0) {
            sb.append("\n  average input consumed: ").append(totalInputConsumed / iterations);
        }

        sb.append("\n  max input not consumed: ").append(maxInputNotConsumed);
        sb.append("\n  min input not consumed: ").append(minInputNotConsumed);
        if (iterations > 0) {
            sb.append("\n  average input not consumed: ")
                    .append(totalInputNotConsumed / iterations);
        }

        sb.append("\n  total ouput produced: ").append(totalOutputProduced);
        sb.append("\n  max ouput produced: ").append(maxOutputProduced);
        sb.append("\n  min ouput produced: ").append(minOutputProduced);
        if (iterations > 0) {
            sb.append("\n  average output produced: ").append(totalOutputProduced / iterations);
        }

        sb.append("\n  total seeks: ").append(totalSeeks);
        sb.append("\n  total seek len: ").append(totalSeekLen);
        sb.append("\n  max seek len: ").append(maxSeekLen);
        sb.append("\n  min seek len: ").append(minSeekLen);
        if (totalSeeks > 0) {
            sb.append("\n  average seek len: ").append(totalSeekLen / totalSeeks);
        }

        sb.append("\n}");
        return sb.toString();
    }

    public void begin() {
        startTimeMs = System.currentTimeMillis();
    }

    public void end() {
        endTimeMs = System.currentTimeMillis();
    }

}
