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
  'QR code not found': 'QR-код не найден',
  'QR already scanned': 'Этот QR уже найден вами',
  'QR unavailable': 'QR недоступен',
  'You have already scanned this QR code': 'Вы уже сканировали этот QR-код',
  'Quest completion limit reached': 'Лимит выполнений квеста исчерпан',
  'Quest is not started': 'Сначала возьмите квест',
  'Quest is no longer available for you': 'Этот квест для вас больше недоступен',
  'Quest is not assigned to you': 'Это задание назначено другому игроку',
  'Assignee is not a participant of this game session': 'Выбранный игрок не в текущей сессии',
  'QR code scan limit reached': 'Лимит сканирований QR-кода исчерпан',
  'QR code is inactive': 'QR-код неактивен',
  'Secret code not found': 'Промокод не найден',
  'This secret is not assigned to you': 'Этот промокод не назначен вам',
  'Secret code has already been used': 'Промокод уже использован',
  'Reward type not yet implemented: NUMBER': 'Награда «число» пока не реализована',
  'Reward type not yet implemented: QUEST': 'Награда «квест» пока не реализована',
  'Trade not found': 'Обмен не найден',
  'You are not a participant of this trade': 'Вы не участник этого обмена',
  'Trade is not in a valid state for this operation': 'Обмен в недопустимом статусе для этой операции',
  'Trade item is not owned by the expected player': 'Предмет обмена принадлежит другому игроку',
  'Insufficient coins for this trade': 'Недостаточно монет для обмена',
  'Cannot create a trade with yourself': 'Нельзя создать обмен с самим собой',
  'Trade must include at least one item or coin offer': 'Обмен должен включать монеты или предметы',
  'Item not found': 'Предмет не найден',
  'Item belongs to a different game session': 'Предмет из другой игровой сессии',
  'You do not own this item': 'Этот предмет вам не принадлежит',
  'Leaderboard is disabled for this game session': 'Рейтинг отключён для этой сессии',
  'Transfers and trades are disabled while the auction is active':
    'Переводы и обмены отключены на время аукциона',
  'Auction is not open yet': 'Аукцион ещё не открыт',
  'Auction lot not found': 'Лот аукциона не найден',
  'Auction lot is not in a valid state for this operation':
    'Лот в недопустимом статусе для этой операции',
  'Bid amount is too low': 'Ставка слишком низкая',
  'Cannot sell a lot with no bids': 'Нельзя продать лот без ставок',
  'This lore fragment has already been found': 'Этот фрагмент уже найден',
  'You already have a lore fragment. Sell, trade, or give it to another player to take a new one.':
    'У тебя уже есть фрагмент лора. Продай, обменяй или отдай его другому игроку, чтобы взять новый.',
  'A player can hold only one lore fragment at a time':
    'У игрока может быть только один фрагмент лора. Обменяйте или отдайте текущий, чтобы получить другой.',
  'Unique item already exists in this game session': 'Этот уникальный предмет уже найден в сессии',
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
  FAILED: 'Закрыто',
  AVAILABLE: 'Доступно',
  DISABLED: 'Отключён',
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

export const questType: Record<string, string> = {
  COLLECT_ITEMS: 'Собрать предметы',
  FIND_LOCATIONS: 'Найти локации',
  COLLECT_NUMBERS: 'Собрать числа',
  CUSTOM: 'Особое',
  SOCIAL: 'Социальное',
};

export const itemRarity: Record<string, string> = {
  COMMON: 'Обычный',
  UNCOMMON: 'Необычный',
  RARE: 'Редкий',
  EPIC: 'Эпический',
  LEGENDARY: 'Легендарный',
};

export const tradeStatus: Record<string, string> = {
  PENDING: 'Ожидает',
  ACCEPTED: 'Принят',
  DECLINED: 'Отклонён',
  CANCELLED: 'Отменён',
};

export const auctionLotStatus: Record<string, string> = {
  DRAFT: 'Черновик',
  OPEN: 'Торги',
  SOLD: 'Продан',
  CANCELLED: 'Снят',
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
