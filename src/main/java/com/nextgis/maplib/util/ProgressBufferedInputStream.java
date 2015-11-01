package com.nextgis.maplib.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Stream counting the total progressed data
 */
public class ProgressBufferedInputStream extends BufferedInputStream{
    protected int mTotalStreamSize;
    protected int mCurrentReadSize;
    public ProgressBufferedInputStream(InputStream in, int totalStreamSize) {
        super(in, Constants.IO_BUFFER_SIZE);
        mTotalStreamSize = totalStreamSize;
        mCurrentReadSize = 0;
    }

    public ProgressBufferedInputStream(InputStream in, int size, int totalStreamSize) {
        super(in, size);
        mTotalStreamSize = totalStreamSize;
        mCurrentReadSize = 0;
    }

    @Override
    public synchronized int available() throws IOException {
        return mTotalStreamSize - mCurrentReadSize;
    }

    @Override
    public synchronized int read()
            throws IOException
    {
        int b = super.read();
        if (b >= 0)
            mCurrentReadSize += 1;
        return b;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len)
            throws IOException
    {
        int n = super.read(b, off, len);
        if (n > 0)
            mCurrentReadSize += n;
        return n;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        int n = super.read(buffer);
        if (n > 0)
            mCurrentReadSize += n;
        return n;
    }

    @Override
    public synchronized long skip(long skip)
            throws IOException
    {
        long n = super.skip(skip);
        if (n > 0)
            mCurrentReadSize += n;
        return n;
    }

    @Override
    public synchronized void reset()
            throws IOException
    {
    /* A call to reset can still succeed if mark is not supported, but the
     * resulting stream position is undefined, so it's not allowed here. */
        if (!markSupported())
            throw new IOException("Mark not supported.");
        super.reset();
        mCurrentReadSize = markpos;
    }
}
