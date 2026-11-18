package com.library.modules.author.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.modules.author.entity.Author;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthorMapper extends BaseMapper<Author> {
}
