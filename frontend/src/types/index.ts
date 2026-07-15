export interface HealthResponse {
  status: string;
}

export type ParticipantRole = 'ADMIN' | 'PLAYER';

export type GameSessionStatus = 'STARTING' | 'ACTIVE' | 'PAUSED' | 'FINISHED';

export interface User {
  id: string;
  nickname: string;
  role: ParticipantRole;
  username?: string;
}

export interface Session {
  id: string;
  name: string;
  date?: string;
  status?: GameSessionStatus;
  mapImageUrl?: string | null;
}

export interface GameConfig {
  startingCoins: number;
  maxTradeOffers: number;
  numbersTotal: number;
  fogOfWarEnabled: boolean;
  secretsEnabled: boolean;
  leaderboardEnabled: boolean;
}

export interface GameSessionInfo extends Session {
  createdAt?: string;
  updatedAt?: string;
  config?: GameConfig | null;
}

export interface LoginResponse {
  token: string;
  user: User;
  session: Session;
}

export interface LocationPoint {
  id: string;
  zone: string;
  x: number;
  y: number;
  hidden: boolean;
  discovered: boolean;
  name: string | null;
  description: string | null;
}

export interface Wallet {
  id: string;
  userId: string;
  gameSessionId: string;
  balance: number;
  version: number;
  updatedAt: string;
}

export type CoinTransactionType =
  | 'REWARD'
  | 'TRADE'
  | 'ADMIN'
  | 'QR'
  | 'QUEST'
  | 'SECRET'
  | 'AUCTION'
  | 'TRANSFER_OUT'
  | 'TRANSFER_IN';

export interface CoinTransfer {
  id: string;
  senderUserId: string;
  senderNickname: string;
  receiverUserId: string;
  receiverNickname: string;
  amount: number;
  createdAt: string;
}

export interface CreateCoinTransferRequest {
  receiverUserId: string;
  amount: number;
}

export interface CoinTransaction {
  id: string;
  userId: string;
  gameSessionId: string;
  amount: number;
  type: CoinTransactionType;
  referenceId: string | null;
  description: string | null;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export type ItemRarity = 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';

export interface ItemTemplate {
  id: string;
  gameSessionId: string;
  name: string;
  description: string | null;
  imageUrl: string | null;
  rarity: ItemRarity;
  isUnique: boolean;
  createdAt: string;
}

export interface Item {
  id: string;
  ownerId: string;
  gameSessionId: string;
  acquiredFrom: string;
  acquiredAt: string;
  template: ItemTemplate;
}

export type QuestType =
  | 'COLLECT_ITEMS'
  | 'FIND_LOCATIONS'
  | 'COLLECT_NUMBERS'
  | 'CUSTOM'
  | 'SOCIAL';

export type QuestCompletionPolicy = 'EVERY_PLAYER' | 'LIMITED';

export type QuestDefinitionStatus = 'ACTIVE' | 'DISABLED';

export type PlayerQuestStatus = 'ACTIVE' | 'COMPLETED' | 'FAILED';

export interface Quest {
  id: string;
  gameSessionId: string;
  title: string;
  description: string;
  type: QuestType;
  status: QuestDefinitionStatus;
  completionPolicy: QuestCompletionPolicy;
  completionLimit: number | null;
  assigneeUserId: string | null;
  completedCount: number;
  available: boolean;
  targetConfig: Record<string, unknown>;
  rewardConfig: Record<string, unknown> | null;
  createdAt: string;
}

export interface QuestProgress {
  id: string;
  questId: string;
  userId: string;
  gameSessionId: string;
  status: PlayerQuestStatus;
  progress: Record<string, unknown>;
  completionNote: string | null;
  completedAt: string | null;
  createdAt: string;
  quest: Quest;
}

export interface LeaderboardEntry {
  rank: number;
  userId: string;
  nickname: string;
  balance: number;
}

export interface SessionPlayer {
  id: string;
  nickname: string;
}

export type QrRewardType = 'COIN' | 'ITEM' | 'NONE';

export interface QrScanResponse {
  qrCodeId: string;
  code: string;
  rewardType: QrRewardType;
  coinAmount: number | null;
  grantedItem: Item | null;
  locationDiscovered: boolean;
}

export interface QrScanRewardInfo {
  coins: number | null;
  item: string | null;
}

export interface QrScanResultResponse {
  success: boolean;
  title: string | null;
  reward: QrScanRewardInfo | null;
  message: string;
}

export type SecretRewardType = 'COIN' | 'ITEM' | 'NUMBER' | 'QUEST' | 'NONE';

export interface SecretRedeemResponse {
  secretId: string;
  code: string;
  title: string;
  rewardType: SecretRewardType;
  coinAmount: number | null;
  grantedItem: Item | null;
}

export type TradeStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED';

export interface TradeItem {
  id: string;
  playerItemId: string;
  ownerId: string;
  template: ItemTemplate;
}

export interface TradeCoin {
  id: string;
  userId: string;
  amount: number;
}

export interface Trade {
  id: string;
  gameSessionId: string;
  initiatorId: string;
  receiverId: string;
  status: TradeStatus;
  items: TradeItem[];
  coins: TradeCoin[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateTradeRequest {
  receiverId: string;
  initiatorItemIds?: string[];
  receiverItemIds?: string[];
  initiatorCoins?: number;
  receiverCoins?: number;
}

export type GameEventType =
  | 'ANNOUNCEMENT'
  | 'BONUS_PERIOD'
  | 'LOCATION_REVEAL'
  | 'LEADERBOARD_FREEZE'
  | 'AUCTION'
  | 'CUSTOM';

export type GameEventStatus = 'SCHEDULED' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';

export interface GameEvent {
  id: string;
  gameSessionId: string;
  type: GameEventType;
  title: string;
  description: string | null;
  status: GameEventStatus;
  startAt: string | null;
  endAt: string | null;
  config: Record<string, unknown> | null;
  createdBy: string;
  createdAt: string;
}

export interface GameEventBroadcast {
  eventId: string;
  gameSessionId: string;
  status: GameEventStatus;
  title: string;
  changedAt: string;
}
