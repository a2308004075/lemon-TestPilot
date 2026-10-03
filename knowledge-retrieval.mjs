// 知识检索服务：semantic（向量）→ keyword（关键词）降级链，软引用记入 retrieval_hits（不写 relations，不阻塞知识撤销）
export async function createRetrieval(db, { readProviderKey }) {
  await db.exec([
    "CREATE TABLE IF NOT EXISTS knowledge_embeddings (knowledge_id VARCHAR(64) PRIMARY KEY, vector BLOB NOT NULL, vector_model VARCHAR(128) NOT NULL, dim INT NOT NULL, updated_at VARCHAR(40) NOT NULL)",
    "CREATE TABLE IF NOT EXISTS retrieval_hits (id INT PRIMARY KEY AUTO_INCREMENT, run_id VARCHAR(64) NOT NULL, route_id VARCHAR(64) NOT NULL, knowledge_id VARCHAR(64) NOT NULL, title_snapshot VARCHAR(255) NOT NULL, version_snapshot INT NOT NULL, category VARCHAR(64) NOT NULL, score DOUBLE NOT NULL, method VARCHAR(16) NOT NULL, created_at VARCHAR(40) NOT NULL)"
  ]);
  const now = () => new Date().toISOString();
  const MAX_RECALL = 5;
  const SEMANTIC_MIN = 0.3;

  async function embeddingProvider() {
    const p = await db.one("SELECT * FROM providers WHERE enabled=1 AND purpose='embedding' AND key_mask<>'' ORDER BY name LIMIT 1");
    if (!p) return null;
    const key = await readProviderKey(p.id).catch(() => '');
    return key ? { ...p, key } : null;
  }
  async function embed(texts) {
    const p = await embeddingProvider();
    if (!p) return null;
    try {
      const response = await fetch(`${p.base_url.replace(/\/$/, '')}/embeddings`, { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${p.key}` }, body: JSON.stringify({ model: p.model, input: texts }), signal: AbortSignal.timeout(p.timeout * 1000) });
      if (!response.ok) return null;
      const data = (await response.json()).data;
      if (!Array.isArray(data) || data.length !== texts.length) return null;
      return { model: p.model, vectors: data.map(x => Float32Array.from(x.embedding || [])) };
    } catch { return null; }
  }
  const cosine = (a, b) => { let dot = 0, na = 0, nb = 0; for (let i = 0; i < a.length; i++) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; } return na && nb ? dot / Math.sqrt(na * nb) : 0; };
  const toBuffer = f => Buffer.from(f.buffer, f.byteOffset, f.byteLength);
  const toF32 = b => new Float32Array(b.buffer.slice(b.byteOffset, b.byteOffset + b.byteLength));

  async function candidates(categories) {
    if (!categories?.length) return [];
    const marks = categories.map(() => '?').join(',');
    return await db.all(`SELECT id,title,category,version,risk,content,updated_at FROM knowledge WHERE status='已发布' AND category IN (${marks})`, ...categories);
  }
  async function ensureEmbeddings(rows, model) {
    const stale = [];
    for (const row of rows) {
      const e = await db.one('SELECT updated_at FROM knowledge_embeddings WHERE knowledge_id=?', row.id);
      if (!e || e.updated_at < row.updated_at) stale.push(row);
    }
    for (let i = 0; i < stale.length; i += 16) {
      const batch = stale.slice(i, i + 16);
      const embedded = await embed(batch.map(r => `${r.title}\n${r.content}`));
      if (!embedded) return;
      for (let j = 0; j < batch.length; j++) await db.run('REPLACE INTO knowledge_embeddings VALUES(?,?,?,?,?)', batch[j].id, toBuffer(embedded.vectors[j]), model, embedded.vectors[j].length, now());
    }
  }
  function tokenize(text) {
    const lower = String(text).toLowerCase(), tokens = new Set();
    for (const m of lower.matchAll(/[a-z][a-z0-9_-]{2,}/g)) tokens.add(m[0]);
    for (const m of lower.matchAll(/[\u4e00-\u9fa5]{2,}/g)) { const s = m[0]; for (let n = 2; n <= 3; n++) for (let i = 0; i + n <= s.length; i++) tokens.add(s.slice(i, i + n)); }
    return [...tokens].slice(0, 160);
  }
  function shot(rows) {
    return rows.map(r => ({ id: r.id, title: r.title, category: r.category, version: r.version, risk: r.risk, score: Math.round((r.score || 0) * 1000) / 1000, snippet: String(r.content || '').slice(0, 160) }));
  }
  async function retrieve(text, categories, { limit = MAX_RECALL } = {}) {
    const rows = await candidates(categories);
    if (!rows.length) return { method: 'none', hits: [], notice: '未检索到相关知识' };
    const query = await embed([text]);
    if (query) {
      await ensureEmbeddings(rows, query.model);
      const stored = await db.all(`SELECT knowledge_id,vector FROM knowledge_embeddings WHERE knowledge_id IN (${rows.map(() => '?').join(',')})`, ...rows.map(r => r.id));
      const byId = new Map(rows.map(r => [r.id, r]));
      const scored = stored.map(s => { const row = byId.get(s.knowledge_id); return row ? { ...row, score: cosine(query.vectors[0], toF32(s.vector)) } : null; }).filter(Boolean).sort((a, b) => b.score - a.score);
      const hits = scored.filter(x => x.score >= SEMANTIC_MIN).slice(0, limit);
      if (hits.length) return { method: 'semantic', hits: shot(hits) };
    }
    const tokens = tokenize(text);
    if (tokens.length) {
      const scored = rows.map(r => { let score = 0; const title = r.title.toLowerCase(), content = String(r.content || '').toLowerCase(); for (const t of tokens) { if (title.includes(t)) score += 3; if (content.includes(t)) score += 1; } return { ...r, score }; }).filter(x => x.score > 0).sort((a, b) => b.score - a.score).slice(0, limit);
      if (scored.length) return { method: 'keyword', hits: shot(scored) };
    }
    return { method: 'none', hits: [], notice: '未检索到相关知识' };
  }
  async function recordHits(runId, routeId, hits, method) {
    for (const h of hits) await db.run('INSERT INTO retrieval_hits(run_id,route_id,knowledge_id,title_snapshot,version_snapshot,category,score,method,created_at) VALUES(?,?,?,?,?,?,?,?,?)', runId, routeId, h.id, h.title, h.version, h.category, h.score, method, now());
  }
  return { retrieve, recordHits };
}
