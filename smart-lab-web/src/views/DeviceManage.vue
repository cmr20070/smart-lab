<template>
  <div class="page-container">
    <div class="page-hero">
      <div class="hero-left">
        <div class="hero-icon">
          <el-icon :size="26"><Monitor /></el-icon>
        </div>
        <div>
          <h2 class="hero-title">{{ canManageDevice ? '设备管理' : '全部设备' }}</h2>
          <p class="hero-sub">{{ canManageDevice ? '管理实验室全部设备的入库、状态变更与报废流程' : '浏览实验室全部设备，可查看详情并发起借用' }}</p>
        </div>
      </div>
      <div class="hero-actions">
        <button v-if="isStudentOrTeacher" class="batch-btn" :class="{ active: batchMode }" @click="toggleBatchMode">
          <el-icon :size="16"><Grid /></el-icon>
          {{ batchMode ? '退出批量' : '批量借用' }}
        </button>
        <el-button :icon="Download" plain @click="handleExport">导出Excel</el-button>
        <el-button v-if="canManageDevice" :icon="Plus" type="primary" size="large" @click="handleAdd">设备入库</el-button>
      </div>
    </div>

    <div class="status-stats">
      <div v-for="s in statusCards" :key="s.value" class="status-card" @click="quickFilter(s.value)">
        <div class="status-dot" :style="{ background: s.color }"></div>
        <div class="status-info">
          <span class="status-num">{{ statusCounts[s.value] || 0 }}</span>
          <span class="status-name">{{ s.label }}</span>
        </div>
      </div>
    </div>

    <div class="toolbar-card">
      <div class="search-fields">
        <el-input v-model="searchForm.keyword" placeholder="搜索设备名称 / 编号 / 型号" clearable style="width: 280px"
          :prefix-icon="Search" @clear="fetchData" @keyup.enter="fetchData" />
        <el-input v-model="searchForm.category" placeholder="分类" clearable style="width: 140px" @clear="fetchData" />
        <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 160px" @change="fetchData">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </div>
      <div v-if="selectedDevices.length > 0" class="batch-bar">
        <span class="batch-count">已选 <b>{{ selectedDevices.length }}</b> 项</span>
        <el-button size="small" @click="handleSelectAll(true)">全选</el-button>
        <el-button size="small" @click="handleInvertSelect">反选</el-button>
        <el-button size="small" @click="selectedDevices = []">取消选择</el-button>
        <el-button size="small" type="danger" plain @click="handleBatchDelete">批量删除</el-button>
      </div>
    </div>

    
    <el-card v-if="canManageDevice" class="table-card" shadow="never">
      <el-table :data="tableData" border v-loading="loading" style="width: 100%"
        @selection-change="handleDeviceSelectionChange" ref="deviceTableRef" highlight-current-row>
        <el-table-column v-if="canManageDevice" type="selection" width="42" />
        <el-table-column label="序号" width="50" align="center">
          <template #default="{ $index }">
            <span class="seq">{{ (page - 1) * size + $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="设备名称" min-width="130">
          <template #default="{ row }">
            <div class="device-cell">
              <div class="device-avatar">{{ row.name?.charAt(0) || '?' }}</div>
              <div class="device-info">
                <div class="device-name">{{ row.name }}</div>
                <div class="device-code">{{ row.code }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="80">
          <template #default="{ row }">
            <el-tag effect="dark" round :style="{ background: getCategoryColor(row.category) }">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="型号" width="90" show-overflow-tooltip />
        <el-table-column label="描述" width="88" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="dev-desc-cell" :class="{ 'is-empty': !row.description }">{{ row.description || '暂无描述' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="所属实验室" min-width="105">
          <template #default="{ row }">{{ getLabName(row.labId) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="98" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="dark" round class="status-tag">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="图片" width="140" align="center">
          <template #default="{ row }">
            <el-image v-if="row.imageUrl" :src="getImageUrl(row.imageUrl)" :preview-src-list="[getImageUrl(row.imageUrl)]"
              style="width:104px;height:104px;border-radius:12px;object-fit:cover;box-shadow:0 3px 12px rgba(15,23,42,.16);border:1px solid #e2e8f0;background:#f8fafc;padding:1px;display:inline-block;vertical-align:middle" fit="cover" />
            <div v-else class="no-image"><el-icon><Picture /></el-icon></div>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="230" align="center">
          <template #default="{ row }">
            <el-button v-if="canManageDevice" size="small" style="margin:2px" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="canManageDevice" size="small" style="margin:2px" :icon="Switch" @click="handleStatusChange(row)">状态</el-button>
            <template v-if="row.status < 4 && row.status !== 2">
              <el-button v-if="canManageDevice" size="small" style="margin:2px" type="warning" plain @click="handleDiscard(row)">
                {{ userStore.userInfo?.role === 'ADMIN' ? '报废' : '申请报废' }}
              </el-button>
            </template>
            <template v-if="row.status === 6">
              <el-button v-if="canApproveDiscard" size="small" style="margin:2px" type="success" :icon="Check" @click="handleDiscardApprove(row)">通过</el-button>
              <el-button v-if="canApproveDiscard" size="small" style="margin:2px" type="danger" :icon="Close" plain @click="handleDiscardReject(row)">驳回</el-button>
            </template>
            <el-button v-if="canManageDevice" size="small" style="margin:2px" type="danger" :icon="Delete" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div v-if="batchMode && cardSelected.length > 0" class="batch-bar">
      <span class="batch-count">已选 <b>{{ cardSelected.length }}</b> 台设备</span>
      <el-button size="small" @click="selectAllCards">全选本页</el-button>
      <el-button size="small" @click="cardSelected = []">取消选择</el-button>
      <el-button size="small" type="primary" @click="openBatchBorrow">提交批量借用</el-button>
    </div>

    <div v-if="!canManageDevice && loading" class="grid-skeleton">
      <div v-for="i in 8" :key="i" class="sk-item">
        <div class="sk-img"></div>
        <div class="sk-text" style="width:70%"></div>
        <div class="sk-text short"></div>
      </div>
    </div>
    <div v-else-if="!canManageDevice && tableData.length === 0" class="empty-hint">
      <el-icon :size="48"><Monitor /></el-icon>
      <p>没有找到匹配的设备</p>
    </div>
    <div v-else-if="!canManageDevice" class="device-grid">
      <div
        v-for="item in tableData"
        :key="item.id"
        class="dev-card"
        :class="{ selected: batchMode && isCardSelected(item), disabled: item.status !== 1 }"
        @click="handleCardClick(item)"
      >
        <div v-if="batchMode && item.status === 1" class="dc-check" @click.stop="toggleCardSelect(item)">
          <el-icon v-if="isCardSelected(item)" :size="22" color="#2563eb"><CircleCheckFilled /></el-icon>
          <el-icon v-else :size="22" color="#cbd5e1"><CircleCheck /></el-icon>
        </div>
        <div class="dc-image">
          <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" @error="e => e.target.style.display='none'" />
          <el-icon v-else :size="36"><Monitor /></el-icon>
        </div>
        <div class="dc-body">
          <div class="dc-header">
            <h4 class="dc-name">{{ item.name }}</h4>
            <el-tag :type="statusTagType(item.status)" size="small" effect="dark" round>{{ statusLabel(item.status) }}</el-tag>
          </div>
          <div class="dc-meta">
            <span class="dm-item"><span class="dm-lbl">编号</span>{{ item.code }}</span>
            <span class="dm-item"><span class="dm-lbl">分类</span>{{ item.category || '-' }}</span>
          </div>
          <div class="dc-meta">
            <span class="dm-item"><span class="dm-lbl">型号</span>{{ item.model || '-' }}</span>
            <span class="dm-item"><span class="dm-lbl">实验室</span>{{ getLabName(item.labId) || '-' }}</span>
          </div>
          <div class="dc-desc" v-if="item.description">{{ item.description }}</div>
        </div>
      </div>
    </div>

    <div class="pager-bar">
      <span class="pager-info">共 <b>{{ total }}</b> 条记录</span>
      <el-pagination
        v-model:current-page="page" v-model:page-size="size" :total="total"
        :page-sizes="canManageDevice ? [10, 20, 30, 50] : [8, 16, 24]" layout="sizes, prev, pager, next, jumper"
        @size-change="fetchData" @current-change="fetchData" background
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close class="modern-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="设备名称" prop="name">
              <el-input v-model="form.name" placeholder="如 示波器" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="编号" prop="code">
              <el-input v-model="form.code" :disabled="isEdit" placeholder="如 DEV-001" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-input v-model="form.category" placeholder="如 电子仪器" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号">
              <el-input v-model="form.model" placeholder="如 DS1054Z" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规格">
              <el-input v-model="form.specification" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生产厂家">
              <el-input v-model="form.manufacturer" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="采购日期">
              <el-date-picker v-model="form.purchaseDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格">
              <el-input-number v-model="form.price" :precision="2" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="所属实验室">
          <el-select v-model="form.labId" placeholder="请选择实验室" filterable style="width: 100%">
            <el-option v-for="lab in labList" :key="lab.id" :label="lab.name" :value="lab.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="设备描述（选填）" />
        </el-form-item>
        <el-form-item label="设备图片">
          <div class="upload-area">
            <div v-if="form.imageUrl" class="preview-box">
              <el-image :src="getImageUrl(form.imageUrl)" style="width:100%;height:100%;border-radius:10px" fit="cover" />
              <div class="preview-actions">
                <el-button size="small" type="danger" circle :icon="Delete" @click="removeImage" />
              </div>
            </div>
            <el-upload v-else :show-file-list="false" :before-upload="beforeDeviceUpload" :http-request="handleDeviceUpload" accept="image/*">
              <div class="upload-placeholder">
                <el-icon :size="28"><Plus /></el-icon>
                <span>点击上传图片</span>
                <span class="upload-hint">JPG / PNG，≤2MB</span>
              </div>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="batchBorrowVisible" title="批量借用设备" width="680px" :close-on-click-modal="false">
      <div class="batch-dlg">
        <div class="batch-header">
          <el-icon :size="16"><InfoFilled /></el-icon>
          <span>已选择 <b>{{ cardSelected.length }}</b> 台设备</span>
          <template v-if="isStudentRole">
            <span class="batch-limit-tag">学生单次最多借3台，每天最多借3台</span>
          </template>
          <template v-if="isTeacherRole">
            <span class="batch-limit-tag">教师单次最多借100台，每天最多借100台</span>
          </template>
        </div>
        <div class="batch-device-list">
          <div v-for="device in cardSelected" :key="device.id" class="batch-device-item">
            <div class="bdi-left">
              <div class="bdi-thumb">
                <img v-if="device.imageUrl" :src="getImageUrl(device.imageUrl)" @error="e => e.target.style.display='none'" />
                <el-icon v-else :size="20"><Monitor /></el-icon>
              </div>
              <div class="bdi-info">
                <h5 class="bdi-name">{{ device.name }}</h5>
                <div class="bdi-meta">
                  <span>{{ device.code }}</span>
                  <span v-if="device.model"> · {{ device.model }}</span>
                  <span v-if="getLabName(device.labId)"> · {{ getLabName(device.labId) }}</span>
                </div>
              </div>
            </div>
            <div class="bdi-status">
              <el-tag :type="device.status === 1 ? 'success' : 'danger'" size="small" effect="plain">
                {{ device.status === 1 ? '空闲' : '不可借' }}
              </el-tag>
            </div>
          </div>
        </div>
        <el-form :model="batchForm" label-position="top" style="margin-top: 16px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="借用时间">
                <el-date-picker v-model="batchForm.borrowTime" type="datetime" placeholder="默认当前时间" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="预计归还时间" required>
                <el-date-picker v-model="batchForm.expectReturnTime" type="datetime" placeholder="选择归还时间" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="借用用途" required>
                <el-input v-model="batchForm.purpose" placeholder="如：实验课使用、科研项目等" maxlength="200" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="备注说明（可选）">
                <el-input v-model="batchForm.remark" placeholder="补充信息..." maxlength="100" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="batchBorrowVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingBatch" @click="submitBatchBorrow">提交批量借用</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" width="580px">
      <template #header>
        <div class="dlg-head">
          <span class="dlg-ico" style="background:linear-gradient(135deg,#2563eb,#1d4ed8)"><el-icon :size="16"><Monitor /></el-icon></span>
          设备详情
        </div>
      </template>
      <div v-if="detailDevice" class="detail-wrap">
        <div class="detail-hero">
          <div class="detail-img">
            <img v-if="detailDevice.imageUrl" :src="getImageUrl(detailDevice.imageUrl)" @error="onDetailImgError" />
            <div v-else class="no-img"><el-icon :size="40"><Monitor /></el-icon><span>暂无照片</span></div>
          </div>
          <div class="detail-hinfo">
            <h3>{{ detailDevice.name }}</h3>
            <div class="detail-tags">
              <span class="dt" v-if="detailDevice.category">{{ detailDevice.category }}</span>
              <span class="dt teal" v-if="getLabName(detailDevice.labId)">{{ getLabName(detailDevice.labId) }}</span>
              <span class="dt price" v-if="detailDevice.price">¥{{ formatPrice(detailDevice.price) }}</span>
            </div>
            <div class="detail-status" :class="'ds-' + detailDevice.status">
              <span class="ds-dot"></span>{{ statusLabel(detailDevice.status) }}
            </div>
          </div>
        </div>
        <div class="detail-fields">
          <div class="df"><span class="df-lbl">编号</span><span class="df-val">{{ detailDevice.code }}</span></div>
          <div class="df"><span class="df-lbl">型号</span><span class="df-val">{{ detailDevice.model || '-' }}</span></div>
          <div class="df"><span class="df-lbl">厂商</span><span class="df-val">{{ detailDevice.manufacturer || '-' }}</span></div>
          <div class="df"><span class="df-lbl">购入日期</span><span class="df-val">{{ detailDevice.purchaseDate || '-' }}</span></div>
          <div class="df full"><span class="df-lbl">规格参数</span><span class="df-val">{{ detailDevice.specification || '-' }}</span></div>
          <div class="df full"><span class="df-lbl">描述</span><span class="df-val">{{ detailDevice.description || '-' }}</span></div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="detailDevice?.status === 1"
          type="primary"
          @click="handleDetailBorrow(detailDevice)"
          style="background:linear-gradient(135deg,#2563eb,#1d4ed8)"
        >申请借用</el-button>
        <el-button type="warning" @click="handleDetailRepair(detailDevice)">
          <el-icon><Tools /></el-icon>申请维修
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusDialogVisible" title="状态变更" width="560px" class="modern-dialog">
      <el-form label-width="100px">
        <el-form-item label="当前状态">
          <el-tag :type="statusTagType(currentRow?.status)" effect="dark" round>{{ statusLabel(currentRow?.status) }}</el-tag>
        </el-form-item>
        <el-form-item label="变更状态">
          <el-select v-model="targetStatus" style="width: 100%">
            <el-option label="空闲" :value="1" />
            <el-option label="借出" :value="2" />
            <el-option label="维修中" :value="3" />
            <el-option v-if="userStore.userInfo?.role === 'ADMIN'" label="已报废" :value="5" />
          </el-select>
        </el-form-item>

        <template v-if="targetStatus === 2">
          <el-form-item label="借用人" required>
            <el-select v-model="operatorId" filterable placeholder="选择借用人" style="width: 100%">
              <el-option v-for="u in allUsers" :key="u.id" :label="u.username + ' (' + u.role + ')'" :value="u.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="预计归还" required>
            <el-date-picker v-model="expectReturnTime" type="datetime" placeholder="选择预计归还时间"
              :disabled-date="disabledPastDate" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
          </el-form-item>
          <el-form-item label="借用用途">
            <el-input v-model="purpose" type="textarea" :rows="2" maxlength="500" placeholder="选填" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="statusRemark" type="textarea" :rows="2" maxlength="500" placeholder="选填" />
          </el-form-item>
        </template>

        <template v-if="targetStatus === 3">
          <el-form-item label="维修人" required>
            <el-select v-model="operatorId" filterable placeholder="选择维修人" style="width: 100%">
              <el-option v-for="u in labAdmins" :key="u.id" :label="u.username" :value="u.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="故障等级">
            <el-select v-model="faultLevel" style="width: 100%">
              <el-option label="一般" :value="1" />
              <el-option label="较严重" :value="2" />
              <el-option label="严重" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="故障描述" required>
            <el-input v-model="faultDescription" type="textarea" :rows="2" placeholder="请描述故障现象" maxlength="1000" />
          </el-form-item>
          <el-form-item label="维修说明">
            <el-input v-model="repairDescription" type="textarea" :rows="2" placeholder="选填" maxlength="1000" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="statusDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmStatusChange">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="borrowDialogVisible" width="500px" :close-on-click-modal="false" title="申请借用设备">
      <div v-if="borrowDevice" class="bd-wrap">
        <div class="bd-device">
          <div class="bd-thumb">
            <img v-if="borrowDevice.imageUrl" :src="getImageUrl(borrowDevice.imageUrl)" />
            <el-icon v-else :size="28"><Monitor /></el-icon>
          </div>
          <div class="bd-dinfo">
            <h4>{{ borrowDevice.name }}</h4>
            <div class="bd-dmeta">
              <span>{{ borrowDevice.code }}</span>
              <span v-if="borrowDevice.model"> · {{ borrowDevice.model }}</span>
            </div>
            <el-tag type="success" effect="light" round size="small">空闲可借</el-tag>
          </div>
        </div>
        <el-form :model="borrowForm" label-position="top" style="margin-top:16px">
          <el-form-item label="借用用途" required>
            <el-input v-model="borrowForm.purpose" type="textarea" :rows="3" placeholder="请说明借用用途..." maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="预计归还时间" required>
            <el-date-picker v-model="borrowForm.expectReturnTime" type="datetime" placeholder="选择归还时间" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
          </el-form-item>
          <el-form-item label="备注说明（可选）">
            <el-input v-model="borrowForm.remark" placeholder="补充信息..." maxlength="100" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="borrowDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingBorrow" @click="submitBorrow">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="repairDialogVisible" width="500px" :close-on-click-modal="false" title="申请设备维修">
      <div v-if="repairDevice" class="bd-wrap">
        <div class="bd-device">
          <div class="bd-thumb">
            <img v-if="repairDevice.imageUrl" :src="getImageUrl(repairDevice.imageUrl)" />
            <el-icon v-else :size="28"><Monitor /></el-icon>
          </div>
          <div class="bd-dinfo">
            <h4>{{ repairDevice.name }}</h4>
            <div class="bd-dmeta">
              <span>{{ repairDevice.code }}</span>
            </div>
            <el-tag type="warning" effect="light" round size="small">{{ statusLabel(repairDevice.status) }}</el-tag>
          </div>
        </div>
        <el-form :model="repairForm" label-position="top" style="margin-top:16px">
          <el-form-item label="故障级别" required>
            <el-select v-model="repairForm.urgency" style="width:100%">
              <el-option label="一般" value="一般" />
              <el-option label="较急" value="较急" />
              <el-option label="紧急" value="紧急" />
            </el-select>
          </el-form-item>
          <el-form-item label="故障描述" required>
            <el-input v-model="repairForm.description" type="textarea" :rows="3" placeholder="请描述故障现象..." maxlength="300" show-word-limit />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="repairDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingRepair" @click="submitRepair">提交维修申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getDeviceList, addDevice, updateDevice, deleteDevice, updateDeviceStatus, submitDiscard, approveDiscard, approveFinalDiscard, rejectDiscard } from '@/api/device'
import { batchApplyBorrow } from '@/api/borrow'
import { reportFault } from '@/api/repair'
import { getAllLabs } from '@/api/lab'
import request from '@/utils/request'
import { downloadFile } from '@/utils/request'
import { uploadImage, deleteImage } from '@/api/upload'
import { getImageUrl } from '@/utils/image'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Download, Search, Refresh, Edit, Delete, Switch, Check, Close,
  Monitor, Picture, CircleCheckFilled, CircleCheck, InfoFilled, Grid, Tools
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const role = computed(() => userStore.userInfo?.role)
const canManageDevice = computed(() => role.value === 'ADMIN' || role.value === 'LAB_ADMIN')
const canApproveDiscard = computed(() => role.value === 'ADMIN')
const isStudentOrTeacher = computed(() => role.value === 'STUDENT' || role.value === 'TEACHER')
const isTeacherRole = computed(() => role.value === 'TEACHER')
const isStudentRole = computed(() => role.value === 'STUDENT')

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const labList = ref([])
const deviceTableRef = ref(null)
const selectedDevices = ref([])
const cardSelected = ref([])

const route = useRoute()
const searchForm = reactive({ keyword: '', category: '', status: '' })

const handleDeviceSelectionChange = (rows) => { selectedDevices.value = rows }
const isRowSelected = (row) => selectedDevices.value.some(x => x.id === row.id)
const setRowSelected = (row, selected) => {
  deviceTableRef.value?.toggleRowSelection(row, selected)
  const idx = selectedDevices.value.findIndex(x => x.id === row.id)
  if (selected && idx === -1) selectedDevices.value.push(row)
  else if (!selected && idx !== -1) selectedDevices.value.splice(idx, 1)
}
const syncTableSelection = () => {
  nextTick(() => {
    if (!deviceTableRef.value) return
    tableData.value.forEach(row => deviceTableRef.value.toggleRowSelection(row, isRowSelected(row)))
  })
}

const isCardSelected = (item) => cardSelected.value.some(x => x.id === item.id)
const toggleCardSelect = (item) => {
  if (!batchMode.value || item.status !== 1) return
  const idx = cardSelected.value.findIndex(x => x.id === item.id)
  if (idx >= 0) cardSelected.value.splice(idx, 1)
  else cardSelected.value.push(item)
}
const batchMode = ref(false)
const toggleBatchMode = () => {
  batchMode.value = !batchMode.value
  if (!batchMode.value) cardSelected.value = []
}
const selectAllCards = () => {
  cardSelected.value = tableData.value.filter(d => d.status === 1)
}

const batchBorrowVisible = ref(false)
const batchForm = reactive({ purpose: '', borrowTime: '', expectReturnTime: '', remark: '' })
const submittingBatch = ref(false)

const openBatchBorrow = () => {
  if (cardSelected.value.length === 0) {
    ElMessage.warning('请先选择要借用的设备')
    return
  }
  batchForm.purpose = ''
  batchForm.borrowTime = ''
  batchForm.expectReturnTime = ''
  batchForm.remark = ''
  batchBorrowVisible.value = true
}

const submitBatchBorrow = async () => {
  if (!batchForm.purpose.trim()) return ElMessage.warning('请填写借用用途')
  if (!batchForm.expectReturnTime) return ElMessage.warning('请选择预计归还时间')
  submittingBatch.value = true
  try {
    const list = cardSelected.value.map(d => ({
      deviceId: d.id,
      borrowTime: batchForm.borrowTime || null,
      expectReturnTime: batchForm.expectReturnTime,
      purpose: batchForm.purpose,
      remark: batchForm.remark
    }))
    await batchApplyBorrow(list)
    ElMessage.success(`成功借用 ${list.length} 台设备`)
    cardSelected.value = []
    batchBorrowVisible.value = false
    fetchData()
  } catch (e) {} finally {
    submittingBatch.value = false
  }
}
const handleSelectAll = (checked) => {
  tableData.value.forEach(row => setRowSelected(row, checked))
}
const handleInvertSelect = () => {
  tableData.value.forEach(row => setRowSelected(row, !isRowSelected(row)))
}

const categoryColors = {
  '电子仪器': '#2563eb',
  '计算机': '#3b82f6',
  '物理': '#0ea5e9',
  '化学': '#3b82f6',
}
const getCategoryColor = (c) => categoryColors[c] || '#2563eb'

const statusOptions = [
  { value: 1, label: '空闲' },
  { value: 2, label: '借出' },
  { value: 3, label: '维修中' },
  { value: 4, label: '待审核' },
  { value: 5, label: '已报废' },
  { value: 6, label: '待审核' },
]

const statusCards = computed(() => [
  { value: 1, label: '空闲', color: '#2563eb' },
  { value: 2, label: '借出', color: '#3b82f6' },
  { value: 3, label: '维修中', color: '#0ea5e9' },
  { value: 4, label: '待审核', color: '#1e40af' },
  { value: 6, label: '待审核', color: '#1e40af' },
  { value: 5, label: '已报废', color: '#1d4ed8' },
])

const statusCounts = reactive({})

const fetchStatusCounts = async () => {
  try {
    const allRes = await getDeviceList({ page: 1, size: 9999 })
    const list = allRes.data?.records || []
    Object.keys(statusCounts).forEach(k => delete statusCounts[k])
    list.forEach(d => {
      statusCounts[d.status] = (statusCounts[d.status] || 0) + 1
    })
  } catch (e) {}
}

const quickFilter = (status) => {
  searchForm.status = searchForm.status === status ? '' : status
  fetchData()
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedDevices.value.length} 台设备?`, '批量删除', { type: 'warning' })
    for (const row of selectedDevices.value) await deleteDevice(row.id)
    ElMessage.success('批量删除成功')
    selectedDevices.value = []
    fetchData()
    fetchStatusCounts()
  } catch (e) {}
}

const dialogVisible = ref(false)
const dialogTitle = ref('设备入库')
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  imageUrl: '',
  name: '', code: '', category: '', model: '', specification: '',
  manufacturer: '', purchaseDate: '', price: null, labId: null, description: '',
})

const rules = {
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编号', trigger: 'blur' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }],
}

const statusDialogVisible = ref(false)
const currentRow = ref(null)
const targetStatus = ref(1)
const operatorId = ref(null)
const allUsers = ref([])
const labAdmins = ref([])
const expectReturnTime = ref(null)
const purpose = ref('')
const statusRemark = ref('')
const faultLevel = ref(1)
const faultDescription = ref('')
const repairDescription = ref('')

const disabledPastDate = (date) => {
  const now = new Date()
  return date.getTime() < new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
}

const loadUsers = async () => {
  try {
    const allRes = await request.get('/user/all')
    allUsers.value = allRes.data || []
  } catch (e) {}
  try {
    const labRes = await request.get('/user/all', { params: { role: 'LAB_ADMIN' } })
    labAdmins.value = labRes.data || []
  } catch (e) {}
}

const statusTagType = (status) => {
  const map = { 1: 'success', 2: 'primary', 3: 'warning', 4: 'info', 5: 'danger', 6: '' }
  return map[status] || 'info'
}
const statusLabel = (status) => {
  const map = { 1: '空闲', 2: '借出', 3: '维修中', 4: '待管理员审核', 5: '已报废', 6: '待管理员审核' }
  return map[status] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.category) params.category = searchForm.category
    if (searchForm.status) params.status = searchForm.status
    const res = await getDeviceList(params)
    tableData.value = res.data.records
    total.value = res.data.total
    syncTableSelection()
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const handleExport = () => {
  const params = new URLSearchParams()
  if (searchForm.keyword) params.append('keyword', searchForm.keyword)
  if (searchForm.category) params.append('category', searchForm.category)
  if (searchForm.status) params.append('status', searchForm.status)
  downloadFile('/export/device?' + params.toString(), '设备列表')
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.category = ''
  searchForm.status = ''
  fetchData()
}

const handleCardClick = (row) => {
  if (batchMode.value && row.status === 1) {
    toggleCardSelect(row)
  } else {
    detailDevice.value = row
    detailDialogVisible.value = true
  }
}

const detailDialogVisible = ref(false)
const detailDevice = ref(null)

const handleDetailBorrow = (device) => {
  borrowDevice.value = device
  detailDialogVisible.value = false
  borrowDialogVisible.value = true
}

const handleDetailRepair = (device) => {
  repairDevice.value = device
  detailDialogVisible.value = false
  repairDialogVisible.value = true
}

const formatPrice = (price) => {
  if (!price) return ''
  return parseFloat(price).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const onDetailImgError = (e) => {
  e.target.style.display = 'none'
}

const borrowDialogVisible = ref(false)
const borrowDevice = ref(null)
const borrowForm = reactive({ purpose: '', borrowTime: '', expectReturnTime: '', remark: '' })
const submittingBorrow = ref(false)

const submitBorrow = async () => {
  if (!borrowForm.purpose.trim()) return ElMessage.warning('请填写借用用途')
  if (!borrowForm.expectReturnTime) return ElMessage.warning('请选择预计归还时间')
  submittingBorrow.value = true
  try {
    const res = await request({
      url: '/borrow/apply',
      method: 'post',
      data: {
        deviceId: borrowDevice.value.id,
        purpose: borrowForm.purpose,
        borrowTime: borrowForm.borrowTime || null,
        expectReturnTime: borrowForm.expectReturnTime,
        remark: borrowForm.remark
      }
    })
    ElMessage.success('借用申请已提交')
    borrowDialogVisible.value = false
    fetchData()
  } catch (e) {} finally {
    submittingBorrow.value = false
  }
}

const repairDialogVisible = ref(false)
const repairDevice = ref(null)
const repairForm = reactive({ description: '', urgency: '一般' })
const submittingRepair = ref(false)

const handleQuickBorrow = (row) => {
  request({ url: '/borrow/apply', method: 'post', data: { deviceId: row.id, expectReturnTime: '' } }).catch(() => {})
  ElMessage.info('请前往"借用预约"页面填写详细借用信息')
}

const submitRepair = async () => {
  if (!repairForm.description.trim()) return ElMessage.warning('请填写故障描述')
  submittingRepair.value = true
  try {
    await reportFault({
      deviceId: repairDevice.value.id,
      faultLevel: repairForm.urgency === '一般' ? 1 : repairForm.urgency === '较急' ? 2 : 3,
      faultDescription: repairForm.description
    })
    ElMessage.success('维修申请已提交')
    repairDialogVisible.value = false
  } catch (e) {} finally {
    submittingRepair.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  editId.value = null
  dialogTitle.value = '设备入库'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑设备'
  Object.assign(form, row)
  dialogVisible.value = true
}

const resetForm = () => {
  if (formRef.value) formRef.value.resetFields()
  Object.assign(form, {
    name: '', code: '', category: '', model: '', specification: '',
    manufacturer: '', purchaseDate: '', price: null, labId: null, description: '', imageUrl: '',
  })
}

const beforeDeviceUpload = (file) => {
  if (!file.type.startsWith('image/')) { ElMessage.error('只能上传图片文件'); return false }
  if (file.size / 1024 / 1024 > 2) { ElMessage.error('图片大小不能超过2MB'); return false }
  return true
}

const getLabName = (labId) => {
  if (!labId) return ''
  const lab = labList.value.find(l => l.id === labId)
  return lab ? lab.name : ''
}

const removeImage = async () => { if (form.imageUrl) { await deleteImage(form.imageUrl).catch(() => {}) }; form.imageUrl = '' }

const handleDeviceUpload = async (options) => {
  try {
    const res = await uploadImage(options.file, 'device')
    form.imageUrl = res.data.url
    ElMessage.success('上传成功')
  } catch (e) { ElMessage.error('上传失败') }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateDevice(editId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await addDevice({ ...form })
      ElMessage.success('入库成功')
    }
    dialogVisible.value = false
    fetchData()
    fetchStatusCounts()
  } catch (e) { console.error(e) }
  finally { submitting.value = false }
}

const handleStatusChange = async (row) => {
  currentRow.value = row
  targetStatus.value = row.status
  operatorId.value = null
  expectReturnTime.value = null
  purpose.value = ''
  statusRemark.value = ''
  faultLevel.value = 1
  faultDescription.value = ''
  repairDescription.value = ''
  await loadUsers()
  statusDialogVisible.value = true
}

const confirmStatusChange = async () => {
  try {
    if (targetStatus.value === 2 && !operatorId.value) { ElMessage.warning('请选择借用人'); return }
    if (targetStatus.value === 2 && !expectReturnTime.value) { ElMessage.warning('请选择预计归还时间'); return }
    if (targetStatus.value === 3 && !operatorId.value) { ElMessage.warning('请选择维修人'); return }
    if (targetStatus.value === 3 && (!faultDescription.value || !faultDescription.value.trim())) { ElMessage.warning('请填写故障描述'); return }

    const data = { status: targetStatus.value }
    if (operatorId.value) {
      data.operatorId = operatorId.value
      if (targetStatus.value === 2) {
        data.operatorName = allUsers.value.find(u => u.id === operatorId.value)?.username
      } else if (targetStatus.value === 3) {
        data.operatorName = labAdmins.value.find(u => u.id === operatorId.value)?.username
      }
    }
    if (targetStatus.value === 2) {
      data.expectReturnTime = expectReturnTime.value
      data.purpose = purpose.value
      data.remark = statusRemark.value
    }
    if (targetStatus.value === 3) {
      data.faultLevel = faultLevel.value
      data.faultDescription = faultDescription.value
      data.repairDescription = repairDescription.value
      data.remark = statusRemark.value
    }

    await updateDeviceStatus(currentRow.value.id, data)
    ElMessage.success('状态变更成功')
    statusDialogVisible.value = false
    fetchData()
    fetchStatusCounts()
  } catch (e) { console.error(e) }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该设备吗？', '提示', { type: 'warning' })
    .then(async () => { await deleteDevice(row.id); ElMessage.success('删除成功'); fetchData(); fetchStatusCounts() })
    .catch(() => {})
}

const handleDiscard = (row) => {
  ElMessageBox.prompt('请输入报废原因', '报废申请', { type: 'warning', inputType: 'textarea' })
    .then(async ({ value }) => {
      await submitDiscard(row.id, value || '')
      ElMessage.success('报废申请已提交')
      fetchData()
      fetchStatusCounts()
    }).catch(() => {})
}

const handleDiscardApprove = async (row) => {
  ElMessageBox.confirm('管理员审核通过后，设备将正式报废，无法恢复。确定通过？', '确认', { type: 'warning' })
    .then(async () => { await approveDiscard(row.id); ElMessage.success('审核通过'); fetchData(); fetchStatusCounts() })
    .catch(() => {})
}

const handleDiscardFinalApprove = async (row) => {
  ElMessageBox.confirm('管理员终审通过后，设备将正式报废，无法恢复。确定终审通过？', '确认', { type: 'warning' })
    .then(async () => { await approveFinalDiscard(row.id); ElMessage.success('终审通过，设备已报废'); fetchData(); fetchStatusCounts() })
    .catch(() => {})
}

const handleDiscardReject = async (row) => {
  ElMessageBox.confirm('确定驳回该报废申请吗？设备将恢复为"空闲"状态', '确认', { type: 'warning' })
    .then(async () => { await rejectDiscard(row.id); ElMessage.success('已驳回'); fetchData(); fetchStatusCounts() })
    .catch(() => {})
}

watch(searchForm, () => {
  page.value = 1
}, { deep: true })

onMounted(async () => {
  if (!canManageDevice.value) size.value = 8
  if (route.query.keyword) searchForm.keyword = String(route.query.keyword)
  fetchData()
  fetchStatusCounts()
  try {
    const res = await getAllLabs()
    labList.value = res.data
  } catch (e) {}
})
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; gap: 16px; }

.page-hero {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
  border-radius: 20px; color: #fff;
  box-shadow: 0 10px 32px rgba(37, 99, 235, 0.3);
}
.hero-left { display: flex; align-items: center; gap: 16px; }
.hero-icon {
  width: 52px; height: 52px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
}
.hero-title { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: 0.5px; }
.hero-sub { margin: 4px 0 0; font-size: 13px; opacity: 0.85; }
.hero-actions { display: flex; gap: 10px; align-items: center; }
.batch-btn {
  background: rgba(255,255,255,0.2); border: 1px solid rgba(255,255,255,0.35);
  border-radius: 10px; padding: 8px 18px; font-size: 14px; color: #fff; font-weight: 600;
  cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 8px;
  font-family: inherit;
}
.batch-btn:hover { background: rgba(255,255,255,0.3); }
.batch-btn.active { background: #fff; color: #1d4ed8; border-color: #fff; }
.batch-btn.active:hover { background: #f0f5ff; }

.status-stats {
  display: flex; gap: 12px; flex-wrap: wrap;
  background: #fff; border-radius: 14px; padding: 16px; border: 1px solid var(--border-light);
}
.status-card {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 16px; border-radius: 10px;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.25s;
  min-width: 110px;
}
.status-card:hover { background: #f1f5f9; transform: translateY(-2px); }
.status-dot {
  width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0;
  box-shadow: 0 0 0 3px currentColor;
}
.status-info { display: flex; flex-direction: column; line-height: 1.2; }
.status-num { font-size: 18px; font-weight: 700; color: #0f172a; }
.status-name { font-size: 11px; color: #64748b; }

.toolbar-card {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 18px;
  background: #fff; border-radius: 14px; border: 1px solid var(--border-light);
}
.search-fields { display: flex; align-items: center; gap: 12px; }
.batch-bar {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 14px;
  background: linear-gradient(135deg, #fef2f2, #fee2e2);
  border-radius: 10px;
}
.batch-count { color: #1d4ed8; font-size: 13px; font-weight: 500; }
.batch-count b { font-weight: 700; }

.table-card { border-radius: 14px; border: 1px solid var(--border-light); }
.table-card :deep(.el-card__body) { padding: 0; }

.seq {
  display: inline-block; width: 22px; height: 22px; line-height: 22px;
  border-radius: 50%; background: #f0f4ff; color: #2563eb;
  font-size: 12px; font-weight: 600;
}

.device-cell { display: flex; align-items: center; gap: 10px; }
.device-avatar {
  width: 36px; height: 36px; border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff; font-weight: 700; font-size: 14px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.device-info { min-width: 0; }
.device-name { font-weight: 500; color: #0f172a; }
.device-code { font-size: 12px; color: #94a3b8; font-family: Consolas, monospace; }

.status-tag { font-size: 11px; padding: 0 10px; height: 22px; line-height: 20px; white-space: nowrap; }

.no-image {
  width: 104px; height: 104px;
  background: #f1f5f9; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: #cbd5e1;
}

.pager-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 4px;
}
.pager-info { font-size: 13px; color: #64748b; }
.pager-info b { color: #0f172a; font-weight: 600; }

.modern-dialog :deep(.el-dialog) { border-radius: 20px; overflow: hidden; }
.modern-dialog :deep(.el-dialog__header) {
  margin: 0; padding: 20px 24px;
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
  color: #fff;
}
.modern-dialog :deep(.el-dialog__body) { padding: 24px 24px 8px; }
.modern-dialog :deep(.el-dialog__footer) { padding: 16px 24px; border-top: 1px solid #f1f5f9; }

.upload-area { display: flex; gap: 12px; }
.upload-placeholder {
  width: 140px; height: 140px;
  border: 2px dashed #cbd5e1; border-radius: 12px;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px;
  color: #94a3b8; cursor: pointer; transition: all 0.2s;
}
.upload-placeholder:hover { border-color: #2563eb; color: #2563eb; background: #f0f4ff; }
.upload-placeholder .upload-hint { font-size: 11px; }

.preview-box {
  width: 140px; height: 140px; border-radius: 12px; overflow: hidden; position: relative;
}
.preview-actions {
  position: absolute; bottom: 8px; right: 8px; opacity: 0; transition: opacity 0.2s;
}
.preview-box:hover .preview-actions { opacity: 1; }

.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; }

/* ===== Device Card Grid (teacher/student) ===== */
.grid-skeleton { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; padding: 20px 0; }
.sk-item { background: #fff; border-radius: 14px; padding: 16px; min-height: 220px; }
.sk-img { height: 120px; border-radius: 10px; background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; margin-bottom: 12px; }
.sk-text { height: 11px; border-radius: 5px; background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; margin-bottom: 8px; }
.sk-text.short { width: 45%; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

.empty-hint { text-align: center; padding: 80px 0; color: #94a3b8; }
.empty-hint p { margin-top: 12px; font-size: 14px; }

.device-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 20px 0;
}
@media (max-width: 1400px) { .device-grid, .grid-skeleton { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 1000px) { .device-grid, .grid-skeleton { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px)  { .device-grid, .grid-skeleton { grid-template-columns: repeat(1, 1fr); } }

.dev-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.25s;
  display: flex;
  flex-direction: column;
  position: relative;
}
.dev-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.18);
  border-color: #2563eb;
}
.dev-card.selected {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.25), 0 12px 28px rgba(37, 99, 235, 0.18);
}
.dev-card.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.dev-card.disabled:hover { transform: none; box-shadow: none; border-color: #e2e8f0; }

.dc-check {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 3;
}
.dc-image {
  height: 150px;
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}
.dc-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s;
}
.dev-card:hover .dc-image img { transform: scale(1.05); }
.dc-image .el-icon { color: rgba(255,255,255,0.5); }

.dc-body { padding: 14px 16px; flex: 1; display: flex; flex-direction: column; gap: 8px; }
.dc-header { display: flex; align-items: center; justify-content: space-between; }
.dc-name { font-size: 15px; font-weight: 600; color: #0f172a; margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; flex: 1; }
.dc-meta { display: flex; gap: 16px; }
.dm-item { font-size: 12px; color: #475569; display: flex; gap: 4px; }
.dm-lbl { color: #94a3b8; flex-shrink: 0; }
.dc-desc {
  font-size: 12px; color: #64748b; line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  margin-top: 4px;
}

.batch-bar { background: linear-gradient(135deg, #eff6ff, #dbeafe); }

.batch-dlg { max-height: 60vh; overflow-y: auto; }
.batch-header { display: flex; align-items: center; gap: 8px; padding: 8px 14px; background: #f0f9ff; border-radius: 8px; font-size: 14px; color: #1e40af; margin-bottom: 12px; flex-wrap: wrap; }
.batch-limit-tag { font-size: 12px; color: #d97706; background: #fef3c7; padding: 2px 8px; border-radius: 6px; margin-left: 8px; }
.batch-device-list { display: flex; flex-direction: column; gap: 8px; max-height: 240px; overflow-y: auto; }
.batch-device-item { display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; background: #f8fafc; border-radius: 10px; border: 1px solid #f1f5f9; }
.bdi-left { display: flex; align-items: center; gap: 12px; }
.bdi-thumb { width: 40px; height: 40px; border-radius: 8px; background: #e2e8f0; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.bdi-thumb img { width: 100%; height: 100%; object-fit: cover; }
.bdi-name { font-size: 14px; font-weight: 600; color: #1e293b; margin: 0 0 2px; }
.bdi-meta { font-size: 12px; color: #94a3b8; }
.bdi-status { flex-shrink: 0; }

.dlg-head { display: flex; align-items: center; gap: 10px; font-size: 16px; font-weight: 600; }
.dlg-ico {
  width: 30px; height: 30px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; color: #fff;
}
.bd-wrap { padding: 4px 0; }
.bd-device {
  display: flex; gap: 14px; padding: 14px; background: #f8fafc;
  border-radius: 12px; margin-bottom: 18px;
}
.bd-thumb {
  width: 72px; height: 72px; border-radius: 10px; background: #fff;
  display: flex; align-items: center; justify-content: center; color: #94a3b8;
  overflow: hidden; flex-shrink: 0;
}
.bd-thumb img { width: 100%; height: 100%; object-fit: cover; }
.bd-dinfo h4 { margin: 0 0 6px; font-size: 15px; color: #0f172a; }
.bd-dmeta { font-size: 12px; color: #64748b; margin-bottom: 6px; }

.detail-wrap { padding: 4px 0; }
.detail-hero {
  display: flex; gap: 20px; padding-bottom: 18px;
  border-bottom: 1px solid #eef2f7; margin-bottom: 18px;
}
.detail-img {
  width: 140px; height: 140px; border-radius: 12px; background: #f8fafc;
  overflow: hidden; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; color: #94a3b8;
}
.detail-img img { width: 100%; height: 100%; object-fit: cover; }
.no-img { display: flex; flex-direction: column; align-items: center; gap: 6px; font-size: 11px; }
.detail-hinfo { flex: 1; min-width: 0; }
.detail-hinfo h3 { margin: 0 0 10px; font-size: 18px; color: #0f172a; }
.detail-tags { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.dt {
  font-size: 12px; padding: 3px 10px; background: #eef2ff;
  color: #2563eb; border-radius: 6px; font-weight: 500;
}
.dt.teal { background: #cffafe; color: #0891b2; }
.dt.price { background: #fef3c7; color: #1e40af; font-weight: 600; }
.detail-status {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 12px; border-radius: 100px; font-size: 12px; font-weight: 600;
}
.detail-status .ds-dot { width: 6px; height: 6px; border-radius: 50%; }
.ds-1 { background: #dcfce7; color: #16a34a; } .ds-1 .ds-dot { background: #16a34a; }
.ds-2 { background: #dbeafe; color: #2563eb; } .ds-2 .ds-dot { background: #2563eb; }
.ds-3 { background: #fef3c7; color: #1e40af; } .ds-3 .ds-dot { background: #1e40af; }
.ds-4 { background: #eedeff; color: #2563eb; } .ds-4 .ds-dot { background: #2563eb; }
.ds-5, .ds-6 { background: #fee2e2; color: #1d4ed8; } .ds-5 .ds-dot, .ds-6 .ds-dot { background: #1d4ed8; }
.detail-fields { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.df { display: flex; flex-direction: column; gap: 4px; }
.df.full { grid-column: span 2; }
.df-lbl { font-size: 11px; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; }
.df-val { font-size: 14px; color: #0f172a; font-weight: 500; }

</style>