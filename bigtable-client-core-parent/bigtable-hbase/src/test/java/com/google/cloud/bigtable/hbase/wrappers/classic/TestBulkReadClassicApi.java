/*
 * Copyright 2020 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.bigtable.hbase.wrappers.classic;

import static com.google.cloud.bigtable.hbase.adapters.Adapters.FLAT_ROW_ADAPTER;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFutures;
import com.google.cloud.bigtable.data.v2.models.Query;
import com.google.cloud.bigtable.grpc.async.BulkRead;
import com.google.cloud.bigtable.grpc.scanner.FlatRow;
import com.google.cloud.bigtable.hbase.wrappers.BulkReadWrapper;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.apache.hadoop.hbase.client.Result;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public class TestBulkReadClassicApi {

  private static final String TABLE_ID = "fake-Table-id";
  private static final FlatRow FLAT_ROW =
      FlatRow.newBuilder()
          .withRowKey(ByteString.copyFromUtf8("row-key"))
          .addCell("cf", ByteString.copyFromUtf8("q1"), 10000L, ByteString.copyFromUtf8("value"))
          .build();

  private BulkRead delegate;
  private BulkReadWrapper bulkReadWrapper;

  @Before
  public void setUp() {
    delegate = Mockito.mock(BulkRead.class);
    bulkReadWrapper = new BulkReadClassicApi(delegate);
  }

  @Test
  public void testAdd() throws ExecutionException, InterruptedException {
    Query query = Query.create(TABLE_ID).rowKey("row-key");
    when(delegate.add(query)).thenReturn(ApiFutures.immediateFuture(FLAT_ROW));
    Result result = bulkReadWrapper.add(query).get();
    assertArrayEquals(FLAT_ROW.getRowKey().toByteArray(), result.getRow());
    assertArrayEquals(FLAT_ROW_ADAPTER.adaptResponse(FLAT_ROW).rawCells(), result.rawCells());
    verify(delegate).add(query);
  }

  @Test
  public void testFlush() {
    doNothing().when(delegate).flush();
    bulkReadWrapper.flush();
    verify(delegate).flush();
  }

  @Test
  public void testClose() throws IOException {
    bulkReadWrapper.close();
    try {
      bulkReadWrapper.add(Query.create(TABLE_ID).rowKey("row-key-2"));
      Assert.fail("Bulk read should be closed");
    } catch (IllegalStateException actualException) {
      assertEquals("can't add request when the bulk read is closed.", actualException.getMessage());
    }
  }
}