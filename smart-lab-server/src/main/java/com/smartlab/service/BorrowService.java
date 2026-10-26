package com.smartlab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartlab.dto.ApproveDTO;
import com.smartlab.dto.BorrowDTO;
import com.smartlab.entity.Borrow;

public interface BorrowService extends IService<Borrow> {
    void apply(BorrowDTO dto);
    void approve(ApproveDTO dto);
    void returnDevice(Long borrowId);
    void cancelBorrow(Long borrowId);
}
