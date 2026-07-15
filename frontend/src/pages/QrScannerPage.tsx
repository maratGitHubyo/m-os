import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { extractPublicIdFromQrText, scanQrByPublicId, scanQrCode } from '../api/qr';
import { translateError } from '../i18n/ru';
import type { QrScanResponse, QrScanResultResponse } from '../types';

function formatScanMessage(result: QrScanResultResponse): string {
  if (!result.success) {
    if (result.message === 'QR already scanned') {
      return 'Этот QR уже найден вами';
    }
    if (result.message === 'QR unavailable') {
      return 'QR недоступен';
    }
    return translateError(result.message);
  }

  return 'Найден артефакт!';
}

export function QrScannerPage() {
  const { publicId: pathPublicId } = useParams<{ publicId?: string }>();
  const [searchParams] = useSearchParams();
  const processingRef = useRef(false);
  const deepLinkHandledRef = useRef<string | null>(null);
  const [scanning, setScanning] = useState(false);
  const [lastResult, setLastResult] = useState<QrScanResultResponse | null>(null);
  const [legacyResult, setLegacyResult] = useState<QrScanResponse | null>(null);
  const [lastError, setLastError] = useState<string | null>(null);
  const [manualCode, setManualCode] = useState('');
  const [showManual, setShowManual] = useState(false);

  const hasOutcome = Boolean(lastResult || legacyResult || lastError);
  const success = Boolean(lastResult?.success || legacyResult);

  const processScan = useCallback(async (text: string, allowLegacy: boolean) => {
    if (processingRef.current) {
      return;
    }

    const trimmed = text.trim();
    if (!trimmed) {
      return;
    }

    const publicId = extractPublicIdFromQrText(trimmed);
    if (!publicId && !allowLegacy) {
      return;
    }

    processingRef.current = true;
    setScanning(true);
    setLastError(null);
    setLegacyResult(null);
    setLastResult(null);

    try {
      if (publicId) {
        const response = await scanQrByPublicId(publicId);
        setLastResult(response);
        if (!response.success) {
          setLastError(formatScanMessage(response));
        }
        return;
      }

      const legacyResponse = await scanQrCode(trimmed);
      setLegacyResult(legacyResponse);
    } catch (err) {
      const message =
        err instanceof Error ? translateError(err.message) : 'Не удалось отсканировать QR';
      setLastError(message);
    } finally {
      setScanning(false);
      window.setTimeout(() => {
        processingRef.current = false;
      }, 800);
    }
  }, []);

  useEffect(() => {
    const fromQuery = searchParams.get('pid') ?? searchParams.get('code');
    const deepLinkId = pathPublicId ?? fromQuery;
    if (!deepLinkId || deepLinkHandledRef.current === deepLinkId) {
      return;
    }
    deepLinkHandledRef.current = deepLinkId;
    void processScan(deepLinkId, true);
  }, [pathPublicId, searchParams, processScan]);

  const handleManualSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    await processScan(manualCode, true);
  };

  return (
    <section className="qr-page qr-scanner-page">
      <h1>Сканировать QR</h1>

      {scanning && (
        <div className="qr-hero qr-hero--processing" role="status">
          <p className="qr-hero__eyebrow">Обработка</p>
          <p className="qr-hero__title">Получаем награду…</p>
        </div>
      )}

      {!scanning && hasOutcome && (
        <div
          className={`qr-hero ${success ? 'qr-hero--success' : 'qr-hero--error'}`}
          role="status"
        >
          {lastResult?.success ? (
            <>
              <p className="qr-hero__eyebrow">Успех</p>
              <p className="qr-hero__title">{formatScanMessage(lastResult)}</p>
              {lastResult.title && <p className="qr-hero__name">{lastResult.title}</p>}
              <ul className="qr-hero__rewards">
                {lastResult.reward?.item && <li>{lastResult.reward.item}</li>}
                {lastResult.reward?.coins != null && (
                  <li>+{lastResult.reward.coins} M-Coins</li>
                )}
              </ul>
            </>
          ) : legacyResult ? (
            <>
              <p className="qr-hero__eyebrow">Успех</p>
              <p className="qr-hero__title">Найден артефакт!</p>
              <ul className="qr-hero__rewards">
                {legacyResult.grantedItem && (
                  <li>{legacyResult.grantedItem.template.name}</li>
                )}
                {legacyResult.coinAmount != null && (
                  <li>+{legacyResult.coinAmount} M-Coins</li>
                )}
                {legacyResult.locationDiscovered && <li>Открыта локация на карте</li>}
              </ul>
            </>
          ) : (
            <>
              <p className="qr-hero__eyebrow">Не вышло</p>
              <p className="qr-hero__title">{lastError ?? 'QR недоступен'}</p>
            </>
          )}
          <Link to="/inventory" className="btn btn--secondary qr-hero__cta">
            Смотреть инвентарь
          </Link>
        </div>
      )}

      {!scanning && !hasOutcome && (
        <article className="qr-howto">
          <p className="qr-howto__lead">
            Не сканируйте QR внутри приложения — откройте обычную камеру телефона.
          </p>
          <ol className="qr-howto__steps">
            <li>
              <strong>Откройте камеру</strong>
              <span>стандартное приложение камеры на телефоне</span>
            </li>
            <li>
              <strong>Наведите на QR</strong>
              <span>до появления ссылки на экранчике</span>
            </li>
            <li>
              <strong>Перейдите по ссылке</strong>
              <span>награда придёт автоматически после входа</span>
            </li>
          </ol>
          <p className="qr-howto__note">
            Если вы ещё не вошли в M-OS — сначала войдите, затем отсканируйте QR снова.
          </p>
        </article>
      )}

      <div className="qr-manual">
        <button
          type="button"
          className="qr-manual__toggle"
          onClick={() => setShowManual((open) => !open)}
        >
          {showManual ? 'Скрыть ручной ввод' : 'Нет камеры? Ввести код вручную'}
        </button>
        {showManual && (
          <form className="player-form" onSubmit={(event) => void handleManualSubmit(event)}>
            <label className="form-field">
              <span>Код или ссылка из QR</span>
              <input
                type="text"
                value={manualCode}
                onChange={(event) => setManualCode(event.target.value)}
                placeholder="QR-COIN или http://…/qr/…"
                autoComplete="off"
              />
            </label>
            <button type="submit" className="btn btn--secondary" disabled={scanning}>
              Получить награду
            </button>
          </form>
        )}
      </div>
    </section>
  );
}
