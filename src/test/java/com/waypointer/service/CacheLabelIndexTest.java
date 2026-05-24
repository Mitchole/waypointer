package com.waypointer.service;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class CacheLabelIndexTest
{
    @Test
    public void loadsAtLeastOneRowFromMapLabelsTsv()
    {
        CacheLabelIndex idx = new CacheLabelIndex();
        assertTrue("expected map-labels-raw.tsv to contribute rows", idx.size() > 0);
    }
}
