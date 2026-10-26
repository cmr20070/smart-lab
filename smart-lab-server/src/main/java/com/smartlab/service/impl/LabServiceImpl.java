package com.smartlab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartlab.entity.Lab;
import com.smartlab.mapper.LabMapper;
import com.smartlab.service.LabService;
import org.springframework.stereotype.Service;

@Service
public class LabServiceImpl extends ServiceImpl<LabMapper, Lab> implements LabService {
}
