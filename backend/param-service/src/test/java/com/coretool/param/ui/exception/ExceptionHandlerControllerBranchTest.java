package com.coretool.param.ui.exception;

import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.ui.response.ResponseObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 补充黑名单以外的异常处理契约（方法与 OperationLog WebMvcTest 同属 ui 冒烟层）。
 */
class ExceptionHandlerControllerBranchTest {

    private ExceptionHandlerController advice;

    @BeforeEach
    void setUp() {
        advice = new ExceptionHandlerController();
    }

    @Test
    void handleBizException_shouldReturn500WithBodyMessage() {
        ResponseEntity<ResponseObject<Object>> res =
                advice.handleBizException(null, new BizException("biz-msg"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isFalse();
        assertThat(res.getBody().getMessage()).isEqualTo("biz-msg");
    }

    @Test
    void handleDomainRule_shouldReturn500WithBodyMessage() {
        ResponseEntity<ResponseObject<Object>> res =
                advice.handleDomainRule(null, new DomainRuleException("RULE"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isFalse();
        assertThat(res.getBody().getMessage()).isEqualTo("RULE");
    }

    @Test
    void defaultErrorHandler_shouldMapMissingParameter_toBadRequest() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("productId", "String");

        ResponseEntity<ResponseObject<Object>> res = advice.defaultErrorHandler(null, ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getMessage()).contains("请求参数缺失");
        assertThat(res.getBody().getData()).isNotNull();
    }

    @Test
    void defaultErrorHandler_shouldMapMethodNotSupported_to405() {
        ResponseEntity<ResponseObject<Object>> res =
                advice.defaultErrorHandler(
                        null, new HttpRequestMethodNotSupportedException("PATCH"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(res.getBody().getMessage()).isEqualTo("不支持的请求方法");
    }

    @Test
    void defaultErrorHandler_shouldMapNoHandlerFound_to404() {
        NoHandlerFoundException ex =
                new NoHandlerFoundException("GET", "/missing", new HttpHeaders());

        ResponseEntity<ResponseObject<Object>> res = advice.defaultErrorHandler(null, ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody().getMessage()).isEqualTo("接口不存在");
    }

    @Test
    void defaultErrorHandler_shouldMapGenericException_to500WithFriendlyMessage() {
        ResponseEntity<ResponseObject<Object>> res =
                advice.defaultErrorHandler(null, new IllegalStateException("x"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody().getMessage()).contains("反馈意见");
    }
}
