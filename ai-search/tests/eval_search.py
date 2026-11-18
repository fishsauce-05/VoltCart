#!/usr/bin/env python
"""Measures search quality on labelled queries over the demo catalogue, with the vectors stored by the backend.

usage: python tests/eval_search.py ../backend/data/app.db http://localhost:8000 > ../../report/results/search_quality.txt

Text: for each query, the semantic ranking (cosine with the stored text vectors, as SearchService does) is compared
with the keyword fallback (every word must appear, as ProductRepository.searchKeyword does). Success@3 counts the
queries with at least one relevant product in the top 3; Precision@3 is the share of relevant products in the top 3.
Image: every product picture is altered (mirrored, cropped, recoloured) and used as a query; the script reports how
often the original product, and a product of the same category, comes first.
"""
import io
import json
import sqlite3
import struct
import sys
import urllib.request

from PIL import Image, ImageEnhance, ImageOps

QUERIES = [
    ("noise cancelling headphones for long flights", {124, 126, 129, 132, 133}),
    ("tai nghe chống ồn", {124, 125, 126, 128, 129, 130, 131, 132, 133}),
    ("wireless earbuds", {125, 128, 130, 131, 135}),
    ("tai nghe nhét tai giá rẻ", {130, 131, 135}),
    ("headphones with long battery life", {127, 129, 133, 134}),
    ("on-ear headphones", {127, 134}),
    ("gaming laptop", {103, 105, 111}),
    ("laptop chơi game", {103, 105, 111}),
    ("light laptop for travel", {101, 102, 104, 107, 109}),
    ("laptop mỏng nhẹ cho sinh viên", {101, 102, 106, 107, 110}),
    ("laptop for programming", {101, 102, 104, 107, 109}),
    ("MacBook", {102, 109}),
    ("budget phone with a big battery", {116, 118, 120, 122}),
    ("điện thoại pin trâu giá rẻ", {116, 118, 120, 122}),
    ("phone with a stylus pen", {115}),
    ("foldable phone", {121}),
    ("điện thoại chụp ảnh đẹp", {113, 115, 117, 119}),
    ("Samsung phone", {115, 116, 121}),
    ("iPhone", {113, 114}),
    ("Sony headphones", {124, 131}),
]
OFF_TOPIC = ["nồi cơm điện", "running shoes", "washing machine"]


def floats(blob):
    return list(struct.unpack(f"<{len(blob) // 4}f", blob)) if blob else None


def cosine(a, b):
    dot = sum(x * y for x, y in zip(a, b))
    na = sum(x * x for x in a) ** 0.5
    nb = sum(y * y for y in b) ** 0.5
    return dot / (na * nb) if na and nb else 0.0


def post_json(base, path, payload):
    req = urllib.request.Request(base + path, json.dumps(payload).encode(), {"Content-Type": "application/json"})
    return json.load(urllib.request.urlopen(req))


def post_image(base, data):
    boundary = "----evalboundary"
    body = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"image\"; filename=\"q.png\"\r\n"
            f"Content-Type: image/png\r\n\r\n").encode() + data + f"\r\n--{boundary}--\r\n".encode()
    req = urllib.request.Request(base + "/embed/image", body,
                                 {"Content-Type": f"multipart/form-data; boundary={boundary}"})
    return json.load(urllib.request.urlopen(req))["vector"]


def keyword(db, text):
    words = [w for w in text.lower().split() if w]
    rows = db.execute("""
        SELECT p.productId, lower(p.name || ' ' || p.brand || ' ' || p.description || ' ' || coalesce(l.cpu, '') || ' ' ||
               coalesce(ph.chipset, '') || ' ' || coalesce(b.formFactor, '') || ' ' ||
               CASE p.category WHEN 'LAPTOP' THEN 'laptop notebook' WHEN 'PHONE' THEN 'phone smartphone'
                    ELSE 'headphones earbuds bluetooth' END)
        FROM products p JOIN shops s ON s.shopId = p.shopId
        LEFT JOIN laptops l ON l.productId = p.productId LEFT JOIN phones ph ON ph.productId = p.productId
        LEFT JOIN bluetoothHeadphones b ON b.productId = p.productId
        WHERE p.status = 'PUBLISHED' AND s.status = 'ACTIVE' ORDER BY p.name""").fetchall()
    return [pid for pid, t in rows if all(w in t for w in words)]


def main():
    db = sqlite3.connect(sys.argv[1])
    base = sys.argv[2].rstrip("/")
    vectors = {pid: (floats(t), floats(i)) for pid, t, i in
               db.execute("SELECT productId, textVector, imageVector FROM productVectors")}
    names = dict(db.execute("SELECT productId, name FROM products"))
    cats = dict(db.execute("SELECT productId, category FROM products"))
    print(f"Catalogue: {len(vectors)} indexed products; {len(QUERIES)} labelled queries\n")
    print(f"{'query':30} {'semantic top 3':51} S@3  P@3 | kw S@3")
    s_sem = s_key = p_sem = 0
    rel_scores = []
    for text, relevant in QUERIES:
        q = post_json(base, "/embed/text", {"text": text})["vector"]
        ranked = sorted(vectors, key=lambda pid: -cosine(q, vectors[pid][0]))
        top = ranked[:3]
        hit = any(p in relevant for p in top)
        prec = sum(p in relevant for p in top) / 3
        k = keyword(db, text)[:3]
        khit = any(p in relevant for p in k)
        s_sem += hit
        s_key += khit
        p_sem += prec
        rel_scores += [cosine(q, vectors[p][0]) for p in relevant if p in vectors]
        shown = ", ".join(names[p][:15] for p in top)
        print(f"{text[:30]:30} {shown[:51]:51} {int(hit):3} {prec:.2f} | {int(khit):3} ({len(keyword(db, text))})")
    n = len(QUERIES)
    print(f"\nSemantic search: Success@3 = {s_sem}/{n} ({s_sem / n:.0%}), mean Precision@3 = {p_sem / n:.2f}")
    print(f"Keyword search:  Success@3 = {s_key}/{n} ({s_key / n:.0%})")
    print(f"Cosine of relevant products: min {min(rel_scores):.2f}, mean {sum(rel_scores) / len(rel_scores):.2f}")
    print("\nOff-topic queries (no relevant product exists):")
    for text in OFF_TOPIC:
        q = post_json(base, "/embed/text", {"text": text})["vector"]
        best = max(vectors, key=lambda pid: cosine(q, vectors[pid][0]))
        print(f"  {text:20} best cosine {cosine(q, vectors[best][0]):.2f} ({names[best]})")

    print("\nImage queries: each product picture mirrored, cropped to 85% and recoloured")
    same = cat = 0
    scores = []
    for pid, (_t, img) in vectors.items():
        url = db.execute("SELECT imageUrl FROM products WHERE productId = ?", (pid,)).fetchone()[0]
        path = "../backend/src/main/resources/static" + url
        im = Image.open(path).convert("RGB")
        w, h = im.size
        im = ImageOps.mirror(im).crop((int(w * .075), int(h * .075), int(w * .925), int(h * .925)))
        im = ImageEnhance.Color(im).enhance(0.4)
        buf = io.BytesIO()
        im.save(buf, "PNG")
        q = post_image(base, buf.getvalue())
        best = max(vectors, key=lambda p: cosine(q, vectors[p][1]))
        scores.append(cosine(q, vectors[best][1]))
        same += best == pid
        cat += cats[best] == cats[pid]
    n = len(vectors)
    print(f"  original product first: {same}/{n} ({same / n:.0%}); same category first: {cat}/{n} ({cat / n:.0%})")
    print(f"  cosine of the first result: min {min(scores):.2f}, mean {sum(scores) / n:.2f}")

    print("\nNew pictures (not in the catalogue), with the rule of SearchService: score >= max(0.75, best - 0.06)")
    for name, wanted in (("query_headphones.png", "HEADPHONES"), ("query_laptop.png", "LAPTOP")):
        with open(f"../../report/demo/{name}", "rb") as f:
            q = post_image(base, f.read())
        ranked = sorted(((cosine(q, v[1]), p) for p, v in vectors.items()), reverse=True)
        cut = max(0.75, ranked[0][0] - 0.06)
        kept = [p for s, p in ranked if s >= cut]
        right = sum(cats[p] == wanted for p in kept)
        best_other = max(s for s, p in ranked if cats[p] != wanted)
        print(f"  {name:22} best {ranked[0][0]:.2f}, cut {cut:.2f}: {len(kept)} shown, {right} of them {wanted}; "
              f"best score of another category {best_other:.2f}")


if __name__ == "__main__":
    main()
