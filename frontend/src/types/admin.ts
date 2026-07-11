import type {
  GameSessionInfo,
  GameEvent,
  ItemTemplate,
  LeaderboardEntry,
  Quest,
  VictoryCondition,
} from './index';

export interface AdminDashboardResponse {
  currentSession: GameSessionInfo;
  playersCount: number;
  activeQuests: number;
  locationsCount: number;
  itemsCount: number;
  qrCount: number;
  leaderboard: LeaderboardEntry[];
}

export interface AdminWalletOperationRequest {
  amount: number;
  description: string;
}

export interface AdminScoreChangeRequest {
  category: 'TOTAL' | 'EXPLORER' | 'COLLECTOR' | 'TRADER' | 'QUEST';
  points: number;
  reason: string;
}

export interface CreateItemTemplateRequest {
  name: string;
  description: string;
  imageUrl?: string;
  rarity: 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';
  isUnique: boolean;
}

export interface AdminGrantItemRequest {
  userId: string;
  itemTemplateId: string;
}

export interface CreateLocationRequest {
  name: string;
  description: string;
  x: number;
  y: number;
  zone: string;
  hidden: boolean;
}

export interface UpdateLocationRequest {
  name?: string;
  description?: string;
  x?: number;
  y?: number;
  zone?: string;
  hidden?: boolean;
}

export interface AdminLocationPoint {
  id: string;
  gameSessionId: string;
  name: string;
  description: string;
  x: number;
  y: number;
  zone: string;
  hidden: boolean;
}

export interface CreateQrCodeRequest {
  code: string;
  locationPointId?: string | null;
  rewardType: 'COIN' | 'ITEM' | 'NONE';
  rewardPayload?: Record<string, unknown>;
  scanPolicy: 'FIRST_PLAYER' | 'EVERY_PLAYER' | 'LIMITED';
  scanLimit?: number | null;
}

export type QrRewardKind = 'NONE' | 'COIN' | 'ITEM' | 'LOCATION';

export interface CreateQrCodeSimpleRequest {
  title: string;
  rewardKind: QrRewardKind;
  coinAmount?: number | null;
  itemTemplateId?: string | null;
  locationPointId?: string | null;
  scanPolicy?: 'FIRST_PLAYER' | 'EVERY_PLAYER' | 'LIMITED';
  scanLimit?: number | null;
}

export interface QrCodeInfo {
  id: string;
  gameSessionId: string;
  publicId: string;
  title: string;
  locationPointId: string | null;
  code: string;
  rewardType: 'COIN' | 'ITEM' | 'NONE';
  rewardPayload: Record<string, unknown> | null;
  scanPolicy: 'FIRST_PLAYER' | 'EVERY_PLAYER' | 'LIMITED';
  scanLimit: number | null;
  active: boolean;
}

export interface CreatePlayerSecretRequest {
  userId: string;
  code: string;
  title: string;
  description: string;
  rewardType: 'COIN' | 'ITEM' | 'NUMBER' | 'QUEST' | 'NONE';
  rewardPayload?: Record<string, unknown>;
}

export interface PlayerSecretInfo {
  id: string;
  userId: string;
  gameSessionId: string;
  code: string;
  title: string;
  description: string;
  rewardType: 'COIN' | 'ITEM' | 'NUMBER' | 'QUEST' | 'NONE';
  rewardPayload: Record<string, unknown> | null;
  used: boolean;
  usedAt: string | null;
}

export interface CreateCollectibleNumberRequest {
  numberValue: number;
}

export interface CollectibleNumberInfo {
  id: string;
  gameSessionId: string;
  numberValue: number;
  createdAt: string;
}

export interface CreateQuestRequest {
  title: string;
  description?: string;
  type: Quest['type'];
  targetConfig: Record<string, unknown>;
  rewardConfig?: Record<string, unknown> | null;
  status?: 'ACTIVE' | 'DISABLED';
}

export interface UpdateQuestRequest {
  title?: string;
  description?: string;
  targetConfig?: Record<string, unknown>;
  rewardConfig?: Record<string, unknown> | null;
  status?: 'ACTIVE' | 'DISABLED';
}

export interface CreateVictoryConditionRequest {
  type: VictoryCondition['type'];
  targetValue?: Record<string, unknown>;
  description: string;
  active?: boolean;
}

export interface VictoryAdminStatusResponse {
  gameSessionId: string;
  anyAchieved: boolean;
  sessionFinished: boolean;
  conditions: VictoryCondition[];
}

export interface CreateGameEventRequest {
  type: GameEvent['type'];
  title: string;
  description: string;
  startAt?: string | null;
  endAt?: string | null;
  config?: Record<string, unknown> | null;
}

export interface UpdateGameEventStatusRequest {
  status: GameEvent['status'];
}

export type AuditAction =
  | 'COIN_CREDIT'
  | 'COIN_DEBIT'
  | 'ITEM_GRANT'
  | 'ITEM_TRANSFER'
  | 'QR_SCAN'
  | 'TRADE_COMPLETE'
  | 'QUEST_COMPLETE'
  | 'SECRET_REDEEM'
  | 'ADMIN_ACTION'
  | 'LOCATION_DISCOVER'
  | 'SCORE_AWARD'
  | 'SCORE_ADD'
  | 'SCORE_SUBTRACT'
  | 'EVENT_CREATE'
  | 'EVENT_STATUS_CHANGE'
  | 'VICTORY_ACHIEVED'
  | 'NUMBER_GRANT'
  | 'QUEST_CREATE'
  | 'QUEST_START'
  | 'QUEST_PROGRESS'
  | 'TRADE_CREATE'
  | 'TRADE_ACCEPT'
  | 'TRADE_DECLINE'
  | 'TRADE_CANCEL'
  | 'SESSION_START'
  | 'SESSION_PAUSE'
  | 'SESSION_FINISH';

export interface AuditLogEntry {
  id: string;
  userId: string;
  gameSessionId: string;
  action: AuditAction;
  entityType: string;
  entityId: string;
  description: string;
  metadata: Record<string, unknown> | null;
  createdAt: string;
}

export interface AdminPlayerRow {
  userId: string;
  nickname: string;
  rank: number;
  totalScore: number;
  username: string | null;
  role: string | null;
  active: boolean | null;
  coins: number | null;
}

export type { ItemTemplate };
