/*
 * Copyright (c) 2008 Amazon.com, Inc.  All rights reserved.
 */

package com.amazon.ion.streaming;

import com.amazon.ion.IonException;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.TtTimestamp;
import com.amazon.ion.impl.IonBinary;
import com.amazon.ion.impl.IonConstants;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implements the iterator pattern over various representations
 * of Ion, including text, binary and the IonValue tree.  This
 * class includes the static factory methods that will return
 * the appropriate IonIterator depending on the input value. This
 * included the logic to look into a byte array for the magic
 * cookie to determine it the input is binary or text (UTF-8)
 * Ion.
 */
public abstract class IonIterator
    implements IonReader
{
    static final protected Iterator<?> EMPTY_ITERATOR = new Iterator() {
        public boolean hasNext() { return false; }
        public Object  next()    { throw new NoSuchElementException(); }
        public void    remove()  { throw new UnsupportedOperationException(); }
    };



    /**
     * Creates an IonIterator instance over a byte array.
     *
     * @param buf must not be null.
     * @throws IonException if there's a problem reading the cookie, or if the
     * data does not start with {@link IonConstants#BINARY_VERSION_MARKER_1_0}.
     */
    public static IonReader makeIterator(byte[] buf, UnifiedCatalog catalog) {
        IonReader iter = makeIterator(buf, 0, buf.length, catalog);
        return iter;
    }

    /**
     * Creates an IonIterator instance over a portion of a byte array.
     *
     * @param buf must not be null.
     * @param start must be non-negative and less than the length of buf.
     * @param len must be non-negative and not extend (from start) past the end of buf.
     * @throws IonException if there's a problem reading the cookie, or if the
     * data does not start with {@link IonConstants#BINARY_VERSION_MARKER_1_0}.
     */
    public static IonReader makeIterator(byte[] buf, int start, int len, UnifiedCatalog catalog) {
        IonReader iter;
        if (IonBinary.matchBinaryVersionMarker(buf, start, len)) {
            iter = new IonBinaryIterator(buf, start, len, catalog);
        }
        else {
            iter = new IonTextIterator(buf, start, len, catalog);
        }
        return iter;
    }

    /**
     * Creates an IonIterator instance over Ion in text format in a Java
     * String.  The text may be parsed incrementally.
     *
     * @param ionText must not be null.
     * @throws IonException if there's a problem reading the cookie, or if the
     * data does not start with {@link IonConstants#BINARY_VERSION_MARKER_1_0}.
     */
    public static IonReader makeIterator(String ionText, UnifiedCatalog catalog) {
        IonReader iter;
        iter = new IonTextIterator(ionText, catalog);
        return iter;
    }


    /**
     * Returns true when there is addition content that can be read in the value.
     * The iteration takes place at the same "level" in the value it only
     * steps into a child value using stepInto().  So this returns whether
     * or not there is a sibling value that may be visited using next(). This
     * must be called before calling next() or next() may fail.  It may be
     * called multiple times, which does not move the current position.
     *
     */
    public abstract boolean    hasNext();

    /**
     * Positions the iterator on the next value.  This returns the underlying
     * IonType of the value that is found.  Once so positioned the contents of
     * this value can be accessed with the get methods.  This traverses the
     * contents at a constant level.
     * @throws NoSuchElementException if there are no more elements.
     */
    public abstract IonType    next();

    /**
     * Determines the number of children in the current value. The iterator
     * must be positioned on (but not yet stepped into) a container.
     * this operation is typically very efficient if the iterator is
     * traversing a tree, reasonably efficient (it does have to count)
     * when traversing a binary value, and it can be fairly expensive
     * if the undertly value is text.  As such this should only be used
     * when the benefits of knowing the number of elements is known to
     * outweight the costs of the call.  Using a flexible representation
     * in the caller is usually more efficient.
     */
    public abstract int         getContainerSize();

    /**
     * Positions the iterator in the contents of the current value.  The current
     * value must be a collection (sexp, list, or struct).  Once stepInto() has
     * been called hasNext() and next() will operate on the child members.  At
     * any time stepOut() may be called to move the iterator back to the parent
     * value.
     * @throws IllegalStateException if the current value isn't an Ion collection.
     */
    public abstract void       stepInto();

    /**
     * Positions the iterator after the current parents value.  Once stepOut()
     * has been called hasNext() must be called to see if a value follows
     * the parent.
     * @throws IllegalStateException if the current value wasn't stepped into.
     */
    public abstract void       stepOut();

    /**
     * returns the depth into the Ion value this iterator has traversed. The
     * top level, where it started out is depth 0.
     */
    public abstract int         getDepth();

    /**
     * Returns the current symbol table.
     */
    public abstract UnifiedSymbolTable getSymbolTable();

    /**
     * Returns IonType of the current value, or null if there is no valid current value.
     */
    public abstract IonType    getType();

    /**
     * Return an int representing the Ion type id of the current. This is the value
     * stored in the high nibble of the binary type descriptor byte, or -1 if there
     * is no valid current value.
     */
    public abstract int        getTypeId();

    /**
     * Return the annotations of the current value as an array of strings.  The
     * return value is null if there are no annotations on the current value.
     */
    public abstract String[]   getTypeAnnotations();

    /**
     * Return the symbol id's of the annotations on the current value as an
     * array of ints.  The return value is null if there are no annotations
     * on the current value.
     */
    public abstract int[]      getTypeAnnotationIds();

    /**
     * Return the annotations on the curent value as an iterator.  The
     * iterator is empty (hasNext() returns false on the first call) if
     * there are no annotations on the current value.
     */
    public abstract Iterator<String>   iterateTypeAnnotations();

    /**
     * Return the symbol table ids of the current values annotation as
     * an iterator.  The iterator is empty (hasNext() returns false on
     * the first call) if there are no annotations on the current value.
     */
    public abstract Iterator<Integer>   iterateTypeAnnotationIds();

    /**
     * Return an symbol table id of the field name of the current value. Or -1 if
     * there is no valid current value or if the current value is not a member
     * of a struct.
     */
    public abstract int        getFieldId();

    /**
     * Return the field name of the current value. Or null if there is no valid
     * current value or if the current value is not a member of a struct.
     */
    public abstract String     getFieldName();

    /* later, maybe, TODO
    / * *
     * Return the current value as an IonValue. This is only valid is the
     * iterator is associated with an IonSystem context.  This returns null if
     * there is no valid current value.
     * /
    public abstract int        getOrdinal();
    */

    /* later, maybe, for now, pass in the IonSystem in the method below
    / * *
     * Return the ordinal of the current value. Or null if there is no valid
     * current value or if the current value is not a member of a list or sexp.
     * /
    public abstract IonValue   getIonValue();
    */

    /**
     * Return the current value as an IonValue using the passed in IonSystem
     * context. This returns null if there is no valid current value.
     *
     * @param sys ion context for the returned value to be created under. This does not have be the same as the context of the iterators value, if it has one.
     */
    @Deprecated
    // move to system.newIonValue(IonIterator iterator)
    public abstract IonValue   getIonValue(IonSystem sys);

    /**
     * Returns the whether or not the current value a null ion value.
     * This is valid on all Ion types.  It should be called before
     * calling getters that return value types (int, long, boolean,
     * double).
     */
    public abstract boolean     isNullValue();

    /**
     * returns true if the iterator is currently operating over
     * members of a structure.  It returns false if the iteration
     * is in a list, a sexp, or a datagram.
     */
    public abstract boolean isInStruct();

    /**
     * Returns the current value as an boolean.  This is only valid if there is
     * an underlying value and the value is an ion boolean value.
     */
    public abstract boolean     booleanValue();

    /**
     * Returns the current value as an int.  This is only valid if there is
     * an underlying value and the value is of a numeric type (int, float, or
     * decimal).
     */
    public abstract int        intValue();

    /**
     * Returns the current value as a long.  This is only valid if there is
     * an underlying value and the value is of a numeric type (int, float, or
     * decimal).
     */
    public abstract long       longValue();

    /**
     * Returns the current value as a double.  This is only valid if there is
     * an underlying value and the value is either float, or decimal.
     */
    public abstract double     doubleValue();

    /**
     * Returns the current value as a BigDecimal.  This is only valid if there is
     * an underlying value and the value is decimal.
     */
    public abstract BigDecimal bigDecimalValue();

    /**
     * Returns the current value as a java.util.Date.  This is only valid if
     * there is an underlying value and the value is an Ion timestamp.
     */
    public abstract Date       dateValue();

    /**
     * Returns the current value as a timeinfo.  This is only valid if
     * there is an underlying value and the value is an Ion timestamp.
     */
    public abstract TtTimestamp timestampValue();

    /**
     * Returns the current value as a Java String.  This is only valid if there is
     * an underlying value and the value is either string or symbol.
     */
    public abstract String     stringValue();

    /**
     * Returns the current value as an int symbol id.  This is only valid if there is
     * an underlying value and the value is an Ion symbol.
     */
    public abstract int        getSymbolId();

    /**
     * Returns the current value as a byte array.  This is only valid if there is
     * an underlying value and the value is either blob or clob.
     */
    public abstract byte[]     newBytes();

    /**
     * Copies the current value into the passed in a byte array.  This is only
     * valid if there is an underlying value and the value is either blob or clob.
     *
     * @param buffer destination to copy the value into, this must not be null.
     * @param offset the first position to copy into, this must be non null and less than the length of buffer.
     * @param len the number of bytes available in the buffer to copy into, this must be long enough to hold the whole value and not extend outside of buffer.
     */
    public abstract int        getBytes(byte[] buffer, int offset, int len);

    /**
     * Returns the current value as a String using the Ion toString() serialization
     * format.  This is only valid if there is an underlying value.  This is
     * logically equivalent to getIonValue().toString() but may be more efficient
     * and does not require an IonSystem context to operate.
     */
    public String valueToString()
    {
        IonType t = this.getType();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IonWriter writer = new IonTextWriter(out);
        try
        {
            writer.writeIonValue(t, this);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
        String s = out.toString();
        return s;
    }
}