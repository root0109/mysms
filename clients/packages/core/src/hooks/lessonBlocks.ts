/**
 * A lesson's block content (`Lesson.content`), kept in the Editor.js shape `{ blocks: [{ type, data }] }` so content
 * imported from other learning systems reads unchanged. Readers render the blocks as plain text and links only: markup
 * inside a block's text is dropped, never interpreted.
 */
export type LessonBlock =
  | { type: 'paragraph'; text: string }
  | { type: 'header'; text: string; level: 2 | 3 }
  | { type: 'list'; ordered: boolean; items: string[] }
  | { type: 'upload'; fileId: string; name: string; mimeType: string }
  | { type: 'embed'; url: string };

export type LessonBlockType = LessonBlock['type'];

type Data = Record<string, unknown>;

const text = (value: unknown): string =>
  typeof value === 'string'
    ? value
        .replace(/<br\s*\/?>/gi, '\n')
        .replace(/<[^>]*>/g, '')
        .replace(/&nbsp;/g, ' ')
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')
        .replace(/&quot;/g, '"')
        .replace(/&#39;/g, "'")
        .replace(/&amp;/g, '&')
    : '';

/** An https address with a host and no spaces; matched as text because React Native's URL lacks most getters. */
const HTTPS_LINK = /^https:\/\/([^\s/?#@]+)([/?#][^\s]*)?$/i;

/** Only https addresses are followed; anything else (javascript:, data:, http:) is no link. */
export function safeLink(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const link = value.trim();
  return HTTPS_LINK.test(link) ? link : null;
}

/** The host a reader is told an outside link opens (`example.org` of `https://example.org/a`). */
export function linkHost(link: string): string {
  return HTTPS_LINK.exec(link)?.[1] ?? link;
}

function block(raw: unknown): LessonBlock | null {
  if (!raw || typeof raw !== 'object') return null;
  const { type, data: rawData } = raw as { type?: unknown; data?: unknown };
  const data = (rawData && typeof rawData === 'object' ? rawData : {}) as Data;
  switch (type) {
    case 'paragraph':
      return { type, text: text(data.text) };
    case 'header':
      return { type, text: text(data.text), level: data.level === 3 ? 3 : 2 };
    case 'list':
      return {
        type,
        ordered: data.style === 'ordered',
        items: Array.isArray(data.items) ? data.items.map(text) : [],
      };
    case 'upload': {
      const fileId = typeof data.fileId === 'string' ? data.fileId : '';
      if (!fileId) return null;
      return {
        type,
        fileId,
        name: text(data.fileName) || fileId,
        mimeType: typeof data.fileType === 'string' ? data.fileType : '',
      };
    }
    case 'embed': {
      const url = safeLink(data.source);
      return url ? { type, url } : null;
    }
    default:
      return null;
  }
}

/** Whether every block can be stored: outside links must be https addresses. */
export function blocksAreValid(blocks: readonly LessonBlock[]): boolean {
  return blocks.every((b) => b.type !== 'embed' || safeLink(b.url) !== null);
}

/** The blocks of a lesson's stored content; blocks of unknown kinds or without what they need are left out. */
export function parseLessonBlocks(content: unknown): LessonBlock[] {
  if (!content || typeof content !== 'object') return [];
  const { blocks } = content as { blocks?: unknown };
  if (!Array.isArray(blocks)) return [];
  return blocks.map(block).filter((b): b is LessonBlock => b !== null);
}

function stored(b: LessonBlock): { type: LessonBlockType; data: Data } {
  switch (b.type) {
    case 'paragraph':
      return { type: b.type, data: { text: b.text } };
    case 'header':
      return { type: b.type, data: { text: b.text, level: b.level } };
    case 'list':
      return { type: b.type, data: { style: b.ordered ? 'ordered' : 'unordered', items: b.items } };
    case 'upload':
      return { type: b.type, data: { fileId: b.fileId, fileName: b.name, fileType: b.mimeType } };
    case 'embed':
      return { type: b.type, data: { source: b.url } };
  }
}

/** The content document a lesson stores for these blocks. */
export function lessonContentOf(blocks: readonly LessonBlock[]): {
  blocks: { type: LessonBlockType; data: Data }[];
} {
  return { blocks: blocks.map(stored) };
}

/** An empty block of a kind, as the editor adds it. */
export function emptyBlock(type: Exclude<LessonBlockType, 'upload'>): LessonBlock {
  switch (type) {
    case 'paragraph':
      return { type, text: '' };
    case 'header':
      return { type, text: '', level: 2 };
    case 'list':
      return { type, ordered: false, items: [''] };
    case 'embed':
      return { type, url: '' };
  }
}

/** The blocks with the one at `index` moved by `by` places (out-of-range moves leave them as they are). */
export function moveBlock<T>(blocks: readonly T[], index: number, by: -1 | 1): T[] {
  const to = index + by;
  if (index < 0 || index >= blocks.length || to < 0 || to >= blocks.length) return [...blocks];
  const next = [...blocks];
  [next[index], next[to]] = [next[to] as T, next[index] as T];
  return next;
}
