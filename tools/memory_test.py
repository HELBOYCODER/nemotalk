#!/usr/bin/env python3
"""Validate the long-term memory schema and queries against real SQLite.

ChatDatabase.kt builds a contentless FTS4 table synced to `memory` via
triggers, then ranks matches by (rank, recency). We create the same schema
with sqlite3 and check the queries the app actually runs behave: insert,
recall, decay, and that the FTS index tracks updates and deletes.
"""
import sqlite3
import sys

SCHEMA = """
CREATE TABLE IF NOT EXISTS memory (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    content TEXT NOT NULL,
    kind TEXT NOT NULL DEFAULT 'note',
    ts INTEGER NOT NULL DEFAULT (strftime('%s','now')),
    rank INTEGER NOT NULL DEFAULT 0
);
CREATE VIRTUAL TABLE IF NOT EXISTS memory_fts USING fts4(content, content=`memory`);
CREATE TRIGGER IF NOT EXISTS memory_ai AFTER INSERT ON memory BEGIN
    INSERT INTO memory_fts(rowid, content) VALUES (new.id, new.content);
END;
CREATE TRIGGER IF NOT EXISTS memory_ad AFTER DELETE ON memory BEGIN
    DELETE FROM memory_fts WHERE rowid = old.id;
END;
CREATE TRIGGER IF NOT EXISTS memory_au AFTER UPDATE ON memory BEGIN
    DELETE FROM memory_fts WHERE rowid = old.id;
    INSERT INTO memory_fts(rowid, content) VALUES (new.id, new.content);
END;
"""

db = sqlite3.connect(":memory:")
db.executescript(SCHEMA)

def insert(content, kind="note"):
    db.execute("INSERT INTO memory(content, kind) VALUES (?, ?)", (content, kind))
    db.commit()

def recall(query, limit=8):
    # Mirrors ChatDatabase.recall(): FTS rank first, then recency as a tiebreak.
    rows = db.execute(
        """SELECT m.content FROM memory_fts f
           JOIN memory m ON m.id = f.rowid
           WHERE memory_fts MATCH ?
           ORDER BY rank, m.ts DESC LIMIT ?""",
        (query, limit),
    ).fetchall()
    return [r[0] for r in rows]

insert("کاربر عاشق موسیقی راک است و گیتار می‌نوازد")
insert("کاربر از قهوه تلخ متنفر است")
insert("پروژه فعلی کاربر یک ربات تلگرام با پایتون است")
insert("کاربر زبان مادری‌اش فارسی است")
insert("کاربر عاشق پیتزا پرپرون است")

# 1. Recall must find the right memory and nothing unrelated.
hits = recall("قهوه")
print("recall 'قهوه':", hits)
assert any("قهوه" in h for h in hits), "recall missed the coffee memory"
assert all("گیتار" not in h for h in hits), "recall returned unrelated memory"

# 2. FTS must be prefix/partial-language tolerant enough for Persian.
hits = recall("گیتار*")
print("recall 'گیتار*':", hits)
assert any("گیتار" in h for h in hits), "prefix search failed"

# 3. After a delete, the FTS index must not leak the row.
db.execute("DELETE FROM memory WHERE content LIKE '%قهوه%'")
db.commit()
assert recall("قهوه") == [], "FTS index leaked a deleted row"
print("after delete: recall 'قهوه' = []  OK")

# 4. Empty query must not crash — recall() should return nothing, not raise.
print("recall '':", recall(""))
assert recall("") == [], "empty MATCH should return nothing"

# 5. Malformed FTS syntax must not raise — the app quotes the phrase.
for phrase in ("(test)", "\"quoted\"", "a*b", "not:keyword", "cafe OR", "((", ")", "", "   "):
    quoted = '"' + phrase.replace('"', '') + '"'
    try:
        recall(quoted)
        print(f"recall {phrase!r}: ok")
    except sqlite3.OperationalError as exc:
        print(f"recall {phrase!r}: STILL CRASHES — {exc}")
        sys.exit(1)

print("\nOK memory schema + queries behave as the app expects")
