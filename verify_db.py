import sqlite3
import os

db_path = r'C:\Users\jonaw\AppData\Local\Temp\backup_verify\coolbox_database'
if not os.path.exists(db_path):
    print(f"Error: {db_path} not found")
    exit(1)

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# Check columns of food_items
cursor.execute("PRAGMA table_info(food_items)")
columns = cursor.fetchall()
print("Columns in food_items:")
for col in columns:
    print(f"- {col[1]} ({col[2]})")

# Count columns
col_names = [col[1] for col in columns]
print(f"\nTotal columns: {len(col_names)}")

# Verification logic simulation
if 'itemType' not in col_names:
    print("\nSUCCESS: 'itemType' column is MISSING in this old backup.")
    print("The new code will correctly default it to 0.")
else:
    print("\nWARNING: 'itemType' column ALREADY EXISTS in this backup.")

conn.close()
