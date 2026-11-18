/** Opens a short-lived download link the backend signed (grade exports, syllabi, certificates). */
export function openDownload(link: { url?: string } | undefined) {
  if (link?.url) window.open(link.url, '_blank', 'noopener');
}
