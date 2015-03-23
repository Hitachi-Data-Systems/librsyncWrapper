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
 * This class provides an example of how to use the LibrsyncWrapper to generate rsync signatures,
 * deltas, and patches.
 * 
 * This test uses FileInputStreams and FileOutputStreams, but you could easily use any other type of
 * stream, or not use streams at all for that matter.
 *
 * @author Beth Tirado, Hitachi Data Systems
 */
public class LibrsyncWrapperTest {

    /**
     * This method will read a base file identified by <baseFileIn>, read a changed file identified
     * by <changedFileIn>, and write to files called <signatureFileOut>, <deltaFileOut>, and
     * <recomposedFileOut>.
     * 
     * After the method successfully completes, <recomposedFileOut> should be exactly the same as
     * <changedFileIn>.
     * 
     * @param args
     */
    public static void main(String args[]) {
        try {
            System.loadLibrary("rsyncWrapper");
            System.loadLibrary("rsync");

            if (args.length != 5) {
                System.out
                        .println("Usage: LibrsyncWrapperTest <baseFileIn> <changedFileIn> <signatureFileOut> <deltaFileOut> <recomposedFileOut>");
                System.exit(1);
            }
            File baseFile = new File(args[0]);
            File changedFile = new File(args[1]);
            File signatureFile = new File(args[2]);
            File deltaFile = new File(args[3]);
            File recomposedFile = new File(args[4]);

            ByteBuffer inBuf = ByteBuffer.allocateDirect(1024 * 1024);
            ByteBuffer outBuf = ByteBuffer.allocateDirect(1024 * 1024);

            //
            // generate the signature of the base file
            //
            InputStream baseIn = new FileInputStream(baseFile);
            OutputStream signatureOut = new FileOutputStream(signatureFile);
            int blockSize = (int) Math.round(Math.sqrt(baseFile.length()) / 8) * 8;
            LibrsyncWrapper.generateSignature("genSig - " + baseFile.getPath(),
                                              new InputStreamProducer(baseIn),
                                              new OutputStreamConsumer(signatureOut),
                                              blockSize, inBuf, outBuf, new RsyncStatistics());
            baseIn.close();
            signatureOut.close();

            //
            // generate a delta based on the signature previously generated and the changed file
            //
            InputStream signatureIn = new FileInputStream(signatureFile);
            InputStream changedFileIn = new FileInputStream(changedFile);
            OutputStream deltaFileOut = new FileOutputStream(deltaFile);
            LibrsyncWrapper.generateDelta("genDelta - " + changedFile.getPath(), new
                                          InputStreamProducer(signatureIn),
                                          new InputStreamProducer(changedFileIn), new
                                          OutputStreamConsumer(deltaFileOut), inBuf, outBuf,
                                          new RsyncStatistics());
            signatureIn.close();
            changedFileIn.close();
            deltaFileOut.close();

            //
            // generate a patch (i.e. regenerate the changed file) based on the base file and the
            // previously generated delta
            //
            RandomAccessFile baseRAF = new RandomAccessFile(baseFile, "r");
            InputStream deltaFileIn = new FileInputStream(deltaFile);
            OutputStream recomposedFileOut = new FileOutputStream(recomposedFile);
            LibrsyncWrapper.generatePatch("genPatch", new FileInputSeeker(baseRAF),
                                          new InputStreamProducer(deltaFileIn),
                                          new OutputStreamConsumer(recomposedFileOut), inBuf,
                                          outBuf, new RsyncStatistics());

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
