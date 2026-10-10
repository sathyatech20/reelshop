import { useState } from "react";

const COLORS = {
  bg: "#0A0A0F",
  surface: "#111118",
  card: "#16161E",
  border: "#1E1E2E",
  accent: "#FF4D6D",
  gold: "#FFB347",
  green: "#00E5A0",
  blue: "#00BFFF",
  purple: "#9B5DE5",
  text: "#F0F0F8",
  muted: "#8888AA",
  dim: "#44445A",
};

const TABLES = {
  sellers: {
    color: COLORS.accent,
    icon: "🏪",
    label: "sellers",
    desc: "Core seller/shop identity",
    columns: [
      { name: "id",                    type: "BIGSERIAL",    pk: true,  note: "Primary key" },
      { name: "phone_number",          type: "VARCHAR(15)",  unique: true, notnull: true, note: "Login identifier" },
      { name: "is_phone_verified",     type: "BOOLEAN",      notnull: true, default: "false" },
      { name: "shop_name",             type: "VARCHAR(100)", note: "Display name" },
      { name: "shop_slug",             type: "VARCHAR(100)", unique: true, note: "URL path: /s/{slug}" },
      { name: "shop_bio",              type: "VARCHAR(250)" },
      { name: "profile_image_url",     type: "TEXT" },
      { name: "profile_image_public_id", type: "VARCHAR(200)", note: "Cloudinary ID" },
      { name: "category",              type: "VARCHAR(30)",  note: "FASHION|JEWELRY|FOOD_SNACKS|HOME_DECOR|BEAUTY|OTHER" },
      { name: "whatsapp_number",       type: "VARCHAR(15)" },
      { name: "upi_id",                type: "VARCHAR(100)" },
      { name: "theme_id",              type: "VARCHAR(30)",  default: "'midnight'" },
      { name: "status",                type: "VARCHAR(20)",  notnull: true, default: "'ACTIVE'", note: "ACTIVE|SUSPENDED|DELETED" },
      { name: "is_setup_complete",     type: "BOOLEAN",      notnull: true, default: "false" },
      { name: "total_qr_scans",        type: "BIGINT",       notnull: true, default: "0", note: "Denormalised counter" },
      { name: "total_whatsapp_clicks", type: "BIGINT",       notnull: true, default: "0" },
      { name: "created_at",            type: "TIMESTAMP",    notnull: true },
      { name: "updated_at",            type: "TIMESTAMP",    notnull: true },
    ],
    indexes: [
      "idx_seller_phone  (phone_number) UNIQUE",
      "idx_seller_slug   (shop_slug)    UNIQUE",
      "idx_seller_status (status)",
    ],
  },
  products: {
    color: COLORS.green,
    icon: "📦",
    label: "products",
    desc: "Product listings per seller",
    columns: [
      { name: "id",                    type: "BIGSERIAL",    pk: true },
      { name: "seller_id",             type: "BIGINT",       fk: "sellers.id", notnull: true },
      { name: "name",                  type: "VARCHAR(150)", notnull: true },
      { name: "description",           type: "TEXT" },
      { name: "price",                 type: "NUMERIC(10,2)", notnull: true, note: "INR" },
      { name: "short_code",            type: "VARCHAR(10)",  unique: true, notnull: true, note: "For /p/{code}" },
      { name: "stock_status",          type: "VARCHAR(20)",  notnull: true, default: "'AVAILABLE'", note: "AVAILABLE|OUT_OF_STOCK" },
      { name: "status",                type: "VARCHAR(20)",  notnull: true, default: "'ACTIVE'", note: "ACTIVE|DELETED" },
      { name: "qr_scan_count",         type: "BIGINT",       notnull: true, default: "0" },
      { name: "whatsapp_click_count",  type: "BIGINT",       notnull: true, default: "0" },
      { name: "upi_click_count",       type: "BIGINT",       notnull: true, default: "0" },
      { name: "qr_image_url",          type: "TEXT",         note: "Cached QR PNG URL" },
      { name: "qr_image_public_id",    type: "VARCHAR(200)", note: "Cloudinary ID" },
      { name: "created_at",            type: "TIMESTAMP",    notnull: true },
      { name: "updated_at",            type: "TIMESTAMP",    notnull: true },
    ],
    indexes: [
      "idx_product_seller (seller_id)",
      "idx_product_code   (short_code) UNIQUE",
      "idx_product_status (status)",
      "idx_product_seller_status (seller_id, status)",
    ],
  },
  product_images: {
    color: COLORS.purple,
    icon: "🖼️",
    label: "product_images",
    desc: "Up to 3 images per product",
    columns: [
      { name: "id",            type: "BIGSERIAL",    pk: true },
      { name: "product_id",    type: "BIGINT",       fk: "products.id", notnull: true },
      { name: "image_url",     type: "TEXT",         notnull: true, note: "Cloudinary URL" },
      { name: "public_id",     type: "VARCHAR(200)", note: "Cloudinary public_id" },
      { name: "display_order", type: "SMALLINT",     note: "0=primary photo" },
    ],
    indexes: [
      "idx_img_product (product_id)",
    ],
  },
  qr_scans: {
    color: COLORS.gold,
    icon: "◈",
    label: "qr_scans",
    desc: "Immutable scan event log",
    columns: [
      { name: "id",          type: "BIGSERIAL",   pk: true },
      { name: "product_id",  type: "BIGINT",      fk: "products.id", notnull: true },
      { name: "seller_id",   type: "BIGINT",      fk: "sellers.id",  notnull: true, note: "Denorm for fast queries" },
      { name: "scan_source", type: "VARCHAR(20)", default: "'QR_CODE'", note: "QR_CODE|SHORT_LINK|DIRECT" },
      { name: "user_agent",  type: "VARCHAR(300)" },
      { name: "ip_address",  type: "VARCHAR(50)" },
      { name: "scanned_at",  type: "TIMESTAMP",   notnull: true },
    ],
    indexes: [
      "idx_scan_product  (product_id)",
      "idx_scan_seller   (seller_id)",
      "idx_scan_created  (scanned_at)",
      "idx_scan_seller_date (seller_id, scanned_at)",
    ],
  },
  otp_records: {
    color: COLORS.blue,
    icon: "🔐",
    label: "otp_records",
    desc: "OTP audit log — no OTP values stored",
    columns: [
      { name: "id",            type: "BIGSERIAL",   pk: true },
      { name: "phone_number",  type: "VARCHAR(15)", notnull: true },
      { name: "status",        type: "VARCHAR(20)", notnull: true, note: "SENT|VERIFIED|EXPIRED|FAILED" },
      { name: "attempt_count", type: "SMALLINT",    default: "0" },
      { name: "ip_address",    type: "VARCHAR(50)" },
      { name: "created_at",    type: "TIMESTAMP",   notnull: true },
      { name: "verified_at",   type: "TIMESTAMP" },
    ],
    indexes: [
      "idx_otp_phone   (phone_number)",
      "idx_otp_created (created_at)",
    ],
  },
};

const REDIS_KEYS = [
  { key: "otp:{phoneNumber}",       value: "6-digit OTP string",     ttl: "5 min",  note: "Deleted on verify" },
  { key: "otp_rate:{phoneNumber}",  value: "Integer count",          ttl: "1 hour", note: "Rate limit: max 5/hr" },
];

function Badge({ children, color }) {
  return (
    <span style={{
      background: color + "22", color,
      fontSize: 9, fontFamily: "'DM Mono', monospace",
      fontWeight: 600, letterSpacing: 1,
      padding: "2px 6px", borderRadius: 4,
      border: `1px solid ${color}44`,
      textTransform: "uppercase",
      flexShrink: 0,
    }}>{children}</span>
  );
}

function TableCard({ table }) {
  const [open, setOpen] = useState(true);
  const t = TABLES[table];
  return (
    <div style={{
      background: COLORS.card,
      border: `1px solid ${t.color}44`,
      borderRadius: 16,
      overflow: "hidden",
      boxShadow: `0 0 20px ${t.color}11`,
    }}>
      {/* header */}
      <div
        onClick={() => setOpen(o => !o)}
        style={{
          padding: "14px 18px",
          background: t.color + "18",
          borderBottom: open ? `1px solid ${t.color}33` : "none",
          display: "flex", alignItems: "center", gap: 10,
          cursor: "pointer",
        }}
      >
        <span style={{ fontSize: 18 }}>{t.icon}</span>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 14, fontWeight: 700, color: COLORS.text, fontFamily: "'DM Mono', monospace" }}>
            {t.label}
          </div>
          <div style={{ fontSize: 11, color: COLORS.muted, fontFamily: "'DM Sans', sans-serif" }}>{t.desc}</div>
        </div>
        <span style={{ color: COLORS.muted, fontSize: 12 }}>{open ? "▲" : "▼"}</span>
      </div>

      {/* columns */}
      {open && (
        <div>
          {/* column header row */}
          <div style={{
            display: "grid", gridTemplateColumns: "180px 140px 1fr",
            padding: "8px 18px", gap: 8,
            borderBottom: `1px solid ${COLORS.border}`,
          }}>
            {["COLUMN", "TYPE", "NOTES"].map(h => (
              <span key={h} style={{ fontSize: 9, color: COLORS.dim, fontFamily: "'DM Mono', monospace", letterSpacing: 1.5 }}>{h}</span>
            ))}
          </div>

          {t.columns.map((col, i) => (
            <div key={i} style={{
              display: "grid", gridTemplateColumns: "180px 140px 1fr",
              padding: "8px 18px", gap: 8, alignItems: "center",
              borderBottom: `1px solid ${COLORS.border}33`,
              background: i % 2 === 0 ? "transparent" : COLORS.surface + "66",
            }}>
              {/* name */}
              <div style={{ display: "flex", alignItems: "center", gap: 6, flexWrap: "wrap" }}>
                <span style={{
                  fontSize: 12, color: col.pk ? t.color : COLORS.text,
                  fontFamily: "'DM Mono', monospace",
                  fontWeight: col.pk ? 700 : 400,
                }}>{col.name}</span>
                {col.pk     && <Badge color={t.color}>PK</Badge>}
                {col.fk     && <Badge color={COLORS.purple}>FK</Badge>}
                {col.unique && <Badge color={COLORS.gold}>UQ</Badge>}
                {col.notnull && !col.pk && <Badge color={COLORS.muted}>NN</Badge>}
              </div>
              {/* type */}
              <span style={{ fontSize: 11, color: COLORS.blue, fontFamily: "'DM Mono', monospace" }}>{col.type}</span>
              {/* notes */}
              <div style={{ fontSize: 11, color: COLORS.muted, fontFamily: "'DM Sans', sans-serif" }}>
                {col.fk && <span style={{ color: COLORS.purple, marginRight: 6 }}>→ {col.fk}</span>}
                {col.default && <span style={{ color: COLORS.green, marginRight: 6 }}>default: {col.default}</span>}
                {col.note}
              </div>
            </div>
          ))}

          {/* indexes */}
          <div style={{ padding: "10px 18px", background: COLORS.surface }}>
            <div style={{ fontSize: 9, color: COLORS.dim, fontFamily: "'DM Mono', monospace", letterSpacing: 1.5, marginBottom: 6 }}>INDEXES</div>
            {t.indexes.map((idx, i) => (
              <div key={i} style={{ fontSize: 11, color: COLORS.muted, fontFamily: "'DM Mono', monospace", marginBottom: 3 }}>
                <span style={{ color: t.color }}>▸ </span>{idx}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default function App() {
  const [tab, setTab] = useState("schema");

  return (
    <div style={{ background: COLORS.bg, minHeight: "100vh", fontFamily: "'DM Sans', sans-serif" }}>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;600;700&family=DM+Mono:wght@400;600&display=swap');
        @import url('https://api.fontshare.com/v2/css?f[]=clash-display@700,800&display=swap');
        * { box-sizing: border-box; margin: 0; padding: 0; }
        ::-webkit-scrollbar { width: 6px; } ::-webkit-scrollbar-track { background: #111118; } ::-webkit-scrollbar-thumb { background: #2A2A3A; border-radius: 3px; }
      `}</style>

      {/* header */}
      <div style={{ padding: "24px 28px 0", marginBottom: 20 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 6 }}>
          <div style={{ width: 32, height: 32, borderRadius: 8, background: "linear-gradient(135deg,#FF4D6D,#FF6B35)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 16 }}>◈</div>
          <span style={{ fontSize: 18, fontWeight: 800, color: COLORS.text, fontFamily: "'Clash Display', sans-serif", letterSpacing: -0.5 }}>ReelShop</span>
          <span style={{ fontSize: 10, color: COLORS.muted, background: COLORS.surface, padding: "3px 10px", borderRadius: 6, border: `1px solid ${COLORS.border}`, fontFamily: "'DM Mono', monospace", letterSpacing: 1 }}>DB DESIGN</span>
        </div>
        <h1 style={{ fontSize: 26, fontWeight: 800, color: COLORS.text, fontFamily: "'Clash Display', sans-serif", letterSpacing: -0.8 }}>
          Data Model & Schema
        </h1>
        <p style={{ fontSize: 13, color: COLORS.muted, marginTop: 4 }}>PostgreSQL · 5 tables · Redis for OTP</p>
      </div>

      {/* tabs */}
      <div style={{ padding: "0 28px", display: "flex", gap: 8, marginBottom: 24, borderBottom: `1px solid ${COLORS.border}` }}>
        {["schema", "relationships", "redis"].map(t => (
          <button key={t} onClick={() => setTab(t)} style={{
            background: "transparent",
            color: tab === t ? COLORS.accent : COLORS.muted,
            border: "none",
            borderBottom: tab === t ? `2px solid ${COLORS.accent}` : "2px solid transparent",
            padding: "10px 16px",
            fontSize: 12, fontFamily: "'DM Mono', monospace",
            fontWeight: 600, letterSpacing: 1,
            cursor: "pointer", textTransform: "uppercase",
            marginBottom: -1,
          }}>{t}</button>
        ))}
      </div>

      <div style={{ padding: "0 28px 60px" }}>

        {/* SCHEMA TAB */}
        {tab === "schema" && (
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            {Object.keys(TABLES).map(table => (
              <TableCard key={table} table={table} />
            ))}
          </div>
        )}

        {/* RELATIONSHIPS TAB */}
        {tab === "relationships" && (
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            {/* ER summary */}
            <div style={{ background: COLORS.card, border: `1px solid ${COLORS.border}`, borderRadius: 16, padding: 24 }}>
              <div style={{ fontSize: 11, color: COLORS.muted, fontFamily: "'DM Mono', monospace", letterSpacing: 1.5, marginBottom: 16 }}>ENTITY RELATIONSHIPS</div>
              {[
                { from: "sellers", to: "products",       type: "1 → N", note: "One seller has many products" },
                { from: "products", to: "product_images", type: "1 → N", note: "One product has up to 3 images" },
                { from: "products", to: "qr_scans",      type: "1 → N", note: "One product has many scan events" },
                { from: "sellers",  to: "qr_scans",      type: "1 → N", note: "Denormalised — seller_id stored for fast dashboard queries" },
              ].map((rel, i) => (
                <div key={i} style={{
                  display: "flex", alignItems: "center", gap: 14,
                  padding: "12px 0",
                  borderBottom: i < 3 ? `1px solid ${COLORS.border}` : "none",
                }}>
                  <div style={{ background: COLORS.accent + "22", color: COLORS.accent, padding: "4px 12px", borderRadius: 8, fontFamily: "'DM Mono', monospace", fontSize: 12, fontWeight: 700, minWidth: 80, textAlign: "center" }}>{rel.from}</div>
                  <div style={{ fontSize: 14, color: COLORS.muted }}>→</div>
                  <div style={{ background: COLORS.green + "22", color: COLORS.green, padding: "4px 12px", borderRadius: 8, fontFamily: "'DM Mono', monospace", fontSize: 12, fontWeight: 700, minWidth: 100, textAlign: "center" }}>{rel.to}</div>
                  <div style={{ background: COLORS.gold + "22", color: COLORS.gold, padding: "3px 10px", borderRadius: 6, fontFamily: "'DM Mono', monospace", fontSize: 10, letterSpacing: 1 }}>{rel.type}</div>
                  <div style={{ fontSize: 12, color: COLORS.muted, fontFamily: "'DM Sans', sans-serif" }}>{rel.note}</div>
                </div>
              ))}
            </div>

            {/* Design decisions */}
            <div style={{ background: COLORS.card, border: `1px solid ${COLORS.border}`, borderRadius: 16, padding: 24 }}>
              <div style={{ fontSize: 11, color: COLORS.muted, fontFamily: "'DM Mono', monospace", letterSpacing: 1.5, marginBottom: 16 }}>KEY DESIGN DECISIONS</div>
              {[
                { icon: "⚡", title: "Denormalised scan counters", detail: "qr_scan_count on products + total_qr_scans on sellers are updated atomically via UPDATE ... SET x = x + 1. Avoids full COUNT(*) aggregation on every dashboard load." },
                { icon: "🔐", title: "OTP values never in PostgreSQL", detail: "Redis stores the actual 6-digit OTP with 5-min TTL. otp_records table is for audit + rate limiting only. No plaintext secrets in DB ever." },
                { icon: "🔗", title: "short_code on products is immutable", detail: "Once a QR is printed and posted in a reel, the URL must never change. short_code is set on creation and never updated." },
                { icon: "🗑️", title: "Soft deletes on sellers + products", detail: "status = 'DELETED' instead of hard DELETE. Preserves qr_scans history and prevents broken public URLs after a seller deactivates." },
                { icon: "🧩", title: "seller_id denormalised in qr_scans", detail: "Storing seller_id directly on qr_scans avoids an extra JOIN when rendering the seller dashboard (most frequent read path)." },
              ].map((d, i) => (
                <div key={i} style={{ display: "flex", gap: 14, marginBottom: 16 }}>
                  <span style={{ fontSize: 20, flexShrink: 0 }}>{d.icon}</span>
                  <div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: COLORS.text, fontFamily: "'DM Sans', sans-serif", marginBottom: 3 }}>{d.title}</div>
                    <div style={{ fontSize: 12, color: COLORS.muted, lineHeight: 1.6 }}>{d.detail}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* REDIS TAB */}
        {tab === "redis" && (
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <div style={{ background: COLORS.card, border: `1px solid ${COLORS.border}`, borderRadius: 16, padding: 24 }}>
              <div style={{ fontSize: 11, color: COLORS.muted, fontFamily: "'DM Mono', monospace", letterSpacing: 1.5, marginBottom: 16 }}>REDIS KEY PATTERNS</div>
              {REDIS_KEYS.map((rk, i) => (
                <div key={i} style={{
                  background: COLORS.surface, borderRadius: 12, padding: "16px",
                  border: `1px solid ${COLORS.border}`, marginBottom: 12,
                }}>
                  <div style={{ fontSize: 13, color: COLORS.blue, fontFamily: "'DM Mono', monospace", marginBottom: 8, fontWeight: 600 }}>{rk.key}</div>
                  <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
                    <div>
                      <div style={{ fontSize: 9, color: COLORS.dim, fontFamily: "'DM Mono', monospace", letterSpacing: 1, marginBottom: 3 }}>VALUE</div>
                      <div style={{ fontSize: 11, color: COLORS.text, fontFamily: "'DM Sans', sans-serif" }}>{rk.value}</div>
                    </div>
                    <div>
                      <div style={{ fontSize: 9, color: COLORS.dim, fontFamily: "'DM Mono', monospace", letterSpacing: 1, marginBottom: 3 }}>TTL</div>
                      <div style={{ fontSize: 11, color: COLORS.gold, fontFamily: "'DM Mono', monospace" }}>{rk.ttl}</div>
                    </div>
                    <div>
                      <div style={{ fontSize: 9, color: COLORS.dim, fontFamily: "'DM Mono', monospace", letterSpacing: 1, marginBottom: 3 }}>BEHAVIOUR</div>
                      <div style={{ fontSize: 11, color: COLORS.muted, fontFamily: "'DM Sans', sans-serif" }}>{rk.note}</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Why Redis? */}
            <div style={{ background: COLORS.card, border: `1px solid ${COLORS.green}33`, borderRadius: 16, padding: 20 }}>
              <div style={{ fontSize: 11, color: COLORS.green, fontFamily: "'DM Mono', monospace", letterSpacing: 1.5, marginBottom: 12 }}>WHY REDIS FOR OTP?</div>
              {[
                "Automatic expiry via TTL — no cron job needed to clean up expired OTPs",
                "Atomic INCR for rate limiting — no race conditions possible",
                "Sub-millisecond reads — verify OTP in < 1ms vs 10-50ms for PostgreSQL",
                "OTP consumed on verify (DEL key) — replay attacks impossible",
              ].map((p, i) => (
                <div key={i} style={{ display: "flex", gap: 10, marginBottom: 10 }}>
                  <span style={{ color: COLORS.green, fontSize: 12, flexShrink: 0, marginTop: 1 }}>✓</span>
                  <span style={{ fontSize: 12, color: COLORS.muted, lineHeight: 1.5 }}>{p}</span>
                </div>
              ))}
            </div>

            {/* Phase 2 future keys */}
            <div style={{ background: COLORS.card, border: `1px solid ${COLORS.border}`, borderRadius: 16, padding: 20 }}>
              <div style={{ fontSize: 11, color: COLORS.muted, fontFamily: "'DM Mono', monospace", letterSpacing: 1.5, marginBottom: 12 }}>PHASE 2 KEYS (FUTURE)</div>
              {[
                { key: "session:{sellerId}",          ttl: "30d",  note: "JWT revocation list" },
                { key: "shop_cache:{slug}",           ttl: "5min", note: "Public shop page cache" },
                { key: "product_cache:{shortCode}",   ttl: "5min", note: "Product page cache" },
              ].map((rk, i) => (
                <div key={i} style={{ display: "flex", gap: 12, alignItems: "center", marginBottom: 10, opacity: 0.6 }}>
                  <span style={{ fontSize: 11, color: COLORS.purple, fontFamily: "'DM Mono', monospace", flex: 1 }}>{rk.key}</span>
                  <Badge color={COLORS.gold}>{rk.ttl}</Badge>
                  <span style={{ fontSize: 11, color: COLORS.muted }}>{rk.note}</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
