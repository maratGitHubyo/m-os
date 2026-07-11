import { useEffect, useState } from 'react';
import { createQrCodeSimple } from '../../api/admin/qr';
import { listItemTemplates } from '../../api/admin/items';
import { downloadQrImage, openQrImage } from '../../api/qr';
import { fetchLocations } from '../../api/locations';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { translateError } from '../../i18n/ru';
import { showToast } from '../../stores/toastStore';
import type { ItemTemplate, QrCodeInfo, QrRewardKind } from '../../types/admin';
import type { LocationPoint } from '../../types';

export function QrCodesPage() {
  const [codes, setCodes] = useState<QrCodeInfo[]>([]);
  const [title, setTitle] = useState('');
  const [rewardKind, setRewardKind] = useState<QrRewardKind>('COIN');
  const [coinAmount, setCoinAmount] = useState('50');
  const [itemTemplateId, setItemTemplateId] = useState('');
  const [locationPointId, setLocationPointId] = useState('');
  const [scanPolicy, setScanPolicy] = useState<'FIRST_PLAYER' | 'EVERY_PLAYER' | 'LIMITED'>(
    'EVERY_PLAYER',
  );
  const [scanLimit, setScanLimit] = useState('');
  const [templates, setTemplates] = useState<ItemTemplate[]>([]);
  const [locations, setLocations] = useState<LocationPoint[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [createdQr, setCreatedQr] = useState<QrCodeInfo | null>(null);

  useEffect(() => {
    const loadOptions = async () => {
      try {
        const [itemTemplates, locationPoints] = await Promise.all([
          listItemTemplates(),
          fetchLocations(),
        ]);
        setTemplates(itemTemplates);
        setLocations(locationPoints);
      } catch {
        // dropdowns stay empty if load fails
      }
    };

    void loadOptions();
  }, []);

  const handleCreate = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    setCreatedQr(null);

    try {
      const created = await createQrCodeSimple({
        title,
        rewardKind,
        coinAmount: rewardKind === 'COIN' ? Number(coinAmount) : null,
        itemTemplateId: rewardKind === 'ITEM' ? itemTemplateId : null,
        locationPointId: rewardKind === 'LOCATION' ? locationPointId : null,
        scanPolicy,
        scanLimit: scanPolicy === 'LIMITED' && scanLimit ? Number(scanLimit) : null,
      });
      setCodes((current) => [...current, created]);
      setCreatedQr(created);
      showToast(`QR «${created.title}» создан`);
      setTitle('');
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
        description="Создайте QR с наградой и скачайте изображение для печати на мероприятии."
      />

      <FormCard title="Создать QR-код">
        <form className="admin-form-grid" onSubmit={(event) => void handleCreate(event)}>
          <label className="form-field form-field--wide">
            <span>Название</span>
            <input value={title} onChange={(event) => setTitle(event.target.value)} required />
          </label>

          <fieldset className="form-field form-field--wide">
            <legend>Тип награды</legend>
            <label className="radio-option">
              <input
                type="radio"
                name="rewardKind"
                value="NONE"
                checked={rewardKind === 'NONE'}
                onChange={() => setRewardKind('NONE')}
              />
              Нет
            </label>
            <label className="radio-option">
              <input
                type="radio"
                name="rewardKind"
                value="COIN"
                checked={rewardKind === 'COIN'}
                onChange={() => setRewardKind('COIN')}
              />
              Монеты
            </label>
            <label className="radio-option">
              <input
                type="radio"
                name="rewardKind"
                value="ITEM"
                checked={rewardKind === 'ITEM'}
                onChange={() => setRewardKind('ITEM')}
              />
              Предмет
            </label>
            <label className="radio-option">
              <input
                type="radio"
                name="rewardKind"
                value="LOCATION"
                checked={rewardKind === 'LOCATION'}
                onChange={() => setRewardKind('LOCATION')}
              />
              Локация
            </label>
          </fieldset>

          {rewardKind === 'COIN' && (
            <label className="form-field">
              <span>Количество</span>
              <input
                type="number"
                min={1}
                value={coinAmount}
                onChange={(event) => setCoinAmount(event.target.value)}
                required
              />
            </label>
          )}

          {rewardKind === 'ITEM' && (
            <label className="form-field">
              <span>Предмет</span>
              <select
                className="form-select"
                value={itemTemplateId}
                onChange={(event) => setItemTemplateId(event.target.value)}
                required
              >
                <option value="">Выберите предмет</option>
                {templates.map((template) => (
                  <option key={template.id} value={template.id}>
                    {template.name}
                  </option>
                ))}
              </select>
            </label>
          )}

          {rewardKind === 'LOCATION' && (
            <label className="form-field">
              <span>Локация</span>
              <select
                className="form-select"
                value={locationPointId}
                onChange={(event) => setLocationPointId(event.target.value)}
                required
              >
                <option value="">Выберите локацию</option>
                {locations.map((location) => (
                  <option key={location.id} value={location.id}>
                    {location.name}
                  </option>
                ))}
              </select>
            </label>
          )}

          <label className="form-field">
            <span>Политика сканирования</span>
            <select
              className="form-select"
              value={scanPolicy}
              onChange={(event) => setScanPolicy(event.target.value as typeof scanPolicy)}
            >
              <option value="EVERY_PLAYER">Каждый игрок один раз</option>
              <option value="FIRST_PLAYER">Только первый игрок</option>
              <option value="LIMITED">Лимит сканирований</option>
            </select>
          </label>

          {scanPolicy === 'LIMITED' && (
            <label className="form-field">
              <span>Лимит сканирований</span>
              <input
                type="number"
                min={1}
                value={scanLimit}
                onChange={(event) => setScanLimit(event.target.value)}
                required
              />
            </label>
          )}

          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Создать QR-код
          </button>
        </form>
      </FormCard>

      {createdQr && (
        <article className="card qr-created-panel">
          <h2>QR создан</h2>
          <p>
            <strong>{createdQr.title}</strong>
          </p>
          <div className="button-row">
            <button
              type="button"
              className="btn btn--secondary"
              onClick={() =>
                void downloadQrImage(createdQr.id, `qr-${createdQr.publicId}.png`).catch(() =>
                  showToast('Не удалось скачать QR', 'error'),
                )
              }
            >
              Скачать QR
            </button>
            <button
              type="button"
              className="btn btn--primary"
              onClick={() => openQrImage(createdQr.id)}
            >
              Открыть QR
            </button>
          </div>
        </article>
      )}

      <h2 className="section-title">QR-коды (сессия)</h2>
      {codes.length === 0 ? (
        <p className="empty-state">В этой сессии пока нет QR-кодов.</p>
      ) : (
        <DataTable
          rows={codes}
          rowKey={(row) => row.id}
          columns={[
            { key: 'title', header: 'Название', render: (row) => row.title },
            { key: 'reward', header: 'Награда', render: (row) => row.rewardType },
            { key: 'policy', header: 'Политика', render: (row) => row.scanPolicy },
            {
              key: 'actions',
              header: 'QR',
              render: (row) => (
                <span className="button-row">
                  <button
                    type="button"
                    className="btn btn--small btn--secondary"
                    onClick={() =>
                      void downloadQrImage(row.id, `qr-${row.publicId}.png`).catch(() =>
                        showToast('Не удалось скачать QR', 'error'),
                      )
                    }
                  >
                    Скачать
                  </button>
                  <button
                    type="button"
                    className="btn btn--small btn--primary"
                    onClick={() => openQrImage(row.id)}
                  >
                    Открыть
                  </button>
                </span>
              ),
            },
          ]}
        />
      )}
    </section>
  );
}
