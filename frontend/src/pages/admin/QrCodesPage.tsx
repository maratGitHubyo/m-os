import { useState } from 'react';
import { createQrCode } from '../../api/admin/qr';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
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
      showToast(`QR "${created.code}" created`);
      setCode('');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to create QR', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader
        title="QR Codes"
        description="Create QR codes. List shows codes created in this browser session (no list API)."
      />

      <FormCard title="Create QR code">
        <form className="admin-form-grid" onSubmit={(event) => void handleCreate(event)}>
          <label className="form-field">
            <span>Code</span>
            <input value={code} onChange={(event) => setCode(event.target.value)} required />
          </label>
          <label className="form-field">
            <span>Reward type</span>
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
            <span>Scan policy</span>
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
            <span>Reward payload (JSON)</span>
            <input value={payloadJson} onChange={(event) => setPayloadJson(event.target.value)} />
          </label>
          <label className="form-field">
            <span>Location point ID</span>
            <input value={locationPointId} onChange={(event) => setLocationPointId(event.target.value)} />
          </label>
          <label className="form-field">
            <span>Scan limit</span>
            <input value={scanLimit} onChange={(event) => setScanLimit(event.target.value)} />
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Create QR
          </button>
        </form>
      </FormCard>

      <h2 className="section-title">QR codes (session)</h2>
      {codes.length === 0 ? (
        <p className="empty-state">No QR codes in this session yet.</p>
      ) : (
        <DataTable
          rows={codes}
          rowKey={(row) => row.id}
          columns={[
            { key: 'code', header: 'Code', render: (row) => row.code },
            { key: 'reward', header: 'Reward', render: (row) => row.rewardType },
            { key: 'policy', header: 'Policy', render: (row) => row.scanPolicy },
            { key: 'active', header: 'Active', render: (row) => (row.active ? 'Yes' : 'No') },
          ]}
        />
      )}
    </section>
  );
}
