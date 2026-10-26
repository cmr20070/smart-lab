package com.smartlab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartlab.dto.RepairDTO;
import com.smartlab.entity.Repair;

public interface RepairService extends IService<Repair> {
    void reportFault(RepairDTO dto);
    void assignRepair(Long repairId);
    void completeRepair(Long repairId, String repairDescription, java.math.BigDecimal cost);
}
