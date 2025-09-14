from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import StreamingResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from io import BytesIO
from PIL import Image

from app.utils.crypto import encrypt_and_mac, decrypt_and_verify, CryptoError
from app.utils.stego import embed_png, extract_from_png, StegoError

app = FastAPI(title="Secure Image Messenger API")

# CORS za Android (dozvoli sve za dev)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class MessageOut(BaseModel):
    message: str

@app.post("/encrypt_and_embed")
async def encrypt_and_embed_endpoint(
    message: str = Form(...),
    password: str = Form(...),
    cover: UploadFile | None = File(default=None)
):
    try:
        blob = encrypt_and_mac(message.encode("utf-8"), password)
        if cover is not None:
            img = Image.open(BytesIO(await cover.read()))
        else:
            # generiši pseudo-noise cover dovoljne veličine
            import math, numpy as np
            required_bits = (4 + 4 + len(blob)) * 8
            pixels_needed = math.ceil(required_bits / 3)
            side = int(math.ceil(pixels_needed ** 0.5))
            arr = (np.random.rand(side, side, 3) * 255).astype("uint8")
            img = Image.fromarray(arr, mode="RGB")

        png_bytes = embed_png(img, blob)
        return StreamingResponse(BytesIO(png_bytes), media_type="image/png", headers={
            "Content-Disposition": "attachment; filename=secret.png"
        })
    except (CryptoError, StegoError) as e:
        return JSONResponse(status_code=400, content={"detail": str(e)})

@app.post("/extract_and_decrypt", response_model=MessageOut)
async def extract_and_decrypt_endpoint(
    image: UploadFile = File(...),
    password: str = Form(...)
):
    try:
        png_bytes = await image.read()
        payload = extract_from_png(png_bytes)
        plaintext = decrypt_and_verify(payload, password)
        return {"message": plaintext.decode("utf-8")}
    except (CryptoError, StegoError, UnicodeDecodeError) as e:
        return JSONResponse(status_code=400, content={"detail": str(e)})
