package org.duh102.duhbot.moolah.db.migration;

import java.sql.Connection;
import java.util.List;

public interface Migrator {
    DatabaseVersion getVersion();
    List<DatabaseVersion>  getCompatibleBaseVersions();
    // These methods must either complete successfully or roll back by
    // themselves, there is no external backup methodology in case of failure
    void upgrade(Connection connection);
    void downgrade(DatabaseVersion downgradeTo, Connection connection);
    boolean downgradeCausesDataLoss();
}
