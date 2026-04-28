package com.coretool.param.ui.exception;

import com.coretool.param.domain.exception.BlacklistViolationException;
import com.coretool.param.ui.response.ChangeSourceBlacklistViolationPayload;
import com.coretool.param.ui.response.ResponseObject;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExceptionHandlerBlacklistContractTest {

    @Test
    void blacklistViolation_returns500WithViolatedKeywordRegexInData() {
        ExceptionHandlerController advice = new ExceptionHandlerController();
        BlacklistViolationException ex = new BlacklistViolationException("PARAM_CHANGE_SOURCE_FORBIDDEN: 命中 forbidden", "forbidden");

        ResponseEntity<ResponseObject<ChangeSourceBlacklistViolationPayload>> res =
                advice.handleBlacklistViolation(null, ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(false, res.getBody().isSuccess());
        assertNotNull(res.getBody().getData());
        assertEquals("forbidden", res.getBody().getData().getViolatedKeywordRegex());
    }
}
