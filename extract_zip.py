import zipfile
import os

zip_path = r'C:\Users\jonaw\Downloads\CoolBox_Backup_1774193575395.cbk'
extract_path = r'C:\Users\jonaw\AppData\Local\Temp\backup_verify'

if not os.path.exists(zip_path):
    print(f"Error: {zip_path} not found")
    exit(1)

try:
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        print(f"Files in ZIP: {zip_ref.namelist()}")
        zip_ref.extractall(extract_path)
    print("Extraction successful")
except Exception as e:
    print(f"Error extracting ZIP: {e}")
    exit(1)
