package com.coretool.param.ui.vo;

/**
 * 拉分支：基于基线版本创建新版本，可选复制参数（参数复制见 spec-03，当前未接表则仅创建版本行）。
 */
@lombok.Data
public class VersionBranchRequest {
    private String baseVersionId;
    private String newVersionId;
    private String newVersionName;
    private String versionNumber;
    private String versionDescription;
    private Boolean copyParameters;
}
