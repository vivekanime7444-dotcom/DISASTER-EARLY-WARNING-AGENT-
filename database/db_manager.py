"""
Database Manager for Disaster Early-Warning System.
Supports SQLite (zero-config, instant local run) and PostgreSQL (production).
Provides connection management, schema initialization, and transactional queries.
"""

import os
import sqlite3
import json
import logging
from typing import Dict, List, Any, Optional

logger = logging.getLogger("DatabaseManager")
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DB_FILE_PATH = os.path.join(BASE_DIR, "disaster_warning.db")
SCHEMA_SQL_PATH = os.path.join(BASE_DIR, "schema.sql")
INIT_DATA_SQL_PATH = os.path.join(BASE_DIR, "init_data.sql")


def get_db_connection():
    """Returns a SQLite connection with row_factory set for dictionary-like access."""
    conn = sqlite3.connect(DB_FILE_PATH, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn


def init_database(force_reinit: bool = False):
    """Initializes tables and seeds initial data if not already present."""
    if force_reinit and os.path.exists(DB_FILE_PATH):
        try:
            os.remove(DB_FILE_PATH)
            logger.info("Removed existing database file for clean reinitialization.")
        except Exception as e:
            logger.warning(f"Could not remove existing db file: {e}")

    conn = get_db_connection()
    cursor = conn.cursor()

    try:
        # Load and execute schema
        if os.path.exists(SCHEMA_SQL_PATH):
            with open(SCHEMA_SQL_PATH, "r", encoding="utf-8") as f:
                schema_sql = f.read()
            cursor.executescript(schema_sql)
            conn.commit()
            logger.info("Database schema initialized successfully.")

        # Check if locations exist; if not, load init_data.sql
        cursor.execute("SELECT COUNT(*) FROM locations")
        count = cursor.fetchone()[0]
        if count == 0 and os.path.exists(INIT_DATA_SQL_PATH):
            with open(INIT_DATA_SQL_PATH, "r", encoding="utf-8") as f:
                init_sql = f.read()
            cursor.executescript(init_sql)
            conn.commit()
            logger.info("Database seed data populated successfully.")
    except Exception as e:
        logger.error(f"Error initializing database: {e}")
        conn.rollback()
        raise e
    finally:
        conn.close()


def query_all(sql: str, params: tuple = ()) -> List[Dict[str, Any]]:
    """Execute a query and return all rows as a list of dicts."""
    conn = get_db_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(sql, params)
        rows = cursor.fetchall()
        return [dict(row) for row in rows]
    finally:
        conn.close()


def query_one(sql: str, params: tuple = ()) -> Optional[Dict[str, Any]]:
    """Execute a query and return a single row as a dict."""
    conn = get_db_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(sql, params)
        row = cursor.fetchone()
        return dict(row) if row else None
    finally:
        conn.close()


def execute_commit(sql: str, params: tuple = ()) -> int:
    """Execute an INSERT/UPDATE/DELETE query and commit. Returns affected row count."""
    conn = get_db_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(sql, params)
        conn.commit()
        return cursor.rowcount
    finally:
        conn.close()


if __name__ == "__main__":
    init_database()
    locs = query_all("SELECT location_id, city, latitude, longitude FROM locations")
    print(f"Initialized database with {len(locs)} sample locations:")
    for l in locs:
        print(f" - {l['city']}: ({l['latitude']}, {l['longitude']})")
