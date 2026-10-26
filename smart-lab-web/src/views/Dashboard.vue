<template>
  <!-- ==================== HERO BANNER (全宽) ==================== -->
  <section class="hero" :style="{ '--bg-from': bgFrom, '--bg-to': bgTo }">
    <div class="hero-deco">
      <div class="deco-circle c1"></div>
      <div class="deco-circle c2"></div>
      <div class="deco-circle c3"></div>
      <div class="deco-grid"></div>
    </div>
    <div class="hero-inner">
      <div class="hero-left">
        <div class="brand-chip">
          <span class="chip-logo"><el-icon :size="12"><Cpu /></el-icon></span>
          智慧校园设备管理系统 · 智能实验室管理平台        </div>
        <h1 class="hero-title">
          {{ sloganMain }} <span class="hero-highlight">{{ sloganSub }}</span>
        </h1>
        <div class="hero-meta">
          <div class="hero-user">
            <div class="hu-avatar" :style="{ background: roleGradient }">
              <el-icon :size="15"><component :is="roleIcon" /></el-icon>
            </div>
            <span class="hu-greet">{{ greeting }}，<b>{{ userStore.userInfo ? userStore.userInfo.username : '游客' }}</b></span>
            <span class="hu-role"><span class="dot"></span>{{ roleLabel }}</span>
          </div>
          <div class="hero-actions">
            <template v-if="!isLoggedIn">
              <button class="btn-main" @click="$router.push('/login')">
                <el-icon :size="14"><User /></el-icon>登录
              </button>
              <button class="btn-outline" @click="$router.push('/register')">
                <el-icon :size="14"><UserFilled /></el-icon>注册账号
              </button>
            </template>
            <template v-else>
              <button class="btn-main" @click="scrollToDevices" v-if="isStudentOrTeacher">
                <el-icon :size="14"><Search /></el-icon>浏览设备
              </button>
              <button class="btn-main" @click="$router.push('/borrow')" v-else>
                <el-icon :size="14"><Tickets /></el-icon>审批管理
              </button>
              <button class="btn-outline" @click="$router.push('/borrow')">
                <el-icon :size="14"><Document /></el-icon>
                {{ isStudentOrTeacher ? '我的借用' : '更多操作' }}
              </button>
            </template>
          </div>
        </div>
      </div>
      <div class="hero-right">
        <div class="hero-metrics">
          <div v-for="m in heroMetrics" :key="m.label" class="hm">
            <div class="hm-val">{{ m.value }}</div>
            <div class="hm-lbl">{{ m.label }}</div>
          </div>
        </div>
        <div class="hero-scene" aria-hidden="true">
          <div class="sc-console">
            <div class="sc-circle" :title="topDevice ? '点击查看 ' + topDevice.name : ''" @click="goDeviceDetail">
              <img v-if="topDevice && topDevice.imageUrl" :src="getImageUrl(topDevice.imageUrl)" :alt="topDevice.name || ''" @error="e => e.target.style.display = 'none'" />
              <el-icon v-else :size="32"><Monitor /></el-icon>
              <span class="sc-name" v-if="topDevice">{{ topDevice.name }}</span>
            </div>
            <span class="sc-badge" v-if="topDevice">{{ topDevice.count }} 次</span>
          </div>
          <div class="sc-float sc-f1"><el-icon :size="16"><Monitor /></el-icon><b>{{ heroOnline }}</b><span>在线</span></div>
          <div class="sc-float sc-f2"><el-icon :size="16"><Tickets /></el-icon><b>{{ heroBorrow }}</b><span>借用</span></div>
        </div>
      </div>
    </div>
  </section>

  <div class="home">
    <!-- ==================== 学生/教师首页内容 ==================== -->
    <template v-if="isStudentOrTeacher">

      <!-- 最新设备 -->
      <section ref="deviceSection" class="block">
        <div class="block-head">
          <h2 class="block-title"><span class="bt-bar"></span>最新设备</h2>
          <div class="block-head-right">
            <div class="search-bar">
              <el-icon :size="14"><Search /></el-icon>
              <input v-model="searchKeyword" placeholder="搜索设备..." @keyup.enter="loadDevices" />
              <button v-if="searchKeyword" class="sb-clear" @click="searchKeyword=''; loadDevices()">
                <el-icon :size="12"><Close /></el-icon>
              </button>
            </div>
            <button
              class="block-more batch-toggle"
              :class="{ active: batchMode }"
              @click="toggleBatchMode"
              title="批量借用模式"
            >
              <el-icon :size="14"><Grid /></el-icon>
              {{ batchMode ? '退出批量' : '批量借用' }}
            </button>
            <button class="block-more" @click="$router.push('/device')">查看全部 ></button>
          </div>
        </div>

        <div class="filter-row">
          <button
            v-for="tab in allCategories"
            :key="tab.key"
            class="ft"
            :class="{ active: selectedCategory === tab.key }"
            @click="selectedCategory = tab.key; loadDevices()"
          >{{ tab.label }}</button>
        </div>

        <div v-if="batchMode && cardSelected.length > 0" class="batch-bar">
          <span class="batch-count">已选 <b>{{ cardSelected.length }}</b> 台设备</span>
          <el-button size="small" @click="selectAllCards">全选本页</el-button>
          <el-button size="small" @click="cardSelected = []">取消选择</el-button>
          <el-button size="small" type="primary" @click="openBatchBorrow">提交批量借用</el-button>
        </div>

        <div v-if="loadingDevices" class="grid-skeleton">
          <div v-for="i in 8" :key="i" class="sk-item">
            <div class="sk-img"></div>
            <div class="sk-text" style="width:70%"></div>
            <div class="sk-text short"></div>
          </div>
        </div>
        <div v-else-if="deviceList.length === 0" class="empty-hint">
          <el-icon :size="40"><Monitor /></el-icon>
          <p>没有找到匹配的设备</p>
        </div>
        <div v-else class="device-grid">
          <div
            v-for="device in deviceList"
            :key="device.id"
            class="dev-card"
            :class="{ selected: batchMode && isCardSelected(device) }"
            @click="handleCardClick(device)"
          >
            <div v-if="batchMode && device.status === 1" class="dc-check" @click.stop="toggleCardSelect(device)">
              <el-icon v-if="isCardSelected(device)" :size="20" color="#2563eb"><CircleCheckFilled /></el-icon>
              <el-icon v-else :size="20" color="#cbd5e1"><CircleCheck /></el-icon>
            </div>
            <div class="dev-media">
              <img
                v-if="device.imageUrl"
                :src="getImageUrl(device.imageUrl)"
                :alt="device.name"
                loading="lazy"
                @error="handleImgError"
              />
              <div v-else class="dev-ph">
                <el-icon :size="42"><Monitor /></el-icon>
              </div>
              <span class="dev-badge" :class="'dbs-' + device.status">
                {{ getStatusLabel(device.status) }}
              </span>
            </div>
            <div class="dev-info">
              <h3 class="dev-name" :title="device.name">{{ device.name }}</h3>
              <div class="dev-meta">
                <span class="dm-cat">{{ device.category || '未分类' }}</span>
                <span v-if="device.labName" class="dm-lab">@{{ device.labName }}</span>
              </div>
            </div>
            <div class="dev-foot">
              <span class="dev-code">{{ device.code }}</span>
              <div class="dev-btns">
                <button
                  class="dev-btn repair"
                  @click.stop="openRepairDialog(device)"
                  title="申请维修"
                >
                  <el-icon :size="14"><Tools /></el-icon>
                </button>
                <button
                  class="dev-btn"
                  :class="{ disabled: device.status !== 1 }"
                  :disabled="device.status !== 1"
                  @click.stop="device.status === 1 && openBorrowDialog(device)"
                >
                  {{ device.status === 1 ? '借用' : '不可借' }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="pager" v-if="totalDevices > devicePageSize">
          <el-pagination
            v-model:current-page="devicePage"
            v-model:page-size="devicePageSize"
            :total="totalDevices"
            :page-sizes="[8]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @size-change="loadDevices"
            @current-change="loadDevices"
          />
        </div>
      </section>

    </template>

    <!-- ==================== 管理员首页内容 ==================== -->
    <template v-else>
      <!-- 统计卡片 -->
      <section class="block">
        <div class="stat-grid">
          <div
            v-for="(card, idx) in statCards"
            :key="card.title"
            class="stat-card"
            :style="{ '--i': idx }"
          >
            <div class="sc-icon" :style="{ background: card.gradient }">
              <el-icon :size="20"><component :is="card.icon" /></el-icon>
            </div>
            <div class="sc-info">
              <div class="sc-val">{{ card.value }}</div>
              <div class="sc-lbl">{{ card.title }}</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 待办 & 图表 -->
      <section class="block">
        <div class="panel-grid">
          <div class="panel">
            <div class="panel-head">
              <h3>待办事项</h3>
              <span class="panel-hint">需要您处理</span>
            </div>
            <div class="todo-list">
              <div class="todo" v-if="overviewStats.pendingBorrow > 0" @click="$router.push('/borrow')">
                <span class="td-bar borrow"></span>
                <div class="td-body">
                  <div class="td-title">待审批借用</div>
                  <div class="td-desc">有 {{ overviewStats.pendingBorrow }} 条申请等待审批</div>
                </div>
                <span class="td-count borrow">{{ overviewStats.pendingBorrow }}</span>
              </div>
              <div class="todo" v-if="overviewStats.pendingRepair > 0" @click="$router.push('/repair')">
                <span class="td-bar repair"></span>
                <div class="td-body">
                  <div class="td-title">待处理维修</div>
                  <div class="td-desc">有 {{ overviewStats.pendingRepair }} 个故障报修待处理</div>
                </div>
                <span class="td-count repair">{{ overviewStats.pendingRepair }}</span>
              </div>
              <div class="todo" v-if="overviewStats.overdueCount > 0" @click="$router.push('/borrow')">
                <span class="td-bar overdue"></span>
                <div class="td-body">
                  <div class="td-title">逾期未还</div>
                  <div class="td-desc">有 {{ overviewStats.overdueCount }} 台设备超期未归还</div>
                </div>
                <span class="td-count overdue">{{ overviewStats.overdueCount }}</span>
              </div>
              <div class="todo done" v-if="hasPendingItems">
                <el-icon :size="24" color="#2563eb"><CircleCheckFilled /></el-icon>
                <div class="td-body">
                  <div class="td-title">暂无待办</div>
                  <div class="td-desc">所有事项都已处理完毕 🎉</div>
                </div>
              </div>
            </div>
          </div>

          <div class="panel">
            <div class="panel-head">
              <h3>设备状态分布</h3>
            </div>
            <div ref="deviceChartRef" class="chart-area"></div>
          </div>

          <div class="panel">
            <div class="panel-head">
              <h3>热门设备排行</h3>
            </div>
            <div ref="borrowChartRef" class="chart-area"></div>
          </div>
        </div>
      </section>

      <!-- 快捷操作 -->
      <section class="block">
        <div class="block-head">
          <h2 class="block-title"><span class="bt-bar"></span>快捷操作</h2>
        </div>
        <div class="quick-grid">
          <div class="quick-item" @click="$router.push('/borrow')">
            <div class="qi-icon" style="background:linear-gradient(135deg,#2563eb,#1d4ed8)"><el-icon :size="22"><Tickets /></el-icon></div>
            <span>借用审批</span>
          </div>
          <div class="quick-item" @click="$router.push('/borrow')">
            <div class="qi-icon" style="background:linear-gradient(135deg,#2563eb,#1d4ed8)"><el-icon :size="22"><RefreshRight /></el-icon></div>
            <span>设备归还</span>
          </div>
          <div class="quick-item" @click="$router.push('/repair')">
            <div class="qi-icon" style="background:linear-gradient(135deg,#0ea5e9,#1e40af)"><el-icon :size="22"><Tools /></el-icon></div>
            <span>故障报修</span>
          </div>
          <div class="quick-item" v-if="canManageLab" @click="$router.push('/lab')">
            <div class="qi-icon" style="background:linear-gradient(135deg,#3b82f6,#0ea5e9)"><el-icon :size="22"><OfficeBuilding /></el-icon></div>
            <span>实验室</span>
          </div>
          <div class="quick-item" v-if="canManageDevice" @click="$router.push('/device')">
            <div class="qi-icon" style="background:linear-gradient(135deg,#1e40af,#1e3a8a)"><el-icon :size="22"><Monitor /></el-icon></div>
            <span>全部设备</span>
          </div>
          <div class="quick-item" v-if="canViewStats" @click="$router.push('/statistics')">
            <div class="qi-icon" style="background:linear-gradient(135deg,#3b82f6,#2563eb)"><el-icon :size="22"><DataAnalysis /></el-icon></div>
            <span>统计分析</span>
          </div>
        </div>
      </section>
    </template>

    <!-- ==================== 借用对话框 ==================== -->
    <el-dialog v-model="borrowDialogVisible" width="500px" :close-on-click-modal="false">
      <template #header>
        <div class="dlg-head">
          <span class="dlg-ico" :style="{ background: roleGradient }"><el-icon :size="16"><Tickets /></el-icon></span>
          申请借用设备
        </div>
      </template>
      <div class="bd-wrap" v-if="borrowDevice">
        <div class="bd-device">
          <div class="bd-thumb">
            <img v-if="borrowDevice.imageUrl" :src="getImageUrl(borrowDevice.imageUrl)" @error="handleImgError" />
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
        <el-form :model="borrowForm" label-position="top">
          <el-form-item label="借用开始时间">
            <el-date-picker v-model="borrowForm.borrowTime" type="datetime" :disabled-date="disablePast" placeholder="不选默认立即借用" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
          </el-form-item>
          <el-form-item label="借用用途" required>
            <el-input v-model="borrowForm.purpose" type="textarea" :rows="3" placeholder="请说明借用用途..." maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="预计归还时间" required>
            <el-date-picker v-model="borrowForm.expectReturnTime" type="datetime" :disabled-date="disablePast" placeholder="选择归还时间" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
          </el-form-item>
          <el-form-item label="备注说明（可选）">
            <el-input v-model="borrowForm.remark" placeholder="补充信息..." maxlength="100" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="borrowDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingBorrow" @click="submitBorrow" :style="{ background: roleGradient }">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 批量借用对话框 ==================== -->
    <el-dialog v-model="batchBorrowVisible" title="批量借用设备" width="680px" :close-on-click-modal="false">
      <div class="batch-dlg">
        <div class="batch-header">
          <el-icon :size="16"><InfoFilled /></el-icon>
          <span>已选择 <b>{{ cardSelected.length }}</b> 台设备</span>
          <template v-if="isStudent">
            <span class="batch-limit-tag">学生单次最多借3台，每天最多借3台</span>
          </template>
          <template v-if="isTeacher">
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
                  <span v-if="device.labName"> · {{ device.labName }}</span>
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
                <el-date-picker v-model="batchForm.borrowTime" type="datetime" placeholder="默认当前时间" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" :disabled-date="disablePast" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="预计归还时间" required>
                <el-date-picker v-model="batchForm.expectReturnTime" type="datetime" placeholder="选择归还时间" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" :disabled-date="disablePast" />
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
        <el-button type="primary" :loading="submittingBatch" @click="submitBatchBorrow" :style="{ background: roleGradient }">提交批量借用</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 设备详情对话框 ==================== -->
    <el-dialog v-model="detailDialogVisible" width="580px">
      <template #header>
        <div class="dlg-head">
          <span class="dlg-ico" :style="{ background: roleGradient }"><el-icon :size="16"><Monitor /></el-icon></span>
          设备详情
        </div>
      </template>
      <div v-if="detailDevice" class="detail-wrap">
        <div class="detail-hero">
          <div class="detail-img">
            <img v-if="detailDevice.imageUrl" :src="getImageUrl(detailDevice.imageUrl)" @error="handleImgError" />
            <div v-else class="no-img"><el-icon :size="40"><Monitor /></el-icon><span>暂无照片</span></div>
          </div>
          <div class="detail-hinfo">
            <h3>{{ detailDevice.name }}</h3>
            <div class="detail-tags">
              <span class="dt" v-if="detailDevice.category">{{ detailDevice.category }}</span>
              <span class="dt teal" v-if="detailDevice.labName">{{ detailDevice.labName }}</span>
              <span class="dt price" v-if="detailDevice.price">¥{{ formatPrice(detailDevice.price) }}</span>
            </div>
            <div class="detail-status" :class="'ds-' + detailDevice.status">
              <span class="ds-dot"></span>{{ getStatusLabel(detailDevice.status) }}
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
          @click="borrowDevice = detailDevice; detailDialogVisible = false; borrowDialogVisible = true"
          :style="{ background: roleGradient }"
        >申请借用</el-button>
        <el-button type="warning" @click="repairDevice = detailDevice; detailDialogVisible = false; repairDialogVisible = true">
          <el-icon><Tools /></el-icon>申请维修
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 维修申请对话框 ==================== -->
    <el-dialog v-model="repairDialogVisible" width="500px" :close-on-click-modal="false">
      <template #header>
        <div class="dlg-head">
          <span class="dlg-ico" style="background:linear-gradient(135deg,#0ea5e9,#1e40af)"><el-icon :size="16"><Tools /></el-icon></span>
          申请设备维修
        </div>
      </template>
      <div class="bd-wrap" v-if="repairDevice">
        <div class="bd-device">
          <div class="bd-thumb">
            <img v-if="repairDevice.imageUrl" :src="getImageUrl(repairDevice.imageUrl)" @error="handleImgError" />
            <el-icon v-else :size="28"><Monitor /></el-icon>
          </div>
          <div class="bd-dinfo">
            <h4>{{ repairDevice.name }}</h4>
            <div class="bd-dmeta">
              <span>{{ repairDevice.code }}</span>
              <span v-if="repairDevice.model"> · {{ repairDevice.model }}</span>
            </div>
            <el-tag :type="repairDevice.status === 3 ? 'warning' : 'info'" effect="light" round size="small">
              {{ getStatusLabel(repairDevice.status) }}
            </el-tag>
          </div>
        </div>
        <el-form :model="repairForm" label-position="top">
          <el-form-item label="故障类型" required>
            <el-select v-model="repairForm.type" placeholder="选择故障类型" style="width:100%">
              <el-option label="硬件故障" value="HARDWARE" />
              <el-option label="软件故障" value="SOFTWARE" />
              <el-option label="老化磨损" value="AGING" />
              <el-option label="损坏丢失" value="DAMAGE" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item label="故障描述" required>
            <el-input v-model="repairForm.description" type="textarea" :rows="3" placeholder="请详细描述故障现象..." maxlength="300" show-word-limit />
          </el-form-item>
          <el-form-item label="紧急程度">
            <el-radio-group v-model="repairForm.urgency">
              <el-radio value="LOW">一般</el-radio>
              <el-radio value="MEDIUM">较急</el-radio>
              <el-radio value="HIGH">紧急</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="位置（可选）">
            <el-input v-model="repairForm.location" placeholder="设备所在位置" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="repairDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingRepair" @click="submitRepair" style="background:linear-gradient(135deg,#0ea5e9,#1e40af)">
          提交维修申请
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 代学生申请对话框 ==================== -->
    <el-dialog v-model="subStudentDialogVisible" width="560px" :close-on-click-modal="false" class="teacher-sub-dialog">
      <template #header>
        <div class="tsd-header">
          <div class="tsd-icon">
            <el-icon :size="24"><UserFilled /></el-icon>
          </div>
          <div>
            <h3 style="margin:0 0 4px">👤 代学生申请设备</h3>
            <p style="margin:0;font-size:13px;color:#909399">教师可代实验课学生快速申请设备，审批通过后设备直接交给学生</p>
          </div>
        </div>
      </template>
      <el-form label-position="top" :model="subStudentForm">
        <el-form-item label="选择学生 *">
          <el-select v-model="subStudentForm.studentId" filterable placeholder="搜索并选择学生" style="width:100%">
            <el-option v-for="s in studentList" :key="s.id" :label="s.username + (s.realName ? ' (' + s.realName + ')' : '')" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择设备 *">
          <el-select v-model="subStudentForm.deviceId" filterable placeholder="搜索并选择可用设备" style="width:100%" @change="onSubDeviceChange">
            <el-option v-for="d in availableSubDevices" :key="d.id" :label="d.name + ' (' + d.code + ')'" :value="d.id">
              <span>{{ d.name }}</span>
              <span style="float:right;color:#909399;font-size:12px">{{ d.labName || '' }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="借用时段">
          <div style="display:flex;gap:12px;width:100%">
            <el-date-picker v-model="subStudentForm.borrowTime" type="datetime" :disabled-date="disablePast" placeholder="开始时间" value-format="YYYY-MM-DD HH:mm:ss" style="flex:1" />
            <el-date-picker v-model="subStudentForm.expectReturnTime" type="datetime" :disabled-date="disablePast" placeholder="预计归还" value-format="YYYY-MM-DD HH:mm:ss" style="flex:1" />
          </div>
        </el-form-item>
        <el-form-item label="用途 *">
          <el-input v-model="subStudentForm.purpose" type="textarea" :rows="2" placeholder="说明用途，如：XX实验课、课程编号XXX" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subStudentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="subStudentSubmitting" @click="submitSubStudent" style="background:linear-gradient(135deg,#2563eb,#1d4ed8)">
          确认代提交
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getDashboardStats, getDeviceStatusStats, getBorrowStats } from '@/api/statistics'
import { getDeviceList } from '@/api/device'
import { getBorrowList, applyBorrow, returnDevice, cancelBorrow, batchApplyBorrow } from '@/api/borrow'
import { reportFault } from '@/api/repair'
import { getAllLabs } from '@/api/lab'
import { getAllUsers } from '@/api/user'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User as UserIcon, OfficeBuilding as OfficeBuildingIcon, Monitor as MonitorIcon,
  Tickets as TicketsIcon, Tools as ToolsIcon, PieChart, DataAnalysis, TrendCharts,
  CircleCheckFilled, RefreshRight, Search, Cpu, Document,
  Clock, WarningFilled, InfoFilled, Grid, Close, CircleCheck,
  DataLine, ArrowRight, UserFilled,
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const isLoggedIn = computed(() => !!userStore.token)
const role = computed(() => userStore.userInfo?.role || 'STUDENT')
const isAdmin = computed(() => role.value === 'ADMIN')
const isLabAdmin = computed(() => role.value === 'LAB_ADMIN')
const isTeacher = computed(() => role.value === 'TEACHER')
const isStudent = computed(() => role.value === 'STUDENT')
const isStudentOrTeacher = computed(() => role.value === 'STUDENT' || role.value === 'TEACHER')

const canManageLab = computed(() => isAdmin.value || isLabAdmin.value)
const canManageDevice = computed(() => isAdmin.value || isLabAdmin.value)
const canViewStats = computed(() => isTeacher.value || isLabAdmin.value || isAdmin.value)

const disablePast = (time) => {
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return time.getTime() < todayStart.getTime()
}

const roleGradient = computed(() => 'linear-gradient(135deg, #2563eb, #1d4ed8)')

const bgFrom = computed(() => '#2563eb')
const bgTo = computed(() => '#1e40af')

const sloganMain = computed(() => ({
  ADMIN: '智慧管理',
  LAB_ADMIN: '高效运维',
  TEACHER: '轻松教学',
}[role.value] || '便捷使用'))

const sloganSub = computed(() => ({
  ADMIN: '让实验室运行井然有序',
  LAB_ADMIN: '让每台设备发挥最大价值',
  TEACHER: '让教学设备随手可用',
}[role.value] || '让设备随借随用'))

const roleLabel = computed(() => ({ ADMIN: '系统管理员', LAB_ADMIN: '实验室管理员', TEACHER: '教师' }[role.value] || '学生'))
const roleIcon = computed(() => UserIcon)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const myStats = reactive({ total: 0, borrowing: 0 })
const availableDeviceCount = ref(0)
const totalDevices = ref(0)
const heroOnline = ref(0)
const heroBorrow = ref(0)
const topDevice = ref(null)

const heroMetrics = computed(() => {
  if (isStudentOrTeacher.value) {
    return [
      { label: '我的借用', value: myStats.total },
      { label: '使用中', value: myStats.borrowing },
      { label: '可借设备', value: availableDeviceCount.value },
      { label: '设备总数', value: totalDevices.value },
    ]
  }
  return [
    { label: '待审批', value: overviewStats.pendingBorrow },
    { label: '待维修', value: overviewStats.pendingRepair },
    { label: '逾期设备', value: overviewStats.overdueCount },
    { label: '利用率', value: overviewStats.utilization + '%' },
  ]
})

const getImageUrl = (url) => {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  return encodeURI(url)
}
const handleImgError = (e) => {
  const parent = e.target?.parentElement
  if (parent && !parent.querySelector('.dev-ph')) {
    e.target.remove()
    const ph = document.createElement('div')
    ph.className = 'dev-ph'
    ph.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="42" height="42" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="color:#94a3b8"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>'
    parent.appendChild(ph)
  }
}
const getStatusLabel = (code) => ({ 1: '空闲', 2: '借出', 3: '维修中', 4: '待审核', 5: '已报废', 6: '待终审' }[code] || '未知')
const getBorrowStatusLabel = (code) => ({ 1: '待审批', 2: '已通过', 3: '已拒绝', 4: '已归还', 5: '已逾期', 6: '已取消' }[code] || '未知')
const formatPrice = (price) => {
  if (!price) return ''
  return parseFloat(price).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
const formatTime = (t) => {
  if (!t) return '-'
  return String(t).replace('T', ' ').substring(0, 16)
}

const deviceList = ref([])
const cardSelected = ref([])
const loadingDevices = ref(false)
const devicePage = ref(1)
const devicePageSize = ref(8)
const searchKeyword = ref('')
const selectedCategory = ref('')
const categories = ref([])
const labMap = ref({})

const allCategories = computed(() => [
  { key: '', label: '全部' },
  ...categories.value.map(c => ({ key: c, label: c })),
])

const loadLabMap = async () => {
  try {
    const res = await getAllLabs()
    const labs = res.data || []
    const map = {}
    labs.forEach(l => { map[l.id] = l.name })
    labMap.value = map
  } catch (e) {}
}

const loadDevices = async () => {
  loadingDevices.value = true
  try {
    const res = await getDeviceList({
      page: devicePage.value,
      size: devicePageSize.value,
      keyword: searchKeyword.value,
      category: selectedCategory.value,
    })
    const records = res.data?.records || []
    deviceList.value = records.map(d => ({
      ...d,
      labName: d.labName || (labMap.value[d.labId] || ''),
    }))
    totalDevices.value = res.data?.total || 0
    if (records.length > 0 && categories.value.length === 0) {
      categories.value = [...new Set(records.map(d => d.category).filter(Boolean))]
    }
  } catch (e) {
    deviceList.value = []
  } finally {
    loadingDevices.value = false
  }
}

const loadCategories = async () => {
  try {
    const res = await getDeviceList({ page: 1, size: 500 })
    const records = res.data?.records || []
    categories.value = [...new Set(records.map(d => d.category).filter(Boolean))]
    availableDeviceCount.value = records.filter(d => d.status === 1).length
    totalDevices.value = records.length
  } catch (e) {}
}

const router = useRouter()
const goDeviceDetail = () => {
  if (topDevice.value && topDevice.value.name) {
    router.push({ path: '/device', query: { keyword: topDevice.value.name } })
  }
}

const loadHeroScene = async () => {
  try {
    const [devRes, brwRes, listRes] = await Promise.all([
      getDeviceStatusStats().catch(() => ({ data: {} })),
      getBorrowStats().catch(() => ({ data: {} })),
      getDeviceList({ page: 1, size: 500 }).catch(() => ({ data: { records: [] } })),
    ])
    const dev = (devRes && devRes.data) || {}
    heroOnline.value = dev.idle || 0
    heroBorrow.value = dev.borrowed || 0

    const tops = (brwRes && brwRes.data && brwRes.data.topDevices) || []
    const records = (listRes && listRes.data && listRes.data.records) || []
    const imgByName = {}
    records.forEach(d => { if (d.name) imgByName[d.name] = d.imageUrl })
    const first = tops[0]
    if (first && first.name) {
      topDevice.value = {
        name: first.name,
        count: first.count || 0,
        imageUrl: imgByName[first.name] || '',
      }
    } else {
      topDevice.value = null
    }
  } catch (e) {}
}

const myBorrowList = ref([])
const loadingMyBorrows = ref(false)

const loadMyBorrows = async () => {
  loadingMyBorrows.value = true
  try {
    const res = await getBorrowList({ page: 1, size: 6 })
    myBorrowList.value = res.data?.records || []
    const allRes = await getBorrowList({ page: 1, size: 500 })
    const all = allRes.data?.records || []
    myStats.total = all.length
    myStats.borrowing = all.filter(b => b.status === 2 || b.status === 5).length
  } catch (e) {
    myBorrowList.value = []
  } finally {
    loadingMyBorrows.value = false
  }
}

const borrowDialogVisible = ref(false)
const borrowDevice = ref(null)
const submittingBorrow = ref(false)
const borrowForm = reactive({ borrowTime: '', purpose: '', expectReturnTime: '', remark: '' })

const openBorrowDialog = (device) => {
  borrowDevice.value = device
  borrowForm.borrowTime = ''
  borrowForm.purpose = ''
  borrowForm.expectReturnTime = ''
  borrowForm.remark = ''
  borrowDialogVisible.value = true
}

const isCardSelected = (device) => cardSelected.value.some(x => x.id === device.id)
const toggleCardSelect = (device) => {
  if (!batchMode.value || device.status !== 1) return
  const idx = cardSelected.value.findIndex(x => x.id === device.id)
  if (idx >= 0) cardSelected.value.splice(idx, 1)
  else cardSelected.value.push(device)
}

const batchMode = ref(false)
const toggleBatchMode = () => {
  batchMode.value = !batchMode.value
  if (!batchMode.value) cardSelected.value = []
}
const selectAllCards = () => {
  cardSelected.value = deviceList.value.filter(d => d.status === 1)
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
    loadDevices()
  } catch (e) {} finally {
    submittingBatch.value = false
  }
}

const submitBorrow = async () => {
  if (!borrowForm.purpose.trim()) return ElMessage.warning('请填写借用用途')
  if (!borrowForm.expectReturnTime) return ElMessage.warning('请选择预计归还时间')
  submittingBorrow.value = true
  try {
    await applyBorrow({
      deviceId: borrowDevice.value.id,
      borrowTime: borrowForm.borrowTime || undefined,
      purpose: borrowForm.purpose,
      expectReturnTime: borrowForm.expectReturnTime,
      remark: borrowForm.remark,
    })
    ElMessage.success('借用申请已提交，请等待审批')
    borrowDialogVisible.value = false
    loadMyBorrows()
    loadDevices()
  } catch (e) {}
  submittingBorrow.value = false
}

const handleReturn = async (item) => {
  try {
    await ElMessageBox.confirm(`确认归还设备「${item.deviceName}」？`, '归还确认', { type: 'warning' })
    await returnDevice(item.id)
    ElMessage.success('归还成功')
    loadMyBorrows()
    loadDevices()
  } catch (e) {}
}

const handleCancelBorrow = async (item) => {
  try {
    await ElMessageBox.confirm('确认取消借用申请？', '取消确认', { type: 'warning' })
    await cancelBorrow(item.id)
    ElMessage.success('已取消')
    loadMyBorrows()
    loadDevices()
  } catch (e) {}
}

const detailDialogVisible = ref(false)
const detailDevice = ref(null)
const showDeviceDetail = (device) => { detailDevice.value = device; detailDialogVisible.value = true }

const handleCardClick = (device) => {
  if (batchMode.value && device.status === 1) {
    toggleCardSelect(device)
  } else {
    showDeviceDetail(device)
  }
}

const repairDialogVisible = ref(false)
const repairDevice = ref(null)
const submittingRepair = ref(false)
const repairForm = reactive({ type: '', description: '', urgency: 'MEDIUM', location: '' })

const openRepairDialog = (device) => {
  repairDevice.value = device
  repairForm.type = ''
  repairForm.description = ''
  repairForm.urgency = 'MEDIUM'
  repairForm.location = ''
  repairDialogVisible.value = true
}

const submitRepair = async () => {
  if (!repairForm.type) return ElMessage.warning('请选择故障类型')
  if (!repairForm.description.trim()) return ElMessage.warning('请填写故障描述')
  submittingRepair.value = true
  try {
    const levelMap = { LOW: 1, MEDIUM: 2, HIGH: 3 }
    const level = levelMap[repairForm.urgency] || 2
    const remark = `故障类型: ${repairForm.type}` + (repairForm.location ? ` | 位置: ${repairForm.location}` : '')
    await reportFault({
      deviceId: repairDevice.value.id,
      faultDescription: repairForm.description.trim(),
      faultLevel: level,
      remark: remark,
    })
    ElMessage.success('维修申请已提交，我们会尽快处理')
    repairDialogVisible.value = false
    loadDevices()
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '提交失败'
    ElMessage.error(msg)
    console.error('报修失败:', e)
  }
  submittingRepair.value = false
}

// ==================== 教师代学生申请 ====================
const subStudentDialogVisible = ref(false)
const subStudentSubmitting = ref(false)
const studentList = ref([])
const subStudentForm = reactive({
  studentId: null,
  deviceId: null,
  borrowTime: '',
  expectReturnTime: '',
  purpose: '',
})

const availableSubDevices = computed(() => deviceList.value.filter(d => d.status === 1))

const openSubStudentDialog = async () => {
  try {
    const res = await getAllUsers('STUDENT')
    studentList.value = res.data || []
  } catch (e) {
    console.error('加载学生列表失败', e)
    ElMessage.error('加载学生列表失败')
    return
  }
  subStudentForm.studentId = null
  subStudentForm.deviceId = null
  subStudentForm.borrowTime = ''
  subStudentForm.expectReturnTime = ''
  subStudentForm.purpose = ''
  subStudentDialogVisible.value = true
}

const onSubDeviceChange = () => {}

const submitSubStudent = async () => {
  if (!subStudentForm.studentId) return ElMessage.warning('请选择学生')
  if (!subStudentForm.deviceId) return ElMessage.warning('请选择设备')
  if (!subStudentForm.purpose?.trim()) return ElMessage.warning('请填写用途')

  subStudentSubmitting.value = true
  try {
    const selectedStudent = studentList.value.find(s => s.id === subStudentForm.studentId)
    await applyBorrow({
      studentId: subStudentForm.studentId,
      studentName: selectedStudent?.username || selectedStudent?.realName,
      deviceId: subStudentForm.deviceId,
      borrowTime: subStudentForm.borrowTime || undefined,
      expectReturnTime: subStudentForm.expectReturnTime || undefined,
      purpose: '[教师代申请] ' + subStudentForm.purpose.trim(),
    })
    ElMessage.success('代申请已提交，等待审批')
    subStudentDialogVisible.value = false
    loadDevices()
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '提交失败'
    ElMessage.error(msg)
    console.error('代学生申请失败:', e)
  }
  subStudentSubmitting.value = false
}

const deviceSection = ref(null)
const scrollToDevices = () => deviceSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })

const overviewStats = reactive({ pendingBorrow: 0, pendingRepair: 0, overdueCount: 0, utilization: 0 })
const hasPendingItems = computed(() => overviewStats.pendingBorrow === 0 && overviewStats.pendingRepair === 0 && overviewStats.overdueCount === 0)

const statCards = ref([
  { title: '设备总数', value: 0, icon: MonitorIcon, gradient: 'linear-gradient(135deg,#2563eb,#1d4ed8)' },
  { title: '实验室数', value: 0, icon: OfficeBuildingIcon, gradient: 'linear-gradient(135deg,#3b82f6,#0ea5e9)' },
  { title: '在借中', value: 0, icon: TicketsIcon, gradient: 'linear-gradient(135deg,#0ea5e9,#1d4ed8)' },
  { title: '待处理', value: 0, icon: ToolsIcon, gradient: 'linear-gradient(135deg,#1e40af,#0c2340)' },
])

const deviceChartRef = ref(null)
const borrowChartRef = ref(null)
let deviceChart = null
let borrowChart = null

const handleResize = () => {
  deviceChart?.resize()
  borrowChart?.resize()
}

const renderDeviceChart = (data) => {
  const el = deviceChartRef.value
  if (!el) return
  try {
    deviceChart?.dispose()
    deviceChart = echarts.init(el)
    const items = [
      { value: data.idle || 0, name: '空闲', color: '#2563eb' },
      { value: data.borrowed || 0, name: '借出', color: '#3b82f6' },
      { value: data.repairing || 0, name: '维修中', color: '#0ea5e9' },
      { value: data.pendingDiscard || 0, name: '待审核', color: '#1e40af' },
      { value: data.pendingFinal || 0, name: '待终审', color: '#3b82f6' },
      { value: data.scrapped || 0, name: '已报废', color: '#1d4ed8' },
    ]
    const hasData = items.some(i => i.value > 0)
    deviceChart.setOption({
      tooltip: { trigger: 'item', backgroundColor: 'rgba(15,23,42,0.92)', border: 'none', padding: [12, 16], textStyle: { color: '#fff', fontSize: 13 }, formatter: '{b}: {c}台 ({d}%)' },
      legend: { bottom: 0, left: 'center', icon: 'circle', itemWidth: 10, itemHeight: 10, itemGap: 16, textStyle: { fontSize: 12, color: '#64748b' } },
      graphic: !hasData ? [{ type: 'text', left: 'center', top: 'middle', style: { text: '暂无数据', fill: '#94a3b8', fontSize: 14 } }] : [],
      series: [{
        type: 'pie', radius: ['52%', '80%'], center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
        label: { show: false },
        emphasis: { scale: true, scaleSize: 8, label: { show: true, fontSize: 13, fontWeight: 'bold', formatter: '{b}\n{c}台' } },
        data: hasData
          ? items.map(i => ({ value: i.value, name: i.name, itemStyle: { color: i.color } }))
          : [{ value: 1, name: '暂无', itemStyle: { color: '#e2e8f0' } }],
      }],
    })
  } catch (e) {}
}

const renderBorrowChart = (data) => {
  const el = borrowChartRef.value
  if (!el) return
  try {
    borrowChart?.dispose()
    borrowChart = echarts.init(el)
    const topDevices = (data && data.topDevices) || []
    if (topDevices.length === 0) {
      borrowChart.setOption({ graphic: { type: 'text', left: 'center', top: 'middle', style: { text: '暂无借用记录', fill: '#94a3b8', fontSize: 14 } } })
      return
    }
    borrowChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'rgba(15,23,42,0.92)', border: 'none', padding: [12, 16], textStyle: { color: '#fff', fontSize: 13 } },
      grid: { left: 100, right: 50, top: 10, bottom: 10 },
      xAxis: { type: 'value', axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }, axisLabel: { color: '#94a3b8', fontSize: 11 } },
      yAxis: { type: 'category', data: topDevices.map(d => d.name).reverse(), inverse: true, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#475569', fontSize: 12, width: 80, overflow: 'truncate' } },
      series: [{
        type: 'bar',
        data: topDevices.map((d, idx) => ({
          value: d.count,
          itemStyle: {
            borderRadius: [0, 8, 8, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: idx === 0 ? '#0ea5e9' : '#fb923c' },
              { offset: 1, color: idx === 0 ? '#1d4ed8' : '#fdba74' },
            ]),
          },
        })).reverse(),
        barWidth: 18,
        label: { show: true, position: 'right', color: '#0ea5e9', fontSize: 13, fontWeight: 600 },
      }],
    })
  } catch (e) {}
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  loadHeroScene()
  if (isStudentOrTeacher.value) {
    await loadLabMap()
    loadCategories()
    loadDevices()
    loadMyBorrows()
    return
  }
  try {
    const res = await getDashboardStats()
    const d = res.data || {}
    statCards.value[0].value = d.deviceCount || 0
    statCards.value[1].value = d.labCount || 0
    statCards.value[2].value = d.borrowedCount || 0
    statCards.value[3].value = d.pendingTotal || 0
    overviewStats.pendingBorrow = d.pendingBorrow || 0
    overviewStats.pendingRepair = d.pendingRepair || 0
    overviewStats.overdueCount = d.overdueCount || 0
    overviewStats.utilization = d.utilization || 0
  } catch (e) {}
  await nextTick()
  try {
    const [devRes, brwRes] = await Promise.all([
      getDeviceStatusStats().catch(() => ({ data: {} })),
      getBorrowStats().catch(() => ({ data: {} })),
    ])
    renderDeviceChart((devRes && devRes.data) || {})
    renderBorrowChart((brwRes && brwRes.data) || {})
  } catch (e) {}
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  deviceChart?.dispose()
  borrowChart?.dispose()
})
</script>

<style scoped>
.home {
  max-width: 1360px;
  margin: 0 auto;
  padding: 28px 28px 60px;
}

/* ==================== HERO (鍏ㄥ绱у噾) ==================== */
.hero {
  position: relative;
  margin: 0 -24px 28px;
  padding: 32px 40px;
  color: #fff;
  overflow: hidden;
  background: linear-gradient(135deg, var(--bg-from) 0%, var(--bg-to) 100%);
}

.hero-deco {
  position: absolute; inset: 0; pointer-events: none; overflow: hidden;
}
.deco-circle { position: absolute; border-radius: 50%; opacity: 0.15; }
.c1 { width: 320px; height: 320px; top: -120px; right: -40px; background: #fff; }
.c2 { width: 200px; height: 200px; bottom: -80px; left: -20px; background: #fff; opacity: 0.08; }
.deco-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.05) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: radial-gradient(ellipse at 30% 50%, black 30%, transparent 70%);
}

.hero-inner {
  position: relative; z-index: 2;
  max-width: 1360px; margin: 0 auto;
  display: flex; align-items: center; justify-content: space-between; gap: 40px;
}
.hero-left { flex: 1; min-width: 0; }

.brand-chip {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 4px 12px;
  background: rgba(255,255,255,0.16);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255,255,255,0.22);
  border-radius: 100px;
  font-size: 11px; font-weight: 500; letter-spacing: 0.3px;
  margin-bottom: 10px;
}
.chip-logo {
  display: inline-flex; align-items: center; justify-content: center;
  width: 18px; height: 18px; border-radius: 5px;
  background: rgba(255,255,255,0.25);
}

.hero-title {
  font-size: 26px; font-weight: 800; line-height: 1.3;
  letter-spacing: -0.3px; margin: 0 0 12px;
}
.hero-highlight { position: relative; color: #fff; }
.hero-highlight::after {
  content: ''; position: absolute;
  left: 0; right: 0; bottom: 1px; height: 6px;
  background: rgba(255,255,255,0.25);
  border-radius: 3px; z-index: -1;
}

.hero-meta {
  display: flex; align-items: center; gap: 24px; flex-wrap: wrap;
}
.hero-user { display: flex; align-items: center; gap: 10px; }
.hu-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  border: 2px solid rgba(255,255,255,0.3);
  flex-shrink: 0;
}
.hu-greet { font-size: 13px; opacity: 0.92; }
.hu-greet b { font-weight: 700; color: #fff; }
.hu-role {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 11px; padding: 2px 10px;
  background: rgba(255,255,255,0.2);
  border-radius: 20px;
}
.hu-role .dot {
  width: 5px; height: 5px; border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 0 2px rgba(74,222,128,0.3);
}

.hero-actions { display: flex; gap: 10px; }
.btn-main {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 9px 18px;
  border: none; border-radius: 10px;
  background: #fff; color: var(--bg-from);
  font-size: 13px; font-weight: 600;
  cursor: pointer; font-family: inherit;
  box-shadow: 0 4px 14px rgba(0,0,0,0.15);
  transition: all 0.2s ease;
}
.btn-main:hover { transform: translateY(-1px); box-shadow: 0 6px 18px rgba(0,0,0,0.22); }
.btn-outline {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 9px 18px;
  border: 1px solid rgba(255,255,255,0.4);
  border-radius: 10px;
  background: rgba(255,255,255,0.08);
  backdrop-filter: blur(6px);
  color: #fff; font-size: 13px; font-weight: 600;
  cursor: pointer; font-family: inherit;
  transition: all 0.2s ease;
}
.btn-outline:hover { background: rgba(255,255,255,0.18); transform: translateY(-1px); }

.hero-right {
  flex: 0 0 auto;
  display: flex; align-items: center; gap: 20px;
}

.hero-metrics { display: flex; gap: 10px; }
.hm {
  padding: 8px 14px;
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.18);
  border-radius: 10px;
  min-width: 80px;
  text-align: center;
  transition: all 0.2s;
}
.hm:hover { background: rgba(255,255,255,0.2); transform: translateY(-2px); }
.hm-val { font-size: 20px; font-weight: 800; line-height: 1.1; }
.hm-lbl { font-size: 10px; opacity: 0.75; margin-top: 2px; letter-spacing: 0.3px; }

.hero-scene { position: relative; width: 180px; height: 80px; }
.sc-console {
  position: absolute; top: -4px; left: 40px;
  width: 88px; height: 88px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 10px 22px -8px rgba(0, 0, 0, 0.3);
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
}
.sc-circle {
  width: 100%; height: 100%;
  border-radius: 50%;
  position: relative;
  background: #eff6ff;
  color: #93c5fd;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
  cursor: pointer;
}
.sc-circle img { width: 100%; height: 100%; object-fit: cover; display: block; }
.sc-circle .el-icon { position: absolute; }
.sc-name {
  position: absolute; left: 6px; right: 6px; bottom: 5px;
  font-size: 9px; color: #fff; text-align: center;
  background: rgba(29, 78, 216, 0.65); border-radius: 100px;
  padding: 1px 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.sc-badge {
  position: absolute; top: 2px; right: -2px;
  background: #fff; color: #0ea5e9; font-size: 10px; font-weight: 700;
  padding: 2px 8px; border-radius: 100px;
  box-shadow: 0 4px 10px -2px rgba(0, 0, 0, 0.2);
}
.sc-float {
  position: absolute;
  display: flex; align-items: center; gap: 6px;
  padding: 6px 10px; background: #fff; color: #1e293b;
  border-radius: 8px; font-size: 11px;
  box-shadow: 0 6px 14px -4px rgba(0,0,0,0.25);
  animation: floatY 4s ease-in-out infinite;
}
.sc-float svg { color: #2563eb; flex-shrink: 0; }
.sc-float b { font-size: 14px; font-weight: 800; color: #0f172a; }
.sc-float span { color: #64748b; }
.sc-f1 { top: -6px; right: 0; animation-delay: 0s; }
.sc-f2 { bottom: 4px; right: 10px; animation-delay: -1.5s; }

@keyframes floatY {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

/* ==================== BLOCKS ==================== */
.block {
  margin-bottom: 48px;
}
.block-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
  gap: 16px;
}
.block-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}
.bt-bar {
  width: 4px;
  height: 22px;
  background: linear-gradient(180deg, var(--bg-from), var(--bg-to));
  border-radius: 4px;
}
.block-head-right { display: flex; align-items: center; gap: 16px; }
.block-more {
  background: none; border: none;
  font-size: 13px; color: var(--bg-from);
  font-weight: 600; cursor: pointer;
  font-family: inherit;
  transition: opacity 0.2s;
}
.block-more:hover { opacity: 0.7; }
.batch-toggle {
  background: linear-gradient(135deg, #f0f9ff, #e0f2fe); border: 1px solid #bae6fd;
  border-radius: 8px; padding: 6px 14px; font-size: 13px; color: #0369a1; font-weight: 600;
  cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 6px;
}
.batch-toggle:hover { background: linear-gradient(135deg, #e0f2fe, #bae6fd); opacity: 1; }
.batch-toggle.active { background: #2563eb; color: #fff; border-color: #2563eb; }
.batch-toggle.active:hover { background: #1d4ed8; }

/* Search Bar */
.search-bar {
  display: flex; align-items: center; gap: 8px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 0 12px;
  transition: all 0.2s;
  width: 240px;
}
.search-bar:focus-within { border-color: var(--bg-from); box-shadow: 0 0 0 3px rgba(99,102,241,0.1); }
.search-bar input {
  border: none; outline: none; background: transparent;
  font-size: 13px; color: #334155;
  padding: 9px 0; width: 100%;
  font-family: inherit;
}
.search-bar input::placeholder { color: #94a3b8; }
.sb-clear {
  background: none; border: none; cursor: pointer;
  color: #94a3b8; padding: 0; display: flex;
}

/* Filter row */
.filter-row {
  display: flex; gap: 8px; flex-wrap: wrap;
  margin-bottom: 22px;
}
.ft {
  padding: 7px 18px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 100px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}
.ft:hover { background: #f8fafc; color: #0f172a; }
.ft.active {
  background: linear-gradient(135deg, var(--bg-from), var(--bg-to));
  border-color: transparent;
  color: #fff;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(99,102,241,0.2);
}

/* ==================== Device Grid ==================== */
.device-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.dev-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #eef2f7;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4,0,0.2,1);
  display: flex;
  flex-direction: column;
}
.dev-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 32px -12px rgba(15,23,42,0.15);
  border-color: transparent;
}

.dev-media {
  position: relative;
  height: 160px;
  background: #f8fafc;
  overflow: hidden;
}
.dev-media img {
  width: 100%; height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}
.dev-card:hover .dev-media img { transform: scale(1.05); }
.dev-ph {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  color: #cbd5e1;
}

.dev-badge {
  position: absolute;
  top: 10px; right: 10px;
  padding: 4px 10px;
  border-radius: 100px;
  font-size: 11px;
  font-weight: 600;
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(8px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.dbs-1 { color: #1d4ed8; }
.dbs-2 { color: #2563eb; }
.dbs-3 { color: #1e40af; }
.dbs-4 { color: #2563eb; }
.dbs-5, .dbs-6 { color: #1d4ed8; }

.dev-card.selected {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.25), 0 16px 32px -12px rgba(15,23,42,0.15);
}
.dc-check {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 5;
  background: rgba(255,255,255,0.95);
  border-radius: 50%;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  border-radius: 10px;
  margin-bottom: 12px;
}
.batch-count { color: #1d4ed8; font-size: 13px; font-weight: 500; }
.batch-count b { font-weight: 700; }

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

.dev-info {
  padding: 14px 16px 10px;
  flex: 1;
}
.dev-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dev-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.dm-cat {
  font-size: 11px;
  padding: 2px 8px;
  background: linear-gradient(135deg, #eef2ff, #e0e7ff);
  color: #2563eb;
  border-radius: 6px;
  font-weight: 500;
}
.dm-lab {
  font-size: 11px;
  padding: 2px 8px;
  background: linear-gradient(135deg, #ecfeff, #cffafe);
  color: #0891b2;
  border-radius: 6px;
  font-weight: 500;
}

.dev-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px 14px;
  border-top: 1px solid #f1f5f9;
}
.dev-code {
  font-size: 12px;
  color: #94a3b8;
  font-family: 'SF Mono', 'Menlo', monospace;
}
.dev-btns { display: flex; gap: 6px; }
.dev-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 7px 14px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--bg-from), var(--bg-to));
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}
.dev-btn:hover { transform: scale(1.03); }
.dev-btn.repair {
  padding: 7px 9px;
  background: #fff;
  color: #0ea5e9;
  border: 1px solid #fde68a;
  box-shadow: 0 1px 3px rgba(245,158,11,0.15);
}
.dev-btn.repair:hover { background: #fffbeb; transform: scale(1.05); box-shadow: 0 2px 8px rgba(245,158,11,0.25); }
.dev-btn.disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
  transform: none;
}
.dev-btn:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}

/* Skeleton & Empty */
.grid-skeleton {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}
.sk-item {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #eef2f7;
}
.sk-img {
  height: 160px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: sk 1.4s infinite;
}
.sk-text {
  height: 16px;
  margin: 14px 16px 8px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: sk 1.4s infinite;
}
.sk-text.short { width: 50%; height: 12px; margin-bottom: 16px; }
@keyframes sk { 0%{background-position:200% 0} 100%{background-position:-200% 0} }

.empty-hint {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
}
.empty-hint p { margin-top: 12px; font-size: 14px; }
.empty-hint.small { padding: 30px 20px; }

.pager {
  margin-top: 28px;
  display: flex;
  justify-content: center;
}

/* ==================== Borrow List ==================== */
.borrow-skel { display: flex; flex-direction: column; gap: 12px; }
.sk-b {
  height: 70px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: sk 1.4s infinite;
  border-radius: 12px;
}

.borrow-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.br-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eef2f7;
  transition: all 0.25s;
}
.br-item:hover { box-shadow: 0 8px 20px -8px rgba(15,23,42,0.1); }
.br-indicator {
  width: 4px; height: 36px;
  border-radius: 4px;
  background: #cbd5e1;
  flex-shrink: 0;
}
.br-2 .br-indicator { background: #3b82f6; }
.br-1 .br-indicator { background: #0ea5e9; }
.br-4 .br-indicator { background: #2563eb; }
.br-5 .br-indicator { background: #1d4ed8; }
.br-3 .br-indicator, .br-6 .br-indicator { background: #94a3b8; }

.br-body { flex: 1; min-width: 0; }
.br-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.br-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}
.br-status {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 100px;
}
.bs-1 { background: #fef3c7; color: #1e40af; }
.bs-2 { background: #dbeafe; color: #2563eb; }
.bs-3 { background: #fee2e2; color: #1d4ed8; }
.bs-4 { background: #dcfce7; color: #16a34a; }
.bs-5 { background: #fee2e2; color: #1d4ed8; }
.bs-6 { background: #f1f5f9; color: #64748b; }

.br-sub { font-size: 13px; color: #64748b; margin-bottom: 4px; }
.br-time { font-size: 12px; color: #94a3b8; display: flex; align-items: center; gap: 4px; }

.br-actions { display: flex; gap: 8px; flex-shrink: 0; }
.br-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 8px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 12px; font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}
.br-btn:hover { background: #f8fafc; border-color: #cbd5e1; }
.br-btn.primary {
  border-color: transparent;
  background: linear-gradient(135deg, var(--bg-from), var(--bg-to));
  color: #fff;
}
.br-btn.primary:hover { transform: scale(1.03); }

/* ==================== Admin Stats ==================== */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
}
.stat-card {
  display: flex; align-items: center; gap: 16px;
  padding: 22px;
  background: #fff;
  border-radius: 14px;
  border: 1px solid #eef2f7;
  transition: all 0.3s;
  animation: fadeUp 0.5s ease-out both;
  animation-delay: calc(var(--i) * 80ms);
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 28px -12px rgba(15,23,42,0.15);
}
.sc-icon {
  width: 52px; height: 52px;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 8px 16px -4px rgba(0,0,0,0.15);
}
.sc-info { flex: 1; }
.sc-val {
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1;
  margin-bottom: 4px;
}
.sc-lbl { font-size: 13px; color: #64748b; }

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ==================== Admin Panels ==================== */
.panel-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.panel {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #eef2f7;
  padding: 22px;
  min-height: 320px;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.panel-head h3 {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}
.panel-hint { font-size: 12px; color: #94a3b8; }

.chart-area { height: 260px; }

/* Todo list */
.todo-list { display: flex; flex-direction: column; gap: 12px; }
.todo {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px;
  background: #f8fafc;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.todo:hover { background: #f1f5f9; transform: translateX(4px); }
.todo.done { cursor: default; }
.todo.done:hover { transform: none; }
.td-bar {
  width: 4px; height: 32px;
  border-radius: 4px;
  flex-shrink: 0;
}
.td-bar.borrow { background: #3b82f6; }
.td-bar.repair { background: #0ea5e9; }
.td-bar.overdue { background: #1d4ed8; }
.td-body { flex: 1; }
.td-title { font-size: 14px; font-weight: 600; color: #0f172a; margin-bottom: 2px; }
.td-desc { font-size: 12px; color: #64748b; }
.td-count {
  padding: 4px 12px;
  border-radius: 100px;
  font-size: 13px;
  font-weight: 700;
}
.td-count.borrow { background: #dbeafe; color: #2563eb; }
.td-count.repair { background: #fef3c7; color: #1e40af; }
.td-count.overdue { background: #fee2e2; color: #1d4ed8; }

/* ==================== Quick Actions ==================== */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}
.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px 16px;
  background: #fff;
  border-radius: 14px;
  border: 1px solid #eef2f7;
  cursor: pointer;
  transition: all 0.25s;
}
.quick-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 28px -12px rgba(15,23,42,0.15);
  border-color: transparent;
}
.qi-icon {
  width: 52px; height: 52px;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  box-shadow: 0 6px 16px -4px rgba(0,0,0,0.15);
}
.quick-item span { font-size: 13px; font-weight: 600; color: #334155; }

/* ==================== Dialogs ==================== */
:deep(.el-dialog) { border-radius: 16px; }
:deep(.el-dialog__header) { padding: 20px 24px 8px; margin-right: 0; }
:deep(.el-dialog__body) { padding: 12px 24px; }
:deep(.el-dialog__footer) { padding: 12px 24px 20px; }

.dlg-head {
  display: flex; align-items: center; gap: 10px;
  font-size: 16px; font-weight: 700;
  color: #0f172a;
}
.dlg-ico {
  width: 30px; height: 30px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
}

.bd-wrap { padding: 4px 0; }
.bd-device {
  display: flex; gap: 14px;
  padding: 14px;
  background: #f8fafc;
  border-radius: 12px;
  margin-bottom: 18px;
}
.bd-thumb {
  width: 72px; height: 72px;
  border-radius: 10px;
  background: #fff;
  display: flex; align-items: center; justify-content: center;
  color: #94a3b8;
  overflow: hidden;
  flex-shrink: 0;
}
.bd-thumb img { width: 100%; height: 100%; object-fit: cover; }
.bd-dinfo h4 { margin: 0 0 6px; font-size: 15px; color: #0f172a; }
.bd-dmeta { font-size: 12px; color: #64748b; margin-bottom: 6px; }

.detail-wrap { padding: 4px 0; }
.detail-hero {
  display: flex; gap: 20px;
  padding-bottom: 18px;
  border-bottom: 1px solid #eef2f7;
  margin-bottom: 18px;
}
.detail-img {
  width: 140px; height: 140px;
  border-radius: 12px;
  background: #f8fafc;
  overflow: hidden;
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  color: #94a3b8;
}
.detail-img img { width: 100%; height: 100%; object-fit: cover; }
.no-img { display: flex; flex-direction: column; align-items: center; gap: 6px; font-size: 11px; }
.detail-hinfo { flex: 1; min-width: 0; }
.detail-hinfo h3 { margin: 0 0 10px; font-size: 18px; color: #0f172a; }
.detail-tags { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.dt {
  font-size: 12px;
  padding: 3px 10px;
  background: #eef2ff;
  color: #2563eb;
  border-radius: 6px;
  font-weight: 500;
}
.dt.teal { background: #cffafe; color: #0891b2; }
.dt.price { background: #fef3c7; color: #1e40af; font-weight: 600; }
.detail-status {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 12px;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 600;
}
.detail-status .ds-dot { width: 6px; height: 6px; border-radius: 50%; }
.ds-1 { background: #dcfce7; color: #16a34a; } .ds-1 .ds-dot { background: #16a34a; }
.ds-2 { background: #dbeafe; color: #2563eb; } .ds-2 .ds-dot { background: #2563eb; }
.ds-3 { background: #fef3c7; color: #1e40af; } .ds-3 .ds-dot { background: #1e40af; }
.ds-4 { background: #eedeff; color: #2563eb; } .ds-4 .ds-dot { background: #2563eb; }
.ds-5, .ds-6 { background: #fee2e2; color: #1d4ed8; } .ds-5 .ds-dot, .ds-6 .ds-dot { background: #1d4ed8; }

.detail-fields {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.df {
  display: flex; flex-direction: column; gap: 4px;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
}
.df.full { grid-column: span 2; }
.df-lbl { font-size: 11px; color: #94a3b8; font-weight: 500; }
.df-val { font-size: 13px; color: #0f172a; font-weight: 500; word-break: break-all; }

/* ==================== Responsive ==================== */
@media (max-width: 1200px) {
  .panel-grid { grid-template-columns: 1fr; }
}
@media (max-width: 1024px) {
  .hero { padding: 44px 40px; }
  .hero-inner { flex-direction: column; text-align: center; }
  .hero-left { max-width: 100%; }
  .hero-title { font-size: 36px; }
  .hero-user { justify-content: center; }
  .hero-actions { justify-content: center; }
  .hero-metrics { justify-content: center; }
  .hero-right { width: 320px; margin-top: 20px; }
  .scene { width: 320px; height: 260px; }
  .sc-main { width: 270px; }
}
@media (max-width: 640px) {
  .home { padding: 20px 16px 40px; }
  .hero { padding: 36px 24px; border-radius: 18px; }
  .hero-title { font-size: 28px; }
  .block-head { flex-direction: column; align-items: flex-start; gap: 12px; }
  .block-head-right { width: 100%; justify-content: space-between; }
  .search-bar { width: auto; flex: 1; }
  .device-grid { grid-template-columns: 1fr; }
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .detail-hero { flex-direction: column; }
  .detail-img { width: 100%; height: 180px; }
  .detail-fields { grid-template-columns: 1fr; }
  .df.full { grid-column: span 1; }
}

.teacher-actions-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px 24px;
  border: 1px solid #e2e8f0;
  margin-bottom: 16px;
}
.ta-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}
.ta-header .ta-icon {
  width: 46px; height: 46px;
  background: linear-gradient(135deg, #2563eb, #1e40af);
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
}
.ta-header h3 { margin: 0; font-size: 16px; font-weight: 700; color: #0f172a; }
.ta-header p { margin: 2px 0 0; font-size: 13px; color: #64748b; }
.ta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.ta-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 10px;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}
.ta-item:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
  transform: translateY(-2px);
}
.ta-item-icon {
  width: 42px; height: 42px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.ta-item-info h4 { margin: 0; font-size: 14px; font-weight: 600; color: #1e293b; }
.ta-item-info p { margin: 2px 0 0; font-size: 12px; color: #94a3b8; }

/* ======== 教师工作台 ======== */
.teacher-workbench { margin-bottom: 20px; }
.tw-badge {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
}
.tw-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.tw-card {
  background: #fff;
  border-radius: 16px;
  padding: 22px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #f0f0f0;
  position: relative;
  overflow: hidden;
}
.tw-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--tw-c1, #2563eb), var(--tw-c2, #1e40af));
  opacity: 0;
  transition: opacity 0.3s;
}
.tw-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(37, 99, 235, 0.15);
  border-color: transparent;
}
.tw-card:hover::before { opacity: 1; }
.tw-card-1 { --tw-c1: #2563eb; --tw-c2: #1d4ed8; }
.tw-card-2 { --tw-c1: #0ea5e9; --tw-c2: #0369a1; }
.tw-card-3 { --tw-c1: #6366f1; --tw-c2: #4f46e5; }
.tw-card-4 { --tw-c1: #3b82f6; --tw-c2: #2563eb; }

.tw-icon {
  width: 52px; height: 52px;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--tw-c1, #2563eb), var(--tw-c2, #1e40af));
  color: #fff;
  flex-shrink: 0;
}
.tw-card-1 .tw-icon { background: linear-gradient(135deg, #2563eb, #1d4ed8); }
.tw-card-2 .tw-icon { background: linear-gradient(135deg, #0ea5e9, #0369a1); }
.tw-card-3 .tw-icon { background: linear-gradient(135deg, #6366f1, #4f46e5); }
.tw-card-4 .tw-icon { background: linear-gradient(135deg, #3b82f6, #2563eb); }

.tw-info { flex: 1; min-width: 0; }
.tw-title { font-size: 15px; font-weight: 600; color: #1e293b; margin-bottom: 4px; }
.tw-desc { font-size: 12px; color: #94a3b8; }
.tw-arrow { color: #cbd5e1; transition: transform 0.3s, color 0.3s; flex-shrink: 0; }
.tw-card:hover .tw-arrow { color: #2563eb; transform: translateX(4px); }

/* 代学生申请 dialog */
.teacher-sub-dialog :deep(.el-dialog) { border-radius: 16px; overflow: hidden; }
.teacher-sub-dialog :deep(.el-dialog__header) { padding: 0; margin: 0; }
.teacher-sub-dialog :deep(.el-dialog__body) { padding: 20px 24px 8px; }
.teacher-sub-dialog :deep(.el-dialog__footer) { padding: 12px 24px 20px; border-top: 1px solid #f0f0f0; }
.tsd-header {
  display: flex; align-items: center; gap: 14px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}
.tsd-icon {
  width: 48px; height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

@media (max-width: 1100px) {
  .tw-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 600px) {
  .tw-grid { grid-template-columns: 1fr; }
}

</style>