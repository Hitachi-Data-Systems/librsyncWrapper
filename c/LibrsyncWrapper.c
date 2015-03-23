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

//
// This file implements the C side of the JNI interface
// defined in LibrsyncWrapper.java (and in the JNI generated file
// com_hds_aw_commons_librsync_LibrsyncWrapper.h).
// 
// The librsync library can be found here:  https://github.com/librsync/librsync
// 
// Author: Beth Tirado, Hitachi Data Systems
//
#include "com_hds_aw_commons_librsync_LibrsyncWrapper.h"
#include <unistd.h>
#include "librsync.h"
#include <stdlib.h>

typedef struct wrapper_copy_arg {
    jobject     *patchBaseFileSeeker;
    JNIEnv      *env;
} wrapper_copy_arg_t;

typedef struct job_result {
    rs_job_t       *job;
    void           *arg;
} job_result_t;


void logMessage(char *s)
{
    printf(s);
    printf("\n");
    fflush(stdout);
}

/**
 * Begin signature creation on a file.  Calls librsync's rs_sig_begin().
 *
 * @return a long, which is a pointer to the rs_job_t
 */
JNIEXPORT jlong JNICALL Java_com_hds_aw_commons_librsync_LibrsyncWrapper_signatureBegin
  (JNIEnv *env, jclass cls, jint blockSize)
{
    rs_job_t *job = rs_sig_begin((size_t) blockSize, RS_MD4_LENGTH);
    return (jlong) job;
}


/**
 * Begin loading a signature.  Calls librsync's rs_loadsig_begin()
 *
 * @return a LoadSignatureResult, which has the job and the pointer to where the signature will be loaded
 */
JNIEXPORT jlong JNICALL Java_com_hds_aw_commons_librsync_LibrsyncWrapper_loadSignatureBegin
  (JNIEnv *env, jclass cls, jobject result)
{
    rs_signature_t *signature;
    rs_job_t *job = rs_loadsig_begin(&signature);

    jclass resultClass = (*env)->GetObjectClass(env, result);
    jmethodID jobMethod = (*env)->GetMethodID(env, resultClass, "setJob", "(J)V");
    if (jobMethod == NULL) {
        return RS_INTERNAL_ERROR;
    }

    jmethodID signatureMethod = (*env)->GetMethodID(env, resultClass, "setSignature", "(J)V");
    if (signatureMethod == NULL) {
        return RS_INTERNAL_ERROR;
    }

    (*env)->CallObjectMethod(env, result, jobMethod, job);
    (*env)->CallObjectMethod(env, result, signatureMethod, signature);

    return RS_DONE;
}

/**
 * Must be called when done loading the signature.  Calls librsync's rs_build_hash_table
 *
 * @return true if successfully built the signature hash table; false otherwise
 */
JNIEXPORT jlong JNICALL Java_com_hds_aw_commons_librsync_LibrsyncWrapper_buildSignatureHashTable
  (JNIEnv *env, jclass cls, jlong signaturePointer)
{
    rs_result result = rs_build_hash_table((rs_signature_t *) signaturePointer);
    return result;
}


/**
 * Begin calculating the delta between a signature and a new file.  Calls librsync's rs_delta_begin()
 *
 * @return a long, which is a pointer to the rs_job_t
 */
JNIEXPORT jlong JNICALL Java_com_hds_aw_commons_librsync_LibrsyncWrapper_deltaBegin
  (JNIEnv *env, jclass cls, jlong signaturePointer) 
{
    rs_job_t *job = rs_delta_begin((rs_signature_t *) signaturePointer);
    return (jlong) job;
}


/**
 * Called by a patch job, to copy a portion of the base file into buf.
 * Calls the Java RsyncInputSeeker to get the portion of the base file.
 */
rs_result wrapper_file_copy_cb(void *arg, rs_long_t pos, size_t *len, void **buf)
{
    wrapper_copy_arg_t *copy_arg = (wrapper_copy_arg_t *)arg;
    JNIEnv      *env = copy_arg->env;

    jclass cls = (*env)->GetObjectClass(env, *copy_arg->patchBaseFileSeeker);
    if (cls == NULL) {
        logMessage("wrapper_file_copy_cb: Failed to find patchBaseFileSeeker class");
        return RS_INTERNAL_ERROR;
    }
    jmethodID seekMethod = (*env)->GetMethodID(env, cls, "seek", "(JI)Ljava/nio/ByteBuffer;");
    if (seekMethod == NULL) {
        logMessage("wrapper_file_copy_cb: Failed to find seek method of RsyncInputSeeker");
        return RS_INTERNAL_ERROR;
    }

    jobject byteBuf = (*env)->CallObjectMethod(env, *copy_arg->patchBaseFileSeeker, seekMethod, (jlong)pos, (jint)*len);
    if (byteBuf == NULL) {
        logMessage("wrapper_file_copy_cb: seek() returned null ByteBuffer");
        return RS_INTERNAL_ERROR;
    }
    jclass bufferClass = (*env)->GetObjectClass(env, byteBuf);
    if (bufferClass == NULL) {
        logMessage("wrapper_file_copy_cb: No class found for returned ByteBuffer");
        return RS_INTERNAL_ERROR;
    }

    jmethodID positionMethod = (*env)->GetMethodID(env, bufferClass, "position", "()I");
    if (positionMethod == NULL) {
        logMessage("wrapper_file_copy_cb: No position() method found in ByteBuffer");
        return RS_INTERNAL_ERROR;
    }
    jint position = (*env)->CallIntMethod(env, byteBuf, positionMethod);

    jmethodID limitMethod = (*env)->GetMethodID(env, bufferClass, "limit", "()I");
    if (limitMethod == NULL) {
        logMessage("wrapper_file_copy_cb: No limit() method found in ByteBuffer");
        return RS_INTERNAL_ERROR;
    }
    jint limit = (*env)->CallIntMethod(env, byteBuf, limitMethod);

    if (limit < 0 || position < 0) {
        logMessage("wrapper_file_copy_cb: Did not read expected number of bytes from RsyncInputSeeker");
        return RS_INTERNAL_ERROR;
    }

    *buf = (void *)(*env)->GetDirectBufferAddress(env, byteBuf) + position;

    return RS_DONE;
}


/**
 * Start applying a delta to a basis to recreate the new file.  Calls librsync's rs_patch_begin()
 *
 */
JNIEXPORT jlong JNICALL Java_com_hds_aw_commons_librsync_LibrsyncWrapper_patchBegin
  (JNIEnv *env, jclass cls, jobject baseFileSeeker, jobject result)
{
    wrapper_copy_arg_t *copy_arg = (wrapper_copy_arg_t *) calloc(1, sizeof(wrapper_copy_arg_t));
    if (!copy_arg) {
        logMessage("patchBegin: couldn't allocate instance of wrapper_copy_arg_t");
        return RS_INTERNAL_ERROR;
    }

    copy_arg->patchBaseFileSeeker = &baseFileSeeker;
    copy_arg->env = env;

    rs_job_t *job = rs_patch_begin((rs_copy_cb *) wrapper_file_copy_cb, copy_arg);

    jclass resultClass = (*env)->GetObjectClass(env, result);
    jmethodID jobMethod = (*env)->GetMethodID(env, resultClass, "setJob", "(J)V");
    if (jobMethod == NULL) {
        logMessage("patchBegin: Null jobMethod in patchBegin");
        return RS_INTERNAL_ERROR;
    }

    jmethodID argMethod = (*env)->GetMethodID(env, resultClass, "setArg", "(J)V");
    if (argMethod == NULL) {
        logMessage("patchBegin: Null argMethod in patchBegin");
        return RS_INTERNAL_ERROR;
    }

    (*env)->CallObjectMethod(env, result, jobMethod, job);
    (*env)->CallObjectMethod(env, result, argMethod, copy_arg);

    return RS_DONE;
}



/**
 * Iterate (once) over a job.  Calls librsync's rs_job_iter().
 * This should be called repeatedly until the job is done.
 */
JNIEXPORT jlong JNICALL Java_com_hds_aw_commons_librsync_LibrsyncWrapper_iterateJob
  (JNIEnv *env, jclass cls, jlong jobPointer, jobject inBuffer, jint inPosition, jint inLimit, jboolean lastInput,
   jobject outBuffer, jint outPosition, jint outLimit, jobject patchBaseFileSeeker, jlong patchArg)
{
    rs_job_t *job = (rs_job_t *) jobPointer;

    // If provided (and if this is a patch job), save the patchBaseFileSeeker and the JNIEnv in the job
    wrapper_copy_arg_t *wrapper_arg;
    if (patchArg != 0) {
        wrapper_arg = (wrapper_copy_arg_t *)patchArg;
    }
    if (patchBaseFileSeeker != NULL && wrapper_arg != NULL) {
        wrapper_arg->patchBaseFileSeeker = &patchBaseFileSeeker;
        wrapper_arg->env = env;
    }

    jclass bufferClass = (*env)->GetObjectClass(env, inBuffer);
    jmethodID positionMethod = (*env)->GetMethodID(env, bufferClass, "position", "(I)Ljava/nio/Buffer;");

    char *inBuf = (char*)(*env)->GetDirectBufferAddress(env, inBuffer);

    size_t avail_in = inLimit - inPosition;
    size_t avail_out = outLimit - outPosition;

    rs_buffers_t buffersInfo;
    char *inBegin = inBuf + inPosition;
    buffersInfo.next_in = inBegin;
    buffersInfo.avail_in = avail_in;
    buffersInfo.eof_in = lastInput;
    if (outBuffer != NULL) {
        buffersInfo.next_out = (char*)(*env)->GetDirectBufferAddress(env, outBuffer) + outPosition;
        buffersInfo.avail_out = avail_out;
    }

    rs_result result = rs_job_iter(job, &buffersInfo);
    if (result == RS_DONE || result == RS_BLOCKED) {
        int readCnt = avail_in - buffersInfo.avail_in;
        (*env)->CallObjectMethod(env, inBuffer, positionMethod, inPosition + readCnt);

        if (outBuffer != NULL) {
            int writeCnt = avail_out - buffersInfo.avail_out;
            (*env)->CallObjectMethod(env, outBuffer, positionMethod, outPosition + writeCnt);
        }
    }

    return result;
}


/**
 * Free resources associated with a job.  Calls librsync's rs_job_free().
 */
JNIEXPORT jlong JNICALL Java_com_hds_aw_commons_librsync_LibrsyncWrapper_freeJob
  (JNIEnv *env, jclass cls, jlong jobPointer, jlong patchArg)
{
    rs_job_t *job = (rs_job_t *) jobPointer;

    if (patchArg != 0) {
        free((wrapper_copy_arg_t *)patchArg);
    }

    return (jlong) rs_job_free((rs_job_t *)jobPointer);
}


/**
 * Free resources associated with a loaded signature.  Calls librsync's rs_free_sumset().
 */
JNIEXPORT void JNICALL Java_com_hds_aw_commons_librsync_LibrsyncWrapper_freeLoadedSignature
  (JNIEnv *env, jclass cls, jlong signaturePointer)
{
    rs_free_sumset((rs_signature_t *)signaturePointer);
}
