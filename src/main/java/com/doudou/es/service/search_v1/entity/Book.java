package com.doudou.es.service.search_v1.entity;

import com.doudou.es.service.search_v1.common.EntityForSearch;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@ApiModel
public class Book implements EntityForSearch {
    @ApiModelProperty(dataType = "int")
    private int id;
    @ApiModelProperty(dataType = "String")
    private String bookName;
    @ApiModelProperty(dataType = "String")
    private String author;
}
