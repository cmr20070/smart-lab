package com.smartlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlab.entity.Device;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
}
