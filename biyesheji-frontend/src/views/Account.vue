<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAddressList, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '../api/user'
import { getOrderPage } from '../api/order'
import { ElMessage } from 'element-plus'

const router = useRouter()
const tab = ref('info')

// 地址
const addresses = ref<any[]>([])
const load = async () => {
  try {
    const r = await getAddressList();
    console.log('地址列表', r.data);
    addresses.value = r.data.data || [];
    if (addresses.value.length === 0) console.log('地址为空')
  } catch (e: any) { ElMessage.error('加载地址失败:' + (e?.response?.data?.message || '网络错误')) }
}
onMounted(() => {
console.log('加载地址');
  load()
})

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
  </div>
</template>
