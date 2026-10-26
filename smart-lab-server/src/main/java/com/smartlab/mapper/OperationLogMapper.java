package com.smartlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlab.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}