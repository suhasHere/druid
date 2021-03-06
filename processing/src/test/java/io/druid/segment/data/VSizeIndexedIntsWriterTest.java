/*
 * Licensed to Metamarkets Group Inc. (Metamarkets) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Metamarkets licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.druid.segment.data;

import com.google.common.primitives.Ints;
import io.druid.segment.writeout.OffHeapMemorySegmentWriteOutMedium;
import io.druid.segment.writeout.WriteOutBytes;
import io.druid.segment.writeout.SegmentWriteOutMedium;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class VSizeIndexedIntsWriterTest
{
  private static final int[] MAX_VALUES = new int[]{0xFF, 0xFFFF, 0xFFFFFF, 0x0FFFFFFF};

  private final SegmentWriteOutMedium segmentWriteOutMedium = new OffHeapMemorySegmentWriteOutMedium();
  private final Random rand = new Random(0);
  private int[] vals;

  @Before
  public void setUp() throws Exception
  {
    vals = null;
  }

  @After
  public void tearDown() throws Exception
  {
    segmentWriteOutMedium.close();
  }

  private void generateVals(final int totalSize, final int maxValue) throws IOException
  {
    vals = new int[totalSize];
    for (int i = 0; i < vals.length; ++i) {
      vals[i] = rand.nextInt(maxValue);
    }
  }

  private void checkSerializedSizeAndData() throws Exception
  {
    int maxValue = vals.length == 0 ? 0 : Ints.max(vals);
    VSizeIndexedIntsWriter writer = new VSizeIndexedIntsWriter(segmentWriteOutMedium, maxValue);

    VSizeIndexedInts intsFromList = VSizeIndexedInts.fromList(
        IntArrayList.wrap(vals), maxValue
    );
    writer.open();
    for (int val : vals) {
      writer.add(val);
    }
    long writtenLength = writer.getSerializedSize();
    WriteOutBytes writeOutBytes = segmentWriteOutMedium.makeWriteOutBytes();
    writer.writeTo(writeOutBytes, null);

    assertEquals(writtenLength, intsFromList.getSerializedSize());

    // read from ByteBuffer and check values
    VSizeIndexedInts intsFromByteBuffer = VSizeIndexedInts.readFromByteBuffer(
        ByteBuffer.wrap(IOUtils.toByteArray(writeOutBytes.asInputStream()))
    );
    assertEquals(vals.length, intsFromByteBuffer.size());
    for (int i = 0; i < vals.length; ++i) {
      assertEquals(vals[i], intsFromByteBuffer.get(i));
    }
  }

  @Test
  public void testAdd() throws Exception
  {
    for (int maxValue : MAX_VALUES) {
      generateVals(rand.nextInt(100) + 10, maxValue);
      checkSerializedSizeAndData();
    }
  }

  @Test
  public void testWriteEmpty() throws Exception
  {
    vals = new int[0];
    checkSerializedSizeAndData();
  }
}
