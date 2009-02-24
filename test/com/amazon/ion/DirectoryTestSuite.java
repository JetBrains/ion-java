// Copyright (c) 2007-2009 Amazon.com, Inc.  All rights reserved.

package com.amazon.ion;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Base class for test suites constructed from the files in a single directory.
 *
 */
public abstract class DirectoryTestSuite
    extends TestSuite
{

    public DirectoryTestSuite(String testdataDir)
    {
        super();

        setName(getClass().getName());

        File goodFilesDir = IonTestCase.getTestdataFile(testdataDir);
        String[] fileNames = goodFilesDir.list();
        if (fileNames == null)
        {
            String message =
                "testdataDir is not a directory: " + testdataDir;
            throw new IllegalArgumentException(message);
        }

        List<String> skip = Arrays.asList(getFilesToSkip());

        // Sort the fileNames so they are listed in order.
        Arrays.sort(fileNames);
        for (String fileName : fileNames)
        {
            if (skip.contains(fileName))
            {
                System.err.println("WARNING: " + getName()
                                   + " skipping " + fileName);
                continue;
            }

            File testFile = new File(goodFilesDir, fileName);

            Test test = makeTest(testFile);
            if (test != null)
            {
                addTest(test);
            }
        }
    }

    /**
     * Creates a test case from one of the files in this suite's directory.
     *
     * @param testFile is a file in the directory of this suite.
     * It must not be <code>null</code>.
     * @return the test case, or <code>null</code> if the file should be
     * ignored.
     */
    protected abstract Test makeTest(File testFile);

    protected String[] getFilesToSkip()
    {
        return new String[0];
    }
}
