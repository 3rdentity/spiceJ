// @formatter:off
/*
 * THIS FILE IS TAKEN FROM THE ORACLE (JDK7) SOURCE
 * 
 *   This file is an unmodified (expect for the package name and the removed JavaDoc) copy of the
 *   Oracle JDK 7 implementation of PipedOutputStream.
 *
 *   According to [1], the JDK source code is distributed under GNU GPL v2, which permits redistribution
 *   and modification.
 *   
 *   [1] http://www.oracle.com/technetwork/articles/javase/opensourcejdk-jsp-136417.html
 */


/*
 * Copyright (c) 1995, 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package at.borkowski.spicej.streams.util;

import java.io.IOException;
import java.io.OutputStream;

public
class PipedOutputStream extends OutputStream {

        /* REMIND: identification of the read and write sides needs to be
           more sophisticated.  Either using thread groups (but what about
           pipes within a thread?) or using finalization (but it may be a
           long time until the next GC). */
    private PipedInputStream sink;

    public PipedOutputStream(PipedInputStream snk)  throws IOException {
        connect(snk);
    }

    public PipedOutputStream() {
    }

    public synchronized void connect(PipedInputStream snk) throws IOException {
        if (snk == null) {
            throw new NullPointerException();
        } else if (sink != null || snk.connected) {
            throw new IOException("Already connected");
        }
        sink = snk;
        snk.in = -1;
        snk.out = 0;
        snk.connected = true;
    }

    public void write(int b)  throws IOException {
        if (sink == null) {
            throw new IOException("Pipe not connected");
        }
        sink.receive(b);
    }

    public void write(byte b[], int off, int len) throws IOException {
        if (sink == null) {
            throw new IOException("Pipe not connected");
        } else if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                   ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        sink.receive(b, off, len);
    }

    public synchronized void flush() throws IOException {
        if (sink != null) {
            synchronized (sink) {
                sink.notifyAll();
            }
        }
    }

    public void close()  throws IOException {
        if (sink != null) {
            sink.receivedLast();
        }
    }
}
