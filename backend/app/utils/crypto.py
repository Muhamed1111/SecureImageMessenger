from Crypto.Cipher import AES
from Crypto.Protocol.KDF import PBKDF2
from Crypto.Random import get_random_bytes
from Crypto.Hash import HMAC, SHA256

PBKDF2_ITERS = 200_000
SALT_LEN = 16
IV_LEN = 16
KEY_LEN = 32  # AES-256

MAGIC = b"SIM1"

class CryptoError(Exception):
    pass

def _derive_keys(password: str, salt: bytes):
    master = PBKDF2(password, salt, dkLen=KEY_LEN * 2, count=PBKDF2_ITERS, hmac_hash_module=SHA256)
    enc_key, mac_key = master[:KEY_LEN], master[KEY_LEN:]
    return enc_key, mac_key

def encrypt_and_mac(plaintext: bytes, password: str) -> bytes:
    salt = get_random_bytes(SALT_LEN)
    iv = get_random_bytes(IV_LEN)
    enc_key, mac_key = _derive_keys(password, salt)

    pad = AES.block_size - (len(plaintext) % AES.block_size)
    padded = plaintext + bytes([pad]) * pad

    cipher = AES.new(enc_key, AES.MODE_CBC, iv)
    ciphertext = cipher.encrypt(padded)

    header = MAGIC + salt + iv
    mac = HMAC.new(mac_key, header + ciphertext, digestmod=SHA256).digest()
    return header + mac + ciphertext

def decrypt_and_verify(blob: bytes, password: str) -> bytes:
    if len(blob) < 4 + SALT_LEN + IV_LEN + 32:
        raise CryptoError("Payload too short")
    magic = blob[:4]
    if magic != MAGIC:
        raise CryptoError("Invalid magic header")
    salt = blob[4:4+SALT_LEN]
    iv = blob[4+SALT_LEN:4+SALT_LEN+IV_LEN]
    mac = blob[4+SALT_LEN+IV_LEN:4+SALT_LEN+IV_LEN+32]
    ciphertext = blob[4+SALT_LEN+IV_LEN+32:]

    enc_key, mac_key = _derive_keys(password, salt)
    try:
        HMAC.new(mac_key, blob[:4+SALT_LEN+IV_LEN] + ciphertext, digestmod=SHA256).verify(mac)
    except ValueError:
        raise CryptoError("HMAC verification failed (wrong password or corrupted data)")

    cipher = AES.new(enc_key, AES.MODE_CBC, iv)
    padded = cipher.decrypt(ciphertext)
    pad = padded[-1]
    if pad == 0 or pad > AES.block_size:
        raise CryptoError("Invalid padding")
    return padded[:-pad]
