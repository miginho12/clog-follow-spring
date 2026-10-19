package com.clog.follow.common;

import com.clog.follow.follow.exception.CannotFollowSelfException;
import com.clog.follow.follow.exception.FollowRequestNotFoundException;
import com.clog.follow.follow.exception.InvalidFollowTransitionException;
import com.clog.follow.follow.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 도메인 예외 → HTTP 상태 매핑 (RFC 9457 ProblemDetail). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CannotFollowSelfException.class)
    public ProblemDetail handleSelfFollow(CannotFollowSelfException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({UserNotFoundException.class, FollowRequestNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidFollowTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidFollowTransitionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
