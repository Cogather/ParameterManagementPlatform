package com.coretool.param.ui.exception;

import com.coretool.param.domain.exception.BlacklistViolationException;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.ui.response.ChangeSourceBlacklistViolationPayload;
import com.coretool.param.ui.response.ResponseObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class ExceptionHandlerController {
    private static final Log LOG = LogFactory.getLog(ExceptionHandlerController.class);

    /**
     * 处理业务异常（HTTP 500，success=false）。
     *
     * @param req 请求
     * @param e   业务异常
     * @return 响应
     */
    @ExceptionHandler(value = BizException.class)
    @ResponseBody
    public ResponseEntity<ResponseObject<Object>> handleBizException(HttpServletRequest req, BizException e) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseObject<Object> body = new ResponseObject<Object>().failure(e.getMessage());
        return new ResponseEntity<>(body, headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理变更来源黑名单命中异常（HTTP 500 + data.violatedKeywordRegex）。
     *
     * @param req 请求
     * @param e   黑名单命中异常
     * @return 响应
     */
    @ExceptionHandler(value = BlacklistViolationException.class)
    @ResponseBody
    public ResponseEntity<ResponseObject<ChangeSourceBlacklistViolationPayload>> handleBlacklistViolation(
            HttpServletRequest req, BlacklistViolationException e) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseObject<ChangeSourceBlacklistViolationPayload> body = new ResponseObject<>();
        body.setSuccess(false);
        body.setMessage(e.getMessage());
        body.setData(new ChangeSourceBlacklistViolationPayload(e.getViolatedKeywordRegex()));
        return new ResponseEntity<>(body, headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理领域规则异常（HTTP 500，success=false）。
     *
     * @param req 请求
     * @param e   领域规则异常
     * @return 响应
     */
    @ExceptionHandler(value = DomainRuleException.class)
    @ResponseBody
    public ResponseEntity<ResponseObject<Object>> handleDomainRule(HttpServletRequest req, DomainRuleException e) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseObject<Object> body = new ResponseObject<Object>().failure(e.getMessage());
        return new ResponseEntity<>(body, headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理未捕获的系统异常（HTTP 状态按异常类型映射，响应体为 {@link ResponseObject}）。
     *
     * @param req 请求
     * @param e   异常
     * @return 响应
     */
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ResponseEntity<ResponseObject<Object>> defaultErrorHandler(HttpServletRequest req, Exception e) {
        LOG.error("Internal Error: ", e);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "内部错误！麻烦通过右上角的'反馈意见'功能反馈，谢谢！";

        if (e instanceof MissingServletRequestParameterException
                || e instanceof MethodArgumentTypeMismatchException) {
            status = HttpStatus.BAD_REQUEST;
            message = "请求参数缺失: " + e.getMessage();
        } else if (e instanceof HttpRequestMethodNotSupportedException) {
            status = HttpStatus.METHOD_NOT_ALLOWED;
            message = "不支持的请求方法";
        } else if (e instanceof NoHandlerFoundException) {
            status = HttpStatus.NOT_FOUND;
            message = "接口不存在";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "内部错误！麻烦通过右上角的'反馈意见'功能反馈，谢谢！";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("code", status.value());
        errorResponse.put("message", message);
        errorResponse.put("timestamp", new Date());

        ResponseObject<Object> body = new ResponseObject<Object>()
                .failure(message)
                .setDataAndReturn(errorResponse);
        return new ResponseEntity<>(body, headers, status);
    }
}

