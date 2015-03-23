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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class (along with the corresponding LibrsyncWrapper.c) provides a thin Java wrapper (with a
 * JNI interface) around the librsync library (available currently on github at
 * https://github.com/librsync/librsync.git). This class provides three easy-to-use public methods for
 * generating an rsync signature, delta, and patch from any data source you provide via a simple
 * interface.
 *
 * For an example of how to use this class, see LibrsyncWrapperTest
 *
 * @author Beth Tirado, Hitachi Data Systems
 */
public class LibrsyncWrapper {

    private final static Logger LOGGER = Logger.getLogger(LibrsyncWrapper.class.getName());

    /**
     * Holds the result from loadSignatureBegin()
     */
    private static class LoadSignatureResult {
        public long jobPointer;
        public long signaturePointer;

        public void setJob(long job) {
            jobPointer = job;
        }

        public void setSignature(long signature) {
            signaturePointer = signature;
        }
    }

    /**
     * Holds the result from patchBegin()
     */
    private static class PatchBeginResult {
        public long jobPointer;
        public long argPointer;

        public void setJob(long job) {
            jobPointer = job;
        }

        public void setArg(long arg) {
            argPointer = arg;
        }
    }

    /**
     * Java equivalent of librsync's rs_result
     */
    private enum RsyncResult {
        RS_DONE(0), // Completed successfully.
        RS_BLOCKED(1), // Blocked waiting for more data.
        RS_RUNNING(2), // Not yet finished or blocked. This value should never be returned to the
                       // caller.
        RS_TEST_SKIPPED(77), // Test neither passed or failed.
        RS_IO_ERROR(100), // Error in file or network IO.
        RS_SYNTAX_ERROR(101), // Command line syntax error.
        RS_MEM_ERROR(102), // Out of memory.
        RS_INPUT_ENDED(103), // End of input file, possibly unexpected.
        RS_BAD_MAGIC(104), // Bad magic number at start of stream. Probably not a librsync file, or
                           // possibly the wrong kind of file or from an incompatible library
                           // version.
        RS_UNIMPLEMENTED(105), // Author is lazy.
        RS_CORRUPT(106), // Unbelievable value in stream.
        RS_INTERNAL_ERROR(107), // Probably a library bug.
        RS_PARAM_ERROR(108); // Bad value passed in to library, probably an application bug.

        private int cValue;
        private static final HashMap<Integer, RsyncResult> map = new HashMap<>();

        private RsyncResult(int cValue) {
            this.cValue = cValue;
        }

        /**
         * convert the long returned from C to an RsyncResult
         * 
         * @param cValue
         *            the value returned from the JNI call
         * 
         * @return the corresponding RsyncResult
         */
        private static RsyncResult fromCvalue(long cValue) {
            if (map.isEmpty()) {
                synchronized (map) {
                    if (map.isEmpty()) {
                        for (RsyncResult rsyncResult : RsyncResult.values()) {
                            map.put(rsyncResult.cValue, rsyncResult);
                        }
                    }
                }
            }
            return map.get(Integer.valueOf((int) cValue));
        }
    }

    /**
     * Generates an rsync signature for a file.
     *
     * @param jobTag
     *            the toString() of this is just used in LOGGER messages to identify the job
     * @param fileProducer
     *            produces the contents of the file for which the signature is generated
     * @param signatureConsumer
     *            consumes the output signature
     * @param blockSize
     *            the rsync block size to use when generating the signature
     * @param inBuf
     *            The direct byte buffer that will be passed to the fileProducer, and from which the
     *            input will be read
     * @param outBuf
     *            The direct byte buffer into which the signature will be written, and that is
     *            passed to the signatureConsumer
     * @param rsyncStats
     *            Optional object to hold statistics about the rsync job. If non-null, then the
     *            statistics will be gathered and written to this object.
     * @throws IOException
     *             if thrown from fileProducer or signatureConsumer
     * @throws RsyncException
     *             if the rsync job returned anything other than RS_DONE
     */
    public static void generateSignature(Object jobTag, RsyncInputProducer fileProducer,
                                         RsyncOutputConsumer signatureConsumer, int blockSize,
                                         ByteBuffer inBuf, ByteBuffer outBuf,
                                         RsyncStatistics rsyncStats)
            throws IOException, RsyncException {
        long job = signatureBegin(blockSize);
        try {
            runJobToCompletion(jobTag, job, fileProducer, signatureConsumer, null, 0, inBuf,
                               outBuf, rsyncStats);
        } finally {
            freeJob(job);
        }
    }

    /**
     * Generates an rsync delta from a base file signature and a changed file
     *
     * @param jobTag
     *            the toString() of this is just used in LOGGER messages to identify the job
     * @param signatureProducer
     *            produces the signature of the base file
     * @param newFileProducer
     *            produces the contents of the new file
     * @param deltaConsumer
     *            consumes the delta produced by librsync
     * @param inBuf
     *            The direct byte buffer that will be passed to the signatureProducer, and from
     *            which the signature will be read
     * @param outBuf
     *            The direct byte buffer into which the delta will be written, and that is passed to
     *            the deltaConsumer
     * @param rsyncStats
     *            Optional object to hold statistics about the rsync job. If non-null, then the
     *            statistics will be gathered and written to this object.
     * @throws IOException
     *             if thrown from signatureProducer or deltaConsumer
     * @throws RsyncException
     *             if the rsync job returned anything other than RS_DONE
     */
    public static void generateDelta(Object jobTag, RsyncInputProducer signatureProducer,
                                     RsyncInputProducer newFileProducer,
                                     RsyncOutputConsumer deltaConsumer,
                                     ByteBuffer inBuf, ByteBuffer outBuf, RsyncStatistics rsyncStats)
            throws IOException, RsyncException {

        LoadSignatureResult loadSigResult = new LoadSignatureResult();
        long deltaJob = 0;
        try {
            // load the signature
            validateResult(loadSignatureBegin(loadSigResult), "loadSignatureBegin");

            runJobToCompletion(jobTag, loadSigResult.jobPointer, signatureProducer, null, null, 0,
                               inBuf, outBuf, rsyncStats);

            freeJob(loadSigResult.jobPointer);
            loadSigResult.jobPointer = 0; // clear it out so we don't free the job again in finally
                                          // block

            // hash the signature
            validateResult(buildSignatureHashTable(loadSigResult.signaturePointer),
                           "build signature hash table");

            // compute delta between two files
            deltaJob = deltaBegin(loadSigResult.signaturePointer);
            runJobToCompletion("delta - " + jobTag.toString(), deltaJob, newFileProducer,
                               deltaConsumer, null, 0, inBuf, outBuf, rsyncStats);
        } finally {
            if (loadSigResult.jobPointer != 0) {
                try {
                    freeJob(loadSigResult.jobPointer);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error freeing signature job", e);
                }
            }

            if (deltaJob != 0) {
                try {
                    freeJob(deltaJob);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error freeing delta job", e);
                }
            }

            if (loadSigResult.signaturePointer != 0) {
                try {
                    freeLoadedSignature(loadSigResult.signaturePointer);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error freeing loaded signature", e);
                }
            }
        }
    }

    /**
     * Generate an rsync patch (reconstituted changed file) from a base file and a previously
     * generated librsync delta
     *
     * @param jobTag
     *            the toString() of this is just used in LOGGER messages to identify the job
     * @param baseFileSeeker
     *            Provides requested chunks of the base file
     * @param deltaProducer
     *            Provides the delta previously generated by librsync
     * @param patchConsumer
     *            Consumes the patch generated by librsync
     * @param inBuf
     *            The direct byte buffer that will be passed to the deltaProducer, and from which
     *            the delta will be read
     * @param outBuf
     *            The direct byte buffer into which the patch will be written, and that is passed to
     *            the patchConsumer
     * @param rsyncStats
     *            Optional object to hold statistics about the rsync job. If non-null, then the
     *            statistics will be gathered and written to this object.
     * @throws IOException
     *             if thrown from deltaProducer or patchConsumer
     * @throws RsyncException
     *             if the rsync job returned anything other than RS_DONE, including if the
     *             baseFileSeeker threw an exception or returned null
     */
    public static void generatePatch(Object jobTag, RsyncInputSeeker baseFileSeeker,
                                     RsyncInputProducer deltaProducer,
                                     RsyncOutputConsumer patchConsumer, ByteBuffer inBuf,
                                     ByteBuffer outBuf, RsyncStatistics rsyncStats)
            throws IOException, RsyncException {
        PatchBeginResult result = new PatchBeginResult();
        try {
            validateResult(patchBegin(baseFileSeeker, result), "patchBegin");
            runJobToCompletion(jobTag, result.jobPointer, deltaProducer,
                               patchConsumer,
                               baseFileSeeker, result.argPointer, inBuf, outBuf, rsyncStats);
        } finally {
            if (result.jobPointer != 0) {
                try {
                    freeJob(result.jobPointer, result.argPointer);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error freeing patch job", e);
                }
            }
        }
    }

    /**
     * Begin signature creation on a file. Calls librsync's rs_sig_begin().
     *
     * @return a long, which is a pointer to the C rs_job_t
     */
    private static native long signatureBegin(int blockSize);

    /**
     * Begin loading a signature. Calls librsync's rs_loadsig_begin()
     *
     * @return
     */
    private static native long loadSignatureBegin(LoadSignatureResult result);

    /**
     * Must be called when done loading the signature. Calls librsync's rs_build_hash_table
     *
     * @param signaturePointer
     * @return RsyncStatus value
     */
    private static native long buildSignatureHashTable(long signaturePointer);

    /**
     * Begin calculating the delta between a signature and a new file. Calls librsync's
     * rs_delta_begin()
     *
     * @return a long, which is a pointer to the C rs_job_t
     */
    private static native long deltaBegin(long signaturePointer);

    /**
     * Start applying a delta to a basis to recreate the new file. Calls librsync's rs_patch_begin()
     *
     * @return
     */
    private static native long patchBegin(RsyncInputSeeker baseFileSeeker, PatchBeginResult result);

    /**
     * Iterate (once) over a job. Calls librsync's rs_job_iter(). This should be called repeatedly
     * until the job is done.
     *
     * @param job
     *            the job returned by one of the <jobType>Begin() methods.
     * @param inBuffer
     *            the ByteByffer from which the job input is read
     * @param inPosition
     *            the position() of the ByteBuffer.
     * @param inLimit
     *            the limit() of the ByteBuffer
     * @param isLastInput
     *            true if this is the end of the input
     * @param outBuffer
     *            ByteBuffer into which output from the job is written
     * @param outPosition
     *            the position() of the outBuffer
     * @param outLimit
     *            the limit() of the outBuffer
     * @param patchBaseFileSeeker
     *            Only used by patch job. Provides chunks of the base file
     * @param patchArg
     *            Only used by a patch job. This value must have been returned by patchBegin()
     * @return an rs_result
     */
    private static native long iterateJob(long job, ByteBuffer inBuffer, int inPosition,
                                          int inLimit, boolean isLastInput,
                                          ByteBuffer outBuffer, int outPosition, int outLimit,
                                          RsyncInputSeeker patchBaseFileSeeker, long patchArg);

    /**
     * Free job resources. Calls librsync's rs_job_free(); This method MUST BE CALLED after a job is
     * completed to free resources associated with the job.
     *
     * @param job
     *            the job returned by one of the <jobType>Begin() methods.
     * @return rs_result from C. Can be converted to an RsyncResult by calling
     *         RsyncResult.fromCvalue()
     */
    private static native long freeJob(long job, long patchJobArg);

    /**
     * Free loaded signature resources. Calls librsync's rs_free_sumset
     * 
     * @param signaturePointer
     *            pointer to the signature that was loaded. Must be the value returned in the result
     *            from signatureBegin()
     */
    private static native void freeLoadedSignature(long signaturePointer);

    /**
     * Throws an IOException if the resultCode is not RS_DONE
     *
     * @param resultCode
     *            the result code to validate
     * @param errMsg
     *            message to throw in the IOException if the result is not RS_DONE
     * @throws IOException
     */
    private static void validateResult(long resultCode, String errMsg) throws RsyncException {
        if (resultCode != RsyncResult.RS_DONE.cValue) {
            throw new RsyncException(errMsg, resultCode);
        }
    }

    /**
     * Run a job until it is complete (successfully or failed), with input taken from the
     * inputProducer, and output given to the outputConsumer. The job should already be set up, and
     * must be freed by the caller after return.
     *
     * @param jobTag
     *            the toString() of this is just used in LOGGER messages to identify the job
     * @param job
     *            handle to the job, returned by the <jobType>Begin methods, such as
     *            signatureBegin()
     * @param inputProducer
     *            produces input for the job. For example, for a signature job, this produces the
     *            contents of the file for which a signature is being generated.
     * @param outputConsumer
     *            consumes the output of the job. For example, for a signature job, this consumes
     *            the signature produced by librsync.
     * @param patchBaseFileSeeker
     *            Should only be provided for a patch job. This seeker is used by a patch job to
     *            retreive (seek) particular sections of the base file
     * @param patchArg
     *            Should only be provided for a patch job, and must be the argPointer returned in
     *            the result from patchBegin()
     * @param inBuf
     *            The direct byte buffer that will be passed to the inputProducer, and from which
     *            the input will be read
     * @param outBuf
     *            The direct byte buffer into which the output will be written, and that is passed
     *            to the outputConsumer
     * @param rsyncStats
     *            Optional object to hold statistics about the rsync job. If non-null, then the
     *            statistics will be gathered and written to this object.
     * @throws IOException
     *             if thrown from inputProducer or outputConsumer
     * @throws RsyncException
     *             if the rsync job returned anything other than RS_DONE, including if the
     *             patchBaseFileSeeker threw an exception or returned null
     */
    private static void runJobToCompletion(Object jobTag, long job,
                                           RsyncInputProducer inputProducer,
                                           RsyncOutputConsumer outputConsumer,
                                           RsyncInputSeeker patchBaseFileSeeker,
                                           long patchArg, ByteBuffer inBuf,
                                           ByteBuffer outBuf, RsyncStatistics jobStats)
            throws IOException, RsyncException {

        RsyncResult result;
        inBuf.clear();
        outBuf.clear();
        outBuf = (outputConsumer == null ? null : outBuf);

        boolean doneProducingInput = false;
        int positionOfNextRead = 0;

        if (jobStats != null) {
            jobStats.begin();
        }

        do {
            if (!doneProducingInput && inBuf.remaining() > 0) {
                doneProducingInput = inputProducer.produceInput(inBuf); // has to set position
                                                                        // only
            }
            inBuf.flip();
            if (positionOfNextRead > 0) {
                inBuf.position(positionOfNextRead); // reset the position back to the mark
                                                    // (where iterateJob left off reading the
                                                    // input)
            }

            int inRemainingBeforeIteration = inBuf.remaining();
            result = RsyncResult.fromCvalue(
                    iterateJob(job, inBuf, inBuf.position(), inBuf.limit(), doneProducingInput,
                               outBuf, (outBuf == null ? 0 : outBuf.position()),
                               (outBuf == null ? 0 : outBuf.limit()),
                               patchBaseFileSeeker, patchArg)
                    );
            if (result != RsyncResult.RS_DONE && result != RsyncResult.RS_BLOCKED) {
                LOGGER.log(Level.WARNING,
                           String.format("Rsync job failed with result %s for job %s",
                                         result,
                                         jobTag.toString()));
                throw new RsyncException(result.cValue);
            }
            int inConsumed = inRemainingBeforeIteration - inBuf.remaining();
            int inNotConsumed = inBuf.remaining();
            if (inBuf.remaining() > 0) { // some available input was not read by iterateJob()
                positionOfNextRead = inBuf.position();
                inBuf.position(inBuf.limit()); // We must write more starting where last write
                                               // left off; not overwrite previously written but
                                               // not yet read
                inBuf.limit(inBuf.capacity()); // So we can write all the way to the end of the
                                               // buffer
            } else {
                inBuf.clear();
                positionOfNextRead = -1;
            }

            int outProduced = 0;
            if (outBuf != null) {
                outBuf.flip();
                outProduced = outBuf.remaining();
                while (outBuf.remaining() > 0) {
                    outputConsumer.consumeOutput(outBuf);
                }
                outBuf.clear();
            }

            if (jobStats != null) {
                jobStats.iterations++;

                jobStats.totalInputConsumed += inConsumed;
                jobStats.maxInputConsumed = Math.max(jobStats.maxInputConsumed, inConsumed);
                jobStats.minInputConsumed = Math.min(jobStats.minInputConsumed, inConsumed);

                jobStats.totalInputNotConsumed += inNotConsumed;
                jobStats.maxInputNotConsumed =
                        Math.max(jobStats.maxInputNotConsumed, inNotConsumed);
                jobStats.minInputNotConsumed =
                        Math.min(jobStats.minInputNotConsumed, inNotConsumed);

                if (outBuf != null) {
                    jobStats.totalOutputProduced += outProduced;
                    jobStats.maxOutputProduced = Math.max(jobStats.maxOutputProduced,
                                                          outProduced);
                    jobStats.minOutputProduced = Math.min(jobStats.minOutputProduced,
                                                          outProduced);
                }

                LOGGER.log(Level.FINE,
                           "Iteration {0} for rsync job <{1}>: input consumed = {2}, input not consumed = {3}, output produced = {4}, total output produced = {5}",
                           new Object[] { jobStats.iterations, jobTag, inConsumed, inNotConsumed,
                                   outProduced, jobStats.totalOutputProduced });
            }
        } while (result != RsyncResult.RS_DONE);

        if (jobStats != null) {
            jobStats.end();
            LOGGER.log(Level.INFO, "Job statistics for job <{0}>: {1}",
                       new Object[] { jobTag, jobStats.toString() });
        }
    }

    /**
     * Free a job. Must be called after finishing processing a job
     *
     * @param jobPointer
     *            Must be the value returned by <jobType>Begin(), e.g. signatureBegin()
     *
     * @return rs_result from C. Can be converted to an RsyncResult by calling
     *         RsyncResult.fromCvalue()
     */
    private static long freeJob(long jobPointer) {
        return freeJob(jobPointer, 0);
    }
}
