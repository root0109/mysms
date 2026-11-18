import { describe, expect, it } from 'vitest';
import {
  blocksAreValid,
  emptyBlock,
  lessonContentOf,
  linkHost,
  moveBlock,
  parseLessonBlocks,
  safeLink,
} from './lessonBlocks';

describe('lesson blocks', () => {
  it('reads every kind and writes it back in the stored shape', () => {
    const content = {
      time: 1,
      blocks: [
        { type: 'header', data: { text: 'Fractions', level: 3 } },
        { type: 'paragraph', data: { text: 'Half <b>of</b> 4 &amp; more<br>next' } },
        { type: 'list', data: { style: 'ordered', items: ['one', '<i>two</i>'] } },
        { type: 'upload', data: { fileId: 'f-1', fileName: 'sheet.pdf', fileType: 'application/pdf' } },
        { type: 'embed', data: { source: 'https://example.org/sim' } },
      ],
    };
    const blocks = parseLessonBlocks(content);
    expect(blocks).toEqual([
      { type: 'header', text: 'Fractions', level: 3 },
      { type: 'paragraph', text: 'Half of 4 & more\nnext' },
      { type: 'list', ordered: true, items: ['one', 'two'] },
      { type: 'upload', fileId: 'f-1', name: 'sheet.pdf', mimeType: 'application/pdf' },
      { type: 'embed', url: 'https://example.org/sim' },
    ]);
    expect(parseLessonBlocks(lessonContentOf(blocks))).toEqual(blocks);
  });

  it('leaves out unknown kinds, uploads without a file and links that are not https', () => {
    expect(
      parseLessonBlocks({
        blocks: [
          { type: 'raw', data: { html: '<script>' } },
          { type: 'upload', data: {} },
          { type: 'embed', data: { source: 'javascript:alert(1)' } },
          { type: 'embed', data: { source: 'http://example.org' } },
          null,
          { type: 'header', data: { text: 'Kept', level: 6 } },
        ],
      }),
    ).toEqual([{ type: 'header', text: 'Kept', level: 2 }]);
    expect(parseLessonBlocks(null)).toEqual([]);
    expect(parseLessonBlocks({ blocks: 'no' })).toEqual([]);
  });

  it('checks links, makes empty blocks and moves blocks within range only', () => {
    expect(safeLink(' https://example.org/a ')).toBe('https://example.org/a');
    expect(safeLink('not a url')).toBeNull();
    expect(safeLink('https://user@evil.example')).toBeNull();
    expect(safeLink('https://a b.org')).toBeNull();
    expect(linkHost('https://example.org:8443/a?b')).toBe('example.org:8443');
    expect(blocksAreValid([{ type: 'embed', url: 'https://example.org' }])).toBe(true);
    expect(blocksAreValid([{ type: 'embed', url: 'ftp://example.org' }])).toBe(false);
    expect(emptyBlock('list')).toEqual({ type: 'list', ordered: false, items: [''] });
    expect(moveBlock(['a', 'b', 'c'], 1, -1)).toEqual(['b', 'a', 'c']);
    expect(moveBlock(['a', 'b', 'c'], 2, 1)).toEqual(['a', 'b', 'c']);
  });
});
