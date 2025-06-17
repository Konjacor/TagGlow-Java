package com.vivo50.service.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author CanoeLike
 * @since 2025-06-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="Note对象", description="")
public class Note implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "笔记实体id")
    @TableId(value = "id", type = IdType.ID_WORKER_STR)
    private String id;

    @ApiModelProperty(value = "该笔记所属用户id")
    private String userId;

    @ApiModelProperty(value = "笔记内容")
    private String content;

    @ApiModelProperty(value = "笔记所在地理位置")
    private String position;

    @ApiModelProperty(value = "记笔记时的天气信息")
    private String weather;

    @ApiModelProperty(value = "笔记主题")
    private Integer classification;

    @ApiModelProperty(value = "记笔记时的时间")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "GMT+8")
    private Date time;

    @ApiModelProperty(value = "记录创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;

    @ApiModelProperty(value = "记录修改时间")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date gmtModified;


    @ApiModelProperty(value = "是否被逻辑删除 0-否 1-是")
    @TableField(value = "is_deleted", fill = FieldFill.INSERT)
    @TableLogic
    private Integer isDeleted;


}
