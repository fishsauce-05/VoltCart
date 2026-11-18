package com.ecommerce.common.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;

/** What every repository shares: the JdbcTemplate and the insert that returns the new key. */
public abstract class BaseRepository {
    protected final JdbcTemplate jdbc;

    protected BaseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Runs a parameterised INSERT and returns the rowid SQLite gave the new row. The pool holds one connection
     * (application.yaml), so last_insert_rowid() is read on the connection that ran the insert.
     */
    protected long insertAndGetId(String sql, Object... params) {
        jdbc.update(sql, params);
        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        if (id == null || id == 0) {
            throw new IllegalStateException("The database returned no key for: " + sql);
        }
        return id;
    }
}
