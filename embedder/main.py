import os
from typing import List, Union
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

MODEL_NAME = os.getenv("MODEL_NAME", "sentence-transformers/all-MiniLM-L6-v2")
print(f"Loading embedding model: {MODEL_NAME}...")
model = SentenceTransformer(MODEL_NAME)
DIMENSIONS = model.get_sentence_embedding_dimension()
print(f"Model loaded successfully! Output dimensions: {DIMENSIONS}")

app = FastAPI(
    title="DevPilot Embedder Service",
    description="High-performance local embeddings microservice for DevPilot RAG",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class OpenAIEmbeddingRequest(BaseModel):
    input: Union[str, List[str]]
    model: str = "all-MiniLM-L6-v2"


class NativeEmbedRequest(BaseModel):
    texts: List[str]


@app.get("/")
def health_check():
    return {
        "status": "UP",
        "service": "DevPilot Python Embedder",
        "model": MODEL_NAME,
        "dimensions": DIMENSIONS,
    }


# Standard OpenAI-compatible embeddings endpoint (Supported by Spring AI OpenAiEmbeddingModel)
@app.post("/v1/embeddings")
@app.post("/embeddings")
def openai_embeddings(req: OpenAIEmbeddingRequest):
    try:
        texts = [req.input] if isinstance(req.input, str) else req.input
        if not texts:
            return {"object": "list", "data": [], "model": req.model, "usage": {"prompt_tokens": 0, "total_tokens": 0}}

        embeddings = model.encode(texts, normalize_embeddings=True, show_progress_bar=False).tolist()

        data = [
            {"object": "embedding", "embedding": emb, "index": i}
            for i, emb in enumerate(embeddings)
        ]

        return {
            "object": "list",
            "data": data,
            "model": req.model,
            "usage": {"prompt_tokens": 0, "total_tokens": 0},
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Simple native REST endpoint
@app.post("/embed")
def native_embed(req: NativeEmbedRequest):
    try:
        if not req.texts:
            return {"embeddings": [], "dimensions": DIMENSIONS}

        embeddings = model.encode(req.texts, normalize_embeddings=True, show_progress_bar=False).tolist()
        return {"embeddings": embeddings, "dimensions": DIMENSIONS}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8000))
    uvicorn.run("main:app", host="0.0.0.0", port=port)
