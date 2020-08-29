package com.example.fileUploadDemo.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "返回实体类", description = "返回信息描述")
public class Resp {
    @ApiModelProperty(value = "状态码")
    private int stat;
    @ApiModelProperty(value = "文件")
    private File file;
    @ApiModelProperty(value = "分片序号列表")
    private List<String> chunkList;
    @ApiModelProperty(value = "备注")
    private String desc;

    public Resp(int stat) {
        this.stat = stat;
    }

    public Resp(int stat, String desc) {
        this.stat = stat;
        this.desc = desc;
    }

    public Resp(int stat, List<String> chunkList, String desc) {
        this.stat = stat;
        this.chunkList = chunkList;
        this.desc = desc;
    }

    public Resp(int stat, File file, String desc) {
        this.stat = stat;
        this.file = file;
        this.desc = desc;
    }

    public void setStat(int stat) {
        this.stat = stat;
    }
    public int getStat() {
        return stat;
    }

    public void setFile(File file) {
        this.file = file;
    }
    public File getFile() {
        return file;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
    public String getDesc() {
        return desc;
    }

    public List<String> getChunkList() {
        return chunkList;
    }

    public void setChunkList(List<String> chunkList) {
        this.chunkList = chunkList;
    }
}
