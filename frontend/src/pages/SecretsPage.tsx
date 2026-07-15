import { useState } from 'react';
import { redeemSecret } from '../api/secrets';
import { PageState } from '../components/ui/PageState';
import { formatEnum, itemRarity, translateError } from '../i18n/ru';
import { showToast } from '../stores/toastStore';
import type { SecretRedeemResponse } from '../types';

function formatSecretReward(result: SecretRedeemResponse): string[] {
  const lines: string[] = [];

  if (result.rewardType === 'COIN' && result.coinAmount != null) {
    lines.push(`+${result.coinAmount} М-коинов`);
  }

  if (result.rewardType === 'ITEM' && result.grantedItem) {
    lines.push(`Предмет: ${result.grantedItem.template.name}`);
  }

  if (result.rewardType === 'NUMBER') {
    lines.push('Получено коллекционное число');
  }

  if (result.rewardType === 'QUEST') {
    lines.push('Активировано задание по промокоду');
  }

  if (lines.length === 0) {
    lines.push('Промокод активирован');
  }

  return lines;
}

export function SecretsPage() {
  const [code, setCode] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<SecretRedeemResponse | null>(null);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    const trimmed = code.trim();
    if (!trimmed) {
      showToast('Введите промокод', 'error');
      return;
    }

    setSubmitting(true);
    setError(null);
    setResult(null);

    try {
      const response = await redeemSecret(trimmed);
      setResult(response);
      showToast('Промокод активирован');
    } catch (err) {
      const message =
        err instanceof Error ? translateError(err.message) : 'Не удалось активировать промокод';
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="secrets-page">
      <h1>Промокод</h1>
      <p className="page-hint">
        Введите персональный промокод от организатора, чтобы получить награду.
      </p>

      <article className="card player-action-card">
        <form className="player-form" onSubmit={handleSubmit}>
          <label className="form-field">
            <span>Промокод</span>
            <input
              type="text"
              value={code}
              onChange={(event) => setCode(event.target.value)}
              placeholder="Например, STAR-DEMO"
              autoComplete="off"
              required
            />
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            {submitting ? 'Активация…' : 'Активировать'}
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
          <strong>{result.title}</strong>
          <p className="result-panel__code">Код: {result.code}</p>
          <ul className="simple-list">
            {formatSecretReward(result).map((line) => (
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
          <p className="empty-state">Введите код и нажмите «Активировать».</p>
        </PageState>
      )}
    </section>
  );
}
