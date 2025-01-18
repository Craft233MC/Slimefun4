package com.xzavier0722.mc.plugin.slimefun4.storage.patch;

import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_INVENTORY_ITEM;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_TABLE_VERSION;

import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.mysql.MysqlConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.ISqlCommonConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlCommonConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlUtils;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlite.SqliteConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataScope;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabasePatchV1 extends DatabasePatch {
    public DatabasePatchV1() {
        this.version = 1;
    }

    @Override
    public void patch(Statement stmt, ISqlCommonConfig config) throws SQLException {
        var table = SqlUtils.mapTable(
                DataScope.TABLE_INFORMATION, config instanceof SqlCommonConfig scc ? scc.tablePrefix() : "");

        if (config instanceof SqliteConfig) {
            stmt.execute("INSERT INTO " + table + " (" + FIELD_TABLE_VERSION + ") SELECT " + getVersion()
                    + " WHERE NOT EXISTS (SELECT 1 FROM " + table + ");");
            stmt.execute("UPDATE " + table + " SET " + FIELD_TABLE_VERSION + " = '1' WHERE rowid = (SELECT rowid FROM "
                    + table + " LIMIT 1);");
        } else {
            stmt.execute("UPDATE " + table + " SET " + FIELD_TABLE_VERSION + " = '1' LIMIT 1");
        }

        if (config instanceof MysqlConfig mysqlConf) {
            var uniInvTable = SqlUtils.mapTable(DataScope.UNIVERSAL_INVENTORY, mysqlConf.tablePrefix());
            stmt.execute("ALTER TABLE " + uniInvTable + " MODIFY COLUMN " + FIELD_INVENTORY_ITEM + " TEXT;");
        }
    }
}
