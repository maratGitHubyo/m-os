package com.mos.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request
    ) {
        log.warn("Authentication failed: path={}", request.getRequestURI());
        return error(request, HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(UserInactiveException.class)
    public ResponseEntity<ErrorResponse> handleUserInactive(
            UserInactiveException ex,
            HttpServletRequest request
    ) {
        log.warn("Inactive user login attempt: path={}", request.getRequestURI());
        return error(request, HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(SessionConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleSessionConfiguration(
            SessionConfigurationException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(
            InsufficientBalanceException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAmount(
            InvalidAmountException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConcurrentModificationException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentModification(
            ConcurrentModificationException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleItemNotFound(
            ItemNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ItemNotOwnedException.class)
    public ResponseEntity<ErrorResponse> handleItemNotOwned(
            ItemNotOwnedException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(UniqueItemAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUniqueItemExists(
            UniqueItemAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ItemSessionMismatchException.class)
    public ResponseEntity<ErrorResponse> handleItemSessionMismatch(
            ItemSessionMismatchException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(LocationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLocationNotFound(
            LocationNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(LocationSessionMismatchException.class)
    public ResponseEntity<ErrorResponse> handleLocationSessionMismatch(
            LocationSessionMismatchException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(QrCodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQrCodeNotFound(
            QrCodeNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(QrAlreadyScannedException.class)
    public ResponseEntity<ErrorResponse> handleQrAlreadyScanned(
            QrAlreadyScannedException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(QrScanLimitReachedException.class)
    public ResponseEntity<ErrorResponse> handleQrScanLimitReached(
            QrScanLimitReachedException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidQrRewardException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQrReward(
            InvalidQrRewardException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(SecretNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSecretNotFound(
            SecretNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SecretNotOwnedException.class)
    public ResponseEntity<ErrorResponse> handleSecretNotOwned(
            SecretNotOwnedException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(SecretAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleSecretAlreadyUsed(
            SecretAlreadyUsedException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidSecretRewardException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSecretReward(
            InvalidSecretRewardException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(SecretRewardNotImplementedException.class)
    public ResponseEntity<ErrorResponse> handleSecretRewardNotImplemented(
            SecretRewardNotImplementedException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.NOT_IMPLEMENTED, ex.getMessage());
    }

    @ExceptionHandler(LeaderboardDisabledException.class)
    public ResponseEntity<ErrorResponse> handleLeaderboardDisabled(
            LeaderboardDisabledException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InsufficientScoreException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientScore(
            InsufficientScoreException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(GameEventNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGameEventNotFound(
            GameEventNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NumberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNumberNotFound(
            NumberNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NumberSessionMismatchException.class)
    public ResponseEntity<ErrorResponse> handleNumberSessionMismatch(
            NumberSessionMismatchException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DuplicateNumberValueException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateNumberValue(
            DuplicateNumberValueException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(QuestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQuestNotFound(
            QuestNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(QuestSessionMismatchException.class)
    public ResponseEntity<ErrorResponse> handleQuestSessionMismatch(
            QuestSessionMismatchException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(QuestAlreadyCompletedException.class)
    public ResponseEntity<ErrorResponse> handleQuestAlreadyCompleted(
            QuestAlreadyCompletedException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(QuestNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleQuestNotActive(
            QuestNotActiveException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(TradeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTradeNotFound(
            TradeNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TradeNotParticipantException.class)
    public ResponseEntity<ErrorResponse> handleTradeNotParticipant(
            TradeNotParticipantException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(TradeInvalidStateException.class)
    public ResponseEntity<ErrorResponse> handleTradeInvalidState(
            TradeInvalidStateException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(TradeItemNotOwnedException.class)
    public ResponseEntity<ErrorResponse> handleTradeItemNotOwned(
            TradeItemNotOwnedException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(TradeInsufficientCoinsException.class)
    public ResponseEntity<ErrorResponse> handleTradeInsufficientCoins(
            TradeInsufficientCoinsException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        return error(request, HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, "Invalid request parameter: " + ex.getName());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception: method={} path={}", request.getMethod(), request.getRequestURI(), ex);
        return error(request, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ResponseEntity<ErrorResponse> error(HttpServletRequest request, HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        request.getRequestURI()
                ));
    }
}
