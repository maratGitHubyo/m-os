export type VictoryConditionType =
  | 'COLLECT_ALL_NUMBERS'
  | 'COLLECT_UNIQUE_ITEMS'
  | 'REACH_SCORE'
  | 'FIND_ALL_LOCATIONS'
  | 'CUSTOM';

export interface VictoryCondition {
  id: string;
  gameSessionId: string;
  type: VictoryConditionType;
  targetValue: Record<string, unknown>;
  description: string;
  active: boolean;
  achieved: boolean;
  achievedAt: string | null;
  achievedByUserId: string | null;
  achievedByMe: boolean;
  progressCurrent: number | null;
  progressTarget: number | null;
  progressLabel: string | null;
}
