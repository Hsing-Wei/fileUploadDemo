package com.example.fileUploadDemo.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "文件", description = "文件信息描述")
public class File {
    @ApiModelProperty(value = "是否存在")
    private boolean isExist;
    @ApiModelProperty(value = "文件名")
    private String name;

    public File(boolean isExist, String name) {
        this.isExist = isExist;
        this.name = name;
    }

    public void setIsExist(boolean isExist) {
        this.isExist = isExist;
    }
    public boolean getIsExist() {
        return isExist;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
