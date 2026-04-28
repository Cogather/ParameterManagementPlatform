package com.coretool.param.ui.response;

import com.coretool.param.constants.CommonConst;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseObjectTest {

    @Test
    void success_shouldSetOkMessage_andData() {
        ResponseObject<Integer> r = new ResponseObject<>();
        r.success(99);
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getMessage()).isEqualTo(CommonConst.OK);
        assertThat(r.getData()).isEqualTo(99);
    }

    @Test
    void success_noArg_shouldOnlySetOkFlag() {
        ResponseObject<Object> r = new ResponseObject<>();
        r.success();
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getMessage()).isEqualTo(CommonConst.OK);
    }

    @Test
    void success_withCustomMessage_shouldNotForceOk() {
        ResponseObject<Object> r = new ResponseObject<>();
        r.success("自定义");
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getMessage()).isEqualTo("自定义");
    }

    @Test
    void failure_shouldSetErrorConstant_whenNoArgs() {
        ResponseObject<Object> r = new ResponseObject<>();
        r.failure();
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getMessage()).isEqualTo(CommonConst.ERROR);
    }

    @Test
    void failure_withReason_shouldRetainMessage() {
        ResponseObject<Object> r = new ResponseObject<>();
        r.failure("原因");
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getMessage()).isEqualTo("原因");
    }

    @Test
    void setDataAndReturn_shouldChain() {
        ResponseObject<Integer> r = new ResponseObject<>();
        ResponseObject<Integer> same = r.setDataAndReturn(42);
        assertThat(same).isSameAs(r);
        assertThat(r.getData()).isEqualTo(42);
    }
}
