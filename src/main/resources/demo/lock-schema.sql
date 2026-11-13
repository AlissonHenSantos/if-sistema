CREATE TABLE IF NOT EXISTS demo_operation_lock (id INTEGER PRIMARY KEY);
MERGE INTO demo_operation_lock (id) KEY (id) VALUES (1);
