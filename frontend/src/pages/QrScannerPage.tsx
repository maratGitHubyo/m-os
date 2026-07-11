import { useCallback, useEffect, useRef, useState } from 'react';
import { Html5Qrcode } from 'html5-qrcode';
import { extractPublicIdFromQrText, scanQrByPublicId, scanQrCode } from '../api/qr';
import { translateError } from '../i18n/ru';
import type { QrScanResponse, QrScanResultResponse } from '../types';

const SCANNER_ELEMENT_ID = 'qr-scanner-viewport';

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

  return '🎉 Найден артефакт!';
}

export function QrScannerPage() {
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const processingRef = useRef(false);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [lastResult, setLastResult] = useState<QrScanResultResponse | null>(null);
  const [legacyResult, setLegacyResult] = useState<QrScanResponse | null>(null);
  const [lastError, setLastError] = useState<string | null>(null);
  const [manualCode, setManualCode] = useState('');

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
    setLastError(null);
    setLegacyResult(null);

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
      setLastResult(null);
      setLegacyResult(legacyResponse);
    } catch (err) {
      const message =
        err instanceof Error ? translateError(err.message) : 'Не удалось отсканировать QR';
      setLastError(message);
      setLastResult(null);
      setLegacyResult(null);
    } finally {
      window.setTimeout(() => {
        processingRef.current = false;
      }, 1500);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    const scanner = new Html5Qrcode(SCANNER_ELEMENT_ID);
    scannerRef.current = scanner;

    const startScanner = async () => {
      try {
        await scanner.start(
          { facingMode: 'environment' },
          { fps: 10, qrbox: { width: 250, height: 250 } },
          (decodedText) => {
            void processScan(decodedText, false);
          },
          () => {
            // ignore frame-level decode noise
          },
        );
        if (!cancelled) {
          setCameraError(null);
        }
      } catch {
        if (!cancelled) {
          setCameraError('Не удалось открыть камеру. Разрешите доступ или введите код вручную.');
        }
      }
    };

    void startScanner();

    return () => {
      cancelled = true;
      void scanner.stop().catch(() => undefined);
      scanner.clear();
      scannerRef.current = null;
    };
  }, [processScan]);

  const handleManualSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    await processScan(manualCode, true);
  };

  return (
    <section className="qr-page qr-scanner-page">
      <h1>Сканировать QR</h1>
      <p className="page-hint">Наведите камеру на QR-код на территории мероприятия.</p>

      <article className="card qr-scanner-card">
        <div id={SCANNER_ELEMENT_ID} className="qr-scanner-viewport" />
        <p className="qr-scanner-hint">Наведи QR</p>
      </article>

      {cameraError && (
        <div className="result-panel result-panel--error" role="alert">
          <strong>Камера</strong>
          <p>{cameraError}</p>
        </div>
      )}

      <article className="card player-action-card">
        <form className="player-form" onSubmit={(event) => void handleManualSubmit(event)}>
          <label className="form-field">
            <span>Или введите код / ссылку вручную</span>
            <input
              type="text"
              value={manualCode}
              onChange={(event) => setManualCode(event.target.value)}
              placeholder="https://mos.local/qr/… или текстовый код"
              autoComplete="off"
            />
          </label>
          <button type="submit" className="btn btn--secondary">
            Отправить
          </button>
        </form>
      </article>

      {(lastResult || legacyResult || lastError) && (
        <div
          className={`result-panel ${
            lastResult?.success || legacyResult ? 'result-panel--success' : 'result-panel--error'
          }`}
        >
          <strong>Последний результат</strong>
          {lastResult?.success ? (
            <>
              <p>{formatScanMessage(lastResult)}</p>
              {lastResult.title && <p className="result-panel__code">{lastResult.title}</p>}
              {lastResult.reward?.item && (
                <p className="result-panel__meta">🎁 {lastResult.reward.item}</p>
              )}
              {lastResult.reward?.coins != null && (
                <p className="result-panel__meta">+{lastResult.reward.coins} M-Coins</p>
              )}
            </>
          ) : legacyResult ? (
            <>
              <p>🎉 Найден артефакт!</p>
              {legacyResult.grantedItem && (
                <p className="result-panel__meta">🎁 {legacyResult.grantedItem.template.name}</p>
              )}
              {legacyResult.coinAmount != null && (
                <p className="result-panel__meta">+{legacyResult.coinAmount} M-Coins</p>
              )}
              {legacyResult.locationDiscovered && (
                <p className="result-panel__meta">Открыта новая локация на карте</p>
              )}
            </>
          ) : (
            <p>{lastError ?? 'QR недоступен'}</p>
          )}
        </div>
      )}
    </section>
  );
}
