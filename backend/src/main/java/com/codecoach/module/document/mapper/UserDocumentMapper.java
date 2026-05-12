package com.codecoach.module.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codecoach.module.document.entity.UserDocument;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserDocumentMapper extends BaseMapper<UserDocument> {
}
