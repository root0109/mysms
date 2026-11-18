import {
  isApiError,
  useCan,
  useDownloadUrl,
  useI18n,
  useUploadFile,
  type FileObject,
  type UploadOwner,
} from '@mysms/core';
import { Banner, Button } from '@mysms/ui-web';
import { useRef, useState } from 'react';
import { transferProblem } from './transferProblem';

export interface FileUploadButtonProps {
  /** The visible label of the button (what the file is for: "Upload logo"). */
  label: string;
  /** The record the file belongs to, when there is one (`schema.table` and id): its module decides who may attach. */
  owner?: UploadOwner;
  /** `accept` of the file picker, e.g. `image/png,image/jpeg`. */
  accept?: string;
  onUploaded: (file: FileObject) => void;
  disabled?: boolean;
}

/**
 * Picks one file and uploads it the platform's way (api/README "Files"): a signed upload URL, the bytes straight to
 * the store, then completion, after which the file is available and its id can be stored on the record. Hidden for a
 * role that may not upload; failures say what to do (quota, plan limit, refused type or size, store not configured).
 */
export function FileUploadButton({
  label,
  owner,
  accept,
  onUploaded,
  disabled = false,
}: FileUploadButtonProps) {
  const { t } = useI18n();
  const mayUpload = useCan('platform.file_object.create_upload_url');
  const mayComplete = useCan('platform.file_object.complete_upload');
  const upload = useUploadFile();
  const picker = useRef<HTMLInputElement>(null);
  const [name, setName] = useState<string | null>(null);
  if (!mayUpload || !mayComplete) return null;
  return (
    <div className="file-transfer">
      <input
        ref={picker}
        type="file"
        hidden
        aria-label={label}
        accept={accept}
        onChange={(event) => {
          const file = event.target.files?.[0];
          event.target.value = '';
          if (!file) return;
          setName(file.name);
          upload.mutate({ source: file, ...(owner ? { owner } : {}) }, { onSuccess: onUploaded });
        }}
      />
      <Button
        variant="secondary"
        loading={upload.isPending}
        disabled={disabled}
        onClick={() => picker.current?.click()}
      >
        {label}
      </Button>
      {upload.isPending && name ? <p className="hint">{t('files.upload.sending', { name })}</p> : null}
      {upload.isSuccess && name ? <p className="hint">{t('files.upload.done', { name })}</p> : null}
      {upload.isError ? <Banner intent="danger">{transferProblem(t, upload.error)}</Banner> : null}
    </div>
  );
}

/**
 * Opens a stored file: the download URL is signed at the moment of the click (links expire within minutes and are
 * never kept). Hidden for a role that may not download; a file the caller may not read is "not available".
 */
export function FileDownloadButton({ fileId, label }: { fileId: string; label: string }) {
  const { t } = useI18n();
  const mayDownload = useCan('platform.file_object.get_download_url');
  const link = useDownloadUrl();
  if (!mayDownload) return null;
  return (
    <span className="file-transfer">
      <Button
        variant="ghost"
        loading={link.isPending}
        onClick={() =>
          link.mutate(fileId, { onSuccess: (download) => window.open(download.url, '_blank', 'noopener') })
        }
      >
        {label}
      </Button>
      {link.isError ? (
        <Banner intent="danger">
          {isApiError(link.error) && link.error.status === 404
            ? t('common.state.notAvailable')
            : transferProblem(t, link.error)}
        </Banner>
      ) : null}
    </span>
  );
}
