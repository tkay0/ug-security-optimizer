package org.ugoptimizer.database.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.ugoptimizer.model.AlgorithmRun;

/** Maps one {@code algorithm_runs} result row to the shared domain model. */
public final class AlgorithmRunMapper {

    private AlgorithmRunMapper() {
    }

    public static AlgorithmRun map(ResultSet resultSet) throws SQLException {
        long timeValue = resultSet.getLong("time_ns");
        Long timeNs = resultSet.wasNull() ? null : timeValue;
        double memoryValue = resultSet.getDouble("memory_kb");
        Double memoryKb = resultSet.wasNull() ? null : memoryValue;
        String dateValue = resultSet.getString("date_run");

        try {
            return new AlgorithmRun(
                    resultSet.getInt("run_id"),
                    resultSet.getString("algorithm_name"),
                    resultSet.getInt("input_size"),
                    timeNs,
                    memoryKb,
                    dateValue == null ? null : Instant.parse(dateValue),
                    resultSet.getString("status"),
                    resultSet.getString("experiment_group"),
                    resultSet.getInt("run_number"));
        } catch (RuntimeException exception) {
            throw new SQLException("Invalid algorithm_runs row", exception);
        }
    }
}
