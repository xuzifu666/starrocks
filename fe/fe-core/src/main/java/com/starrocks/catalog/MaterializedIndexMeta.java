// This file is made available under Elastic License 2.0.
// This file is based on code available under the Apache license here:
//   https://github.com/apache/incubator-doris/blob/master/fe/fe-core/src/main/java/org/apache/doris/catalog/MaterializedIndexMeta.java

// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.starrocks.catalog;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import com.starrocks.analysis.CreateMaterializedViewStmt;
import com.starrocks.analysis.Expr;
import com.starrocks.analysis.SqlParser;
import com.starrocks.analysis.SqlScanner;
import com.starrocks.common.io.Text;
import com.starrocks.common.io.Writable;
import com.starrocks.common.util.SqlParserUtils;
import com.starrocks.persist.gson.GsonPostProcessable;
import com.starrocks.persist.gson.GsonUtils;
import com.starrocks.qe.OriginStatement;
import com.starrocks.qe.SqlModeHelper;
import com.starrocks.thrift.TStorageType;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

public class MaterializedIndexMeta implements Writable, GsonPostProcessable {
    @SerializedName(value = "indexId")
    private long indexId;
    @SerializedName(value = "schema")
    private List<Column> schema = Lists.newArrayList();
    @SerializedName(value = "schemaVersion")
    private int schemaVersion;
    @SerializedName(value = "schemaHash")
    private int schemaHash;
    @SerializedName(value = "shortKeyColumnCount")
    private short shortKeyColumnCount;
    @SerializedName(value = "storageType")
    private TStorageType storageType;
    @SerializedName(value = "keysType")
    private KeysType keysType;
    @SerializedName(value = "defineStmt")
    private OriginStatement defineStmt;

    public MaterializedIndexMeta(long indexId, List<Column> schema, int schemaVersion, int schemaHash,
                                 short shortKeyColumnCount, TStorageType storageType, KeysType keysType,
                                 OriginStatement defineStmt) {
        this.indexId = indexId;
        Preconditions.checkState(schema != null);
        Preconditions.checkState(schema.size() != 0);
        this.schema = schema;
        this.schemaVersion = schemaVersion;
        this.schemaHash = schemaHash;
        this.shortKeyColumnCount = shortKeyColumnCount;
        Preconditions.checkState(storageType != null);
        this.storageType = storageType;
        Preconditions.checkState(keysType != null);
        this.keysType = keysType;
        this.defineStmt = defineStmt;
    }

    public long getIndexId() {
        return indexId;
    }

    public KeysType getKeysType() {
        return keysType;
    }

    public void setKeysType(KeysType keysType) {
        this.keysType = keysType;
    }

    public TStorageType getStorageType() {
        return storageType;
    }

    public List<Column> getSchema() {
        return schema;
    }

    public int getSchemaHash() {
        return schemaHash;
    }

    public short getShortKeyColumnCount() {
        return shortKeyColumnCount;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public String getOriginStmt() {
        if (defineStmt == null) {
            return null;
        } else {
            return defineStmt.originStmt;
        }
    }

    private void setColumnsDefineExpr(Map<String, Expr> columnNameToDefineExpr) {
        for (Map.Entry<String, Expr> entry : columnNameToDefineExpr.entrySet()) {
            for (Column column : schema) {
                if (column.getName().equals(entry.getKey())) {
                    column.setDefineExpr(entry.getValue());
                    break;
                }
            }
        }
    }

    public Column getColumnByName(String columnName) {
        for (Column column : schema) {
            if (column.getName().equalsIgnoreCase(columnName)) {
                return column;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MaterializedIndexMeta)) {
            return false;
        }
        MaterializedIndexMeta indexMeta = (MaterializedIndexMeta) obj;
        if (indexMeta.indexId != this.indexId) {
            return false;
        }
        if (indexMeta.schema.size() != this.schema.size() || !indexMeta.schema.containsAll(this.schema)) {
            return false;
        }
        if (indexMeta.schemaVersion != this.schemaVersion) {
            return false;
        }
        if (indexMeta.schemaHash != this.schemaHash) {
            return false;
        }
        if (indexMeta.shortKeyColumnCount != this.shortKeyColumnCount) {
            return false;
        }
        if (indexMeta.storageType != this.storageType) {
            return false;
        }
        if (indexMeta.keysType != this.keysType) {
            return false;
        }
        return true;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        Text.writeString(out, GsonUtils.GSON.toJson(this));
    }

    public static MaterializedIndexMeta read(DataInput in) throws IOException {
        String json = Text.readString(in);
        return GsonUtils.GSON.fromJson(json, MaterializedIndexMeta.class);
    }

    @Override
    public void gsonPostProcess() throws IOException {
        // analyze define stmt
        if (defineStmt == null) {
            return;
        }
        // parse the define stmt to schema
        SqlParser parser = new SqlParser(new SqlScanner(new StringReader(defineStmt.originStmt),
                SqlModeHelper.MODE_DEFAULT));
        CreateMaterializedViewStmt stmt;
        try {
            stmt = (CreateMaterializedViewStmt) SqlParserUtils.getStmt(parser, defineStmt.idx);
            stmt.isReplay = true;
            Map<String, Expr> columnNameToDefineExpr = stmt.parseDefineExprWithoutAnalyze();
            setColumnsDefineExpr(columnNameToDefineExpr);
        } catch (Exception e) {
            throw new IOException("error happens when parsing create materialized view stmt: " + defineStmt, e);
        }
    }

}
