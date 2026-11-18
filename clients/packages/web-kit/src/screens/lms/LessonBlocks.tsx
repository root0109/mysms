import {
  emptyBlock,
  linkHost,
  moveBlock,
  safeLink,
  useI18n,
  type LessonBlock,
  type LessonBlockType,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Button, Card, SelectField, Switch, TextArea, TextInput } from '@mysms/ui-web';
import { FileDownloadButton, FileUploadButton } from '../../files/FileTransfer';

/** LessonRenderer: a lesson's blocks as text, lists, downloads and outside links (never interpreted markup). */
export function LessonBlocksView({ blocks }: { blocks: readonly LessonBlock[] }) {
  const { t } = useI18n();
  return (
    <div className="lms-blocks">
      {blocks.map((block, index) => {
        switch (block.type) {
          case 'paragraph':
            return (
              <p key={index} className="lms-blocks__text">
                {block.text}
              </p>
            );
          case 'header':
            return block.level === 3 ? <h4 key={index}>{block.text}</h4> : <h3 key={index}>{block.text}</h3>;
          case 'list':
            return block.ordered ? (
              <ol key={index}>
                {block.items.map((item, i) => (
                  <li key={i}>{item}</li>
                ))}
              </ol>
            ) : (
              <ul key={index}>
                {block.items.map((item, i) => (
                  <li key={i}>{item}</li>
                ))}
              </ul>
            );
          case 'upload':
            return <FileDownloadButton key={index} fileId={block.fileId} label={block.name} />;
          case 'embed':
            return (
              <p key={index}>
                <a href={block.url} target="_blank" rel="noopener noreferrer">
                  {t('lms.blocks.openEmbed', { address: linkHost(block.url) })}
                </a>
              </p>
            );
        }
      })}
    </div>
  );
}

const ADDABLE: readonly Exclude<LessonBlockType, 'upload'>[] = ['paragraph', 'header', 'list', 'embed'];

/**
 * LessonEditor: the same blocks, editable — text, headings, lists, outside links and uploaded files (attached to the
 * lesson) — moved up and down or removed. Nothing is saved here; the form saves the blocks with the lesson.
 */
export function LessonBlocksEditor({
  blocks,
  lessonId,
  disabled,
  onChange,
}: {
  blocks: readonly LessonBlock[];
  lessonId: string;
  disabled: boolean;
  onChange: (blocks: LessonBlock[]) => void;
}) {
  const { t } = useI18n();
  const replace = (index: number, block: LessonBlock) =>
    onChange(blocks.map((current, i) => (i === index ? block : current)));
  return (
    <div className="lms-blocks">
      {blocks.map((block, index) => (
        <Card
          key={index}
          title={t(`lms.blocks.kind.${block.type}` as MessageKey)}
          actions={
            disabled ? null : (
              <>
                <Button
                  variant="ghost"
                  disabled={index === 0}
                  onClick={() => onChange(moveBlock(blocks, index, -1))}
                >
                  {t('lms.structure.up')}
                </Button>
                <Button
                  variant="ghost"
                  disabled={index === blocks.length - 1}
                  onClick={() => onChange(moveBlock(blocks, index, 1))}
                >
                  {t('lms.structure.down')}
                </Button>
                <Button variant="ghost" onClick={() => onChange(blocks.filter((_, i) => i !== index))}>
                  {t('lms.blocks.remove')}
                </Button>
              </>
            )
          }
        >
          <BlockFields block={block} disabled={disabled} onChange={(next) => replace(index, next)} />
        </Card>
      ))}
      {disabled ? null : (
        <div className="lms-toolbar">
          {ADDABLE.map((type) => (
            <Button key={type} variant="secondary" onClick={() => onChange([...blocks, emptyBlock(type)])}>
              {t('lms.blocks.add', { kind: t(`lms.blocks.kind.${type}` as MessageKey) })}
            </Button>
          ))}
          <FileUploadButton
            label={t('lms.blocks.upload')}
            owner={{ ownerEntityType: 'lms.lesson', ownerEntityId: lessonId }}
            onUploaded={(file) =>
              onChange([
                ...blocks,
                { type: 'upload', fileId: file.id ?? '', name: file.fileName, mimeType: file.mimeType ?? '' },
              ])
            }
          />
        </div>
      )}
    </div>
  );
}

function BlockFields({
  block,
  disabled,
  onChange,
}: {
  block: LessonBlock;
  disabled: boolean;
  onChange: (block: LessonBlock) => void;
}) {
  const { t } = useI18n();
  switch (block.type) {
    case 'paragraph':
      return (
        <TextArea
          label={t('lms.blocks.text')}
          rows={5}
          value={block.text}
          disabled={disabled}
          onChange={(e) => onChange({ ...block, text: e.target.value })}
        />
      );
    case 'header':
      return (
        <div className="form">
          <TextInput
            label={t('lms.blocks.text')}
            value={block.text}
            disabled={disabled}
            onChange={(e) => onChange({ ...block, text: e.target.value })}
          />
          <SelectField
            label={t('lms.blocks.level')}
            value={String(block.level)}
            disabled={disabled}
            options={[
              { value: '2', label: t('lms.blocks.level2') },
              { value: '3', label: t('lms.blocks.level3') },
            ]}
            onChange={(e) => onChange({ ...block, level: e.target.value === '3' ? 3 : 2 })}
          />
        </div>
      );
    case 'list':
      return (
        <div className="form">
          <TextArea
            label={t('lms.blocks.items')}
            hint={t('lms.blocks.itemsHint')}
            rows={5}
            value={block.items.join('\n')}
            disabled={disabled}
            onChange={(e) => onChange({ ...block, items: e.target.value.split('\n') })}
          />
          <Switch
            label={t('lms.blocks.numbered')}
            checked={block.ordered}
            disabled={disabled}
            onChange={(ordered) => onChange({ ...block, ordered })}
          />
        </div>
      );
    case 'upload':
      return <FileDownloadButton fileId={block.fileId} label={block.name} />;
    case 'embed':
      return (
        <TextInput
          label={t('lms.blocks.address')}
          value={block.url}
          disabled={disabled}
          error={block.url && !safeLink(block.url) ? t('lms.blocks.httpsOnly') : undefined}
          onChange={(e) => onChange({ ...block, url: e.target.value })}
        />
      );
  }
}
