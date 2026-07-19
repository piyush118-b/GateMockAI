# Python package marker
import os
from dotenv import load_dotenv

# Load .env file from the parent directory (extraction-service/.env)
base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
dotenv_path = os.path.join(base_dir, ".env")
if os.path.exists(dotenv_path):
    load_dotenv(dotenv_path)
