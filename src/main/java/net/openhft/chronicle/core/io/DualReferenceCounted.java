/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.core.io;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.StackTrace;

@Deprecated(/* To be removed in x.25 */)
public class DualReferenceCounted implements MonitorReferenceCounted {
    private final MonitorReferenceCounted a;
    private final MonitorReferenceCounted b;
    private volatile int refCount;
    private volatile Throwable error;
    private int refCountB;

    public DualReferenceCounted(MonitorReferenceCounted a, MonitorReferenceCounted b) {
        this.a = a;
        this.b = b;
        this.refCount = a.refCount();
    }

    @Override
    public void warnAndReleaseIfNotReleased() throws ClosedIllegalStateException {
        a.warnAndReleaseIfNotReleased();
    }

    @Override
    public void throwExceptionIfNotReleased() throws IllegalStateException {
        a.throwExceptionIfNotReleased();
    }

    @Override
    public StackTrace createdHere() {
        return a.createdHere();
    }

    @Override
    public void addReferenceChangeListener(ReferenceChangeListener referenceChangeListener) {
        a.addReferenceChangeListener(referenceChangeListener);
    }

    @Override
    public void removeReferenceChangeListener(ReferenceChangeListener referenceChangeListener) {
        a.removeReferenceChangeListener(referenceChangeListener);
    }

    @Deprecated(/* To be removed in x.25 */)
    @Override
    public boolean reservedBy(ReferenceOwner owner) throws IllegalStateException {
        return a.reservedBy(owner);
    }

    @Override
    public synchronized void reserve(ReferenceOwner id) throws IllegalStateException {
        checkError();
        try {
            a.reserve(id);
            b.reserve(id);
            this.refCount = a.refCount();
            this.refCountB = b.refCount();
            if (this.refCount != refCountB)
                throw newAssertionError(this.refCount, refCountB, id);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Throwable e) {
            error = e;
            throw Jvm.rethrow(e);
        }
    }

    private void checkError() {
        if (error != null)
            throw new AssertionError("Unable to use this resource due to previous error", error);
        int aRefCount = a.refCount();
        int bRefCount = b.refCount();
        if (aRefCount != bRefCount) {
            final AssertionError ae = new AssertionError(aRefCount + " != " + bRefCount, error);
            error = ae;
            throw Jvm.rethrow(error);
        }
    }

    @Override
    public synchronized boolean tryReserve(ReferenceOwner id) throws IllegalStateException, IllegalArgumentException {
        checkError();
        try {
            boolean aa = a.tryReserve(id);
            boolean bb = b.tryReserve(id);
            assert aa == bb;
            this.refCount = a.refCount();
            this.refCountB = b.refCount();
            if (this.refCount != refCountB)
                throw newAssertionError(this.refCount, refCountB, id);
            return aa;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Throwable e) {
            error = e;
            throw Jvm.rethrow(e);
        }

    }

    @Override
    public synchronized void release(ReferenceOwner id) throws IllegalStateException {
        checkError();

        try {
            a.release(id);
            b.release(id);
            this.refCount = a.refCount();
            this.refCountB = b.refCount();
            if (this.refCount != refCountB)
                throw newAssertionError(this.refCount, refCountB, id);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Throwable e) {
            error = e;
            throw Jvm.rethrow(e);
        }
    }

    @Override
    public synchronized void releaseLast(ReferenceOwner id) throws IllegalStateException {
        checkError();
        try {
            a.releaseLast(id);
            b.releaseLast(id);
            this.refCount = a.refCount();
            this.refCountB = b.refCount();
            if (this.refCount != refCountB)
                throw new AssertionError(this.refCount + " != " + refCountB + " , id= " + id);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Throwable e) {
            error = e;
            throw Jvm.rethrow(e);
        }
    }

    @Override
    public int refCount() {
        return refCount;
    }

    @Override
    public synchronized void throwExceptionIfReleased() throws ClosedIllegalStateException {
        checkError();
        a.throwExceptionIfReleased();
    }

    @Override
    public synchronized void reserveTransfer(ReferenceOwner from, ReferenceOwner to) throws IllegalStateException {
        checkError();
        try {
            a.reserveTransfer(from, to);
            b.reserveTransfer(from, to);
            this.refCount = a.refCount();
            this.refCountB = b.refCount();
            if (this.refCount != refCountB)
                throw new AssertionError(refCount + " != " + refCountB + " , from= " + from + ", to=" + to);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Throwable e) {
            error = e;
            throw Jvm.rethrow(e);
        }
    }

    @Override
    public int referenceId() {
        return a.referenceId();
    }

    @Override
    public String referenceName() {
        return a.referenceName();
    }

    @Override
    public void unmonitored(boolean unmonitored) {
        a.unmonitored(unmonitored);
        b.unmonitored(unmonitored);
    }

    @Override
    public boolean unmonitored() {
        return a.unmonitored();
    }

    private static AssertionError newAssertionError(final int refCount,
                                                    final int refCountB,
                                                    final ReferenceOwner id) {
        return new AssertionError(refCount + " != " + refCountB + " , id= " + id);
    }

}
