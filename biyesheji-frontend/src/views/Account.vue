<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAddressList, addAddress, updateAddress, deleteAddress, setDefaultAddress, exportAccountData, deleteAccount } from '../api/user'
import { getOrderPage } from '../api/order'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const tab = ref('info')

// 地址
const addresses = ref<any[]>([])
const load = async () => {
  try {
    const r = await getAddressList();
    addresses.value = r.data.data || [];
  } catch (e: any) { ElMessage.error('加载地址失败:' + (e?.response?.data?.message || '网络错误')) }
}
onMounted(load)

// 新增地址表单
const nName = ref(''); const nPhone = ref(''); const nAddr = ref('')
const doAdd = async () => {
  if (!nName.value || !nPhone.value || !nAddr.value) { ElMessage.warning('请填写完整'); return }
  try { await addAddress({ receiverName: nName.value, receiverPhone: nPhone.value, detail: nAddr.value }); nName.value=''; nPhone.value=''; nAddr.value=''; load(); ElMessage.success('已添加') } catch { ElMessage.error('添加失败') }
}

// 编辑：每个地址独立编辑状态
const editing = ref(0)
const eName = ref(''); const ePhone = ref(''); const eAddr = ref('')
const startEdit = (a: any) => { editing.value = a.id; eName.value = a.receiverName; ePhone.value = a.receiverPhone; eAddr.value = a.detail }
const cancelEdit = () => { editing.value = 0 }
const doUpdate = async (id: number) => {
  if (!eName.value || !ePhone.value || !eAddr.value) { ElMessage.warning('请填写完整'); return }
  try {
    const res = await updateAddress(id, { receiverName: eName.value, receiverPhone: ePhone.value, detail: eAddr.value, })
    if(res.data.code === 400) { ElMessage.error(res.data.message || '更新失败'); return }
    editing.value = 0
    load()
    ElMessage.success('已更新')
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '更新失败') }
}
const doDel = async (id: number) => { try { await deleteAddress(id); load() } catch (e: any) { ElMessage.error(e?.response?.data?.message || '删除失败') } }
const doSetDef = async (id: number) => { try { await setDefaultAddress(id); load() } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') } }

const orders = ref<any[]>([])
onMounted(async () => { try { const r = await getOrderPage({ pageNum:1, pageSize:10 }); orders.value = r.data.data.records } catch {} })

const exporting = ref(false)
const deleteVisible = ref(false)
const deleting = ref(false)
const deletePassword = ref('')
const deletePhrase = ref('')
const downloadExport = async () => {
  if (exporting.value) return
  exporting.value = true
  try {
    const data = (await exportAccountData()).data.data
    const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' }))
    const link = document.createElement('a')
    link.href = url
    link.download = `account-export-${new Date().toISOString().slice(0, 10)}.json`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('账户资料已导出')
  } catch {} finally { exporting.value = false }
}
const confirmDelete = async () => {
  if (!deletePassword.value) return ElMessage.warning('请输入当前密码')
  if (deletePhrase.value !== '注销账户') return ElMessage.warning('请输入“注销账户”完成确认')
  deleting.value = true
  try {
    await deleteAccount(deletePassword.value)
    userStore.clearSession()
    ElMessage.success('账户已注销，历史订单将按履约和合规要求保留')
    await router.replace('/login')
  } catch {} finally { deleting.value = false }
}
</script>

<template>
  <div style="max-width:900px;margin:0 auto">
    <div class="section-title">我的账号</div>
    <div style="display:flex;gap:0;margin-bottom:14px">
      <button :style="{background:tab==='info'?'var(--jd-red)':'#fff',color:tab==='info'?'#fff':'#333',border:'1px solid #ddd',padding:'8px 24px',cursor:'pointer',fontSize:'14px'}" @click="tab='info'">个人信息</button>
      <button :style="{background:tab==='orders'?'var(--jd-red)':'#fff',color:tab==='orders'?'#fff':'#333',border:'1px solid #ddd',padding:'8px 24px',cursor:'pointer',fontSize:'14px'}" @click="tab='orders'">我的订单</button>
    </div>

    <div v-if="tab==='info'" style="background:#fff;padding:20px">
      <h3 style="margin-bottom:14px">收货地址管理</h3>

      <div v-if="addresses.length===0" style="padding:30px;text-align:center;color:#999">暂无地址</div>

      <div v-for="a in addresses" :key="a.id" style="padding:10px;border:1px solid #eee;margin-bottom:6px;font-size:13px">
        <!-- 编辑模式 -->
        <div v-if="editing===a.id">
          <input v-model="eName" placeholder="收货人" style="width:90px;height:30px;border:1px solid #ddd;padding:0 6px;font-size:12px;margin-right:6px;border-radius:3px" />
          <input v-model="ePhone" placeholder="手机号" style="width:130px;height:30px;border:1px solid #ddd;padding:0 6px;font-size:12px;margin-right:6px;border-radius:3px" />
          <input v-model="eAddr" placeholder="详细地址" style="width:250px;height:30px;border:1px solid #ddd;padding:0 6px;font-size:12px;margin-right:6px;border-radius:3px" />
          <button @click="doUpdate(a.id)" style="padding:5px 14px;background:var(--jd-red);color:#fff;border:none;border-radius:3px;cursor:pointer;font-size:12px;margin-right:4px">保存</button>
          <button @click="cancelEdit" style="padding:5px 14px;background:#fff;color:#666;border:1px solid #ddd;border-radius:3px;cursor:pointer;font-size:12px">取消</button>
        </div>
        <!-- 展示模式 -->
        <div v-else style="display:flex;align-items:center;gap:10px">
          <span v-if="a.isDefault" style="background:var(--jd-red);color:#fff;font-size:10px;padding:1px 4px;border-radius:2px;flex-shrink:0">默认</span>
          <div style="flex:1;min-width:0">{{ a.receiverName }} · {{ a.receiverPhone }} · {{ a.detail }}</div>
          <button @click="startEdit(a)" style="flex-shrink:0;padding:3px 10px;border:1px solid #ddd;background:#fff;cursor:pointer;font-size:12px;border-radius:3px">编辑</button>
          <button v-if="a.isDefault!=1" @click="doSetDef(a.id)" style="flex-shrink:0;padding:3px 10px;border:1px solid #ddd;background:#fff;cursor:pointer;font-size:12px;border-radius:3px">设为默认</button>
          <button @click="doDel(a.id)" style="flex-shrink:0;padding:3px 10px;border:1px solid #ddd;background:#fff;cursor:pointer;font-size:12px;border-radius:3px;color:var(--jd-red)">删除</button>
        </div>
      </div>

      <div style="border-top:1px solid #eee;margin-top:14px;padding-top:14px">
        <h4 style="margin-bottom:8px">新增地址</h4>
        <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center">
          <input v-model="nName" placeholder="收货人" style="width:90px;height:30px;border:1px solid #ddd;padding:0 6px;font-size:12px;border-radius:3px" />
          <input v-model="nPhone" placeholder="手机号" style="width:130px;height:30px;border:1px solid #ddd;padding:0 6px;font-size:12px;border-radius:3px" />
          <input v-model="nAddr" placeholder="详细地址" style="width:280px;height:30px;border:1px solid #ddd;padding:0 6px;font-size:12px;border-radius:3px" />
          <button @click="doAdd" style="padding:5px 16px;background:var(--jd-red);color:#fff;border:none;border-radius:3px;cursor:pointer;font-size:12px">添加</button>
        </div>
      </div>

      <div style="border-top:1px solid #eee;margin-top:20px;padding-top:16px">
        <h4 style="margin-bottom:8px">数据与账户</h4>
        <p style="color:#666;font-size:13px;line-height:1.7;margin-bottom:10px">你可以导出个人资料、地址和订单记录。导出内容不包含密码、Token 或商家内部备注。</p>
        <el-button :loading="exporting" @click="downloadExport">导出账户资料（JSON）</el-button>
        <el-button type="danger" plain @click="deleteVisible = true">注销账户</el-button>
      </div>
    </div>

    <div v-if="tab==='orders'">
      <div v-if="orders.length===0" style="text-align:center;padding:40px;color:#999">暂无订单</div>
      <div v-for="o in orders" :key="o.orderNo" style="background:#fff;padding:14px;margin-bottom:8px">
        <div style="display:flex;align-items:center;gap:12px">
          <div style="width:280px;flex-shrink:0"><el-tag :type="o.status===0?'danger':o.status===1?'success':''" size="small">{{ (['待支付','已支付','已发货','已完成','已取消','已超时'] as any)[o.status] }}</el-tag><span style="margin-left:8px;font-size:13px;color:#999">{{ o.orderNo }}</span></div>
          <span class="price" style="font-size:18px;width:100px;text-align:right;flex-shrink:0">¥{{ o.totalAmount }}</span>
          <span style="font-size:12px;color:#999;width:90px;text-align:center;flex-shrink:0">{{ String(o.createdAt).substring(0,10) }}</span>
          <el-button size="small" @click="router.push('/order/'+o.orderNo)">详情</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="deleteVisible" title="注销账户" width="min(480px, 92vw)" :close-on-click-modal="!deleting">
      <el-alert title="注销后将无法登录，地址和购物车会被清除；历史订单会为履约、售后及合规要求保留。进行中的订单或待处理退款会阻止注销。" type="warning" show-icon :closable="false" />
      <el-form label-position="top" style="margin-top:16px">
        <el-form-item label="当前密码" required><el-input v-model="deletePassword" type="password" show-password autocomplete="current-password" /></el-form-item>
        <el-form-item label="输入“注销账户”确认" required><el-input v-model="deletePhrase" autocomplete="off" /></el-form-item>
      </el-form>
      <template #footer><el-button :disabled="deleting" @click="deleteVisible = false">取消</el-button><el-button type="danger" :loading="deleting" @click="confirmDelete">确认注销</el-button></template>
    </el-dialog>
  </div>
</template>
