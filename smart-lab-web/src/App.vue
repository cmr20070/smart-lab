<template>
  <router-view />
</template>

<script setup>
</script>

<style>
:root {
  --primary: #2563eb;
  --primary-light: #60a5fa;
  --primary-dark: #1e40af;
  --success: #2563eb;
  --warning: #0ea5e9;
  --danger: #1d4ed8;
  --info: #3b82f6;
  --bg-primary: #f8fafc;
  --bg-secondary: #f1f5f9;
  --bg-card: #ffffff;
  --text-primary: #0f172a;
  --text-secondary: #475569;
  --text-muted: #94a3b8;
  --border: #e2e8f0;
  --border-light: #f1f5f9;
  --shadow-sm: 0 1px 2px rgba(15, 23, 42, 0.05);
  --shadow-md: 0 4px 6px -1px rgba(15, 23, 42, 0.07), 0 2px 4px -2px rgba(15, 23, 42, 0.05);
  --shadow-lg: 0 10px 15px -3px rgba(15, 23, 42, 0.08), 0 4px 6px -4px rgba(15, 23, 42, 0.04);
  --shadow-xl: 0 20px 25px -5px rgba(15, 23, 42, 0.1), 0 8px 10px -6px rgba(15, 23, 42, 0.04);
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 16px;
  --radius-xl: 20px;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: 'Inter', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

body {
  background:
    radial-gradient(ellipse 80% 50% at 50% -10%, rgba(37, 99, 235, 0.08), transparent 60%),
    radial-gradient(ellipse 60% 40% at 100% 0%, rgba(14, 165, 233, 0.06), transparent 55%),
    radial-gradient(ellipse 50% 30% at 0% 100%, rgba(37, 99, 235, 0.04), transparent 50%),
    linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  background-attachment: fixed;
  color: var(--text-primary);
  min-height: 100vh;
}

::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

::selection {
  background: #2563eb;
  color: #fff;
}

.el-button {
  border-radius: 10px !important;
  font-weight: 500;
  transition: all 0.2s ease !important;
}

.el-button--primary {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%) !important;
  border: none !important;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3) !important;
}

.el-button--primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4) !important;
}

.el-card {
  border-radius: 16px !important;
  border: none !important;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04), 0 1px 2px rgba(15, 23, 42, 0.02) !important;
}

.el-input__wrapper {
  border-radius: 10px !important;
}

.el-dialog {
  border-radius: 20px !important;
  overflow: hidden !important;
}

.el-dialog__header {
  padding: 24px 28px 16px !important;
}

.el-dialog__body {
  padding: 8px 28px 24px !important;
}

.el-dialog__footer {
  padding: 16px 28px 24px !important;
  border-top: 1px solid var(--border-light);
}

.el-tag {
  border-radius: 20px !important;
  padding: 0 10px !important;
  font-weight: 500 !important;
}

.el-table {
  --el-table-border-color: var(--border-light);
  --el-table-header-bg-color: #f8fafc;
  --el-table-row-hover-bg-color: #f1f5f9;
  border-radius: 12px !important;
  overflow: hidden !important;
}

.el-table th.el-table__cell {
  font-weight: 600 !important;
  color: var(--text-secondary) !important;
  font-size: 13px !important;
}

.el-table td.el-table__cell {
  font-size: 14px !important;
  color: var(--text-primary) !important;
}

.el-pagination {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: var(--text-secondary);
}

.el-pagination .btn-prev,
.el-pagination .btn-next,
.el-pagination .el-pager li {
  border-radius: 8px !important;
}

.el-pagination .el-pager li.is-active {
  background: linear-gradient(135deg, #2563eb, #1d4ed8) !important;
}

.el-form-item__label {
  font-weight: 500 !important;
  color: var(--text-secondary) !important;
}

.el-tabs__item.is-active {
  font-weight: 600 !important;
}

.el-message-box {
  border-radius: 16px !important;
}

.el-select__wrapper {
  border-radius: 10px !important;
}

.el-dialog__title {
  font-weight: 600 !important;
}
</style>