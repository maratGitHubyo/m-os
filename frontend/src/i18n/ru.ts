/** Перевод сообщений API на русский (backend отдаёт English). */
const ERROR_MESSAGES: Record<string, string> = {
  'Invalid username or password': 'Неверный логин или пароль',
  'Authentication required': 'Требуется авторизация',
  'Access denied': 'Доступ запрещён',
  'Unauthorized': 'Требуется авторизация',
  'Forbidden': 'Доступ запрещён',
  'Internal server error': 'Внутренняя ошибка сервера',
  'Validation failed': 'Ошибка проверки данных',
  'Malformed request body': 'Некорректное тело запроса',
  'Resource not found': 'Ресурс не найден',
  'No ACTIVE or STARTING game session configured': 'Нет активной или запускаемой игровой сессии',
  'Insufficient M-coin balance': 'Недостаточно М-коинов',
  'Cannot transfer coins to yourself': 'Нельзя переводить монеты самому себе',
  'User is not a participant of this game session': 'Игрок не участник текущей сессии',
};

export function translateError(message: string): string {
  return ERROR_MESSAGES[message] ?? message;
}

export const sessionStatus: Record<string, string> = {
  DRAFT: 'Черновик',
  STARTING: 'Запуск',
  ACTIVE: 'Идёт игра',
  PAUSED: 'Пауза',
  FINISHED: 'Завершена',
};

export const questStatus: Record<string, string> = {
  ACTIVE: 'Активно',
  COMPLETED: 'Выполнено',
  FAILED: 'Провалено',
  AVAILABLE: 'Доступно',
};

export const eventStatus: Record<string, string> = {
  SCHEDULED: 'запланировано',
  ACTIVE: 'активно',
  RUNNING: 'идёт',
  PAUSED: 'на паузе',
  FINISHED: 'завершено',
  COMPLETED: 'завершено',
  CANCELLED: 'отменено',
};

export const gameEventType: Record<string, string> = {
  ANNOUNCEMENT: 'Объявление',
  CHALLENGE: 'Испытание',
  BONUS: 'Бонус',
  BONUS_PERIOD: 'Бонусный период',
  LOCATION_REVEAL: 'Открытие локации',
  LEADERBOARD_FREEZE: 'Заморозка рейтинга',
  AUCTION: 'Аукцион',
  CUSTOM: 'Особое',
};

export const victoryType: Record<string, string> = {
  SCORE_THRESHOLD: 'Набрать очки',
  COLLECT_NUMBERS: 'Собрать числа',
  COLLECT_ALL_NUMBERS: 'Собрать все числа',
  COLLECT_UNIQUE_ITEMS: 'Собрать уникальные предметы',
  REACH_SCORE: 'Набрать очки',
  FIND_LOCATIONS: 'Найти локации',
  FIND_ALL_LOCATIONS: 'Найти все локации',
  COLLECT_ITEMS: 'Собрать предметы',
  CUSTOM: 'Особое условие',
};

export const questType: Record<string, string> = {
  COLLECT_ITEMS: 'Собрать предметы',
  FIND_LOCATIONS: 'Найти локации',
  COLLECT_NUMBERS: 'Собрать числа',
  REACH_SCORE: 'Набрать очки',
  CUSTOM: 'Особое',
};

export const itemRarity: Record<string, string> = {
  COMMON: 'Обычный',
  UNCOMMON: 'Необычный',
  RARE: 'Редкий',
  EPIC: 'Эпический',
  LEGENDARY: 'Легендарный',
};

export const roleLabel: Record<string, string> = {
  ADMIN: 'Админ',
  PLAYER: 'Игрок',
};

export function formatEnum(value: string, map: Record<string, string>): string {
  return map[value] ?? value.replaceAll('_', ' ').toLowerCase();
}

export const ui = {
  loading: 'Загрузка…',
  noData: 'Нет данных.',
  cancel: 'Отмена',
  confirm: 'Подтвердить',
  save: 'Сохранить',
  create: 'Создать',
  edit: 'Изменить',
  delete: 'Удалить',
  back: 'Назад',
  dismiss: 'Закрыть',
  goHome: 'На главную',
  coins: 'М-коины',
  points: 'очков',
};
