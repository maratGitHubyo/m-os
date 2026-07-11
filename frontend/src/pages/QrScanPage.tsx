import { useState } from 'react';
import { scanQrCode } from '../api/qr';
import { PageState } from '../components/ui/PageState';
import { formatEnum, itemRarity, translateError } from '../i18n/ru';
import { showToast } from '../stores/toastStore';
import type { QrScanResponse } from '../types';

function formatReward(result: QrScanResponse): string[] {
  const lines: string[] = [];

  if (result.rewardType === 'COIN' && result.coinAmount != null) {
    lines.push(`+${result.coinAmount} М-коинов`);
  }

  if (result.rewardType === 'ITEM' && result.grantedItem) {
    lines.push(`Предмет: ${result.grantedItem.template.name}`);
  }

  if (result.locationDiscovered) {
    lines.push('Открыта новая локация на карте');
  }

  if (result.rewardType === 'NONE' && result.locationDiscovered) {
    return lines;
  }

  if (lines.length === 0) {
    lines.push('QR-код успешно активирован');
  }

  return lines;
}

export function QrScanPage() {
  const [code, setCode] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<QrScanResponse | null>(null);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    const trimmed = code.trim();
    if (!trimmed) {
      showToast('Введите код QR', 'error');
      return;
    }

    setSubmitting(true);
    setError(null);
    setResult(null);

    try {
      const response = await scanQrCode(trimmed);
      setResult(response);
      showToast('QR-код отсканирован');
    } catch (err) {
      const message =
        err instanceof Error ? translateError(err.message) : 'Не удалось отсканировать QR';
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="qr-page">
      <h1>QR-коды</h1>
      <p className="page-hint">
        Введите код с найденного QR-кода на территории, чтобы получить награду.
      </p>

      <article className="card player-action-card">
        <form className="player-form" onSubmit={handleSubmit}>
          <label className="form-field">
            <span>Код QR</span>
            <input
              type="text"
              value={code}
              onChange={(event) => setCode(event.target.value)}
              placeholder="Например, QR-COIN"
              autoComplete="off"
              required
            />
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            {submitting ? 'Сканирование…' : 'Сканировать'}
          </button>
        </form>
      </article>

      {error && (
        <div className="result-panel result-panel--error" role="alert">
          <strong>Ошибка</strong>
          <p>{error}</p>
        </div>
      )}

      {result && (
        <div className="result-panel result-panel--success">
          <strong>Награда получена</strong>
          <p className="result-panel__code">Код: {result.code}</p>
          <ul className="simple-list">
            {formatReward(result).map((line) => (
              <li key={line}>{line}</li>
            ))}
          </ul>
          {result.grantedItem && (
            <p className="result-panel__meta">
              {result.grantedItem.template.name} ·{' '}
              {formatEnum(result.grantedItem.template.rarity, itemRarity)}
            </p>
          )}
        </div>
      )}

      {!error && !result && !submitting && (
        <PageState loading={false} error={null}>
          <p className="empty-state">Введите код и нажмите «Сканировать».</p>
        </PageState>
      )}
    </section>
  );
}
