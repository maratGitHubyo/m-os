import { useState } from 'react';
import { createQrCode } from '../../api/admin/qr';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { translateError } from '../../i18n/ru';
import { showToast } from '../../stores/toastStore';
import type { QrCodeInfo } from '../../types/admin';

export function QrCodesPage() {
  const [codes, setCodes] = useState<QrCodeInfo[]>([]);
  const [code, setCode] = useState('');
  const [rewardType, setRewardType] = useState<'COIN' | 'ITEM' | 'NONE'>('COIN');
  const [scanPolicy, setScanPolicy] = useState<'FIRST_PLAYER' | 'EVERY_PLAYER' | 'LIMITED'>('EVERY_PLAYER');
  const [payloadJson, setPayloadJson] = useState('{"amount": 50}');
  const [scanLimit, setScanLimit] = useState('');
  const [locationPointId, setLocationPointId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleCreate = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      let rewardPayload: Record<string, unknown> = {};
      if (rewardType !== 'NONE' && payloadJson.trim()) {
        rewardPayload = JSON.parse(payloadJson) as Record<string, unknown>;
      }
      const created = await createQrCode({
        code,
        locationPointId: locationPointId || null,
        rewardType,
        rewardPayload,
        scanPolicy,
        scanLimit: scanLimit ? Number(scanLimit) : null,
      });
      setCodes((current) => [...current, created]);
      showToast(`QR-код «${created.code}» создан`);
      setCode('');
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось создать QR-код',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader
        title="QR-коды"
        description="Создание QR-кодов. Список показывает коды, созданные в этой сессии браузера (нет API списка)."
      />

      <FormCard title="Создать QR-код">
        <form className="admin-form-grid" onSubmit={(event) => void handleCreate(event)}>
          <label className="form-field">
            <span>Код</span>
            <input value={code} onChange={(event) => setCode(event.target.value)} required />
          </label>
          <label className="form-field">
            <span>Тип награды</span>
            <select
              className="form-select"
              value={rewardType}
              onChange={(event) => setRewardType(event.target.value as typeof rewardType)}
            >
              <option value="COIN">COIN</option>
              <option value="ITEM">ITEM</option>
              <option value="NONE">NONE</option>
            </select>
          </label>
          <label className="form-field">
            <span>Политика сканирования</span>
            <select
              className="form-select"
              value={scanPolicy}
              onChange={(event) => setScanPolicy(event.target.value as typeof scanPolicy)}
            >
              <option value="FIRST_PLAYER">FIRST_PLAYER</option>
              <option value="EVERY_PLAYER">EVERY_PLAYER</option>
              <option value="LIMITED">LIMITED</option>
            </select>
          </label>
          <label className="form-field form-field--wide">
            <span>Награда (JSON)</span>
            <input value={payloadJson} onChange={(event) => setPayloadJson(event.target.value)} />
          </label>
          <label className="form-field">
            <span>ID точки локации</span>
            <input value={locationPointId} onChange={(event) => setLocationPointId(event.target.value)} />
          </label>
          <label className="form-field">
            <span>Лимит сканирований</span>
            <input value={scanLimit} onChange={(event) => setScanLimit(event.target.value)} />
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Создать QR-код
          </button>
        </form>
      </FormCard>

      <h2 className="section-title">QR-коды (сессия)</h2>
      {codes.length === 0 ? (
        <p className="empty-state">В этой сессии пока нет QR-кодов.</p>
      ) : (
        <DataTable
          rows={codes}
          rowKey={(row) => row.id}
          columns={[
            { key: 'code', header: 'Код', render: (row) => row.code },
            { key: 'reward', header: 'Награда', render: (row) => row.rewardType },
            { key: 'policy', header: 'Политика', render: (row) => row.scanPolicy },
            { key: 'active', header: 'Активен', render: (row) => (row.active ? 'Да' : 'Нет') },
          ]}
        />
      )}
    </section>
  );
}
