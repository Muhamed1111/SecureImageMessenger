from PIL import Image
from io import BytesIO

class StegoError(Exception):
    pass

MAGIC = b"SIMG"  # header za stego sloj

def _to_bitstream(data: bytes):
    for byte in data:
        for i in range(7, -1, -1):
            yield (byte >> i) & 1

def _from_bitstream(bits):
    out = bytearray()
    while True:
        try:
            byte = 0
            for _ in range(8):
                bit = next(bits)
                byte = (byte << 1) | bit
            out.append(byte)
        except StopIteration:
            break
    return bytes(out)

def embed_png(cover_img: Image.Image, payload: bytes) -> bytes:
    import struct
    header = MAGIC + struct.pack(">I", len(payload))
    data = header + payload

    if cover_img.mode != "RGB":
        cover_img = cover_img.convert("RGB")
    pixels = cover_img.load()

    capacity_bits = cover_img.width * cover_img.height * 3
    needed_bits = len(data) * 8
    if needed_bits > capacity_bits:
        raise StegoError(f"Payload too large: need {needed_bits} bits, capacity {capacity_bits}")

    bitgen = iter(_to_bitstream(data))
    for y in range(cover_img.height):
        for x in range(cover_img.width):
            r, g, b = pixels[x, y]
            try:
                r = (r & 0xFE) | next(bitgen)
                g = (g & 0xFE) | next(bitgen)
                b = (b & 0xFE) | next(bitgen)
            except StopIteration:
                pixels[x, y] = (r, g, b)
                buf = BytesIO()
                cover_img.save(buf, format="PNG")
                return buf.getvalue()
            pixels[x, y] = (r, g, b)

    buf = BytesIO()
    cover_img.save(buf, format="PNG")
    return buf.getvalue()

def extract_from_png(png_bytes: bytes) -> bytes:
    import struct
    img = Image.open(BytesIO(png_bytes)).convert("RGB")
    pixels = img.load()

    def bit_iter():
        for y in range(img.height):
            for x in range(img.width):
                r, g, b = pixels[x, y]
                yield r & 1
                yield g & 1
                yield b & 1

    bits = bit_iter()
    header_bytes = _from_bitstream((next(bits) for _ in range(64)))  # 8 bajtova
    if header_bytes[:4] != MAGIC:
        raise StegoError("No SIMG header found in image")
    length = struct.unpack(">I", header_bytes[4:8])[0]

    payload_bits = (next(bits) for _ in range(length * 8))
    payload = _from_bitstream(payload_bits)
    return payload
