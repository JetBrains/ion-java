// Copyright (c) 2011-2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.ion;

import com.amazon.ion.impl.PrivateUtils;
import java.util.Iterator;


public class TextByteArrayIteratorSystemProcessingTest
    extends IteratorSystemProcessingTestCase
{
    private byte[] myBytes;

    @Override
    protected void prepare(String text)
        throws Exception
    {
        myBytes = PrivateUtils.convertUtf16UnitsToUtf8(text);
    }

    @Override
    protected Iterator<IonValue> iterate()
    {
        return system().iterate(myBytes);
    }

    @Override
    protected Iterator<IonValue> systemIterate()
    {
        return system().systemIterate(myBytes);
    }
}
