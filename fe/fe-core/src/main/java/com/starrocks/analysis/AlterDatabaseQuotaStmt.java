// This file is made available under Elastic License 2.0.
// This file is based on code available under the Apache license here:
//   https://github.com/apache/incubator-doris/blob/master/fe/fe-core/src/main/java/org/apache/doris/analysis/AlterDatabaseQuotaStmt.java

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

package com.starrocks.analysis;

import com.starrocks.sql.ast.AstVisitor;

public class AlterDatabaseQuotaStmt extends DdlStmt {
    private String dbName;
    private QuotaType quotaType;
    private String quotaValue;
    private long quota;

    public enum QuotaType {
        NONE,
        DATA,
        REPLICA
    }

    public AlterDatabaseQuotaStmt(String dbName, QuotaType quotaType, String quotaValue) {
        this.dbName = dbName;
        this.quotaType = quotaType;
        this.quotaValue = quotaValue;
    }

    public String getDbName() {
        return dbName;
    }

    public long getQuota() {
        return quota;
    }

    public String getQuotaValue() {
        return quotaValue;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public void setQuota(long quota) {
        this.quota = quota;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    @Override
    public String toSql() {
        return "ALTER DATABASE " + dbName + " SET " + (quotaType == QuotaType.DATA ? "DATA" : "REPLICA") + " QUOTA " +
                quotaValue;
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context) {
        return visitor.visitAlterDbQuotaStmt(this, context);
    }

    @Override
    public boolean isSupportNewPlanner() {
        return true;
    }
}
