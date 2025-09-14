# Secure Image Messenger – Backend

FastAPI servis koji:
1) AES-CBC enkriptuje poruku (PBKDF2 + HMAC integritet),
2) Ubacuje bytes u PNG (LSB 1 bit po kanalu),
3) Vadi i dekriptuje poruku iz PNG-a.

## Zahtjevi
- Python 3.11+

## Instalacija
```bash
cd backend
python -m venv .venv
# Windows PowerShell:
. .\.venv\Scripts\Activate.ps1
# macOS/Linux:
# source .venv/bin/activate

pip install -r requirements.txt
